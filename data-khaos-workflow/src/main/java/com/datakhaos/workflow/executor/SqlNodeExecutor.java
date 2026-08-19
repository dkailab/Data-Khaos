package com.datakhaos.workflow.executor;

import com.datakhaos.common.model.R;
import com.datakhaos.datasource.api.connector.DatasourceApiClient;
import com.datakhaos.datasource.api.model.QueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SQL 节点执行器：通过数据源服务执行单条 SQL（走统一 SQL 审核），返回影响/结果行数。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqlNodeExecutor implements NodeExecutor {

    private final DatasourceApiClient datasourceApiClient;

    @Override
    public String getType() {
        return NodeType.SQL;
    }

    @Override
    public ExecResult execute(NodeContext context) {
        String sql = context.getContent();
        String dsId = context.getDatasourceId();
        if (dsId == null || dsId.isBlank()) {
            throw new IllegalArgumentException("SQL 节点未指定数据源");
        }
        long start = System.currentTimeMillis();
        R<QueryResult> r = datasourceApiClient.executeRaw(dsId, sql);
        if (r == null || r.getCode() != 0) {
            String msg = r == null ? "SQL 执行失败" : r.getMsg();
            throw new IllegalArgumentException(msg);
        }
        QueryResult data = r.getData();
        int rows = data == null || data.getRowCount() == null ? 0 : data.getRowCount();
        return ExecResult.builder()
                .success(true)
                .rows(rows)
                .log("SQL 执行成功，耗时 " + (System.currentTimeMillis() - start) + "ms，影响/结果行数 " + rows)
                .build();
    }
}