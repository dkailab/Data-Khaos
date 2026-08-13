package com.datakhaos.visual.dto;

import com.datakhaos.datasource.api.model.QueryResult;
import lombok.Data;

import java.io.Serializable;

/**
 * 即席查询执行响应：包装查询结果 + 行数截断标记。
 */
@Data
public class AdhocExecuteResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 查询结果 */
    private QueryResult result;

    /** 结果是否被行数上限截断 */
    private boolean truncated;

    /** 截断前的原始总行数（无法精确获知时取当前返回行数） */
    private int originalRowCount;
}
