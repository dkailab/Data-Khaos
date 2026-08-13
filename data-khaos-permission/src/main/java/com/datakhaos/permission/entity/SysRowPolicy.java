package com.datakhaos.permission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 行权限策略表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_row_policy")
public class SysRowPolicy extends BaseEntity {

    private String policyName;

    private String targetTable;

    /** 过滤表达式，支持 #{currentUserId}/#{currentOrgId} */
    private String expression;

    private String expressionDesc;

    private String roleId;

    private String userId;

    /** 项目组ID（支持按组绑定） */
    private String projectGroupId;

    private Integer status;
}
