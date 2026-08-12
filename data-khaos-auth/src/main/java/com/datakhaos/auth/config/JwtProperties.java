package com.datakhaos.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "data-khaos.jwt")
public class JwtProperties {

    /** 签名密钥 */
    private String secret = "data-khaos-jwt-secret-key-2026-hs256-0123456789";

    /** 有效期（秒） */
    private long expireSeconds = 7200;
}
