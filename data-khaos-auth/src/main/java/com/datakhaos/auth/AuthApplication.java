package com.datakhaos.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 认证中心
 */
@SpringBootApplication
@MapperScan("com.datakhaos.auth.mapper")
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
        System.out.println("""

                ==========================================
                 Data Khaos - Auth 认证中心已启动 :8081
                ==========================================
                """);
    }
}
