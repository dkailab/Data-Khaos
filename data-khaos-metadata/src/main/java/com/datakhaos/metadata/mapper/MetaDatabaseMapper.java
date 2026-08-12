package com.datakhaos.metadata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datakhaos.metadata.entity.MetaDatabase;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetaDatabaseMapper extends BaseMapper<MetaDatabase> {
}
