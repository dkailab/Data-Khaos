package com.datakhaos.common.config;

import com.datakhaos.common.security.MetadataContextFilter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

/**
 * 通用自动配置：请求上下文过滤器 + OpenAPI 文档。
 *
 * <p>仅适用于 Servlet 容器服务（Spring MVC）。网关为响应式 WebFlux，不含
 * {@code jakarta.servlet.Filter}；若此处不加 {@code type = SERVLET} 限定，
 * 网关在类 introspection 时会因方法返回类型 {@code FilterRegistrationBean}
 * 引用 servlet 类而抛 {@code NoClassDefFoundError}。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class KhaosWebAutoConfiguration {

    /** 网关透传用户头的上下文过滤器（仅 Servlet 容器服务） */
    @Bean
    @ConditionalOnClass(name = {"jakarta.servlet.Filter"})
    public FilterRegistrationBean<MetadataContextFilter> metadataContextFilter() {
        FilterRegistrationBean<MetadataContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new MetadataContextFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnClass(name = {"io.swagger.v3.oas.models.OpenAPI"})
    @ConditionalOnMissingBean
    public OpenAPI dataKhaosOpenAPI(Environment environment) {
        String name = environment.getProperty("spring.application.name", "data-khaos");
        return new OpenAPI().info(new Info()
                .title("Data Khaos - " + name)
                .description("国产化大数据基础设施全栈平台 API")
                .version(environment.getProperty("data-khaos.version", "1.0.0")));
    }
}
