package com.datakhaos.workflow.executor;

import com.datakhaos.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 节点执行器工厂：按 nodeType 分发到对应执行器。
 */
@Component
public class NodeExecutorFactory {

    private final Map<String, NodeExecutor> executors;

    public NodeExecutorFactory(List<NodeExecutor> executorList) {
        this.executors = executorList.stream()
                .collect(Collectors.toMap(NodeExecutor::getType, Function.identity()));
    }

    public NodeExecutor get(String type) {
        NodeExecutor executor = executors.get(type);
        if (executor == null) {
            throw new BusinessException("不支持的节点类型: " + type);
        }
        return executor;
    }
}