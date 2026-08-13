package com.datakhaos.dquality.controller;

import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.model.R;
import com.datakhaos.dquality.api.model.DqOverviewDto;
import com.datakhaos.dquality.api.model.DqTrendDto;
import com.datakhaos.dquality.entity.DqSnapshot;
import com.datakhaos.dquality.service.SnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 质量快照 / 报告接口
 */
@Tag(name = "数据质量-快照")
@RestController
@RequestMapping("/api/dquality/snapshot")
@RequiredArgsConstructor
public class DqSnapshotController {

    private final SnapshotService snapshotService;

    @Operation(summary = "快照分页")
    @GetMapping("/page")
    public R<PageResult<DqSnapshot>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String taskId,
                                          @RequestParam(required = false) String tableName) {
        return R.ok(snapshotService.page(current, size, taskId, tableName, null));
    }

    @Operation(summary = "快照详情（含规则明细）")
    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable String id) {
        return R.ok(snapshotService.detail(id));
    }

    @Operation(summary = "评分趋势")
    @GetMapping("/trend")
    public R<List<DqTrendDto>> trend(@RequestParam(required = false) String tableName) {
        return R.ok(snapshotService.trend(tableName));
    }

    @Operation(summary = "总览")
    @GetMapping("/overview")
    public R<DqOverviewDto> overview() {
        return R.ok(snapshotService.overview());
    }

    @Operation(summary = "导出报告（CSV，UTF-8 BOM）")
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable String id) {
        String csv = snapshotService.exportCsv(id);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        String filename = "quality_report_" + id + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
    }
}