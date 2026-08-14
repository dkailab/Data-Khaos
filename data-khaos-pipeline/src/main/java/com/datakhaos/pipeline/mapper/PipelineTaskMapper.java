package com.datakhaos.pipeline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datakhaos.pipeline.entity.PipelineTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PipelineTaskMapper extends BaseMapper<PipelineTask> {
}