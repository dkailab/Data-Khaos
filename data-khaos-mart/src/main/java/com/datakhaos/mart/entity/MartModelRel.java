package com.datakhaos.mart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型关联关系表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mart_model_rel")
public class MartModelRel extends BaseEntity {

    /** 模型ID */
    private String modelId;

    /** 事实表 */
    private String factTable;

    /** 维度表 */
    private String dimTable;

    /** 关联键 */
    private String joinKey;

    /** INNER / LEFT / RIGHT */
    private String joinType;
}
