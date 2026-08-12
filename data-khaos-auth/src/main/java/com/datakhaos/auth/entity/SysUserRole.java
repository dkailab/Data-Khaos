package com.datakhaos.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户角色关联表
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    private String id;

    private String userId;

    private String roleId;
}
