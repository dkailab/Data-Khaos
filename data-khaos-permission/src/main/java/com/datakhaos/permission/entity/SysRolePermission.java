package com.datakhaos.permission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色权限关联表
 */
@Data
@TableName("sys_role_permission")
public class SysRolePermission {

    private String id;

    private String roleId;

    private String permissionId;

    /** MENU / API / DATA */
    private String permissionType;
}
