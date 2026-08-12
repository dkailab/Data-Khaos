package com.datakhaos.auth.controller;

import com.datakhaos.auth.entity.SysUser;
import com.datakhaos.auth.service.UserService;
import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理接口
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/auth/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public R<PageResult<SysUser>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String username,
                                       @RequestParam(required = false) String realName) {
        return R.ok(userService.page(current, size, username, realName));
    }

    @Operation(summary = "查询用户详情")
    @GetMapping("/{id}")
    public R<SysUser> getById(@PathVariable String id) {
        return R.ok(userService.getById(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public R<Void> create(@RequestBody SysUser user) {
        userService.create(user);
        return R.ok();
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody SysUser user) {
        user.setId(id);
        userService.update(user);
        return R.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        userService.delete(id);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable String id, @RequestBody java.util.Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return R.ok();
    }

    @Operation(summary = "分配角色")
    @PutMapping("/{id}/roles")
    public R<Void> assignRoles(@PathVariable String id, @RequestBody List<String> roleIds) {
        userService.assignRoles(id, roleIds);
        return R.ok();
    }
}
