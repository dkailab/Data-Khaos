package com.datakhaos.mart;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 数据集市服务
 */
@SpringBootApplication
@MapperScan("com.datakhaos.mart.mapper")
public class MartApplication {

    public static void main(String[] args) {
        SpringApplication.run(MartApplication.class, args);
        System.out.println("""

                ==========================================
                 Data Khaos - Mart 数据集市已启动 :8086
                ==========================================
                """);
    }
}
