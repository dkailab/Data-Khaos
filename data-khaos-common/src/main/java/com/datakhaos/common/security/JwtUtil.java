package com.datakhaos.common.security;

import cn.hutool.core.date.DateUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具（HMAC-SHA256，基于 Hutool）。
 * 令牌结构：{@code header.payload.signature}，payload 含 uid / username / exp。
 */
@Slf4j
public final class JwtUtil {

    private static final String PAYLOAD_UID = "uid";
    private static final String PAYLOAD_USERNAME = "username";

    private JwtUtil() {
    }

    /**
     * 生成 JWT
     *
     * @param userId        用户ID
     * @param username      用户名
     * @param secret        签名密钥
     * @param expireSeconds 有效期（秒）
     */
    public static String createToken(String userId, String username, String secret, long expireSeconds) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expireSeconds * 1000L);
        return JWT.create()
                .setPayload(PAYLOAD_UID, userId)
                .setPayload(PAYLOAD_USERNAME, username)
                .setIssuedAt(now)
                .setExpiresAt(expireAt)
                .setKey(secret.getBytes(StandardCharsets.UTF_8))
                .sign();
    }

    /** 校验令牌签名与有效期 */
    public static boolean verify(String token, String secret) {
        try {
            return JWTUtil.verify(token, secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    /** 解析令牌（不校验），失败返回 null */
    public static JWT parse(String token) {
        try {
            return JWTUtil.parseToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getUid(String token, String secret) {
        return getPayload(token, secret, PAYLOAD_UID);
    }

    public static String getUsername(String token, String secret) {
        return getPayload(token, secret, PAYLOAD_USERNAME);
    }

    public static String getPayload(String token, String secret, String key) {
        JWT jwt = parse(token);
        if (jwt == null) {
            return null;
        }
        jwt.setKey(secret.getBytes(StandardCharsets.UTF_8));
        Object val = jwt.getPayload(key);
        return val == null ? null : String.valueOf(val);
    }

    /** 令牌过期时间 */
    public static Date getExpiresAt(String token) {
        JWT jwt = parse(token);
        if (jwt == null) {
            return null;
        }
        try {
            Object exp = jwt.getPayload(JWT.EXPIRES_AT);
            return exp instanceof Date ? (Date) exp : DateUtil.date(((Number) exp).longValue() * 1000L);
        } catch (Exception e) {
            return null;
        }
    }
}
