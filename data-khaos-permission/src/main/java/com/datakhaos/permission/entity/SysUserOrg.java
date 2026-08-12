package com.datakhaos.permission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户组织关联表
 */
@Data
@TableName("sys_user_org")
public class SysUserOrg {

    private String id;

    private String userId;

    private String orgId;

    private Integer isPrimary;
}
