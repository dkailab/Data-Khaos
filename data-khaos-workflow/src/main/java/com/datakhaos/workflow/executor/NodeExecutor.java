package com.datakhaos.workflow.executor;

/**
 * 节点执行器 SPI：每种节点类型（SQL / SHELL / PYTHON / DATA_OP）一个实现，
 * 通过 Spring 工厂 {@link NodeExecutorFactory} 按 {@link #getType()} 分发，
 * 实现「可插拔节点类型」——新增类型只需实现本接口并注册为 Spring Bean。
 */
public interface NodeExecutor {

    /** 节点类型标识，与 WorkflowNode.nodeType 对应 */
    String getType();

    /** 执行节点 */
    ExecResult execute(NodeContext context) throws Exception;
}