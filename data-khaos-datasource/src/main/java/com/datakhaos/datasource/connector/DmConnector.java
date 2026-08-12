package com.datakhaos.datasource.connector;

import com.datakhaos.datasource.api.model.DsConfig;
import org.springframework.stereotype.Component;

/**
 * 达梦 DM8 连接器（国产化）。驱动 DmJdbcDriver18 由 prod profile 引入。
 */
@Component
public class DmConnector extends AbstractJdbcConnector {

    @Override
    public String getType() {
        return "DM8";
    }

    @Override
    public String getDriverClass() {
        return "dm.jdbc.driver.DmDriver";
    }

    @Override
    public String buildJdbcUrl(DsConfig config) {
        return "jdbc:dm://" + hostPort(config, 5236) + "/" + db(config);
    }
}
