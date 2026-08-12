package com.datakhaos.permission.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.model.PageResult;
import com.datakhaos.permission.entity.SysOrgPermission;
import com.datakhaos.permission.entity.SysOrganization;
import com.datakhaos.permission.entity.SysUserOrg;
import com.datakhaos.permission.mapper.SysMenuMapper;
import com.datakhaos.permission.mapper.SysOrgPermissionMapper;
import com.datakhaos.permission.mapper.SysOrganizationMapper;
import com.datakhaos.permission.mapper.SysUserOrgMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 组织架构与部门权限管理
 */
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final SysOrganizationMapper orgMapper;
    private final SysUserOrgMapper userOrgMapper;
    private final SysOrgPermissionMapper orgPermissionMapper;
    private final SysMenuMapper menuMapper;

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

    /** 组织树（不含根节点包装，返回顶层节点及 children） */
    public List<Map<String, Object>> tree() {
        List<SysOrganization> all = list();
        var map = new java.util.HashMap<String, Map<String, Object>>();
        var roots = new ArrayList<Map<String, Object>>();
        for (SysOrganization o : all) {
            var node = new java.util.HashMap<String, Object>();
            node.put("id", o.getId());
            node.put("parentId", o.getParentId());
            node.put("orgName", o.getOrgName());
            node.put("orgCode", o.getOrgCode());
            node.put("orgType", o.getOrgType());
            node.put("status", o.getStatus());
            node.put("sortOrder", o.getSortOrder());
            node.put("children", new ArrayList<Map<String, Object>>());
            map.put(o.getId(), node);
        }
        for (SysOrganization o : all) {
            var node = map.get(o.getId());
            var parent = map.get(o.getParentId());
            if (parent != null) {
                @SuppressWarnings("unchecked")
                var children = (List<Map<String, Object>>) parent.get("children");
                children.add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
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

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        Long children = orgMapper.selectCount(new LambdaQueryWrapper<SysOrganization>()
                .eq(SysOrganization::getParentId, id));
        if (children != null && children > 0) {
            throw new BusinessException("存在下级组织，无法删除");
        }
        Long members = userOrgMapper.selectCount(new LambdaQueryWrapper<SysUserOrg>()
                .eq(SysUserOrg::getOrgId, id));
        if (members != null && members > 0) {
            throw new BusinessException("组织下存在成员，请先移除成员再删除");
        }
        orgMapper.deleteById(id);
        orgPermissionMapper.delete(new LambdaQueryWrapper<SysOrgPermission>()
                .eq(SysOrgPermission::getOrgId, id));
    }

    /** 查询组织成员 */
    public List<Map<String, Object>> listOrgUsers(String orgId) {
        return orgPermissionMapper.selectOrgUsers(orgId);
    }

    /** 批量设置组织成员（全量替换） */
    @Transactional(rollbackFor = Exception.class)
    public void assignOrgUsers(String orgId, List<String> userIds) {
        userOrgMapper.delete(new LambdaQueryWrapper<SysUserOrg>().eq(SysUserOrg::getOrgId, orgId));
        if (userIds == null) {
            return;
        }
        boolean first = true;
        for (String userId : userIds) {
            if (StrUtil.isBlank(userId)) {
                continue;
            }
            SysUserOrg uo = new SysUserOrg();
            // 移除该用户在其他组织的主组织标记
            userOrgMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysUserOrg>()
                    .eq(SysUserOrg::getUserId, userId)
                    .set(SysUserOrg::getIsPrimary, 0));
            uo.setUserId(userId);
            uo.setOrgId(orgId);
            uo.setIsPrimary(first ? 1 : 0);
            userOrgMapper.insert(uo);
            first = false;
        }
    }

    /** 查询部门已授予的菜单权限ID */
    public List<String> getOrgPermissionIds(String orgId) {
        return orgPermissionMapper.selectList(new LambdaQueryWrapper<SysOrgPermission>()
                        .eq(SysOrgPermission::getOrgId, orgId)
                        .eq(SysOrgPermission::getPermissionType, "MENU"))
                .stream().map(SysOrgPermission::getPermissionId).toList();
    }

    /** 授予部门菜单权限（全量替换） */
    @Transactional(rollbackFor = Exception.class)
    public void assignOrgPermissions(String orgId, List<String> menuIds) {
        orgPermissionMapper.delete(new LambdaQueryWrapper<SysOrgPermission>()
                .eq(SysOrgPermission::getOrgId, orgId));
        if (menuIds == null) {
            return;
        }
        for (String menuId : menuIds) {
            if (StrUtil.isBlank(menuId) || menuMapper.selectById(menuId) == null) {
                continue;
            }
            SysOrgPermission op = new SysOrgPermission();
            op.setOrgId(orgId);
            op.setPermissionId(menuId);
            op.setPermissionType("MENU");
            orgPermissionMapper.insert(op);
        }
    }
}