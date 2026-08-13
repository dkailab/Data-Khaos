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

    /** 验证码强制开启，登录必填，不可关闭 */
    private boolean enabled = true;
}
