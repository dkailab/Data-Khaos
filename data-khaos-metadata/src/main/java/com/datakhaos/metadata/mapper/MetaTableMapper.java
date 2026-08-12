package com.datakhaos.metadata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datakhaos.metadata.entity.MetaTable;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetaTableMapper extends BaseMapper<MetaTable> {
}
