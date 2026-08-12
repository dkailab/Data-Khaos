package com.datakhaos.permission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表权限表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_table_permission")
public class SysTablePermission extends BaseEntity {

    private String datasourceId;

    private String databaseName;

    private String tableName;

    /** SELECT / INSERT / UPDATE / DELETE / ALL */
    private String permissionType;

    private String roleId;

    private String userId;

    /** ROLE / USER */
    private String grantType;

    private Integer status;
}
