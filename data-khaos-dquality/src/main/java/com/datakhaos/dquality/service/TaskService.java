package com.datakhaos.dquality.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.security.MetadataHolder;
import com.datakhaos.dquality.entity.DqRule;
import com.datakhaos.dquality.entity.DqRuleResult;
import com.datakhaos.dquality.entity.DqSnapshot;
import com.datakhaos.dquality.entity.DqTask;
import com.datakhaos.dquality.mapper.DqRuleMapper;
import com.datakhaos.dquality.mapper.DqRuleResultMapper;
import com.datakhaos.dquality.mapper.DqSnapshotMapper;
import com.datakhaos.dquality.mapper.DqTaskMapper;
import com.datakhaos.dquality.service.DqAuthContext.AuthContext;
import com.datakhaos.dquality.service.QualityEngine.EngineResult;
import com.datakhaos.permission.api.service.PermissionConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 质量任务服务：CRUD + 手动执行（调用稽核引擎生成快照）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final DqTaskMapper taskMapper;
    private final DqRuleMapper ruleMapper;
    private final DqSnapshotMapper snapshotMapper;
    private final DqRuleResultMapper ruleResultMapper;
    private final QualityEngine engine;
    private final DqAuthContext auth;
    private final com.datakhaos.dquality.client.NotificationApiClient notificationClient;

    public PageResult<DqTask> page(long current, long size, String keyword, Integer status) {
        AuthContext ctx = auth.current();
        auth.requireCap(ctx, PermissionConstants.CAP_QUALITY_BROWSE);
        Page<DqTask> page = taskMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<DqTask>()
                        .eq(!ctx.superAdmin() && StrUtil.isNotBlank(ctx.projectGroupId()),
                                DqTask::getProjectGroupId, ctx.projectGroupId())
                        .eq(status != null, DqTask::getStatus, status)
                        .like(StrUtil.isNotBlank(keyword), DqTask::getTaskName, keyword)
                        .orderByDesc(DqTask::getCreateTime));
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    public void create(DqTask task) {
        AuthContext ctx = auth.current();
        auth.requireCap(ctx, PermissionConstants.CAP_QUALITY_MANAGE);
        if (StrUtil.isBlank(task.getTaskName())) {
            throw new BusinessException("任务名称不能为空");
        }
        if (StrUtil.isBlank(task.getRuleIds())) {
            throw new BusinessException("请至少绑定一个规则");
        }
        if (task.getStatus() == null) {
            task.setStatus(1);
        }
        task.setId(null);
        task.setProjectGroupId(ctx.superAdmin() ? task.getProjectGroupId() : ctx.projectGroupId());
        task.setCreateBy(MetadataHolder.getUserId());
        taskMapper.insert(task);
    }

    public void update(String id, DqTask task) {
        AuthContext ctx = auth.current();
        auth.requireCap(ctx, PermissionConstants.CAP_QUALITY_MANAGE);
        DqTask exist = taskMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("任务不存在");
        }
        auth.checkGroup(ctx, exist.getProjectGroupId(), "质量任务");
        task.setId(id);
        task.setProjectGroupId(exist.getProjectGroupId());
        task.setCreateBy(null);
        taskMapper.updateById(task);
    }

    public void delete(String id) {
        AuthContext ctx = auth.current();
        auth.requireCap(ctx, PermissionConstants.CAP_QUALITY_MANAGE);
        DqTask exist = taskMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("任务不存在");
        }
        auth.checkGroup(ctx, exist.getProjectGroupId(), "质量任务");
        taskMapper.deleteById(id);
    }

    public void setEnabled(String id, boolean enabled) {
        AuthContext ctx = auth.current();
        auth.requireCap(ctx, PermissionConstants.CAP_QUALITY_MANAGE);
        DqTask exist = taskMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("任务不存在");
        }
        auth.checkGroup(ctx, exist.getProjectGroupId(), "质量任务");
        DqTask upd = new DqTask();
        upd.setId(id);
        upd.setStatus(enabled ? 1 : 0);
        taskMapper.updateById(upd);
    }

    /**
     * 手动执行任务：校验权限后调用内部执行逻辑。
     */
    @Transactional(rollbackFor = Exception.class)
    public DqSnapshot run(String id) {
        AuthContext ctx = auth.current();
        auth.requireCap(ctx, PermissionConstants.CAP_QUALITY_RUN);
        DqTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        auth.checkGroup(ctx, task.getProjectGroupId(), "质量任务");
        return runInternal(task, "MANUAL");
    }

    /**
     * 内部执行任务（供调度系统 / 手动执行复用）：
     * 不做权限校验，由调用方保证已授权（如调度系统以系统身份触发）。
     * 执行完毕后若存在失败规则，则发送站内信告警。
     */
    @Transactional(rollbackFor = Exception.class)
    public DqSnapshot runInternal(String id, String triggerType) {
        DqTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        return runInternal(task, triggerType);
    }

    private DqSnapshot runInternal(DqTask task, String triggerType) {
        List<DqRule> rules = loadRules(task.getRuleIds());
        if (rules.isEmpty()) {
            throw new BusinessException("任务未绑定有效规则，无法执行");
        }

        EngineResult er = engine.execute(rules, MetadataHolder.getUserId(), triggerType);

        // 汇总同一表（取第一个规则的稽核对象）
        DqRule first = rules.get(0);
        DqSnapshot snapshot = new DqSnapshot();
        snapshot.setProjectGroupId(task.getProjectGroupId());
        snapshot.setTaskId(task.getId());
        snapshot.setTaskName(task.getTaskName());
        snapshot.setDatasourceId(first.getDatasourceId());
        snapshot.setDatabaseName(first.getDatabaseName());
        snapshot.setTableName(first.getTableName());
        snapshot.setScore(er.getScore());
        snapshot.setRuleTotal(er.getTotalRules());
        snapshot.setRulePass(er.getPassRules());
        snapshot.setRuleFail(er.getFailRules());
        snapshot.setCostMs(er.getCostMs());
        snapshot.setTriggerType(triggerType);
        snapshot.setCreateBy(MetadataHolder.getUserId());
        snapshotMapper.insert(snapshot);

        for (DqRuleResult r : er.getResults()) {
            r.setId(null);
            r.setSnapshotId(snapshot.getId());
            ruleResultMapper.insert(r);
        }

        log.info("质量任务 [{}] 执行完成: 评分 {}, 通过 {}/{}, 触发方式 {}",
                task.getId(), er.getScore(), er.getPassRules(), er.getTotalRules(), triggerType);

        // 失败告警：存在失败规则且当前是系统调度触发时发送站内信
        if (er.getFailRules() > 0) {
            sendAlert(task, snapshot);
        }
        return snapshot;
    }

    private void sendAlert(DqTask task, DqSnapshot snapshot) {
        String title = String.format("数据质量告警：%s 质量评分 %.2f",
                task.getTaskName(), snapshot.getScore() == null ? 0 : snapshot.getScore());
        String content = String.format(
                "任务【%s】本次稽核完成：共 %d 条规则，通过 %d 条，失败 %d 条，评分 %.2f。\n" +
                        "稽核对象：%s.%s\n请前往数据质量模块查看详情。",
                task.getTaskName(),
                snapshot.getRuleTotal() == null ? 0 : snapshot.getRuleTotal(),
                snapshot.getRulePass() == null ? 0 : snapshot.getRulePass(),
                snapshot.getRuleFail() == null ? 0 : snapshot.getRuleFail(),
                snapshot.getScore() == null ? 0 : snapshot.getScore(),
                snapshot.getDatabaseName(), snapshot.getTableName());
        String receiverId = task.getCreateBy();
        if (receiverId == null || receiverId.isBlank()) {
            receiverId = MetadataHolder.getUserId();
        }
        if (receiverId == null || receiverId.isBlank()) {
            log.info("无接收人，跳过质量告警发送: {}", task.getId());
            return;
        }
        boolean ok = notificationClient.sendSite(receiverId, title, content);
        log.info("质量任务 [{}] 告警发送{}: -> {}", task.getId(), ok ? "成功" : "失败", receiverId);
    }

    private List<DqRule> loadRules(String ruleIdsJson) {
        List<DqRule> result = new ArrayList<>();
        if (StrUtil.isBlank(ruleIdsJson)) {
            return result;
        }
        try {
            List<String> ids = JSONUtil.toList(ruleIdsJson, String.class);
            for (String ruleId : ids) {
                DqRule rule = ruleMapper.selectById(ruleId);
                if (rule != null && (rule.getStatus() == null || rule.getStatus() == 1)) {
                    result.add(rule);
                }
            }
        } catch (Exception e) {
            log.warn("解析任务规则ID失败: {}", ruleIdsJson);
        }
        return result;
    }
}