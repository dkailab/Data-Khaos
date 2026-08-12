package com.datakhaos.approval;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 权限审批服务
 */
@SpringBootApplication
@MapperScan("com.datakhaos.approval.mapper")
public class ApprovalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApprovalApplication.class, args);
        System.out.println("""

                ==========================================
                 Data Khaos - Approval 审批服务已启动 :8083
                ==========================================
                """);
    }
}
