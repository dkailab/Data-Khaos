package com.datakhaos.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流依赖边（DAG 连线，from 完成后触发 to）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_edge")
public class WorkflowEdge extends WorkflowBaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 所属工作流ID */
    private String wfId;

    /** 前驱节点编码 */
    private String fromCode;

    /** 后继节点编码 */
    private String toCode;

    /** 边条件表达式（预留：如 DATA=${day}=='today'；空表示硬依赖） */
    private String conditionExpr;
}