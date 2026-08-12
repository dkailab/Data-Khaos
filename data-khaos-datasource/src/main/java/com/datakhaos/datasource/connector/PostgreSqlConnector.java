package com.datakhaos.datasource.connector;

import com.datakhaos.datasource.api.model.DsConfig;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL 连接器。
 */
@Component
public class PostgreSqlConnector extends AbstractJdbcConnector {

    @Override
    public String getType() {
        return "POSTGRESQL";
    }

    @Override
    public String getDriverClass() {
        return "org.postgresql.Driver";
    }

    @Override
    public String buildJdbcUrl(DsConfig config) {
        return "jdbc:postgresql://" + hostPort(config, 5432) + "/" + db(config);
    }
}
