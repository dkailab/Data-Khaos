package com.datakhaos.query.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 查询平台配置，绑定 {@code data-khaos.query.*}。
 */
@Data
@Component
@ConfigurationProperties(prefix = "data-khaos.query")
public class QueryProperties {

    /**
     * 执行前是否校验表权限（依赖权限服务；默认关闭避免权限服务不可用时误拦截）。
     */
    private boolean permissionCheckEnabled = false;
}
