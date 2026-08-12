package com.datakhaos.datasource.service;

import com.datakhaos.common.security.SqlAuditUtil;

/**
 * SQL 审核（委托 common 的统一实现），保留独立类便于后续扩展数据源侧特有规则。
 */
public final class SqlAuditor {

    private SqlAuditor() {
    }

    public static String audit(String sql) {
        return SqlAuditUtil.audit(sql);
    }
}
