package com.datakhaos.dquality.client;

import com.datakhaos.common.model.R;
import com.datakhaos.dquality.dto.ScheduleJobBrief;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 调度服务 REST 客户端（通过注册中心负载均衡调用 data-khaos-schedule）。
 * 供数据质量查询 QUALITY 类型调度任务，建立「质量任务 <-> 调度任务」关联关系。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleApiClient {

    public static final String SERVICE_URL = "http://data-khaos-schedule/api/schedule/job";

    private final @Qualifier("lbRestTemplate") RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** 查询全部 QUALITY 类型调度任务（简要信息），失败时返回空列表 */
    public List<ScheduleJobBrief> listQualityJobs() {
        try {
            String url = SERVICE_URL + "/page?current=1&size=1000&jobType=QUALITY";
            ResponseEntity<R<Map<String, Object>>> resp = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<R<Map<String, Object>>>() {
                    });
            R<Map<String, Object>> r = resp.getBody();
            if (r == null || r.getCode() != 0 || r.getData() == null) {
                log.warn("查询调度任务失败: code={}, msg={}",
                        r == null ? -1 : r.getCode(), r == null ? "" : r.getMsg());
                return Collections.emptyList();
            }
            Object records = r.getData().get("records");
            List<ScheduleJobBrief> result = new ArrayList<>();
            if (records instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> m) {
                        result.add(toBrief(m));
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("调用调度服务查询 QUALITY 任务失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private ScheduleJobBrief toBrief(Map<?, ?> m) {
        ScheduleJobBrief b = new ScheduleJobBrief();
        b.setJobId(asString(m.get("id")));
        b.setJobName(asString(m.get("jobName")));
        b.setJobType(asString(m.get("jobType")));
        b.setCronExpression(asString(m.get("cronExpression")));
        Object status = m.get("status");
        b.setStatus(status instanceof Number n ? n.intValue() : 0);
        // 解析 params.taskId 建立关联
        String params = asString(m.get("params"));
        if (params != null) {
            try {
                JsonNode node = objectMapper.readTree(params);
                if (node.isObject() && node.hasNonNull("taskId")) {
                    b.setTaskId(node.get("taskId").asText());
                }
            } catch (Exception e) {
                log.warn("解析调度任务 params 失败: jobId={}, params={}", b.getJobId(), params);
            }
        }
        return b;
    }

    private String asString(Object o) {
        return o == null ? null : o.toString();
    }
}
