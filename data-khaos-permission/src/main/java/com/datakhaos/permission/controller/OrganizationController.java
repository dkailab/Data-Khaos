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
