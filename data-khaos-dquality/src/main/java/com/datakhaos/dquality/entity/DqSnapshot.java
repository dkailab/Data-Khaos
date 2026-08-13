package com.datakhaos.dquality.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 稽核快照（每次执行）
 */
@Data
@TableName("dquality_snapshot")
public class DqSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 项目组隔离 */
    private String projectGroupId;

    private String taskId;

    private String datasourceId;

    private String databaseName;

    private String tableName;

    /** 质量评分 0-100 */
    private BigDecimal score;

    private Integer ruleTotal;

    private Integer rulePass;

    private Integer ruleFail;

    /** 明细 JSON */
    private String detail;

    private Long costMs;

    /** MANUAL / SCHEDULE */
    private String triggerType;

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 任务名称（冗余，展示用，不入库） */
    @TableField(exist = false)
    private String taskName;
}