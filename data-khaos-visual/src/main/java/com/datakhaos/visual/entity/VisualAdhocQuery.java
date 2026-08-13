package com.datakhaos.visual.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 即席查询收藏表：用户保存的即席分析 SQL（可含默认参数）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visual_adhoc_query")
public class VisualAdhocQuery extends BaseEntity {

    /** 查询名称 */
    private String name;

    /** 数据源ID */
    private String datasourceId;

    /** 查询SQL（可含 ${param} 占位符） */
    private String sqlText;

    /** 默认参数（JSON：Map<String,Object>） */
    private String paramsJson;

    /** 分组/文件夹（用于归类，可选） */
    private String folder;

    /** 创建人 */
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
