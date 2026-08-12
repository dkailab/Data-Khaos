package com.datakhaos.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 验证码响应
 */
@Data
@AllArgsConstructor
public class CaptchaResponse {

    /** 验证码ID（登录时回传） */
    private String captchaId;

    /** 验证码图片 Base64（data:image/png;base64,xxx） */
    private String imageBase64;
}
