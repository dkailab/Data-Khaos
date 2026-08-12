package com.datakhaos.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 当前登录用户上下文（网关校验令牌后透传）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String username;

    private String realName;

    private List<String> roles;

    private List<String> permissions;
}
