package com.datakhaos.datasource.connector;

import com.datakhaos.datasource.api.model.DsConfig;
import org.springframework.stereotype.Component;

/**
 * MySQL 连接器（也可作为 Doris 的协议基础）。
 */
@Component
public class MySqlConnector extends AbstractJdbcConnector {

    @Override
    public String getType() {
        return "MYSQL";
    }

    @Override
    public String getDriverClass() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    public String buildJdbcUrl(DsConfig config) {
        return "jdbc:mysql://" + hostPort(config, 3306) + "/" + db(config)
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
    }
}
