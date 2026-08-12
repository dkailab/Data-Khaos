package com.datakhaos.query;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SQL 查询平台服务
 */
@SpringBootApplication
@MapperScan("com.datakhaos.query.mapper")
public class QueryApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueryApplication.class, args);
        System.out.println("""

                ==========================================
                 Data Khaos - Query 查询平台已启动 :8087
                ==========================================
                """);
    }
}
