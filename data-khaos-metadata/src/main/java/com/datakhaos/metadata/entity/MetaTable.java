package com.datakhaos.metadata.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 表信息表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("meta_table")
public class MetaTable extends BaseEntity {

    /** 数据库记录ID（meta_database.id） */
    private String databaseId;

    /** 表名 */
    private String tableName;

    /** TABLE / VIEW */
    private String tableType;

    /** 描述 */
    private String description;

    /** 行数 */
    private Long rowCount;

    /** 大小（字节） */
    private Long tableSize;

    /** 最近同步时间 */
    private LocalDateTime syncTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
