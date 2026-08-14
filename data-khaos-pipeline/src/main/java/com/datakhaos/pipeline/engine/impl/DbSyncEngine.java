package com.datakhaos.pipeline.engine.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.util.EncryptUtil;
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
 * <p>当前支持 MySQL → MySQL 同步；其余类型返回明确提示（可扩展）。
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
        return cn.hutool.json.JSONUtil.toJsonStr(Map.of(
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
        Map<String, Object> source = getDs(task.getSourceDsId());
        Map<String, Object> target = getDs(task.getTargetDsId());
        checkMysql(source, target);

        String sourceUrl = url(source);
        String targetUrl = url(target);
        String sourceUser = (String) source.get("username");
        String sourcePwd = decrypt((String) source.get("password"));
        String targetUser = (String) target.get("username");
        String targetPwd = decrypt((String) target.get("password"));

        // 1. 读源
        String selectSql = StrUtil.isNotBlank(task.getSourceQuery())
                ? task.getSourceQuery()
                : "SELECT * FROM `" + task.getSourceTable() + "`";
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(sourceUrl, sourceUser, sourcePwd);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(selectSql)) {
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                columns.add(md.getColumnLabel(i));
            }
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String col : columns) {
                    row.put(col, rs.getObject(col));
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

    private void checkMysql(Map<String, Object> source, Map<String, Object> target) {
        String st = String.valueOf(source.get("ds_type"));
        String tt = String.valueOf(target.get("ds_type"));
        if (!"MYSQL".equalsIgnoreCase(st) || !"MYSQL".equalsIgnoreCase(tt)) {
            throw new BusinessException("DB-Sync 引擎当前仅支持 MySQL 源/目标（" + st + "→" + tt + "），请使用 DataX/SeaTunnel 或其他引擎");
        }
    }

    private String url(Map<String, Object> ds) {
        String host = String.valueOf(ds.get("host"));
        int port = ((Number) ds.get("port")).intValue();
        String db = String.valueOf(ds.get("database_name"));
        return "jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    }

    private String decrypt(String pwd) {
        return StrUtil.isBlank(pwd) ? "" : EncryptUtil.decrypt(pwd, aesKey);
    }

    private Map<String, Object> getDs(String dsId) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT ds_type, host, port, database_name, username, password FROM meta_datasource WHERE id = ?", dsId);
        if (list.isEmpty()) {
            throw new BusinessException("数据源不存在: " + dsId);
        }
        return list.get(0);
    }
}