package com.datakhaos.datasource.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 数据源模块配置，绑定 {@code data-khaos.*} 前缀。
 * 生产环境务必通过环境变量更换 AES 密钥。
 */
@Data
@Component
@ConfigurationProperties(prefix = "data-khaos")
public class DatasourceProperties {

    /** 数据源密码 AES 加密密钥（16 字节），对应配置 data-khaos.aes-key */
    private String aesKey = "dk-aes-key-16byte";
}
