package com.datakhaos.metadata;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 元数据中心服务
 */
@SpringBootApplication
@MapperScan("com.datakhaos.metadata.mapper")
public class MetadataApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetadataApplication.class, args);
        System.out.println("""

                ==========================================
                 Data Khaos - Metadata 元数据中心已启动 :8085
                ==========================================
                """);
    }
}
