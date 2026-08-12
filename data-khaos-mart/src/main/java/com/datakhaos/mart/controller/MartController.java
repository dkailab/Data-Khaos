package com.datakhaos.mart.controller;

import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.model.R;
import com.datakhaos.datasource.api.model.QueryResult;
import com.datakhaos.mart.entity.MartDimLevel;
import com.datakhaos.mart.entity.MartDimension;
import com.datakhaos.mart.entity.MartMetric;
import com.datakhaos.mart.entity.MartModel;
import com.datakhaos.mart.entity.MartModelRel;
import com.datakhaos.mart.service.MartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据集市接口
 */
@Tag(name = "数据集市")
@RestController
@RequestMapping("/api/mart")
@RequiredArgsConstructor
public class MartController {

    private final MartService martService;

    // ==================== 模型 ====================

    @Operation(summary = "分页查询模型")
    @GetMapping("/model/page")
    public R<PageResult<MartModel>> modelPage(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer status) {
        return R.ok(martService.modelPage(current, size, keyword, status));
    }

    @Operation(summary = "模型详情（含指标/维度/关联）")
    @GetMapping("/model/{id}")
    public R<Map<String, Object>> modelDetail(@PathVariable String id) {
        return R.ok(martService.modelDetail(id));
    }

    @Operation(summary = "新增模型")
    @PostMapping("/model")
    public R<Void> createModel(@RequestBody MartModel model) {
        martService.createModel(model);
        return R.ok();
    }

    @Operation(summary = "修改模型")
    @PutMapping("/model")
    public R<Void> updateModel(@RequestBody MartModel model) {
        martService.updateModel(model);
        return R.ok();
    }

    @Operation(summary = "删除模型（级联删除指标/维度/关联）")
    @DeleteMapping("/model/{id}")
    public R<Void> deleteModel(@PathVariable String id) {
        martService.deleteModel(id);
        return R.ok();
    }

    @Operation(summary = "发布模型")
    @PostMapping("/model/{id}/publish")
    public R<Void> publish(@PathVariable String id) {
        martService.publish(id);
        return R.ok();
    }

    @Operation(summary = "下线模型")
    @PostMapping("/model/{id}/offline")
    public R<Void> offline(@PathVariable String id) {
        martService.offline(id);
        return R.ok();
    }

    @Operation(summary = "预览模型数据（事实表前 100 行）")
    @GetMapping("/model/{id}/preview")
    public R<QueryResult> preview(@PathVariable String id) {
        return R.ok(martService.preview(id));
    }

    // ==================== 指标 ====================

    @Operation(summary = "分页查询模型指标")
    @GetMapping("/metric/page")
    public R<PageResult<MartMetric>> metricPage(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String modelId,
                                                @RequestParam(required = false) String keyword) {
        return R.ok(martService.metricPage(current, size, modelId, keyword));
    }

    @Operation(summary = "新增指标")
    @PostMapping("/metric")
    public R<Void> createMetric(@RequestBody MartMetric metric) {
        martService.createMetric(metric);
        return R.ok();
    }

    @Operation(summary = "修改指标")
    @PutMapping("/metric")
    public R<Void> updateMetric(@RequestBody MartMetric metric) {
        martService.updateMetric(metric);
        return R.ok();
    }

    @Operation(summary = "删除指标")
    @DeleteMapping("/metric/{id}")
    public R<Void> deleteMetric(@PathVariable String id) {
        martService.deleteMetric(id);
        return R.ok();
    }

    // ==================== 维度 ====================

    @Operation(summary = "分页查询模型维度")
    @GetMapping("/dimension/page")
    public R<PageResult<MartDimension>> dimensionPage(@RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) String modelId) {
        return R.ok(martService.dimensionPage(current, size, modelId));
    }

    @Operation(summary = "新增维度")
    @PostMapping("/dimension")
    public R<Void> createDimension(@RequestBody MartDimension dimension) {
        martService.createDimension(dimension);
        return R.ok();
    }

    @Operation(summary = "修改维度")
    @PutMapping("/dimension")
    public R<Void> updateDimension(@RequestBody MartDimension dimension) {
        martService.updateDimension(dimension);
        return R.ok();
    }

    @Operation(summary = "删除维度（级联删除层级）")
    @DeleteMapping("/dimension/{id}")
    public R<Void> deleteDimension(@PathVariable String id) {
        martService.deleteDimension(id);
        return R.ok();
    }

    @Operation(summary = "维度层级列表")
    @GetMapping("/dimension/{dimId}/levels")
    public R<List<MartDimLevel>> levels(@PathVariable String dimId) {
        return R.ok(martService.levels(dimId));
    }

    @Operation(summary = "保存维度层级（全量替换）")
    @PostMapping("/dimension/{dimId}/levels")
    public R<Void> saveLevels(@PathVariable String dimId, @RequestBody List<MartDimLevel> levels) {
        martService.saveLevels(dimId, levels);
        return R.ok();
    }

    // ==================== 关联关系 ====================

    @Operation(summary = "模型关联关系列表")
    @GetMapping("/model/{modelId}/rel")
    public R<List<MartModelRel>> rels(@PathVariable String modelId) {
        return R.ok(martService.rels(modelId));
    }

    @Operation(summary = "保存模型关联关系")
    @PostMapping("/model/{modelId}/rel")
    public R<Void> saveRel(@PathVariable String modelId, @RequestBody MartModelRel rel) {
        martService.saveRel(modelId, rel);
        return R.ok();
    }

    @Operation(summary = "删除模型关联关系")
    @DeleteMapping("/rel/{id}")
    public R<Void> deleteRel(@PathVariable String id) {
        martService.deleteRel(id);
        return R.ok();
    }
}
