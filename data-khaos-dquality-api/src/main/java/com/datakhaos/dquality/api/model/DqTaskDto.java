package com.datakhaos.dquality.api.model;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 质量任务 DTO
 */
@Data
public class DqTaskDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    /** 项目组隔离 */
    private String projectGroupId;

    private String taskName;

    /** 关联规则ID集合（JSON数组） */
    private String ruleIds;

    /** 周期表达式（空=一次性/手动） */
    private String cronExpr;

    /** 0停用 1启用 */
    private Integer status = 1;
}