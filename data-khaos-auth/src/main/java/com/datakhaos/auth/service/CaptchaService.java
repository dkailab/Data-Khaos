package com.datakhaos.auth.service;

import cn.hutool.core.util.RandomUtil;
import com.datakhaos.auth.dto.CaptchaResponse;
import com.datakhaos.auth.config.CaptchaProperties;
import com.datakhaos.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码服务（内存存储，5 分钟有效）
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final long EXPIRE_MS = 5 * 60 * 1000L;
    private final Map<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    /**
     * 生成验证码图片
     */
    public CaptchaResponse generate() {
        String code = RandomUtil.randomString("ABCDEFGHJKLMNPQRSTUVWXYZ23456789", 4);
        String captchaId = java.util.UUID.randomUUID().toString().replace("-", "");
        store.put(captchaId, new CaptchaEntry(code, System.currentTimeMillis() + EXPIRE_MS));
        cleanup();
        return new CaptchaResponse(captchaId, "data:image/png;base64," + renderPng(code));
    }

    /**
     * 校验验证码（一次有效）
     */
    public void validate(String captchaId, String code) {
        if (code == null) {
            throw new BusinessException("验证码不能为空");
        }
        CaptchaEntry entry = captchaId == null ? null : store.remove(captchaId);
        if (entry == null || entry.expireAt < System.currentTimeMillis()) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!entry.code.equalsIgnoreCase(code.trim())) {
            throw new BusinessException("验证码错误");
        }
    }

    private String renderPng(String code) {
        int width = 120, height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, width, height);
        // 干扰线
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(RandomUtil.randomInt(150, 220), RandomUtil.randomInt(150, 220), RandomUtil.randomInt(150, 220)));
            g.drawLine(RandomUtil.randomInt(width), RandomUtil.randomInt(height),
                    RandomUtil.randomInt(width), RandomUtil.randomInt(height));
        }
        // 字符
        g.setFont(new Font("Arial", Font.BOLD, 24));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(RandomUtil.randomInt(20, 120), RandomUtil.randomInt(20, 120), RandomUtil.randomInt(20, 120)));
            g.drawString(String.valueOf(code.charAt(i)), 12 + i * 26, RandomUtil.randomInt(24, 34));
        }
        g.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new BusinessException("验证码生成失败");
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> e.getValue().expireAt < now);
    }

    private record CaptchaEntry(String code, long expireAt) {
    }
}
