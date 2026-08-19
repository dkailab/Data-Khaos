package com.datakhaos.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流节点（DAG 顶点）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_node")
public class WorkflowNode extends WorkflowBaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 所属工作流ID */
    private String wfId;

    /** 节点唯一编码（DAG 内） */
    private String nodeCode;

    /** 节点名称 */
    private String nodeName;

    /** 节点类型: SQL / SHELL / PYTHON / DATA_OP */
    private String nodeType;

    /** 节点配置（JSON）：datasourceId/sql/command/script/operator 等 */
    private String configJson;

    /** 画布 X */
    private Integer posX;

    /** 画布 Y */
    private Integer posY;

    /** 超时时间（秒） */
    private Integer timeout;

    /** 失败重试次数 */
    private Integer retryCount;

    /** 重试间隔（秒） */
    private Integer retryInterval;
}