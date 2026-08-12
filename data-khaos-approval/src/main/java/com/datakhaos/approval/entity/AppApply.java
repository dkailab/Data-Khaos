package com.datakhaos.approval.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 权限申请表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_apply")
public class AppApply extends BaseEntity {

    /** 申请人ID */
    private String applicantId;

    /** 申请类型：TABLE / REPORT / DATASOURCE / MENU */
    private String applyType;

    /** 申请目标ID（TABLE 时为数据源ID） */
    private String targetId;

    /** 申请目标名称（TABLE 时为 database.table） */
    private String targetName;

    /** 申请理由 */
    private String reason;

    /** 0:待审批 1:通过 2:驳回 3:已撤销 */
    private Integer status;

    /** 当前审批人ID */
    private String currentApprover;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
