package com.datakhaos.permission.controller;

import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.model.R;
import com.datakhaos.permission.api.model.ProjectGroupDto;
import com.datakhaos.permission.entity.SgProjectGroup;
import com.datakhaos.permission.entity.SgProjectGroupResource;
import com.datakhaos.permission.entity.SgProjectRole;
import com.datakhaos.permission.service.ProjectGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 项目组权限管理：组织(业务线) → 项目组 → 人 三级模型。
 * 人加入项目组即获得组内角色能力位（操作权限）与组下资源（数据权限）。
 */
@Tag(name = "项目组权限")
@RestController
@RequestMapping("/api/permission/project-group")
@RequiredArgsConstructor
public class ProjectGroupController {

    private final ProjectGroupService projectGroupService;

    // ---------- 项目组 ----------

    @Operation(summary = "分页查询项目组")
    @GetMapping("/page")
    public R<PageResult<SgProjectGroup>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String orgId,
                                              @RequestParam(required = false) String keyword) {
        return R.ok(projectGroupService.page(current, size, orgId, keyword));
    }

    @Operation(summary = "查询组织下全部项目组")
    @GetMapping("/list")
    public R<List<SgProjectGroup>> list(@RequestParam(required = false) String orgId) {
        return R.ok(projectGroupService.list(orgId));
    }

    @Operation(summary = "新增项目组")
    @PostMapping
    public R<Void> create(@RequestBody SgProjectGroup group) {
        projectGroupService.save(group);
        return R.ok();
    }

    @Operation(summary = "更新项目组")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody SgProjectGroup group) {
        group.setId(id);
        projectGroupService.update(group);
        return R.ok();
    }

    @Operation(summary = "删除项目组")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        projectGroupService.delete(id);
        return R.ok();
    }

    // ---------- 成员 ----------

    @Operation(summary = "查询项目组成员（含组内角色与能力位）")
    @GetMapping("/{id}/members")
    public R<List<Map<String, Object>>> members(@PathVariable String id) {
        return R.ok(projectGroupService.listMembers(id));
    }

    @Operation(summary = "设置项目组成员（全量替换）")
    @PutMapping("/{id}/members")
    public R<Void> assignMembers(@PathVariable String id,
                                 @RequestBody List<ProjectGroupService.MemberAssign> assigns) {
        projectGroupService.assignMembers(id, assigns);
        return R.ok();
    }

    // ---------- 角色/能力位 ----------

    @Operation(summary = "分页查询项目组角色")
    @GetMapping("/{id}/roles/page")
    public R<PageResult<SgProjectRole>> rolePage(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @PathVariable String id) {
        return R.ok(projectGroupService.rolePage(current, size, id));
    }

    @Operation(summary = "新增项目组角色")
    @PostMapping("/role")
    public R<Void> createRole(@RequestBody SgProjectRole role) {
        projectGroupService.saveRole(role);
        return R.ok();
    }

    @Operation(summary = "更新项目组角色")
    @PutMapping("/role/{id}")
    public R<Void> updateRole(@PathVariable String id, @RequestBody SgProjectRole role) {
        role.setId(id);
        projectGroupService.updateRole(role);
        return R.ok();
    }

    @Operation(summary = "删除项目组角色")
    @DeleteMapping("/role/{id}")
    public R<Void> deleteRole(@PathVariable String id) {
        projectGroupService.deleteRole(id);
        return R.ok();
    }

    // ---------- 资源 ----------

    @Operation(summary = "查询项目组资源")
    @GetMapping("/{id}/resources")
    public R<List<SgProjectGroupResource>> resources(@PathVariable String id) {
        return R.ok(projectGroupService.listResources(id));
    }

    @Operation(summary = "绑定项目组资源")
    @PostMapping("/{id}/resources")
    public R<Void> bindResources(@PathVariable String id,
                                 @RequestBody List<SgProjectGroupResource> resources) {
        projectGroupService.bindResources(id, resources);
        return R.ok();
    }

    @Operation(summary = "删除项目组资源")
    @DeleteMapping("/resource/{id}")
    public R<Void> deleteResource(@PathVariable String id) {
        projectGroupService.deleteResource(id);
        return R.ok();
    }

    // ---------- 权限下发 ----------

    @Operation(summary = "查询用户加入的项目组（含角色与能力位）")
    @GetMapping("/user/{userId}")
    public R<List<ProjectGroupDto>> userGroups(@PathVariable String userId) {
        return R.ok(projectGroupService.getUserProjectGroups(userId));
    }

    @Operation(summary = "查询用户当前项目组（主组）")
    @GetMapping("/user/{userId}/current")
    public R<ProjectGroupDto> currentGroup(@PathVariable String userId) {
        return R.ok(projectGroupService.getCurrentProjectGroup(userId));
    }

    @Operation(summary = "查询用户在项目组内的能力位")
    @GetMapping("/user/{userId}/capability")
    public R<List<String>> userCapability(@PathVariable String userId,
                                          @RequestParam(required = false) String projectGroupId) {
        return R.ok(projectGroupService.getCapabilityFlags(userId, projectGroupId));
    }
}