package com.datakhaos.metadata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 数据库信息表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("meta_database")
public class MetaDatabase extends BaseEntity {

    /** 数据源ID */
    private String datasourceId;

    /** 数据库名 */
    private String databaseName;

    /** 描述 */
    private String description;

    /** 最近同步时间 */
    private LocalDateTime syncTime;
}
