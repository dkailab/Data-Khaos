package com.datakhaos.visual.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 即席查询执行历史：每次执行（成功/失败）均落库，用于审计与复盘。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visual_adhoc_history")
public class VisualAdhocHistory extends BaseEntity {

    /** 关联的收藏查询ID（直接执行则为空） */
    private String adhocId;

    /** 执行用户ID */
    private String userId;

    /** 数据源ID */
    private String datasourceId;

    /** 实际执行的SQL（已解析参数） */
    private String sqlText;

    /** 1:成功 0:失败 */
    private Integer status;

    /** 耗时(ms) */
    private Long costMs;

    /** 结果行数 */
    private Integer rowCount;

    /** 错误信息（失败时有值） */
    private String errorMessage;
}
