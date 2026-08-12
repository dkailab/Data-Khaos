package com.datakhaos.schedule;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 调度系统服务
 */
@SpringBootApplication
@MapperScan("com.datakhaos.schedule.mapper")
@EnableScheduling
public class ScheduleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScheduleApplication.class, args);
        System.out.println("""

                ==========================================
                 Data Khaos - Schedule 调度系统已启动 :8089
                ==========================================
                """);
    }
}
