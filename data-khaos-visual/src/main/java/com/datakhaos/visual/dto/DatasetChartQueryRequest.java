package com.datakhaos.visual.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图表绘制页查询请求：基于数据集 + 维度/指标/筛选/排序生成聚合 SQL 并执行。
 * 字段编码必须来自数据集字段定义（服务端白名单校验，防注入）。
 */
@Data
public class DatasetChartQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据集ID */
    private String datasetId;

    /** 维度字段 */
    private List<FieldRef> dimensions;

    /** 指标字段 */
    private List<MetricRef> metrics;

    /** 筛选条件 */
    private List<FilterRef> filters;

    /** 排序 */
    private List<SortRef> sorts;

    /** 行数限制（默认 1000，上限 10000） */
    private Integer limit;

    @Data
    public static class FieldRef implements Serializable {
        private String fieldCode;
        /** ASC / DESC */
        private String sort;
    }

    @Data
    public static class MetricRef implements Serializable {
        private String fieldCode;
        /** SUM / AVG / COUNT / COUNT_DISTINCT / MAX / MIN */
        private String aggType;
    }

    @Data
    public static class FilterRef implements Serializable {
        private String fieldCode;
        /** EQ / NE / GT / GTE / LT / LTE / IN / NOT_IN / LIKE / BETWEEN */
        private String operator;
        /** 条件值（BETWEEN 取前两个，IN 全量） */
        private List<String> values;
    }

    @Data
    public static class SortRef implements Serializable {
        private String fieldCode;
        /** ASC / DESC */
        private String direction;
    }
}
