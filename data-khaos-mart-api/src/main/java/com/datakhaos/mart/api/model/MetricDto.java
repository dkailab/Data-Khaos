package com.datakhaos.mart.api.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 指标定义
 */
@Data
public class MetricDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String metricName;

    private String metricCode;

    /** ATOMIC / DERIVED */
    private String metricType = "ATOMIC";

    /** 计算表达式 */
    private String expression;

    private String dataType = "BIGINT";

    private String unit;

    private String categoryId;

    private String modelId;

    private String description;
}
