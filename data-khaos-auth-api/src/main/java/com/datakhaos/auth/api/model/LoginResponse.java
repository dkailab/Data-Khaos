package com.datakhaos.auth.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 登录响应：令牌 + 用户基础信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** JWT 访问令牌 */
    private String token;

    /** 令牌有效期（秒） */
    private Long expireIn;

    /** 用户信息 */
    private LoginUser user;

    /** 用户拥有的角色编码 */
    private List<String> roles;

    /** 用户拥有的权限标识集合 */
    private List<String> permissions;
}
