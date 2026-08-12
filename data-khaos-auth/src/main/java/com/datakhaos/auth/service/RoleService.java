package com.datakhaos.auth.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datakhaos.auth.entity.SysRole;
import com.datakhaos.auth.entity.SysUserRole;
import com.datakhaos.auth.mapper.SysRoleMapper;
import com.datakhaos.auth.mapper.SysUserRoleMapper;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色管理
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    public PageResult<SysRole> page(long current, long size, String roleName) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .like(StrUtil.isNotBlank(roleName), SysRole::getRoleName, roleName)
                .orderByAsc(SysRole::getCreateTime);
        Page<SysRole> result = roleMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(current, size, result.getTotal(), result.getRecords());
    }

    public List<SysRole> listAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().eq(SysRole::getStatus, 1));
    }

    public SysRole getById(String id) {
        return roleMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(SysRole role) {
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, role.getRoleCode()));
        if (count != null && count > 0) {
            throw new BusinessException("角色编码已存在: " + role.getRoleCode());
        }
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        roleMapper.insert(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysRole role) {
        roleMapper.updateById(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        if ("1".equals(id)) {
            throw new BusinessException("超级管理员角色不允许删除");
        }
        roleMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
    }

    /**
     * 查询用户拥有的角色编码
     */
    public List<String> getRoleCodesByUserId(String userId) {
        List<String> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectBatchIds(roleIds).stream().map(SysRole::getRoleCode).toList();
    }
}
