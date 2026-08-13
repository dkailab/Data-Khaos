package com.datakhaos.dquality;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 数据质量稽核服务入口
 */
@SpringBootApplication(scanBasePackages = "com.datakhaos")
@MapperScan("com.datakhaos.dquality.mapper")
public class DQualityApplication {

    public static void main(String[] args) {
        SpringApplication.run(DQualityApplication.class, args);
    }
}