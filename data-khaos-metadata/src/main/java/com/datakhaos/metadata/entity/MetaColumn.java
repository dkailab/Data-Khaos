package com.datakhaos.metadata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字段信息表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("meta_column")
public class MetaColumn extends BaseEntity {

    /** 表记录ID（meta_table.id） */
    private String tableId;

    /** 字段名 */
    private String columnName;

    /** 字段类型 */
    private String columnType;

    /** 长度 */
    private Integer columnLength;

    /** 精度 */
    private Integer columnScale;

    /** 是否可空 1:是 0:否 */
    private Integer isNullable;

    /** 是否主键 1:是 0:否 */
    private Integer isPrimaryKey;

    /** 默认值 */
    private String defaultValue;

    /** 描述/注释 */
    private String description;

    /** 排序 */
    private Integer sortOrder;

    /** 敏感级别 0:普通 1:敏感 2:高度敏感 */
    private Integer sensitiveLevel;
}
