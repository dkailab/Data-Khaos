package com.datakhaos.pipeline.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.model.PageResult;
import com.datakhaos.pipeline.entity.PipelineWorker;
import com.datakhaos.pipeline.mapper.PipelineWorkerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 管道 worker 注册服务（可扩展执行节点）
 */
@Service
@RequiredArgsConstructor
public class PipelineWorkerService {

    private final PipelineWorkerMapper mapper;

    public PageResult<PipelineWorker> page(long current, long size) {
        Page<PipelineWorker> page = mapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<PipelineWorker>().orderByDesc(PipelineWorker::getCreateTime));
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(PipelineWorker worker) {
        if (StrUtil.isBlank(worker.getWorkerName())) {
            throw new BusinessException("worker 名称不能为空");
        }
        worker.setId(null);
        worker.setStatus(1);
        worker.setLastHeartbeat(LocalDateTime.now());
        if (StrUtil.isBlank(worker.getEngines())) {
            worker.setEngines("DB_SYNC");
        }
        mapper.insert(worker);
    }

    @Transactional(rollbackFor = Exception.class)
    public void heartbeat(String id) {
        PipelineWorker exist = mapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("worker 不存在: " + id);
        }
        PipelineWorker patch = new PipelineWorker();
        patch.setId(id);
        patch.setStatus(1);
        patch.setLastHeartbeat(LocalDateTime.now());
        mapper.updateById(patch);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        mapper.deleteById(id);
    }
}