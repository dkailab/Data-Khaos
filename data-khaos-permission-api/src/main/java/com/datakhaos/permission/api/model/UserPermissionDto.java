package com.datakhaos.permission.api.model;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户权限视图：角色、权限标识、可见菜单
 */
@Data
public class UserPermissionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;

    /** 角色编码集合 */
    private List<String> roles = new ArrayList<>();

    /** 权限标识集合（含菜单 permission 与 API 权限） */
    private List<String> permissions = new ArrayList<>();

    /** 可见菜单（扁平列表，前端构建树） */
    private List<MenuDto> menus = new ArrayList<>();
}
