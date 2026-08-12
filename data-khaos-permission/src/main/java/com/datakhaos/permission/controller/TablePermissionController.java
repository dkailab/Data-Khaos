package com.datakhaos.permission.controller;

import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.model.R;
import com.datakhaos.permission.entity.SysTablePermission;
import com.datakhaos.permission.service.TablePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 表级权限管理
 */
@Tag(name = "表权限")
@RestController
@RequestMapping("/api/permission/table")
@RequiredArgsConstructor
public class TablePermissionController {

    private final TablePermissionService tablePermissionService;

    @Operation(summary = "分页查询表权限")
    @GetMapping("/page")
    public R<PageResult<SysTablePermission>> page(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String tableName) {
        return R.ok(tablePermissionService.page(current, size, tableName));
    }

    @Operation(summary = "新增表权限")
    @PostMapping
    public R<Void> save(@RequestBody SysTablePermission permission) {
        tablePermissionService.save(permission);
        return R.ok();
    }

    @Operation(summary = "更新表权限")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody SysTablePermission permission) {
        permission.setId(id);
        tablePermissionService.update(permission);
        return R.ok();
    }

    @Operation(summary = "删除表权限")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        tablePermissionService.delete(id);
        return R.ok();
    }

    @Operation(summary = "查询用户在指定数据源上的表权限")
    @GetMapping("/user/{userId}")
    public R<List<Map<String, Object>>> userTablePermissions(@PathVariable String userId) {
        return R.ok(tablePermissionService.getUserTablePermissions(userId));
    }

    @Operation(summary = "校验用户对库表的操作权限")
    @PostMapping("/check")
    public R<Boolean> check(@RequestBody Map<String, String> body) {
        return R.ok(tablePermissionService.check(
                body.get("userId"),
                body.get("datasourceId"),
                body.get("databaseName"),
                body.get("tableName"),
                body.get("permissionType")));
    }
}
