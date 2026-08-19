package com.datakhaos.visual.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户画像 - 标签分类
 */
@Data
@TableName("portrait_tag_category")
public class PortraitTagCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 分类名称 */
    private String name;

    /** 分类编码 */
    private String code;

    /** 排序号 */
    private Integer sortOrder = 0;

    /** 状态: 1启用 0禁用 */
    private Integer status = 1;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;
}