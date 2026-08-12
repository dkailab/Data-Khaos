package com.datakhaos.visual.controller;

import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.model.R;
import com.datakhaos.datasource.api.model.QueryResult;
import com.datakhaos.visual.dto.AdhocQueryRequest;
import com.datakhaos.visual.dto.DrillRequest;
import com.datakhaos.visual.dto.PublishRequest;
import com.datakhaos.visual.entity.VisualBoard;
import com.datakhaos.visual.entity.VisualDashboard;
import com.datakhaos.visual.entity.VisualDashboardItem;
import com.datakhaos.visual.entity.VisualDashboardVersion;
import com.datakhaos.visual.service.VisualService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 可视化引擎接口（仪表板 + 分析板）
 */
@Tag(name = "可视化引擎")
@RestController
@RequestMapping("/api/visual")
@RequiredArgsConstructor
public class VisualController {

    private final VisualService visualService;

    // ==================== 仪表板 ====================

    @Operation(summary = "分页查询仪表板")
    @GetMapping("/dashboard/page")
    public R<PageResult<VisualDashboard>> dashboardPage(@RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) String keyword) {
        return R.ok(visualService.dashboardPage(current, size, keyword));
    }

    @Operation(summary = "仪表板详情")
    @GetMapping("/dashboard/{id}")
    public R<VisualDashboard> dashboard(@PathVariable String id) {
        return R.ok(visualService.getDashboard(id));
    }

    @Operation(summary = "新增仪表板")
    @PostMapping("/dashboard")
    public R<Void> createDashboard(@RequestBody VisualDashboard dashboard) {
        visualService.createDashboard(dashboard);
        return R.ok();
    }

    @Operation(summary = "修改仪表板")
    @PutMapping("/dashboard")
    public R<Void> updateDashboard(@RequestBody VisualDashboard dashboard) {
        visualService.updateDashboard(dashboard);
        return R.ok();
    }

    @Operation(summary = "删除仪表板（级联删除组件）")
    @DeleteMapping("/dashboard/{id}")
    public R<Void> deleteDashboard(@PathVariable String id) {
        visualService.deleteDashboard(id);
        return R.ok();
    }

    // ==================== 版本控制 ====================

    @Operation(summary = "上线仪表板（生成版本快照）")
    @PostMapping("/dashboard/{id}/publish")
    public R<Integer> publish(@PathVariable String id, @RequestBody(required = false) PublishRequest request) {
        return R.ok(visualService.publish(id, request == null ? null : request.getRemark()));
    }

    @Operation(summary = "下线仪表板")
    @PostMapping("/dashboard/{id}/unpublish")
    public R<Void> unpublish(@PathVariable String id) {
        visualService.unpublish(id);
        return R.ok();
    }

    @Operation(summary = "版本列表")
    @GetMapping("/dashboard/{id}/versions")
    public R<List<VisualDashboardVersion>> versions(@PathVariable String id) {
        return R.ok(visualService.versionList(id));
    }

    @Operation(summary = "版本快照详情")
    @GetMapping("/version/{versionId}")
    public R<VisualDashboardVersion> version(@PathVariable String versionId) {
        return R.ok(visualService.versionDetail(versionId));
    }

    @Operation(summary = "回滚到指定版本")
    @PostMapping("/dashboard/{id}/rollback/{versionId}")
    public R<Void> rollback(@PathVariable String id, @PathVariable String versionId) {
        visualService.rollback(id, versionId);
        return R.ok();
    }

    // ==================== 组件 ====================

    @Operation(summary = "仪表板组件列表")
    @GetMapping("/dashboard/{dashboardId}/items")
    public R<List<VisualDashboardItem>> items(@PathVariable String dashboardId) {
        return R.ok(visualService.items(dashboardId));
    }

    @Operation(summary = "新增/修改组件（有 id 则更新）")
    @PostMapping("/item")
    public R<Void> saveItem(@RequestBody VisualDashboardItem item) {
        visualService.saveItem(item);
        return R.ok();
    }

    @Operation(summary = "删除组件")
    @DeleteMapping("/item/{id}")
    public R<Void> deleteItem(@PathVariable String id) {
        visualService.deleteItem(id);
        return R.ok();
    }

    @Operation(summary = "执行组件查询（可传分析板独立筛选）")
    @PostMapping("/item/{id}/execute")
    public R<QueryResult> executeItem(@PathVariable String id,
                                      @RequestParam(required = false) String filters) {
        return R.ok(visualService.executeItem(id, filters));
    }

    @Operation(summary = "组件下钻查询（点击图表数据点，按维度列=值下钻）")
    @PostMapping("/item/{id}/drill")
    public R<QueryResult> drillItem(@PathVariable String id, @RequestBody DrillRequest request) {
        return R.ok(visualService.drillItem(id, request.getColumn(), request.getValue(), request.getFilters()));
    }

    // ==================== 分析板 ====================

    @Operation(summary = "分析板列表")
    @GetMapping("/board/{dashboardId}")
    public R<List<VisualBoard>> boards(@PathVariable String dashboardId) {
        return R.ok(visualService.boards(dashboardId));
    }

    @Operation(summary = "新增分析板")
    @PostMapping("/board")
    public R<Void> createBoard(@RequestBody VisualBoard board) {
        visualService.createBoard(board);
        return R.ok();
    }

    @Operation(summary = "修改分析板")
    @PutMapping("/board")
    public R<Void> updateBoard(@RequestBody VisualBoard board) {
        visualService.updateBoard(board);
        return R.ok();
    }

    @Operation(summary = "删除分析板（级联删除组件）")
    @DeleteMapping("/board/{id}")
    public R<Void> deleteBoard(@PathVariable String id) {
        visualService.deleteBoard(id);
        return R.ok();
    }

    @Operation(summary = "复制分析板（含组件）")
    @PostMapping("/board/{id}/duplicate")
    public R<String> duplicateBoard(@PathVariable String id) {
        return R.ok(visualService.duplicateBoard(id));
    }

    // ==================== 即席分析查询 ====================

    @Operation(summary = "即席分析查询")
    @PostMapping("/analysis/execute")
    public R<QueryResult> adhoc(@RequestBody AdhocQueryRequest request) {
        return R.ok(visualService.executeAdhoc(request.getDatasourceId(), request.getSql()));
    }
}
