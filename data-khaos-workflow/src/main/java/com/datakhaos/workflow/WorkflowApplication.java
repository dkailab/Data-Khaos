package com.datakhaos.workflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 工作流编排系统服务
 */
@SpringBootApplication
@MapperScan("com.datakhaos.workflow.mapper")
@EnableScheduling
public class WorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
        System.out.println("""

                ==========================================
                 Data Khaos - Workflow 工作流编排系统已启动 :8093
                ==========================================
                """);
    }
}