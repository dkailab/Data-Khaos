package com.datakhaos.workflow.dto;

import com.datakhaos.workflow.entity.WorkflowDef;
import com.datakhaos.workflow.entity.WorkflowEdge;
import com.datakhaos.workflow.entity.WorkflowNode;
import lombok.Data;

import java.util.List;

/**
 * 工作流图保存/详情请求：定义 + 节点 + 连线（整体事务替换）。
 */
@Data
public class WorkflowGraphRequest {

    /** 工作流定义 */
    private WorkflowDef def;

    /** 节点列表 */
    private List<WorkflowNode> nodes;

    /** 连线列表 */
    private List<WorkflowEdge> edges;
}