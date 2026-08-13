package com.datakhaos.mart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 维度定义表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mart_dimension")
public class MartDimension extends BaseEntity {

    /** 维度名称 */
    private String dimName;

    /** 项目组ID（权限隔离） */
    private String projectGroupId;

    /** 维度编码 */
    private String dimCode;

    /** COMMON / TIME / ORG */
    private String dimType;

    /** 所属模型ID */
    private String modelId;

    /** 来源表 */
    private String sourceTable;

    /** 来源字段 */
    private String sourceColumn;

    /** 描述 */
    private String description;

    private Integer status;
}
