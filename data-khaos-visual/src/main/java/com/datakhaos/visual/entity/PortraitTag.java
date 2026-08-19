package com.datakhaos.visual.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户画像 - 标签定义
 */
@Data
@TableName("portrait_tag")
public class PortraitTag implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 所属分类ID */
    private String categoryId;

    /** 标签名称 */
    private String name;

    /** 标签编码 */
    private String code;

    /** 标签类型: BOOL布尔/NUMBER数值/STR字符串/ENUM枚举 */
    private String tagType = "STR";

    /** 单位(数值类型) */
    private String unit;

    /** 枚举可选值(JSON数组) */
    private String enumOptions;

    /** 标签说明 */
    private String description;

    /** 状态: 1启用 0禁用 */
    private Integer status = 1;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;
}