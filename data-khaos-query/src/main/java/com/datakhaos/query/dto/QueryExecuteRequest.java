package com.datakhaos.query.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * SQL 执行请求
 */
@Data
public class QueryExecuteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据源ID */
    private String datasourceId;

    /** 数据库 */
    private String databaseName;

    /** 待执行 SQL */
    private String sql;
}
