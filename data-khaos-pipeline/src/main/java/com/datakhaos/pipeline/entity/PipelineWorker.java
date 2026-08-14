package com.datakhaos.pipeline.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管道执行 worker 注册（可扩展引擎执行节点）
 */
@Data
@TableName("pipeline_worker")
public class PipelineWorker implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String workerName;

    /** 支持引擎，逗号分隔 */
    private String engines;

    /** 1=在线 0=离线 */
    private Integer status;

    private LocalDateTime lastHeartbeat;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}