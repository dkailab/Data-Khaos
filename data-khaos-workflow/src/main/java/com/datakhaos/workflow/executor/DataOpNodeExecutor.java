package com.datakhaos.workflow.executor;

import com.datakhaos.datasource.api.connector.DatasourceApiClient;
import org.springframework.stereotype.Component;

/**
 * 数据算子节点执行器：面向数据加工/格式化场景，第一版以 SQL 加工为主
 * （如清洗、去重、汇总、开窗等由算子内 SQL 承载），沿用统一 SQL 执行链路。
 *
 * <p>后续可在不改变接口的前提下扩展真正的算子（如 INNER_JOIN / PIVOT / 脱敏 / 类型转换）。</p>
 */
@Component
public class DataOpNodeExecutor extends SqlNodeExecutor {

    public DataOpNodeExecutor(DatasourceApiClient datasourceApiClient) {
        super(datasourceApiClient);
    }

    @Override
    public String getType() {
        return NodeType.DATA_OP;
    }
}