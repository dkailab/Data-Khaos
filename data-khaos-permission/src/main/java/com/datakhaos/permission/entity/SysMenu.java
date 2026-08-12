package com.datakhaos.permission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单/资源表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    private String parentId;

    private String name;

    private String path;

    private String component;

    /** 权限标识 */
    private String permission;

    private String icon;

    /** 0:目录 1:菜单 2:按钮 3:API */
    private Integer type;

    private Integer sortOrder;

    private Integer status;
}
