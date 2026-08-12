package com.datakhaos.datasource.api.connector;

import com.datakhaos.datasource.api.model.ColumnInfo;
import com.datakhaos.datasource.api.model.DsConfig;
import com.datakhaos.datasource.api.model.QueryResult;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 数据源连接器 SPI。
 *
 * <p>每种数据源实现该接口，通过 Spring 工厂注入到 {@code DataSourceConnectorFactory}，
 * 实现「可插拔数据源」——新增数据源只需实现本接口并注册为 Spring Bean。</p>
 */
public interface DataSourceConnector {

    /** 数据源类型标识，与 DsType.name() 对应 */
    String getType();

    /**
     * 连接驱动 class 名
     */
    String getDriverClass();

    /**
     * 构造 JDBC URL
     */
    String buildJdbcUrl(DsConfig config);

    /** 测试连接 */
    boolean testConnection(DsConfig config);

    /** 获取数据库列表 */
    List<String> getDatabases(DsConfig config);

    /** 获取指定库下的表列表 */
    List<String> getTables(DsConfig config, String database);

    /** 获取表字段信息 */
    List<ColumnInfo> getColumns(DsConfig config, String database, String table);

    /**
     * 执行查询。
     *
     * @param config 数据源配置
     * @param sql    SQL 语句（已通过 SQL 审核）
     * @param params 参数（预留）
     */
    QueryResult executeQuery(DsConfig config, String sql, Map<String, Object> params) throws SQLException;

    /** 获取表数据量 */
    long getTableCount(DsConfig config, String database, String table);
}
