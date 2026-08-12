package com.datakhaos.common.util;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * 对称加解密工具（AES），用于数据源密码等敏感信息的落库保护。
 * 密钥由配置项 {@code data-khaos.aes-key} 提供，生产环境务必更换。
 */
@Slf4j
public final class EncryptUtil {

    private EncryptUtil() {
    }

    public static String encrypt(String plainText, String key) {
        if (plainText == null) {
            return null;
        }
        try {
            AES aes = SecureUtil.aes(key.getBytes(StandardCharsets.UTF_8));
            return aes.encryptHex(plainText);
        } catch (Exception e) {
            log.error("AES 加密失败", e);
            return plainText;
        }
    }

    public static String decrypt(String cipherText, String key) {
        if (cipherText == null) {
            return null;
        }
        try {
            AES aes = SecureUtil.aes(key.getBytes(StandardCharsets.UTF_8));
            return aes.decryptStr(cipherText);
        } catch (Exception e) {
            // 解密失败时原样返回，容忍非加密串
            return cipherText;
        }
    }
}
