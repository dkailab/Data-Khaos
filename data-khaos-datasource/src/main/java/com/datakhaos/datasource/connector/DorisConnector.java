package com.datakhaos.datasource.connector;

import com.datakhaos.datasource.api.model.DsConfig;
import org.springframework.stereotype.Component;

/**
 * Doris 连接器：兼容 MySQL 协议，使用 MySQL 驱动，默认 FE 端口 9030。
 */
@Component
public class DorisConnector extends AbstractJdbcConnector {

    @Override
    public String getType() {
        return "DORIS";
    }

    @Override
    public String getDriverClass() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    public String buildJdbcUrl(DsConfig config) {
        return "jdbc:mysql://" + hostPort(config, 9030) + "/" + db(config)
                + "?useUnicode=true&characterEncoding=utf8&useSSL=false";
    }
}
