package com.datakhaos.dquality.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datakhaos.dquality.entity.DqSnapshot;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DqSnapshotMapper extends BaseMapper<DqSnapshot> {
}