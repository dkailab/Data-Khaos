package com.datakhaos.pipeline;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 数据管道服务（管理面）
 */
@SpringBootApplication
@MapperScan("com.datakhaos.pipeline.mapper")
public class PipelineApplication {

    public static void main(String[] args) {
        SpringApplication.run(PipelineApplication.class, args);
        System.out.println("""

                ==========================================
                 Data Khaos - Pipeline 数据管道服务已启动 :8092
                ==========================================
                """);
    }
}