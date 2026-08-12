package com.datakhaos.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户订阅表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_subscription")
public class NotifySubscription extends BaseEntity {

    /** 用户ID */
    private String userId;

    /** REPORT / METRIC / JOB */
    private String subscribeType;

    /** 目标ID */
    private String targetId;

    /** 渠道 */
    private String channel;

    private Integer status;
}
