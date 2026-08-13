package com.datakhaos.dquality.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 质量任务
 */
@Data
@TableName("dquality_task")
public class DqTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 项目组隔离 */
    private String projectGroupId;

    private String taskName;

    /** 关联规则ID集合（JSON数组） */
    private String ruleIds;

    /** 周期表达式（空=一次性/手动） */
    private String cronExpr;

    /** 0停用 1启用 */
    private Integer status;

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}