package com.datakhaos.notification.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 消息发送请求
 */
@Data
public class SendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模板编码 */
    private String templateCode;

    /** 接收人ID（USER 为用户ID；ROLE/ORG 为角色/组织ID） */
    private String receiverId;

    /** USER / ROLE / ORG */
    private String receiverType;

    /** 渠道（缺省取模板渠道） */
    private String channel;

    /** 模板变量（如 title / content / jobName / message） */
    private Map<String, Object> vars;
}
