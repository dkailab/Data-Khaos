package com.datakhaos.mart.api.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模型市场卡片 DTO（仅已发布模型，含使用统计）
 */
@Data
public class MarketModelDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String modelName;

    private String modelCode;

    /** STAR / SNOWFLAKE */
    private String modelType = "STAR";

    /** 数据源ID */
    private String datasourceId;

    private String description;

    /** 数仓分层ID */
    private String layerId;

    /** 数仓分层编码 ODS/DWD/DWS/ADS */
    private String layerCode;

    /** 数仓分层名称 */
    private String layerName;

    /** 项目组ID（权限隔离） */
    private String projectGroupId;

    /** 版本 */
    private Integer version = 1;

    /** 指标数 */
    private Long metricCount = 0L;

    /** 维度数 */
    private Long dimensionCount = 0L;

    /** 关联数 */
    private Long relCount = 0L;

    private LocalDateTime publishTime;

    private LocalDateTime updateTime;
}