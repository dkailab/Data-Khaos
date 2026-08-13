package com.datakhaos.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 验证码配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "data-khaos.captcha")
public class CaptchaProperties {

    /** 验证码开关：true 强制登录必填；false 关闭（开发/自动化环境） */
    private boolean enabled = true;
}
