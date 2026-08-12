package com.datakhaos.datasource.connector;

import com.datakhaos.datasource.api.model.DsConfig;
import org.springframework.stereotype.Component;

/**
 * Oracle 连接器（SID 方式，thin 协议）。
 */
@Component
public class OracleConnector extends AbstractJdbcConnector {

    @Override
    public String getType() {
        return "ORACLE";
    }

    @Override
    public String getDriverClass() {
        return "oracle.jdbc.OracleDriver";
    }

    @Override
    public String buildJdbcUrl(DsConfig config) {
        int port = config.getPort() != null ? config.getPort() : 1521;
        return "jdbc:oracle:thin:@" + config.getHost() + ":" + port + ":" + db(config);
    }
}
