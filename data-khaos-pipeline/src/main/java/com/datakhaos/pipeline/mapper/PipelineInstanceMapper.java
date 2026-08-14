package com.datakhaos.pipeline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datakhaos.pipeline.entity.PipelineInstance;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PipelineInstanceMapper extends BaseMapper<PipelineInstance> {
}