package com.datakhaos.dquality.dto;

import lombok.Data;

/**
 * 调度任务简要信息（从调度服务聚合而来，供质量任务页面展示关联关系）。
 * 只暴露展示所需字段，避免透传 SQL 等敏感配置。
 */
@Data
public class ScheduleJobBrief {

    /** 调度任务ID */
    private String jobId;

    /** 调度任务名称 */
    private String jobName;

    /** 任务类型（QUALITY） */
    private String jobType;

    /** Cron 表达式 */
    private String cronExpression;

    /** 0:停用 1:启用 */
    private Integer status;

    /** 关联的质量任务ID（由 params.taskId 解析） */
    private String taskId;
}
