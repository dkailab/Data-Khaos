package com.datakhaos.visual.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户画像 - 用户标签值
 */
@Data
@TableName("portrait_user_tag")
public class PortraitUserTag implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 用户唯一标识(业务用户ID) */
    private String userKey;

    /** 用户名称(冗余展示) */
    private String userName;

    /** 标签ID */
    private String tagId;

    /** 标签值 */
    private String tagValue;

    /** 标签时间 */
    private LocalDateTime tagTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
}