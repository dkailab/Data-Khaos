package com.datakhaos.permission.controller;

import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.model.R;
import com.datakhaos.permission.entity.SysMenu;
import com.datakhaos.permission.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/api/permission/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "分页查询菜单")
    @GetMapping("/page")
    public R<PageResult<SysMenu>> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "20") long size,
                                       @RequestParam(required = false) String name) {
        return R.ok(menuService.page(current, size, name));
    }

    @Operation(summary = "查询全部菜单")
    @GetMapping("/list")
    public R<List<SysMenu>> list() {
        return R.ok(menuService.list());
    }

    @Operation(summary = "新增菜单")
    @PostMapping
    public R<Void> create(@RequestBody SysMenu menu) {
        menuService.save(menu);
        return R.ok();
    }

    @Operation(summary = "更新菜单")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody SysMenu menu) {
        menu.setId(id);
        menuService.update(menu);
        return R.ok();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        menuService.delete(id);
        return R.ok();
    }
}
