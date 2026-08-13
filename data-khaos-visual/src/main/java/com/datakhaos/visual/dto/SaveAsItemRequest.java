package com.datakhaos.visual.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 将即席查询存为仪表板组件请求
 */
@Data
public class SaveAsItemRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标仪表板ID */
    private String dashboardId;

    /** 组件标题 */
    private String title;

    /** 图表类型（对应 VisualDashboardItem.chartType） */
    private String chartType;

    /** 数据源ID */
    private String datasourceId;

    /** 查询SQL */
    private String sql;

    /** 组件配置 JSON（xAxisColumn / seriesColumn / valueColumn 等） */
    private String config;
}
