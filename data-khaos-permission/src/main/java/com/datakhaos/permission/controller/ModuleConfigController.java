package com.datakhaos.permission.controller;

import com.datakhaos.common.model.R;
import com.datakhaos.permission.entity.ModuleDisplayConfig;
import com.datakhaos.permission.service.ModuleConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 门户模块展示配置（可插拔模块）
 * 普通用户可读可见模块清单；管理员（module:config）可批量配置全局展示。
 */
@Tag(name = "门户模块配置")
@RestController
@RequestMapping("/api/permission/module-config")
@RequiredArgsConstructor
public class ModuleConfigController {

    private final ModuleConfigService moduleConfigService;

    @Operation(summary = "全部模块配置（含必须标识与可见性）")
    @GetMapping("/list")
    public R<List<ModuleDisplayConfig>> list() {
        return R.ok(moduleConfigService.list());
    }

    @Operation(summary = "仅当前展示模块清单")
    @GetMapping("/visible")
    public R<List<ModuleDisplayConfig>> visible() {
        return R.ok(moduleConfigService.visibleList());
    }

    @Operation(summary = "批量保存模块可见性（需 module:config 能力位）")
    @PutMapping
    public R<Void> batchUpdate(@RequestBody List<ModuleDisplayConfig> updates) {
        moduleConfigService.batchUpdate(updates);
        return R.ok();
    }
}