package com.datakhaos.workflow.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.datakhaos.workflow.entity.WorkflowDef;
import com.datakhaos.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 定时调度：轮询启用且配置了 Cron 的工作流，到点触发（抢占最近一次触发，避免积压补跑）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowScheduler {

    private static final long POLL_MS = 10_000L;

    private final WorkflowService workflowService;

    /** id -> 最近一次比对基准时间 */
    private final Map<String, LocalDateTime> base = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = POLL_MS)
    public void scan() {
        for (WorkflowDef def : workflowService.listAll()) {
            String cronText = def.getCronExpression();
            if (StrUtil.isBlank(cronText)) {
                continue;
            }
            try {
                CronExpression cron = CronExpression.parse(cronText);
                LocalDateTime baseTime = base.getOrDefault(def.getId(), def.getCreateTime());
                LocalDateTime next = cron.next(baseTime);
                LocalDateTime now = LocalDateTime.now();
                if (next != null && !next.isAfter(now)) {
                    // 到点
                    base.put(def.getId(), now);
                    Map<String, Object> params = parseParams(def.getParams());
                    log.info("定时触发工作流 id={} name={} cron={}", def.getId(), def.getName(), cronText);
                    try {
                        workflowService.trigger(def.getId(), params, "SCHEDULE");
                    } catch (Exception e) {
                        log.warn("定时触发工作流失败 id={}: {}", def.getId(), e.getMessage());
                    }
                } else if (next != null) {
                    base.put(def.getId(), next);
                }
            } catch (Exception e) {
                log.warn("工作流 {} 的 Cron 表达式非法: {}", def.getId(), cronText);
            }
        }
    }

    /** 解析运行参数模板 JSON 为 key-value */
    private Map<String, Object> parseParams(String params) {
        Map<String, Object> result = new HashMap<>();
        if (StrUtil.isNotBlank(params)) {
            try {
                JSONObject obj = JSONUtil.parseObj(params);
                obj.forEach((k, v) -> result.put(k, v));
            } catch (Exception e) {
                log.warn("工作流参数模板解析失败: {}", e.getMessage());
            }
        }
        return result;
    }
}