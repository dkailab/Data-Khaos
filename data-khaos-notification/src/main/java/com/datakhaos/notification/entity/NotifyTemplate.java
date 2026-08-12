package com.datakhaos.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息模板表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_template")
public class NotifyTemplate extends BaseEntity {

    /** 模板编码 */
    private String templateCode;

    /** 模板名称 */
    private String templateName;

    /** 渠道 MAIL / SITE / WECHAT / SMS */
    private String channel;

    /** 标题模板 */
    private String titleTemplate;

    /** 内容模板 */
    private String contentTemplate;

    private Integer status;
}
