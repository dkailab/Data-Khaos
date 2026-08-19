package com.datakhaos.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datakhaos.workflow.entity.WorkflowRun;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowRunMapper extends BaseMapper<WorkflowRun> {
}