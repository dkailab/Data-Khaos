package com.datakhaos.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批记录表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_approval_record")
public class AppApprovalRecord extends BaseEntity {

    /** 申请单ID */
    private String applyId;

    /** 审批人ID */
    private String approverId;

    /** 1:通过 2:驳回 3:转交 */
    private Integer action;

    /** 审批意见 */
    private String comment;
}
