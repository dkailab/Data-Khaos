package com.datakhaos.approval.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 转交流程请求
 */
@Data
public class TransferRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 转交给的审批人ID */
    private String toApproverId;

    /** 转交意见 */
    private String comment;
}
