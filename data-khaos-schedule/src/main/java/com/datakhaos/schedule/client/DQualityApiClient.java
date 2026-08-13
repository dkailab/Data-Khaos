package com.datakhaos.schedule.client;

import com.datakhaos.common.constant.CommonConstants;
import com.datakhaos.common.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 数据质量服务 REST 客户端（通过注册中心负载均衡调用 data-khaos-dquality）。
 * 以系统管理员身份触发周期稽核（内部接口，不经过网关鉴权）。
 */
@Slf4j
@Component
public class DQualityApiClient {

    public static final String SERVICE_URL = "http://data-khaos-dquality/api/dquality/internal";

    private final RestTemplate restTemplate;

    public DQualityApiClient(@Qualifier("lbRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** 以系统管理员身份触发质量任务稽核，返回是否调用成功 */
    public R<Void> runTask(String taskId, String triggerType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(CommonConstants.HEADER_USER_ID, CommonConstants.DEFAULT_ADMIN_ID);
            headers.set(CommonConstants.HEADER_USERNAME, "system");
            headers.set(CommonConstants.HEADER_REAL_NAME, "调度系统");
            headers.set(CommonConstants.HEADER_ROLES, String.join(",", List.of(CommonConstants.SUPER_ADMIN)));

            ResponseEntity<R<Void>> resp = restTemplate.exchange(
                    SERVICE_URL + "/task/{id}/run?triggerType={type}",
                    HttpMethod.POST,
                    new HttpEntity<>(null, headers),
                    new ParameterizedTypeReference<R<Void>>() {
                    },
                    taskId, triggerType);
            return resp.getBody();
        } catch (Exception e) {
            log.warn("调用数据质量服务触发稽核失败: {}", e.getMessage());
            return R.fail("调用数据质量服务失败: " + e.getMessage());
        }
    }
}
