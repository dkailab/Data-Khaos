package com.datakhaos.permission.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datakhaos.common.model.PageResult;
import com.datakhaos.permission.entity.SysColumnPolicy;
import com.datakhaos.permission.entity.SysRowPolicy;
import com.datakhaos.permission.mapper.SysColumnPolicyMapper;
import com.datakhaos.permission.mapper.SysRowPolicyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 行/列级数据权限策略管理
 */
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final SysRowPolicyMapper rowPolicyMapper;
    private final SysColumnPolicyMapper columnPolicyMapper;

    // ---------- 行权限 ----------

    public PageResult<SysRowPolicy> rowPage(long current, long size, String targetTable) {
        LambdaQueryWrapper<SysRowPolicy> wrapper = new LambdaQueryWrapper<SysRowPolicy>()
                .like(StrUtil.isNotBlank(targetTable), SysRowPolicy::getTargetTable, targetTable)
                .orderByAsc(SysRowPolicy::getCreateTime);
        var result = rowPolicyMapper.selectPage(com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(current, size), wrapper);
        return PageResult.of(current, size, result.getTotal(), result.getRecords());
    }

    public void saveRow(SysRowPolicy policy) {
        rowPolicyMapper.insert(policy);
    }

    public void updateRow(SysRowPolicy policy) {
        rowPolicyMapper.updateById(policy);
    }

    public void deleteRow(String id) {
        rowPolicyMapper.deleteById(id);
    }

    // ---------- 列权限 ----------

    public PageResult<SysColumnPolicy> columnPage(long current, long size, String targetTable) {
        LambdaQueryWrapper<SysColumnPolicy> wrapper = new LambdaQueryWrapper<SysColumnPolicy>()
                .like(StrUtil.isNotBlank(targetTable), SysColumnPolicy::getTargetTable, targetTable)
                .orderByAsc(SysColumnPolicy::getCreateTime);
        var result = columnPolicyMapper.selectPage(com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(current, size), wrapper);
        return PageResult.of(current, size, result.getTotal(), result.getRecords());
    }

    public void saveColumn(SysColumnPolicy policy) {
        columnPolicyMapper.insert(policy);
    }

    public void updateColumn(SysColumnPolicy policy) {
        columnPolicyMapper.updateById(policy);
    }

    public void deleteColumn(String id) {
        columnPolicyMapper.deleteById(id);
    }
}
