package com.datakhaos.datasource.api.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 字段（列）信息
 */
@Data
public class ColumnInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String columnName;

    private String columnType;

    private Integer columnLength;

    private Integer columnScale;

    private Boolean nullable = true;

    private Boolean primaryKey = false;

    private String defaultValue;

    private String description;

    private Integer sortOrder;

    /** 敏感级别 0:普通 1:敏感 2:高度敏感 */
    private Integer sensitiveLevel = 0;
}
