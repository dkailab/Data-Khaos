package com.datakhaos.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datakhaos.workflow.entity.WorkflowDef;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowDefMapper extends BaseMapper<WorkflowDef> {
}