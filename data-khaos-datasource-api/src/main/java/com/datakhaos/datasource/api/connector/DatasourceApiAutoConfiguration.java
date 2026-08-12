package com.datakhaos.datasource.api.connector;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * 数据源服务客户端自动装配（依赖 common 提供的 lbRestTemplate）。
 */
@AutoConfiguration
@ConditionalOnClass(name = {"jakarta.servlet.Filter", "org.springframework.cloud.client.loadbalancer.LoadBalanced"})
public class DatasourceApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DatasourceApiClient datasourceApiClient(@Qualifier("lbRestTemplate") RestTemplate restTemplate) {
        return new DatasourceApiClient(restTemplate);
    }
}
