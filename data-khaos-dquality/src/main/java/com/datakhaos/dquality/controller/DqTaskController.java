package com.datakhaos.dquality.controller;

import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.model.R;
import com.datakhaos.dquality.dto.ScheduleJobBrief;
import com.datakhaos.dquality.entity.DqSnapshot;
import com.datakhaos.dquality.entity.DqTask;
import com.datakhaos.dquality.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 质量任务接口
 */
@Tag(name = "数据质量-任务")
@RestController
@RequestMapping("/api/dquality/task")
@RequiredArgsConstructor
public class DqTaskController {

    private final TaskService taskService;

    @Operation(summary = "任务分页")
    @GetMapping("/page")
    public R<PageResult<DqTask>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status) {
        return R.ok(taskService.page(current, size, keyword, status));
    }

    @Operation(summary = "质量任务关联的调度任务映射（taskId -> 调度任务列表）")
    @GetMapping("/schedule-map")
    public R<Map<String, List<ScheduleJobBrief>>> scheduleMap() {
        return R.ok(taskService.scheduleMap());
    }

    @Operation(summary = "创建任务")
    @PostMapping
    public R<Void> create(@RequestBody DqTask task) {
        taskService.create(task);
        return R.ok();
    }

    @Operation(summary = "更新任务")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody DqTask task) {
        taskService.update(id, task);
        return R.ok();
    }

    @Operation(summary = "删除任务")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        taskService.delete(id);
        return R.ok();
    }

    @Operation(summary = "启用/停用任务")
    @PostMapping("/{id}/enable")
    public R<Void> enable(@PathVariable String id, @RequestParam boolean enabled) {
        taskService.setEnabled(id, enabled);
        return R.ok();
    }

    @Operation(summary = "手动执行任务")
    @PostMapping("/{id}/run")
    public R<DqSnapshot> run(@PathVariable String id) {
        return R.ok(taskService.run(id));
    }
}