package com.datakhaos.workflow.executor;

import lombok.Builder;
import lombok.Data;

/**
 * 单节点执行结果
 */
@Data
@Builder
public class ExecResult {

    /** 是否成功 */
    private boolean success;

    /** 影响/结果行数（可能为空） */
    private Integer rows;

    /** 执行输出/日志 */
    private String log;
}