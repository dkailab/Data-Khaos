package com.datakhaos.permission;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 权限系统
 */
@SpringBootApplication
@MapperScan("com.datakhaos.permission.mapper")
public class PermissionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PermissionApplication.class, args);
        System.out.println("""

                ==========================================
                 Data Khaos - Permission 权限服务已启动 :8082
                ==========================================
                """);
    }
}
