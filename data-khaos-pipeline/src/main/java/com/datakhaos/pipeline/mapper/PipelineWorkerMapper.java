package com.datakhaos.pipeline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datakhaos.pipeline.entity.PipelineWorker;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PipelineWorkerMapper extends BaseMapper<PipelineWorker> {
}