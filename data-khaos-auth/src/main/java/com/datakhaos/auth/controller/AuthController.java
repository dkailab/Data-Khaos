package com.datakhaos.auth.controller;

import com.datakhaos.auth.dto.CaptchaResponse;
import com.datakhaos.auth.dto.LoginRequest;
import com.datakhaos.auth.service.AuthService;
import com.datakhaos.auth.service.CaptchaService;
import com.datakhaos.auth.api.model.LoginResponse;
import com.datakhaos.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final com.datakhaos.auth.config.JwtProperties jwtProperties;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @Operation(summary = "登出（前端清除令牌即可，无状态服务）")
    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }

    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public R<CaptchaResponse> captcha() {
        return R.ok(captchaService.generate());
    }

    @Operation(summary = "查询当前登录用户信息")
    @GetMapping("/info")
    public R<LoginResponse> info(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = resolveToken(authorization);
        return R.ok(authService.currentUser(token));
    }

    /** 从 Authorization: Bearer xxx 中解析 token */
    private String resolveToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}
