package com.datakhaos.pipeline.engine.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.util.EncryptUtil;
import com.datakhaos.pipeline.engine.EngineUtils;
import com.datakhaos.pipeline.engine.PipelineEngine;
import com.datakhaos.pipeline.entity.PipelineInstance;
import com.datakhaos.pipeline.entity.PipelineTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DB-Sync 兜底引擎：通过 JDBC 直连源/目标数据源完成同步，
 * 不依赖外部引擎二进制，保证最小闭环可用。
 *
 * <p>支持 JDBC 直连类型 MYSQL / HIVE（HiveServer2），可用于 Hive→MySQL、MySQL→MySQL 等组合；
 * 其余类型返回明确提示（可扩展）。
 */
@Slf4j
@Component
public class DbSyncEngine implements PipelineEngine {

    private final JdbcTemplate jdbcTemplate;
    private final String aesKey;

    public DbSyncEngine(JdbcTemplate jdbcTemplate,
                        @Value("${data-khaos.aes-key:dk-aes-key-16byte}") String aesKey) {
        this.jdbcTemplate = jdbcTemplate;
        this.aesKey = aesKey;
    }

    @Override
    public String type() {
        return "DB_SYNC";
    }

    @Override
    public String name() {
        return "DB-Sync（内置直连同步）";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String buildRunConfig(PipelineTask task) {
        return JSONUtil.toJsonStr(Map.of(
                "source", task.getSourceTable(),
                "target", task.getTargetTable(),
                "mode", "JDBC 直连同步"));
    }

    @Override
    public int execute(PipelineTask task, PipelineInstance instance) throws Exception {
        if (StrUtil.isBlank(task.getSourceDsId()) || StrUtil.isBlank(task.getSourceTable())
                || StrUtil.isBlank(task.getTargetDsId()) || StrUtil.isBlank(task.getTargetTable())) {
            throw new BusinessException("DB-Sync 引擎需配置源/目标数据源与表");
        }
        Map<String, Object> source = EngineUtils.getDs(jdbcTemplate, task.getSourceDsId());
        Map<String, Object> target = EngineUtils.getDs(jdbcTemplate, task.getTargetDsId());
        EngineUtils.checkJdbc(source, target);

        String sourceUrl = EngineUtils.jdbcUrl(source);
        String targetUrl = EngineUtils.jdbcUrl(target);
        String sourceUser = (String) source.get("username");
        String sourcePwd = EngineUtils.decryptPwd((String) source.get("password"), aesKey);
        String targetUser = (String) target.get("username");
        String targetPwd = EngineUtils.decryptPwd((String) target.get("password"), aesKey);

        // 1. 读源（Hive 不支持反引号包裹 库.表，MySQL 需要反引号）
        String sourceType = String.valueOf(source.get("ds_type")).toUpperCase();
        String selectSql;
        if (StrUtil.isNotBlank(task.getSourceQuery())) {
            selectSql = task.getSourceQuery();
        } else if ("HIVE".equals(sourceType)) {
            selectSql = "SELECT * FROM " + task.getSourceTable();
        } else {
            selectSql = "SELECT * FROM `" + task.getSourceTable() + "`";
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(sourceUrl, sourceUser, sourcePwd);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(selectSql)) {
            ResultSetMetaData md = rs.getMetaData();
            List<String> rawColumns = new ArrayList<>();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                String label = md.getColumnLabel(i);
                rawColumns.add(label);
                // Hive 结果集列标签形如 表名.列名（如 biz_user.id），需去掉前缀作为目标列名
                columns.add(normalizeColumn(label));
            }
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int j = 0; j < columns.size(); j++) {
                    row.put(columns.get(j), rs.getObject(rawColumns.get(j)));
                }
                rows.add(row);
            }
        }

        // 2. 字段映射（{"源列":"目标列"}，缺省同名字段）
        Map<String, String> mapping = resolveMapping(task.getFieldMapping(), columns);

        // 3. 写目标
        if (rows.isEmpty()) {
            log.info("源表 {} 无数据，跳过写入", task.getSourceTable());
            return 0;
        }
        List<String> targetCols = new ArrayList<>(mapping.values());
        String insertSql = "INSERT INTO `" + task.getTargetTable() + "` (`"
                + String.join("`,`", targetCols) + "`) VALUES ("
                + String.join(",", java.util.Collections.nCopies(targetCols.size(), "?")) + ")";
        try (Connection conn = DriverManager.getConnection(targetUrl, targetUser, targetPwd);
             PreparedStatement ps = conn.prepareStatement(insertSql)) {
            conn.setAutoCommit(false);
            int count = 0;
            for (Map<String, Object> row : rows) {
                int idx = 1;
                for (String srcCol : mapping.keySet()) {
                    ps.setObject(idx++, row.get(srcCol));
                }
                ps.addBatch();
                count++;
                if (count % 500 == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            conn.commit();
            return count;
        }
    }

    /** 源表新列名 → 目标列名 */
    private Map<String, String> resolveMapping(String fieldMapping, List<String> columns) {
        Map<String, String> mapping = new LinkedHashMap<>();
        if (StrUtil.isNotBlank(fieldMapping)) {
            JSONObject obj = JSONUtil.parseObj(fieldMapping);
            for (String col : columns) {
                mapping.put(col, obj.containsKey(col) ? obj.getStr(col) : col);
            }
        } else {
            for (String col : columns) {
                mapping.put(col, col);
            }
        }
        return mapping;
    }

    /** 规范化列名：去掉 Hive 结果集列标签中的 表名. 前缀（如 biz_user.id → id） */
    private String normalizeColumn(String label) {
        if (label == null) {
            return label;
        }
        int dot = label.lastIndexOf('.');
        return dot > 0 ? label.substring(dot + 1) : label;
    }
}