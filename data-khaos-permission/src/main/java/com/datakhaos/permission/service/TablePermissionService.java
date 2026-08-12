package com.datakhaos.permission.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datakhaos.common.model.PageResult;
import com.datakhaos.permission.entity.SysTablePermission;
import com.datakhaos.permission.entity.SysUserRole;
import com.datakhaos.permission.mapper.SysRoleMapper;
import com.datakhaos.permission.mapper.SysTablePermissionMapper;
import com.datakhaos.permission.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 表级权限管理：SELECT/INSERT/UPDATE/DELETE 控制
 */
@Service
@RequiredArgsConstructor
public class TablePermissionService {

    private final SysTablePermissionMapper tablePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    public PageResult<SysTablePermission> page(long current, long size, String tableName) {
        LambdaQueryWrapper<SysTablePermission> wrapper = new LambdaQueryWrapper<SysTablePermission>()
                .like(StrUtil.isNotBlank(tableName), SysTablePermission::getTableName, tableName)
                .orderByAsc(SysTablePermission::getCreateTime);
        var result = tablePermissionMapper.selectPage(
                com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(current, size), wrapper);
        return PageResult.of(current, size, result.getTotal(), result.getRecords());
    }

    public void save(SysTablePermission permission) {
        if (permission.getStatus() == null) {
            permission.setStatus(1);
        }
        tablePermissionMapper.insert(permission);
    }

    public void update(SysTablePermission permission) {
        tablePermissionMapper.updateById(permission);
    }

    public void delete(String id) {
        tablePermissionMapper.deleteById(id);
    }

    /**
     * 查询用户在指定数据源上拥有的表权限（供查询/集市等下游服务做执行前拦截）
     */
    public List<Map<String, Object>> getUserTablePermissions(String userId) {
        List<String> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        // 管理员的 USER 授权
        List<SysTablePermission> userPerms = tablePermissionMapper.selectList(
                new LambdaQueryWrapper<SysTablePermission>()
                        .eq(SysTablePermission::getUserId, userId)
                        .eq(SysTablePermission::getStatus, 1));
        // 角色的授权
        List<SysTablePermission> rolePerms = roleIds.isEmpty() ? List.of()
                : tablePermissionMapper.selectList(new LambdaQueryWrapper<SysTablePermission>()
                .in(SysTablePermission::getRoleId, roleIds)
                .eq(SysTablePermission::getStatus, 1));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysTablePermission p : concat(userPerms, rolePerms)) {
            result.add(Map.of(
                    "datasourceId", p.getDatasourceId() == null ? "" : p.getDatasourceId(),
                    "databaseName", p.getDatabaseName() == null ? "" : p.getDatabaseName(),
                    "tableName", p.getTableName() == null ? "" : p.getTableName(),
                    "permissionType", p.getPermissionType()));
        }
        return result;
    }

    /**
     * 校验用户对指定库表的操作权限（宽松模式：无策略则放行）
     */
    public boolean check(String userId, String datasourceId, String database, String table, String type) {
        List<String> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();

        List<SysTablePermission> list = new ArrayList<>();
        list.addAll(tablePermissionMapper.selectList(new LambdaQueryWrapper<SysTablePermission>()
                .eq(SysTablePermission::getUserId, userId)
                .eq(SysTablePermission::getStatus, 1)));
        if (!roleIds.isEmpty()) {
            list.addAll(tablePermissionMapper.selectList(new LambdaQueryWrapper<SysTablePermission>()
                    .in(SysTablePermission::getRoleId, roleIds)
                    .eq(SysTablePermission::getStatus, 1)));
        }
        if (list.isEmpty()) {
            return true; // 未配置策略，默认放行
        }
        String permissionType = type == null ? "SELECT" : type.toUpperCase();
        for (SysTablePermission p : list) {
            boolean dsMatch = StrUtil.isBlank(p.getDatasourceId()) || p.getDatasourceId().equals(datasourceId);
            boolean dbMatch = StrUtil.isBlank(p.getDatabaseName()) || p.getDatabaseName().equals(database);
            boolean tbMatch = StrUtil.isBlank(p.getTableName()) || p.getTableName().equals(table);
            if (dsMatch && dbMatch && tbMatch) {
                String granted = p.getPermissionType() == null ? "" : p.getPermissionType().toUpperCase();
                if ("ALL".equals(granted) || granted.equals(permissionType)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<SysTablePermission> concat(List<SysTablePermission> a, List<SysTablePermission> b) {
        List<SysTablePermission> list = new ArrayList<>(a);
        list.addAll(b);
        return list;
    }
}
