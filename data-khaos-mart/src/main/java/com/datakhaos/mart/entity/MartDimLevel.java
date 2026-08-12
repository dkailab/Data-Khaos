package com.datakhaos.mart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 维度层级表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mart_dim_level")
public class MartDimLevel extends BaseEntity {

    /** 维度ID */
    private String dimId;

    /** 层级名称 */
    private String levelName;

    /** 层级字段 */
    private String levelColumn;

    /** 层级顺序 */
    private Integer levelOrder;
}
