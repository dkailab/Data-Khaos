package com.datakhaos.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批流程定义表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_approval_flow")
public class AppApprovalFlow extends BaseEntity {

    /** 流程名称 */
    private String flowName;

    /** 申请类型：TABLE / REPORT / DATASOURCE */
    private String applyType;

    /** 步骤序号 */
    private Integer stepOrder;

    /** 审批角色编码 */
    private String approverRole;
}
