package com.datakhaos.permission.api.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 菜单 DTO（用于前端构建路由与权限树）
 */
@Data
public class MenuDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String parentId;

    private String name;

    private String path;

    private String component;

    private String permission;

    private String icon;

    /** 0:目录 1:菜单 2:按钮 3:API */
    private Integer type;

    private Integer sortOrder;
}
