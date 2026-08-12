package com.datakhaos.auth.api.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录用户信息
 */
@Data
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String username;

    private String realName;

    private String avatar;

    /** 用户状态 1:启用 0:禁用 */
    private Integer status;

    private LocalDateTime lastLoginTime;
}
