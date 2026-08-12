package com.datakhaos.metadata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表血缘关系表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("meta_table_lineage")
public class MetaTableLineage extends BaseEntity {

    /** 源表记录ID */
    private String sourceTableId;

    /** 目标表记录ID */
    private String targetTableId;

    /** 源字段 */
    private String sourceColumn;

    /** 目标字段 */
    private String targetColumn;

    /** 关系类型 ETL / MANUAL */
    private String relationType;
}
