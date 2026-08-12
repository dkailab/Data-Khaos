package com.datakhaos.datasource.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 数据源配置表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("meta_datasource")
public class MetaDatasource extends BaseEntity {

    private String dsName;

    /** MYSQL / DM8 / HIVE / DORIS / CLICKHOUSE / POSTGRESQL / ORACLE / TRANSWARP */
    private String dsType;

    private String host;

    private Integer port;

    private String databaseName;

    private String username;

    /** 密码（AES 加密存储），仅允许写入 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 扩展属性（JSON） */
    private String properties;

    private Integer status;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
