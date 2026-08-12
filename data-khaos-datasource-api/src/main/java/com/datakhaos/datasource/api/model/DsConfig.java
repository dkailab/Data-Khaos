package com.datakhaos.datasource.api.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据源连接配置
 */
@Data
public class DsConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String dsName;

    private String dsType;

    private String host;

    private Integer port;

    private String databaseName;

    private String username;

    /** 明文密码（用于内部连接；落库前加密） */
    private String password;

    /** 扩展属性（JSON） */
    private String properties;
}
