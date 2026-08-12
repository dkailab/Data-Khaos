package com.datakhaos.permission.controller;

import com.datakhaos.common.model.R;
import com.datakhaos.permission.api.model.UserPermissionDto;
import com.datakhaos.permission.entity.SysMenu;
import com.datakhaos.permission.service.MenuService;
import com.datakhaos.permission.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限视图与角色绑定接口
 */
@Tag(name = "权限视图")
@RestController
@RequestMapping("/api/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;
    private final MenuService menuService;

    @Operation(summary = "查询用户权限视图（角色/权限/菜单）")
    @GetMapping("/user/{userId}")
    public R<UserPermissionDto> getUserPermission(@PathVariable String userId) {
        return R.ok(permissionService.getUserPermission(userId));
    }

    @Operation(summary = "查询用户权限标识集合")
    @GetMapping("/user/{userId}/permissions")
    public R<List<String>> getUserPermissions(@PathVariable String userId) {
        return R.ok(permissionService.getUserPermissions(userId));
    }

    @Operation(summary = "全部菜单")
    @GetMapping("/menu/all")
    public R<List<SysMenu>> menus() {
        return R.ok(menuService.list());
    }

    @Operation(summary = "绑定角色菜单权限")
    @PostMapping("/role/{roleId}/permissions")
    public R<Void> assignRolePermissions(@PathVariable String roleId, @RequestBody List<String> menuIds) {
        permissionService.assignRolePermissions(roleId, menuIds);
        return R.ok();
    }

    @Operation(summary = "查询角色已绑定的菜单权限")
    @GetMapping("/role/{roleId}/permissions")
    public R<List<String>> getRolePermissions(@PathVariable String roleId) {
        return R.ok(permissionService.getRolePermissionIds(roleId));
    }
}
