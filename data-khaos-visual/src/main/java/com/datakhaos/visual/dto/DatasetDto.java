package com.datakhaos.visual.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 数据集 API 请求/响应 DTO
 */
@Data
public class DatasetDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String code;
    private String description;
    /** SQL / MODEL */
    private String datasetType;
    private String datasourceId;
    /** 数据源类型（MYSQL/HIVE/DORIS 等，资产池展示与联查兼容性判断用） */
    private String datasourceType;
    private String querySql;
    private String modelId;
    /** 刷新间隔秒 */
    private Integer refreshInterval;
    private String visibility;
    private String status;
    private Integer version;
    private String createBy;

    /** 字段列表 */
    private List<DatasetFieldDto> fields;
    /** 变量列表 */
    private List<DatasetVariableDto> variables;

    /** 测试查询结果 */
    private DatasetPreviewResult previewResult;

    @Data
    public static class DatasetFieldDto implements Serializable {
        private String id;
        private String fieldName;
        private String fieldCode;
        /** DIMENSION / METRIC */
        private String fieldType;
        /** STRING / INTEGER / DECIMAL / DATE */
        private String dataType;
        /** 汇总方式: SUM/AVG/COUNT/COUNT_DISTINCT/MAX/MIN */
        private String aggType;
        private String format;
        private Integer sortOrder;
    }

    @Data
    public static class DatasetVariableDto implements Serializable {
        private String varName;
        private String varType;
        private String defaultValue;
    }

    @Data
    public static class DatasetPreviewResult implements Serializable {
        private List<String> columns;
        private List<java.util.Map<String, Object>> rows;
    }

    /** 图表绘制页查询结果：生成的 SQL + 查询数据 */
    @Data
    public static class DatasetChartQueryResult implements Serializable {
        private String sql;
        private com.datakhaos.datasource.api.model.QueryResult result;
        private Boolean truncated;
        private Integer originalRowCount;
    }
}
