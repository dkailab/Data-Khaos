package com.datakhaos.permission.controller;

import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.model.R;
import com.datakhaos.permission.entity.SysOrganization;
import com.datakhaos.permission.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织架构管理
 */
@Tag(name = "组织架构")
@RestController
@RequestMapping("/api/permission/org")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService orgService;

    @Operation(summary = "分页查询组织")
    @GetMapping("/page")
    public R<PageResult<SysOrganization>> page(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "20") long size,
                                               @RequestParam(required = false) String orgName) {
        return R.ok(orgService.page(current, size, orgName));
    }

    @Operation(summary = "查询全部组织")
    @GetMapping("/list")
    public R<List<SysOrganization>> list() {
        return R.ok(orgService.list());
    }

    @Operation(summary = "查询组织树")
    @GetMapping("/tree")
    public R<List<java.util.Map<String, Object>>> tree() {
        return R.ok(orgService.tree());
    }

    @Operation(summary = "查询组织成员")
    @GetMapping("/{id}/users")
    public R<List<java.util.Map<String, Object>>> orgUsers(@PathVariable String id) {
        return R.ok(orgService.listOrgUsers(id));
    }

    @Operation(summary = "设置组织成员（全量替换）")
    @PutMapping("/{id}/users")
    public R<Void> assignOrgUsers(@PathVariable String id, @RequestBody List<String> userIds) {
        orgService.assignOrgUsers(id, userIds);
        return R.ok();
    }

    @Operation(summary = "查询部门已授予的菜单权限")
    @GetMapping("/{id}/permissions")
    public R<List<String>> orgPermissions(@PathVariable String id) {
        return R.ok(orgService.getOrgPermissionIds(id));
    }

    @Operation(summary = "授予部门菜单权限（全量替换）")
    @PutMapping("/{id}/permissions")
    public R<Void> assignOrgPermissions(@PathVariable String id, @RequestBody List<String> menuIds) {
        orgService.assignOrgPermissions(id, menuIds);
        return R.ok();
    }

    @Operation(summary = "新增组织")
    @PostMapping
    public R<Void> create(@RequestBody SysOrganization org) {
        orgService.save(org);
        return R.ok();
    }

    @Operation(summary = "更新组织")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody SysOrganization org) {
        org.setId(id);
        orgService.update(org);
        return R.ok();
    }

    @Operation(summary = "删除组织")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        orgService.delete(id);
        return R.ok();
    }
}
