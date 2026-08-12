package com.datakhaos.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 网关配置，绑定 {@code data-khaos.jwt.*}（密钥需与 data-khaos-auth 保持一致）。
 *
 * <p>Bean 名显式指定为 {@code dataKhaosJwtProperties}，避免与 Spring Cloud Gateway
 * 自动配置的 {@code gatewayProperties}（org.springframework.cloud.gateway.config.GatewayProperties）
 * 冲突。Spring Boot 3 默认禁止 bean 覆盖。
 */
@Data
@Component("dataKhaosJwtProperties")
@ConfigurationProperties(prefix = "data-khaos.jwt")
public class GatewayProperties {

    /** JWT 签名密钥 */
    private String secret = "data-khaos-jwt-secret-key-2026-hs256-0123456789";
}
