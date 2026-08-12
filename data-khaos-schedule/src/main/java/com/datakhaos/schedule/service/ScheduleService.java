package com.datakhaos.schedule.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.model.ResultCode;
import com.datakhaos.schedule.entity.ScheduleJob;
import com.datakhaos.schedule.entity.ScheduleJobDep;
import com.datakhaos.schedule.entity.ScheduleJobLog;
import com.datakhaos.schedule.mapper.ScheduleJobDepMapper;
import com.datakhaos.schedule.mapper.ScheduleJobLogMapper;
import com.datakhaos.schedule.mapper.ScheduleJobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 调度服务：任务 CRUD、启停、手动触发、执行日志与依赖管理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleJobMapper jobMapper;
    private final ScheduleJobLogMapper jobLogMapper;
    private final ScheduleJobDepMapper jobDepMapper;
    private final JobExecutor jobExecutor;
    private final @Qualifier("jobTaskExecutor") ThreadPoolTaskExecutor taskExecutor;

    // ---------- 任务定义 ----------

    public PageResult<ScheduleJob> jobPage(long current, long size, String keyword, String jobType, Integer status) {
        Page<ScheduleJob> page = jobMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<ScheduleJob>()
                        .like(StrUtil.isNotBlank(keyword), ScheduleJob::getJobName, keyword)
                        .eq(StrUtil.isNotBlank(jobType), ScheduleJob::getJobType, jobType)
                        .eq(status != null, ScheduleJob::getStatus, status)
                        .orderByDesc(ScheduleJob::getCreateTime));
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    public ScheduleJob getJob(String id) {
        ScheduleJob job = jobMapper.selectById(id);
        if (job == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在: " + id);
        }
        return job;
    }

    /** 启用的、配置了 cron 的任务（供调度器扫描） */
    public List<ScheduleJob> listEnabledWithCron() {
        return jobMapper.selectList(new LambdaQueryWrapper<ScheduleJob>()
                .eq(ScheduleJob::getStatus, 1)
                .isNotNull(ScheduleJob::getCronExpression)
                .ne(ScheduleJob::getCronExpression, "")
                .orderByAsc(ScheduleJob::getCreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public void createJob(ScheduleJob job) {
        validate(job);
        job.setStatus(job.getStatus() == null ? 0 : job.getStatus());
        job.setRetryCount(job.getRetryCount() == null ? 0 : job.getRetryCount());
        job.setRetryInterval(job.getRetryInterval() == null ? 60 : job.getRetryInterval());
        job.setTimeout(job.getTimeout() == null ? 3600 : job.getTimeout());
        jobMapper.insert(job);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateJob(ScheduleJob job) {
        if (StrUtil.isBlank(job.getId())) {
            throw new BusinessException("任务ID不能为空");
        }
        getJob(job.getId());
        jobMapper.updateById(job);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteJob(String id) {
        getJob(id);
        jobLogMapper.delete(new LambdaQueryWrapper<ScheduleJobLog>().eq(ScheduleJobLog::getJobId, id));
        jobDepMapper.delete(new LambdaQueryWrapper<ScheduleJobDep>().eq(ScheduleJobDep::getJobId, id));
        jobMapper.deleteById(id);
    }

    /** 启用 */
    @Transactional(rollbackFor = Exception.class)
    public void start(String id) {
        ScheduleJob job = getJob(id);
        job.setStatus(1);
        jobMapper.updateById(job);
    }

    /** 停用 */
    @Transactional(rollbackFor = Exception.class)
    public void stop(String id) {
        ScheduleJob job = getJob(id);
        job.setStatus(0);
        jobMapper.updateById(job);
    }

    /** 手动触发（异步执行） */
    public void runNow(String id) {
        ScheduleJob job = getJob(id);
        taskExecutor.execute(() -> jobExecutor.execute(job));
    }

    // ---------- 执行日志 ----------

    public PageResult<ScheduleJobLog> logPage(long current, long size, String jobId) {
        Page<ScheduleJobLog> page = jobLogMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<ScheduleJobLog>()
                        .eq(StrUtil.isNotBlank(jobId), ScheduleJobLog::getJobId, jobId)
                        .orderByDesc(ScheduleJobLog::getCreateTime));
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    // ---------- 依赖管理 ----------

    public List<ScheduleJobDep> deps(String jobId) {
        return jobDepMapper.selectList(new LambdaQueryWrapper<ScheduleJobDep>()
                .eq(ScheduleJobDep::getJobId, jobId)
                .orderByAsc(ScheduleJobDep::getCreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveDep(String jobId, ScheduleJobDep dep) {
        getJob(jobId);
        dep.setJobId(jobId);
        dep.setDepType(dep.getDepType() == null ? "HARD" : dep.getDepType());
        jobDepMapper.insert(dep);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDep(String id) {
        jobDepMapper.deleteById(id);
    }

    private void validate(ScheduleJob job) {
        if (StrUtil.isBlank(job.getJobName()) || StrUtil.isBlank(job.getJobType())) {
            throw new BusinessException("任务名称与类型不能为空");
        }
        if (StrUtil.isNotBlank(job.getCronExpression())) {
            try {
                org.springframework.scheduling.support.CronExpression.parse(job.getCronExpression());
            } catch (Exception e) {
                throw new BusinessException("Cron 表达式不合法: " + job.getCronExpression());
            }
        }
    }
}
