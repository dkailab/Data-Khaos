package com.datakhaos.mart.api.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 维度定义
 */
@Data
public class DimensionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String dimName;

    private String dimCode;

    /** COMMON / TIME / ORG */
    private String dimType = "COMMON";

    private String modelId;

    private String sourceTable;

    private String sourceColumn;

    private String description;
}
