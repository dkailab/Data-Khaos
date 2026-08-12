package com.datakhaos.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datakhaos.auth.config.JwtProperties;
import com.datakhaos.auth.dto.LoginRequest;
import com.datakhaos.auth.entity.SysUser;
import com.datakhaos.auth.mapper.SysUserMapper;
import com.datakhaos.common.constant.CommonConstants;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.model.ResultCode;
import com.datakhaos.common.security.JwtUtil;
import com.datakhaos.common.util.PasswordUtil;
import com.datakhaos.auth.api.model.LoginResponse;
import com.datakhaos.auth.api.model.LoginUser;
import com.datakhaos.permission.api.model.UserPermissionDto;
import com.datakhaos.permission.api.service.PermissionApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 认证服务：登录、令牌签发、当前用户查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final RoleService roleService;
    private final CaptchaService captchaService;
    private final JwtProperties jwtProperties;
    private final PermissionApiClient permissionApiClient;

    /**
     * 登录：校验 → 签发 JWT → 组装用户/角色/权限
     */
    public LoginResponse login(LoginRequest request) {
        if (captchaService.enabled()) {
            captchaService.validate(request.getCaptchaId(), request.getCaptchaCode());
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        String token = JwtUtil.createToken(user.getId(), user.getUsername(),
                jwtProperties.getSecret(), jwtProperties.getExpireSeconds());

        List<String> roles = roleService.getRoleCodesByUserId(user.getId());
        UserPermissionDto permission = permissionApiClient.getUserPermission(user.getId());

        return new LoginResponse(token, jwtProperties.getExpireSeconds(),
                toLoginUser(user), roles, permission.getPermissions());
    }

    /**
     * 根据令牌解析当前登录用户（供前端刷新用户态）
     */
    public LoginResponse currentUser(String token) {
        if (token == null || !JwtUtil.verify(token, jwtProperties.getSecret())) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "令牌无效或已过期，请重新登录");
        }
        String uid = JwtUtil.getUid(token, jwtProperties.getSecret());
        SysUser user = userMapper.selectById(uid);
        if (user == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "用户不存在");
        }
        List<String> roles = roleService.getRoleCodesByUserId(uid);
        UserPermissionDto permission = permissionApiClient.getUserPermission(uid);
        return new LoginResponse(token, jwtProperties.getExpireSeconds(),
                toLoginUser(user), roles, permission.getPermissions());
    }

    private LoginUser toLoginUser(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setStatus(user.getStatus());
        return loginUser;
    }
}
