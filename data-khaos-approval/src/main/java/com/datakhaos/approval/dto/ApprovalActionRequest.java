package com.datakhaos.approval.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 审批动作请求（通过 / 驳回）
 */
@Data
public class ApprovalActionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审批人ID（默认取请求上下文中的当前用户） */
    private String approverId;

    /** 审批意见 */
    private String comment;
}
