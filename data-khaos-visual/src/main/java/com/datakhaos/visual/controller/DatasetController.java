package com.datakhaos.visual.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datakhaos.common.model.R;
import com.datakhaos.visual.dto.DatasetDto;
import com.datakhaos.visual.entity.VisualDataset;
import com.datakhaos.visual.service.DatasetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 数据集管理 API
 */
@RestController
@RequestMapping("/api/visual/dataset")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;

    /**
     * 分页查询数据集列表
     */
    @GetMapping("/page")
    public R<Page<DatasetDto>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String datasetType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orgId) {
        Page<VisualDataset> page = new Page<>(current, size);
        datasetService.page(page, keyword, datasetType, status, orgId);

        Page<DatasetDto> result = new Page<>(current, size, page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toDto).toList());
        return R.ok(result);
    }

    @GetMapping("/{id}")
    public R<DatasetDto> getById(@PathVariable String id) {
        return R.ok(toDto(datasetService.getById(id)));
    }

    @PostMapping
    public R<String> create(@RequestBody DatasetDto dto) {
        return R.ok(datasetService.create(dto));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody DatasetDto dto) {
        datasetService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        datasetService.delete(id);
        return R.ok();
    }

    /**
     * 发布数据集
     */
    @PostMapping("/{id}/publish")
    public R<Integer> publish(@PathVariable String id, @RequestParam(required = false) String remark) {
        return R.ok(datasetService.publish(id, remark));
    }

    /**
     * 下线数据集
     */
    @PostMapping("/{id}/unpublish")
    public R<Void> unpublish(@PathVariable String id) {
        datasetService.unpublish(id);
        return R.ok();
    }

    /**
     * 测试SQL查询(返回字段信息和前100条数据)
     */
    @PostMapping("/preview")
    public R<DatasetDto.DatasetPreviewResult> preview(@RequestBody DatasetDto dto) {
        return R.ok(datasetService.preview(dto.getDatasourceId(), dto.getQuerySql()));
    }

    /**
     * 提取模型字段
     */
    @GetMapping("/extract-fields")
    public R<java.util.List<DatasetDto.DatasetFieldDto>> extractFields(@RequestParam String modelId) {
        return R.ok(datasetService.extractFieldsFromModel(modelId));
    }

    /**
     * 已发布数据集列表（图表绘制页资产池，含字段定义与数据源类型）
     */
    @GetMapping("/list")
    public R<java.util.List<DatasetDto>> listPublished() {
        return R.ok(datasetService.listPublished());
    }

    /**
     * 图表聚合查询：数据集 + 维度/指标/筛选/排序 → 生成 SQL 并执行
     */
    @PostMapping("/query")
    public R<DatasetDto.DatasetChartQueryResult> queryChart(@RequestBody com.datakhaos.visual.dto.DatasetChartQueryRequest request) {
        return R.ok(datasetService.queryChart(request));
    }

    private DatasetDto toDto(VisualDataset entity) {
        return datasetService.toDto(entity);
    }
}
