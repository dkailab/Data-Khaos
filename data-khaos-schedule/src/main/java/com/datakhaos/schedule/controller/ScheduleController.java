package com.datakhaos.schedule.controller;

import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.model.R;
import com.datakhaos.schedule.entity.ScheduleJob;
import com.datakhaos.schedule.entity.ScheduleJobDep;
import com.datakhaos.schedule.entity.ScheduleJobLog;
import com.datakhaos.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 调度系统接口
 */
@Tag(name = "调度系统")
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "分页查询任务")
    @GetMapping("/job/page")
    public R<PageResult<ScheduleJob>> jobPage(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String jobType,
                                              @RequestParam(required = false) Integer status) {
        return R.ok(scheduleService.jobPage(current, size, keyword, jobType, status));
    }

    @Operation(summary = "任务详情")
    @GetMapping("/job/{id}")
    public R<ScheduleJob> job(@PathVariable String id) {
        return R.ok(scheduleService.getJob(id));
    }

    @Operation(summary = "新增任务")
    @PostMapping("/job")
    public R<Void> createJob(@RequestBody ScheduleJob job) {
        scheduleService.createJob(job);
        return R.ok();
    }

    @Operation(summary = "修改任务")
    @PutMapping("/job")
    public R<Void> updateJob(@RequestBody ScheduleJob job) {
        scheduleService.updateJob(job);
        return R.ok();
    }

    @Operation(summary = "删除任务（级联删除日志与依赖）")
    @DeleteMapping("/job/{id}")
    public R<Void> deleteJob(@PathVariable String id) {
        scheduleService.deleteJob(id);
        return R.ok();
    }

    @Operation(summary = "启用任务")
    @PostMapping("/job/{id}/start")
    public R<Void> start(@PathVariable String id) {
        scheduleService.start(id);
        return R.ok();
    }

    @Operation(summary = "停用任务")
    @PostMapping("/job/{id}/stop")
    public R<Void> stop(@PathVariable String id) {
        scheduleService.stop(id);
        return R.ok();
    }

    @Operation(summary = "手动触发任务")
    @PostMapping("/job/{id}/run")
    public R<Void> run(@PathVariable String id) {
        scheduleService.runNow(id);
        return R.ok();
    }

    @Operation(summary = "分页查询执行日志")
    @GetMapping("/log/page")
    public R<PageResult<ScheduleJobLog>> logPage(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String jobId) {
        return R.ok(scheduleService.logPage(current, size, jobId));
    }

    @Operation(summary = "任务依赖列表")
    @GetMapping("/job/{jobId}/dep")
    public R<List<ScheduleJobDep>> deps(@PathVariable String jobId) {
        return R.ok(scheduleService.deps(jobId));
    }

    @Operation(summary = "新增任务依赖")
    @PostMapping("/job/{jobId}/dep")
    public R<Void> saveDep(@PathVariable String jobId, @RequestBody ScheduleJobDep dep) {
        scheduleService.saveDep(jobId, dep);
        return R.ok();
    }

    @Operation(summary = "删除任务依赖")
    @DeleteMapping("/dep/{id}")
    public R<Void> deleteDep(@PathVariable String id) {
        scheduleService.deleteDep(id);
        return R.ok();
    }
}
