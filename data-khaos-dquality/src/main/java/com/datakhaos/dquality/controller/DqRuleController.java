package com.datakhaos.dquality.controller;

import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.model.R;
import com.datakhaos.dquality.entity.DqRule;
import com.datakhaos.dquality.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 质量规则接口
 */
@Tag(name = "数据质量-规则")
@RestController
@RequestMapping("/api/dquality/rule")
@RequiredArgsConstructor
public class DqRuleController {

    private final RuleService ruleService;

    @Operation(summary = "规则分页")
    @GetMapping("/page")
    public R<PageResult<DqRule>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String ruleType,
                                      @RequestParam(required = false) Integer status) {
        return R.ok(ruleService.page(current, size, keyword, ruleType, status));
    }

    @Operation(summary = "规则详情")
    @GetMapping("/{id}")
    public R<DqRule> get(@PathVariable String id) {
        return R.ok(ruleService.get(id));
    }

    @Operation(summary = "创建规则")
    @PostMapping
    public R<Void> create(@RequestBody DqRule rule) {
        ruleService.create(rule);
        return R.ok();
    }

    @Operation(summary = "更新规则")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody DqRule rule) {
        ruleService.update(id, rule);
        return R.ok();
    }

    @Operation(summary = "删除规则")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        ruleService.delete(id);
        return R.ok();
    }

    @Operation(summary = "规则模板下拉")
    @GetMapping("/template/options")
    public R<List<Map<String, Object>>> templates() {
        return R.ok(ruleService.templateOptions());
    }
}