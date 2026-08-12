package com.datakhaos.visual;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 可视化引擎服务
 */
@SpringBootApplication
@MapperScan("com.datakhaos.visual.mapper")
public class VisualApplication {

    public static void main(String[] args) {
        SpringApplication.run(VisualApplication.class, args);
        System.out.println("""

                ==========================================
                 Data Khaos - Visual 可视化引擎已启动 :8088
                ==========================================
                """);
    }
}
