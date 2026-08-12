package com.datakhaos.datasource.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * SQL 执行请求
 */
@Data
public class ExecuteSqlRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待执行 SQL（已通过服务端 SQL 审核） */
    private String sql;

    /** 参数（预留，支持 :name 占位符替换） */
    private Map<String, Object> params;
}
