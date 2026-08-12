package com.datakhaos.datasource.connector;

import com.datakhaos.datasource.api.model.DsConfig;
import org.springframework.stereotype.Component;

/**
 * 星环 Transwarp 连接器：Inceptor 兼容 HiveServer2 协议，使用 Hive 驱动。
 * 如需高可用 / 认证配置，可在 properties 中补充 Kerberos 等扩展参数。
 */
@Component
public class TranswarpConnector extends AbstractJdbcConnector {

    @Override
    public String getType() {
        return "TRANSWARP";
    }

    @Override
    public String getDriverClass() {
        return "org.apache.hive.jdbc.HiveDriver";
    }

    @Override
    public String buildJdbcUrl(DsConfig config) {
        return "jdbc:hive2://" + hostPort(config, 10000) + "/" + db(config);
    }
}
