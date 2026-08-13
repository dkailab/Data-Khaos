package com.datakhaos.dquality.api.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 质量总览 DTO
 */
@Data
public class DqOverviewDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前评分 */
    private BigDecimal currentScore;

    /** 平均通过率 */
    private BigDecimal avgPassRate;

    /** 总规则数 */
    private Integer totalRules;

    /** 活跃规则数 */
    private Integer activeRules;

    /** 总任务数 */
    private Integer totalTasks;

    /** 总执行次数 */
    private Integer totalExecutions;

    /** 最差表 Top 5 */
    private List<WorstTable> worstTables;

    @Data
    public static class WorstTable implements Serializable {
        private static final long serialVersionUID = 1L;
        private String datasourceId;
        private String databaseName;
        private String tableName;
        private BigDecimal score;
        private Integer execCount;
    }
}