package com.datakhaos.schedule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务依赖关系表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("schedule_job_dep")
public class ScheduleJobDep extends BaseEntity {

    /** 任务ID */
    private String jobId;

    /** 依赖任务ID */
    private String depJobId;

    /** HARD / SOFT */
    private String depType;
}
