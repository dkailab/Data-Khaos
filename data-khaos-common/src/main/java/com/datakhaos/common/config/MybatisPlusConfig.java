package com.datakhaos.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置：分页、乐观锁、时间自动填充。
 * 仅当服务引入 mybatis-plus（且非网关）时生效。
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({MybatisPlusInterceptor.class})
public class MybatisPlusConfig {

    /** 默认方言（dev=MySQL；prod 通过 data-khaos.mybatis.db-type=DM 切换） */
    private static final String DEFAULT_DB_TYPE = "MYSQL";

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(Environment environment) {
        String dbType = Binder.get(environment)
                .bind("data-khaos.mybatis.db-type", String.class)
                .orElse(DEFAULT_DB_TYPE);
        DbType type;
        try {
            type = DbType.valueOf(dbType.trim().toUpperCase());
        } catch (Exception e) {
            type = DbType.MYSQL;
        }

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(type);
        pagination.setMaxLimit(500L);
        interceptor.addInnerInterceptor(pagination);
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
