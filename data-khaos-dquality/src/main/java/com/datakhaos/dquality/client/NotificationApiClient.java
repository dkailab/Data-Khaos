package com.datakhaos.dquality.client;

import com.datakhaos.common.model.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 推送服务 REST 客户端（通过注册中心负载均衡调用 data-khaos-notification）。
 * 供数据质量在稽核失败时发送站内信告警。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationApiClient {

    public static final String SERVICE_URL = "http://data-khaos-notification/api/notify";

    private final @Qualifier("lbRestTemplate") RestTemplate restTemplate;

    /** 发送站内信告警 */
    public boolean sendSite(String receiverId, String title, String content) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("templateCode", "DQUALITY_ALERT");
            body.put("receiverId", receiverId);
            body.put("receiverType", "USER");
            body.put("channel", "SITE");
            Map<String, Object> vars = new HashMap<>();
            vars.put("title", title);
            vars.put("content", content);
            body.put("vars", vars);

            ResponseEntity<R<Void>> resp = restTemplate.exchange(
                    SERVICE_URL + "/send", HttpMethod.POST, new HttpEntity<>(body),
                    new ParameterizedTypeReference<R<Void>>() {
                    });
            R<Void> r = resp.getBody();
            return r != null && r.getCode() == 0;
        } catch (Exception e) {
            log.warn("发送站内信告警失败: {}", e.getMessage());
            return false;
        }
    }
}
