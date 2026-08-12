package com.datakhaos.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API 网关服务（WebFlux，非 Servlet 容器）
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("""

                ==========================================
                 Data Khaos - Gateway 网关已启动 :8080
                ==========================================
                """);
    }
}
