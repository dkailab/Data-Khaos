package com.datakhaos.auth.controller;

import com.datakhaos.auth.entity.SysRole;
import com.datakhaos.auth.service.RoleService;
import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理接口
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/auth/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "分页查询角色")
    @GetMapping("/page")
    public R<PageResult<SysRole>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String roleName) {
        return R.ok(roleService.page(current, size, roleName));
    }

    @Operation(summary = "查询全部启用角色")
    @GetMapping("/list")
    public R<List<SysRole>> list() {
        return R.ok(roleService.listAll());
    }

    @Operation(summary = "新增角色")
    @PostMapping
    public R<Void> create(@RequestBody SysRole role) {
        roleService.save(role);
        return R.ok();
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody SysRole role) {
        role.setId(id);
        roleService.update(role);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        roleService.delete(id);
        return R.ok();
    }
}
