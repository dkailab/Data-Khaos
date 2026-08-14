package com.datakhaos.pipeline.controller;

import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.model.R;
import com.datakhaos.pipeline.entity.PipelineWorker;
import com.datakhaos.pipeline.service.PipelineWorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管道 worker 注册接口（可扩展执行节点）
 */
@Tag(name = "数据管道Worker")
@RestController
@RequestMapping("/api/pipeline/worker")
@RequiredArgsConstructor
public class PipelineWorkerController {

    private final PipelineWorkerService workerService;

    @Operation(summary = "分页查询 worker")
    @GetMapping("/page")
    public R<PageResult<PipelineWorker>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size) {
        return R.ok(workerService.page(current, size));
    }

    @Operation(summary = "注册 worker")
    @PostMapping
    public R<Void> register(@RequestBody PipelineWorker worker) {
        workerService.register(worker);
        return R.ok();
    }

    @Operation(summary = "心跳")
    @PutMapping("/{id}/heartbeat")
    public R<Void> heartbeat(@PathVariable String id) {
        workerService.heartbeat(id);
        return R.ok();
    }

    @Operation(summary = "删除 worker")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        workerService.delete(id);
        return R.ok();
    }
}