package com.datakhaos.query.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询历史表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("query_history")
public class QueryHistory extends BaseEntity {

    /** 用户ID */
    private String userId;

    /** 数据源ID */
    private String datasourceId;

    /** 数据库 */
    private String databaseName;

    /** SQL 文本 */
    private String sqlText;

    /** 1:成功 0:失败 */
    private Integer status;

    /** 耗时（毫秒） */
    private Long costMs;

    /** 结果行数 */
    private Integer rowCount;

    /** 错误信息 */
    private String errorMessage;
}
