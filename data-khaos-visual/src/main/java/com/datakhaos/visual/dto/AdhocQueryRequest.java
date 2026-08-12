package com.datakhaos.visual.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 即席分析查询请求
 */
@Data
public class AdhocQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据源ID */
    private String datasourceId;

    /** 查询SQL */
    private String sql;
}
