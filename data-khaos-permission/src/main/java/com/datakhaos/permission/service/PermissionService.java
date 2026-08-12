package com.datakhaos.permission.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.permission.api.model.MenuDto;
import com.datakhaos.permission.api.model.UserPermissionDto;
import com.datakhaos.permission.entity.SysMenu;
import com.datakhaos.permission.entity.SysRole;
import com.datakhaos.permission.entity.SysRolePermission;
import com.datakhaos.permission.entity.SysUserRole;
import com.datakhaos.permission.mapper.SysMenuMapper;
import com.datakhaos.permission.mapper.SysRoleMapper;
import com.datakhaos.permission.mapper.SysRolePermissionMapper;
import com.datakhaos.permission.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 权限核心服务：用户权限视图、角色权限绑定
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String DEFAULT_ADMIN_ID = "1";

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysMenuMapper menuMapper;

    /**
     * 计算用户权限视图（角色 / 权限标识 / 可见菜单）
     */
    public UserPermissionDto getUserPermission(String userId) {
        UserPermissionDto dto = new UserPermissionDto();
        dto.setUserId(userId);

        List<String> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        List<SysRole> roles = roleIds.isEmpty() ? List.of() : roleMapper.selectBatchIds(roleIds);
        List<String> roleCodes = roles.stream().map(SysRole::getRoleCode).toList();
        dto.setRoles(roleCodes);

        boolean superAdmin = roleCodes.contains(SUPER_ADMIN) || DEFAULT_ADMIN_ID.equals(userId);

        List<SysMenu> menus = superAdmin ? allMenus()
                : menusByRoleIds(roleIds);

        dto.setMenus(menus.stream().map(this::toDto).toList());
        dto.setPermissions(menus.stream().map(SysMenu::getPermission)
                .filter(StrUtil::isNotBlank).distinct().toList());
        return dto;
    }

    /** 查询用户权限标识集合 */
    public List<String> getUserPermissions(String userId) {
        return getUserPermission(userId).getPermissions();
    }

    /**
     * 绑定角色权限（全量替换菜单权限）
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRolePermissions(String roleId, List<String> menuIds) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId));
        if (menuIds != null) {
            for (String menuId : menuIds) {
                if (menuMapper.selectById(menuId) == null) {
                    continue;
                }
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(menuId);
                rp.setPermissionType("MENU");
                rolePermissionMapper.insert(rp);
            }
        }
    }

    /** 查询角色已绑定的菜单ID */
    public List<String> getRolePermissionIds(String roleId) {
        return rolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, roleId)
                        .eq(SysRolePermission::getPermissionType, "MENU"))
                .stream().map(SysRolePermission::getPermissionId).toList();
    }

    private List<SysMenu> allMenus() {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, 1)
                .orderByAsc(SysMenu::getSortOrder));
    }

    private List<SysMenu> menusByRoleIds(List<String> roleIds) {
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<String> permissionIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>()
                                .in(SysRolePermission::getRoleId, roleIds)
                                .eq(SysRolePermission::getPermissionType, "MENU"))
                .stream().map(SysRolePermission::getPermissionId).distinct().toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return menuMapper.selectBatchIds(permissionIds).stream()
                .filter(m -> m.getStatus() != null && m.getStatus() == 1)
                .sorted((a, b) -> {
                    int sa = a.getSortOrder() == null ? 0 : a.getSortOrder();
                    int sb = b.getSortOrder() == null ? 0 : b.getSortOrder();
                    return Integer.compare(sa, sb);
                }).toList();
    }

    private MenuDto toDto(SysMenu menu) {
        MenuDto dto = new MenuDto();
        dto.setId(menu.getId());
        dto.setParentId(menu.getParentId());
        dto.setName(menu.getName());
        dto.setPath(menu.getPath());
        dto.setComponent(menu.getComponent());
        dto.setPermission(menu.getPermission());
        dto.setIcon(menu.getIcon());
        dto.setType(menu.getType());
        dto.setSortOrder(menu.getSortOrder());
        return dto;
    }
}
