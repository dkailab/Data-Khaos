package com.datakhaos.pipeline.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 管道执行线程池 + 引擎配置
 */
@Configuration
public class PipelineConfig {

    @Value("${data-khaos.pipeline.core-pool-size:2}")
    private int corePoolSize;
    @Value("${data-khaos.pipeline.max-pool-size:8}")
    private int maxPoolSize;
    @Value("${data-khaos.pipeline.queue-capacity:100}")
    private int queueCapacity;

    @Bean("pipelineTaskExecutor")
    public ThreadPoolTaskExecutor pipelineTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("pipeline-run-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}