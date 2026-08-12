package com.datakhaos.datasource.api.model;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQL 查询结果
 */
@Data
public class QueryResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 列信息 */
    private List<ColumnInfo> columns = new ArrayList<>();

    /** 数据行 */
    private List<Map<String, Object>> rows = new ArrayList<>();

    /** 受影响行数 / 结果行数 */
    private Integer rowCount = 0;

    /** 耗时（毫秒） */
    private Long costMs = 0L;

    /** 是否写操作（DDL/DML 非查询） */
    private Boolean update = false;
}
