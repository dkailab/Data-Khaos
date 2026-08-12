package com.datakhaos.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * 限流配置：基于 Redis 令牌桶，按用户维度（优先用户ID，兜底客户端IP）限流。
 * 依赖配置 spring.cloud.gateway.routes[].filters 中的 RequestRateLimiter。
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just(Optional.ofNullable(
                        exchange.getRequest().getHeaders().getFirst("X-User-Id"))
                .orElseGet(() -> exchange.getRequest().getRemoteAddress() == null
                        ? "anonymous"
                        : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()));
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // 每秒补充 20 个令牌，突发容量 50
        return new RedisRateLimiter(20, 50);
    }
}
