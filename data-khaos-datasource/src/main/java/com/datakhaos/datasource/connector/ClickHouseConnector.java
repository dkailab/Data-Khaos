package com.datakhaos.datasource.connector;

import com.datakhaos.datasource.api.model.DsConfig;
import org.springframework.stereotype.Component;

/**
 * ClickHouse 连接器。
 */
@Component
public class ClickHouseConnector extends AbstractJdbcConnector {

    @Override
    public String getType() {
        return "CLICKHOUSE";
    }

    @Override
    public String getDriverClass() {
        return "com.clickhouse.jdbc.ClickHouseDriver";
    }

    @Override
    public String buildJdbcUrl(DsConfig config) {
        return "jdbc:clickhouse://" + hostPort(config, 8123) + "/" + db(config);
    }
}
