package com.datakhaos.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码ID（启用验证码时必填） */
    private String captchaId;

    /** 验证码 */
    private String captchaCode;
}
