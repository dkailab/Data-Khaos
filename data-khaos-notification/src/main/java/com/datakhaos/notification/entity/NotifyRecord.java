package com.datakhaos.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 推送记录表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_record")
public class NotifyRecord extends BaseEntity {

    /** 模板ID */
    private String templateId;

    /** 接收人ID */
    private String receiverId;

    /** USER / ROLE / ORG */
    private String receiverType;

    /** 渠道 */
    private String channel;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 0:待发送 1:已发送 2:发送失败 */
    private Integer status;

    /** 发送时间 */
    private LocalDateTime sendTime;

    /** 错误信息 */
    private String errorMessage;
}
