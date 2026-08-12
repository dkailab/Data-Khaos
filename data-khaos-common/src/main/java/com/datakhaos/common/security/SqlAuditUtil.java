package com.datakhaos.common.security;

import com.datakhaos.common.exception.BusinessException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 审核工具：在执行前做基础安全检查（所有模块共用同一份实现）。
 * <ul>
 *     <li>拒绝多条语句（堆叠注入）；</li>
 *     <li>拒绝 DDL / 危险操作（CREATE / ALTER / DROP / TRUNCATE / GRANT 等）；</li>
 *     <li>仅允许查询与数据变更语句（SELECT / WITH / SHOW / DESC / EXPLAIN / INSERT / UPDATE / DELETE）。</li>
 * </ul>
 */
public final class SqlAuditUtil {

    /** 被禁止的危险操作关键字 */
    private static final Pattern DANGEROUS = Pattern.compile(
            "\\b(CREATE|ALTER|DROP|TRUNCATE|GRANT|REVOKE|RENAME|CALL|EXEC|EXECUTE|MERGE|SHUTDOWN|ATTACH)\\b",
            Pattern.CASE_INSENSITIVE);

    /** 允许的语句起始关键字 */
    private static final Pattern ALLOWED_LEAD = Pattern.compile(
            "^\\s*(SELECT|WITH|SHOW|DESC|DESCRIBE|EXPLAIN|INSERT|UPDATE|DELETE|REPLACE)\\b",
            Pattern.CASE_INSENSITIVE);

    private SqlAuditUtil() {
    }

    /**
     * 校验并清洗 SQL：去除首尾空白与末尾分号；不通过则抛出 {@link BusinessException}。
     *
     * @return 清洗后的单条 SQL
     */
    public static String audit(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new BusinessException("SQL 不能为空");
        }
        String cleaned = sql.trim();
        if (cleaned.endsWith(";")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }

        String plain = stripQuoted(cleaned);
        if (plain.contains(";")) {
            throw new BusinessException("禁止执行多条 SQL 语句");
        }

        Matcher danger = DANGEROUS.matcher(plain);
        if (danger.find()) {
            throw new BusinessException("SQL 包含被禁止的操作: " + danger.group().toUpperCase());
        }

        if (!ALLOWED_LEAD.matcher(plain).find()) {
            throw new BusinessException("仅允许执行查询或数据变更语句");
        }
        return cleaned;
    }

    /** 移除字符串字面量与注释内容，用于检测语句分隔符与关键字 */
    private static String stripQuoted(String sql) {
        StringBuilder sb = new StringBuilder(sql.length());
        boolean inSingle = false;
        boolean inDouble = false;
        boolean inLine = false;
        boolean inBlock = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (inLine) {
                if (c == '\n') {
                    inLine = false;
                }
                continue;
            }
            if (inBlock) {
                if (c == '*' && i + 1 < sql.length() && sql.charAt(i + 1) == '/') {
                    inBlock = false;
                    i++;
                }
                continue;
            }
            if (inSingle) {
                if (c == '\'') {
                    inSingle = false;
                }
                continue;
            }
            if (inDouble) {
                if (c == '"') {
                    inDouble = false;
                }
                continue;
            }
            if (c == '\'') {
                inSingle = true;
                continue;
            }
            if (c == '"') {
                inDouble = true;
                continue;
            }
            if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                inLine = true;
                i++;
                continue;
            }
            if (c == '/' && i + 1 < sql.length() && sql.charAt(i + 1) == '*') {
                inBlock = true;
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
