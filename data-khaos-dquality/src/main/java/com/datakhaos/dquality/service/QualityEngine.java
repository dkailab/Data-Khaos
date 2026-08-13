package com.datakhaos.dquality.service;

import cn.hutool.core.util.StrUtil;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.model.R;
import com.datakhaos.common.security.MetadataHolder;
import com.datakhaos.common.security.SqlAuditUtil;
import com.datakhaos.dquality.entity.DqRule;
import com.datakhaos.dquality.entity.DqRuleResult;
import com.datakhaos.datasource.api.connector.DatasourceApiClient;
import com.datakhaos.datasource.api.model.QueryResult;
import com.datakhaos.permission.api.service.PermissionApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 质量稽核引擎：根据规则模板生成并执行稽核 SQL，计算结果。
 *
 * 支持 5 种模板：
 * 1. NOT_NULL - 统计某字段空值率 → 空值率 > 阈值则失败
 * 2. UNIQUE - 统计重复组数量 → 存在重复则失败
 * 3. VALUE_RANGE - 统计越界行数 → 越界率 > 阈值则失败
 * 4. CUSTOM_SQL - 用户提供返回"违规行"的 SQL → 行数 > 0 则失败
 * 5. CUSTOM_PROBE - 用户提供统计 SQL，取第一行第一列 → 值 > 阈值则失败
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QualityEngine {

    private final DatasourceApiClient datasourceApiClient;
    private final PermissionApiClient permissionApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 规则模板常量 */
    public static final String TYPE_NOT_NULL = "NOT_NULL";
    public static final String TYPE_UNIQUE = "UNIQUE";
    public static final String TYPE_VALUE_RANGE = "VALUE_RANGE";
    public static final String TYPE_CUSTOM_SQL = "CUSTOM_SQL";
    public static final String TYPE_CUSTOM_PROBE = "CUSTOM_PROBE";

    /**
     * 稽核执行：对一个任务中的所有规则逐个执行，收集结果。
     */
    public EngineResult execute(List<DqRule> rules, String userId, String triggerType) {
        long start = System.currentTimeMillis();
        int total = rules.size();
        int pass = 0;
        int fail = 0;
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal passWeight = BigDecimal.ZERO;
        List<DqRuleResult> results = new ArrayList<>();

        for (DqRule rule : rules) {
            DqRuleResult result = executeRule(rule, userId);
            results.add(result);
            totalWeight = totalWeight.add(BigDecimal.valueOf(rule.getWeight()));
            if (result.getPassed() == 1) {
                pass++;
                passWeight = passWeight.add(BigDecimal.valueOf(rule.getWeight()));
            } else {
                fail++;
            }
        }

        // 计算总评分：score = (passWeight / totalWeight) * 100
        BigDecimal score = BigDecimal.ZERO;
        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            score = passWeight.multiply(BigDecimal.valueOf(100))
                    .divide(totalWeight, 2, RoundingMode.HALF_UP);
        }

        EngineResult er = new EngineResult();
        er.setTotalRules(total);
        er.setPassRules(pass);
        er.setFailRules(fail);
        er.setScore(score);
        er.setCostMs(System.currentTimeMillis() - start);
        er.setResults(results);
        return er;
    }

    /**
     * 执行单个规则。
     */
    private DqRuleResult executeRule(DqRule rule, String userId) {
        DqRuleResult result = new DqRuleResult();
        result.setRuleId(rule.getId());

        String ruleType = rule.getRuleType();
        String dsId = rule.getDatasourceId();
        String db = rule.getDatabaseName();
        String table = rule.getTableName();
        String col = rule.getColumnName();
        BigDecimal threshold = rule.getAlertThreshold();

        // 校验目标表权限（当前用户必须有 SELECT 权限）
        if (!MetadataHolder.isSuperAdmin()) {
            boolean hasPerm = permissionApiClient.checkTablePermission(
                    userId, dsId, db, table, "SELECT");
            if (!hasPerm) {
                result.setPassed(0);
                result.setMessage("无目标表 SELECT 权限，无法稽核");
                return result;
            }
        }

        try {
            String sql = buildSql(rule);
            log.info("执行质量规则 [{}] {}: {}", rule.getId(), rule.getRuleName(), sql);

            R<QueryResult> resp = datasourceApiClient.executeRaw(dsId, sql);
            if (!resp.isSuccess() || resp.getData() == null) {
                result.setPassed(0);
                result.setMessage("执行稽核 SQL 失败: " + (resp.getMsg() != null ? resp.getMsg() : "空结果"));
                return result;
            }

            QueryResult qr = resp.getData();
            computeResult(rule, qr, result);

        } catch (BusinessException e) {
            result.setPassed(0);
            result.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("规则执行异常: {}", rule.getId(), e);
            result.setPassed(0);
            result.setMessage("执行异常: " + e.getMessage());
        }

        result.setThreshold(threshold);
        return result;
    }

    /**
     * 根据规则类型生成稽核 SQL。
     */
    private String buildSql(DqRule rule) {
        String type = rule.getRuleType();
        String db = rule.getDatabaseName();
        String table = rule.getTableName();
        String col = rule.getColumnName();
        String config = rule.getRuleConfig();

        return switch (type) {
            case TYPE_NOT_NULL -> buildNotNullSql(db, table, col);
            case TYPE_UNIQUE -> buildUniqueSql(db, table, col);
            case TYPE_VALUE_RANGE -> buildValueRangeSql(db, table, col, parseConfig(config));
            case TYPE_CUSTOM_SQL, TYPE_CUSTOM_PROBE -> {
                Map<String, Object> cfg = parseConfig(config);
                String customSql = (String) cfg.get("customSql");
                if (StrUtil.isBlank(customSql)) {
                    throw new BusinessException("自定义 SQL 不能为空");
                }
                yield SqlAuditUtil.audit(customSql);
            }
            default -> throw new BusinessException("不支持的规则类型: " + type);
        };
    }

    private String buildNotNullSql(String db, String table, String col) {
        return String.format("SELECT COUNT(*) AS total, COUNT(`%s`) AS not_null FROM `%s`.`%s`",
                col, db, table);
    }

    private String buildUniqueSql(String db, String table, String col) {
        return String.format("SELECT COUNT(*) AS duplicate_count " +
                        "FROM (SELECT `%s` FROM `%s`.`%s` GROUP BY `%s` HAVING COUNT(*) > 1) t",
                col, db, table, col);
    }

    private String buildValueRangeSql(String db, String table, String col, Map<String, Object> config) {
        BigDecimal min = getDecimal(config, "min");
        BigDecimal max = getDecimal(config, "max");
        StringBuilder where = new StringBuilder();
        boolean hasMin = min != null;
        boolean hasMax = max != null;
        if (hasMin) {
            where.append("`").append(col).append("` < ").append(min);
        }
        if (hasMax) {
            if (hasMin) {
                where.append(" OR ");
            }
            where.append("`").append(col).append("` > ").append(max);
        }
        return String.format("SELECT COUNT(*) AS total, COUNT(CASE WHEN %s THEN 1 END) AS bad FROM `%s`.`%s`",
                where, db, table);
    }

    /**
     * 计算规则结果。
     */
    private void computeResult(DqRule rule, QueryResult qr, DqRuleResult result) {
        String type = rule.getRuleType();
        BigDecimal threshold = rule.getAlertThreshold();
        List<Map<String, Object>> rows = qr.getRows();

        if (rows == null || rows.isEmpty()) {
            result.setPassed(1);
            result.setActualValue(BigDecimal.ZERO);
            result.setMessage("无数据，默认通过");
            return;
        }

        Map<String, Object> first = rows.get(0);

        switch (type) {
            case TYPE_NOT_NULL -> {
                long total = getLong(first, "total");
                long notNull = getLong(first, "not_null");
                BigDecimal nullRate = total == 0 ? BigDecimal.ZERO :
                        BigDecimal.valueOf(total - notNull)
                                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
                result.setActualValue(nullRate);
                boolean passed = nullRate.compareTo(threshold) <= 0;
                result.setPassed(passed ? 1 : 0);
                result.setMessage(String.format("总行数 %d, 空值率 %.2f%%",
                        total, nullRate.multiply(BigDecimal.valueOf(100))));
                if (!passed) {
                    // 获取前 10 条空值样本
                    String sampleSql = String.format(
                            "SELECT * FROM `%s`.`%s` WHERE `%s` IS NULL LIMIT 10",
                            rule.getDatabaseName(), rule.getTableName(), rule.getColumnName());
                    fetchSample(rule.getDatasourceId(), sampleSql, result);
                }
            }
            case TYPE_UNIQUE -> {
                long dup = getLong(first, "duplicate_count");
                result.setActualValue(BigDecimal.valueOf(dup));
                // 阈值：允许重复组数 ≤ threshold
                boolean passed = dup <= threshold.longValue();
                result.setPassed(passed ? 1 : 0);
                result.setMessage(String.format("重复组数 %d", dup));
            }
            case TYPE_VALUE_RANGE -> {
                long total = getLong(first, "total");
                long bad = getLong(first, "bad");
                BigDecimal badRate = total == 0 ? BigDecimal.ZERO :
                        BigDecimal.valueOf(bad).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
                result.setActualValue(badRate);
                boolean passed = badRate.compareTo(threshold) <= 0;
                result.setPassed(passed ? 1 : 0);
                result.setMessage(String.format("总行数 %d, 越界行数 %d, 越界率 %.2f%%",
                        total, bad, badRate.multiply(BigDecimal.valueOf(100))));
            }
            case TYPE_CUSTOM_SQL -> {
                int badRows = qr.getRowCount() != null ? qr.getRowCount() : 0;
                result.setActualValue(BigDecimal.valueOf(badRows));
                // 阈值：违规行数 > 阈值 → 失败
                boolean passed = badRows <= threshold.intValue();
                result.setPassed(passed ? 1 : 0);
                result.setMessage(String.format("违规行数 %d", badRows));
                if (!passed && !rows.isEmpty()) {
                    // 序列化样本行
                    try {
                        result.setSampleRows(objectMapper.writeValueAsString(rows.size() > 10 ? rows.subList(0, 10) : rows));
                    } catch (IOException e) {
                        log.warn("序列化样本失败: {}", e.getMessage());
                    }
                }
            }
            case TYPE_CUSTOM_PROBE -> {
                Object firstVal = first.values().iterator().next();
                BigDecimal actual = toBigDecimal(firstVal);
                result.setActualValue(actual);
                // 实际值 > 阈值 → 失败
                boolean passed = actual.compareTo(threshold) <= 0;
                result.setPassed(passed ? 1 : 0);
                result.setMessage(String.format("探查结果 %.4f", actual));
            }
        }
    }

    private void fetchSample(String dsId, String sql, DqRuleResult result) {
        try {
            R<QueryResult> resp = datasourceApiClient.executeRaw(dsId, sql);
            if (resp.isSuccess() && resp.getData() != null && resp.getData().getRows() != null) {
                List<Map<String, Object>> samples = resp.getData().getRows();
                if (!samples.isEmpty()) {
                    result.setSampleRows(objectMapper.writeValueAsString(
                            samples.size() > 10 ? samples.subList(0, 10) : samples));
                }
            }
        } catch (Exception e) {
            log.warn("获取样本失败: {}", e.getMessage());
        }
    }

    private Map<String, Object> parseConfig(String json) {
        if (StrUtil.isBlank(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new BusinessException("解析规则配置 JSON 失败: " + e.getMessage());
        }
    }

    private BigDecimal getDecimal(Map<String, Object> cfg, String key) {
        if (!cfg.containsKey(key)) {
            return null;
        }
        Object v = cfg.get(key);
        if (v == null) {
            return null;
        }
        return toBigDecimal(v);
    }

    private long getLong(Map<String, Object> row, String col) {
        Object v = row.get(col);
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(v.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(v.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 引擎执行结果。
     */
    @lombok.Data
    public static class EngineResult {
        private int totalRules;
        private int passRules;
        private int failRules;
        private BigDecimal score;
        private long costMs;
        private List<DqRuleResult> results;
    }
}