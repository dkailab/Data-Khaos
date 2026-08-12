package com.datakhaos.auth.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datakhaos.auth.entity.SysUser;
import com.datakhaos.auth.entity.SysUserRole;
import com.datakhaos.auth.mapper.SysUserMapper;
import com.datakhaos.auth.mapper.SysUserRoleMapper;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户管理
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;

    public PageResult<SysUser> page(long current, long size, String username, String realName) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StrUtil.isNotBlank(username), SysUser::getUsername, username)
                .like(StrUtil.isNotBlank(realName), SysUser::getRealName, realName)
                .orderByAsc(SysUser::getCreateTime);
        Page<SysUser> result = userMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(current, size, result.getTotal(), result.getRecords());
    }

    public SysUser getById(String id) {
        return userMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(SysUser user) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException("用户名已存在: " + user.getUsername());
        }
        if (StrUtil.isBlank(user.getPassword())) {
            user.setPassword("123456");
        }
        user.setPassword(PasswordUtil.encode(user.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userMapper.insert(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysUser user) {
        SysUser exist = userMapper.selectById(user.getId());
        if (exist == null) {
            throw new BusinessException("用户不存在");
        }
        // 不更新密码字段（密码走独立接口）
        user.setPassword(null);
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        if ("1".equals(id)) {
            throw new BusinessException("默认管理员不允许删除");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String id, String rawPassword) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(PasswordUtil.encode(rawPassword));
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(String userId, List<String> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds != null) {
            for (String roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }

    public List<String> getRoleIdsByUserId(String userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
    }
}
