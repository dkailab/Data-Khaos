package com.datakhaos.visual.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据集定义 - 仪表板的数据源抽象
 * 支持两种模式: SQL(直接写SQL查询) / MODEL(基于数据集市模型)
 */
@Data
@TableName("visual_dataset")
public class VisualDataset implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 数据集名称 */
    private String name;

    /** 数据集编码(英文标识) */
    private String code;

    /** 数据集描述 */
    private String description;

    /** 数据集类型: SQL / MODEL */
    private String datasetType;

    /** SQL模式: 数据源ID */
    private String datasourceId;

    /** SQL模式: 查询SQL */
    private String querySql;

    /** MODEL模式: 模型ID */
    private String modelId;

    /** 字段定义(JSON数组) */
    private String fieldsJson;

    /** 变量定义(JSON数组) */
    private String variablesJson;

    /** 数据刷新间隔(秒, 0表示不自动刷新) */
    private Integer refreshInterval = 0;

    /** 所有者ID */
    private String ownerId;

    /** 组织ID(权限归属) */
    private String orgId;

    /** 可见范围: PRIVATE/ORG/PUBLIC */
    private String visibility = "PRIVATE";

    /** 状态: 草稿DRAFT/已上线PUBLISHED/已下线OFFLINE */
    private String status;

    /** 版本号 */
    private Integer version = 1;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private String createBy;
    private String updateBy;
}
