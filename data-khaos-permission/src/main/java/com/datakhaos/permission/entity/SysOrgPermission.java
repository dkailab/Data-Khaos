package com.datakhaos.permission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 组织部门权限关联表（部门授予的菜单权限，成员自动继承）
 */
@Data
@TableName("sys_org_permission")
public class SysOrgPermission {

    private String id;

    private String orgId;

    private String permissionId;

    /** MENU / API / DATA */
    private String permissionType;
}