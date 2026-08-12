package com.datakhaos.permission.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.model.PageResult;
import com.datakhaos.permission.entity.SysOrganization;
import com.datakhaos.permission.mapper.SysOrganizationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 组织架构管理
 */
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final SysOrganizationMapper orgMapper;

    public PageResult<SysOrganization> page(long current, long size, String orgName) {
        LambdaQueryWrapper<SysOrganization> wrapper = new LambdaQueryWrapper<SysOrganization>()
                .like(StrUtil.isNotBlank(orgName), SysOrganization::getOrgName, orgName)
                .orderByAsc(SysOrganization::getSortOrder);
        var result = orgMapper.selectPage(com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(current, size), wrapper);
        return PageResult.of(current, size, result.getTotal(), result.getRecords());
    }

    public List<SysOrganization> list() {
        return orgMapper.selectList(new LambdaQueryWrapper<SysOrganization>()
                .orderByAsc(SysOrganization::getSortOrder));
    }

    public void save(SysOrganization org) {
        if (org.getStatus() == null) {
            org.setStatus(1);
        }
        orgMapper.insert(org);
    }

    public void update(SysOrganization org) {
        orgMapper.updateById(org);
    }

    public void delete(String id) {
        Long children = orgMapper.selectCount(new LambdaQueryWrapper<SysOrganization>()
                .eq(SysOrganization::getParentId, id));
        if (children != null && children > 0) {
            throw new BusinessException("存在下级组织，无法删除");
        }
        orgMapper.deleteById(id);
    }
}
