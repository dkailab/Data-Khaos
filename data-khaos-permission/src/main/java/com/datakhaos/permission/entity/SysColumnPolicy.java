package com.datakhaos.permission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 列权限策略表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_column_policy")
public class SysColumnPolicy extends BaseEntity {

    private String policyName;

    private String targetTable;

    private String columnName;

    /** MASK / ENCRYPT / HIDE / PLAIN */
    private String maskType;

    /** 脱敏规则，如 left:3,right:4 */
    private String maskRule;

    private String roleId;

    private String userId;

    private Integer status;
}
