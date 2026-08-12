package com.datakhaos.datasource.api.model;

import lombok.Getter;

/**
 * 数据源类型（可插拔扩展）
 */
@Getter
public enum DsType {

    MYSQL("MySQL", "mysql"),
    DM8("达梦DM8", "dm"),
    POSTGRESQL("PostgreSQL", "postgresql"),
    HIVE("Hive", "hive"),
    DORIS("Doris", "mysql"),
    CLICKHOUSE("ClickHouse", "clickhouse"),
    TRANSWARP("星环Transwarp", "hive"),
    ORACLE("Oracle", "oracle");

    /** 展示名称 */
    private final String label;

    /** 默认 JDBC 协议 */
    private final String defaultProtocol;

    DsType(String label, String defaultProtocol) {
        this.label = label;
        this.defaultProtocol = defaultProtocol;
    }

    public static DsType of(String type) {
        for (DsType t : values()) {
            if (t.name().equalsIgnoreCase(type)) {
                return t;
            }
        }
        return MYSQL;
    }
}
