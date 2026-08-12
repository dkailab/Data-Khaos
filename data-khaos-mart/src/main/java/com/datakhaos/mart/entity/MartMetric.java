package com.datakhaos.mart.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 指标定义表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mart_metric")
public class MartMetric extends BaseEntity {

    /** 指标名称 */
    private String metricName;

    /** 指标编码 */
    private String metricCode;

    /** ATOMIC / DERIVED */
    private String metricType;

    /** 计算表达式 */
    private String expression;

    /** 数据类型 */
    private String dataType;

    /** 单位 */
    private String unit;

    /** 指标分类ID */
    private String categoryId;

    /** 所属模型ID */
    private String modelId;

    /** 描述 */
    private String description;

    private Integer status;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
