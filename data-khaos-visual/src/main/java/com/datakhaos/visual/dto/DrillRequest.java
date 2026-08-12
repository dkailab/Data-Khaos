package com.datakhaos.visual.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 组件下钻查询请求
 */
@Data
public class DrillRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 下钻维度列（点击图表对应的列，如 渠道 / 品类） */
    private String column;

    /** 下钻维度值（点击的数据点） */
    private String value;

    /** 分析板独立筛选 JSON（可选，下钻时叠加） */
    private String filters;
}