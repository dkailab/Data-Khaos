package com.datakhaos.datasource;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 数据源接入服务
 */
@SpringBootApplication
@MapperScan("com.datakhaos.datasource.mapper")
public class DatasourceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatasourceApplication.class, args);
        System.out.println("""

                ==========================================
                 Data Khaos - Datasource 数据源服务已启动 :8084
                ==========================================
                """);
    }
}
