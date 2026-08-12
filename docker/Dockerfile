# syntax=docker/dockerfile:1
# ============================================================
# Data Khaos - 通用 Dockerfile（多阶段构建）
#
# 单个 Dockerfile 服务于全部 11 个可运行服务，通过构建参数
# MODULE 指定打包哪个模块。构建阶段一次性编译整个 Maven
# reactor（各服务共享该层缓存），运行阶段只拷贝对应 jar。
#
# 用法：
#   docker build --build-arg MODULE=data-khaos-gateway -t data-khaos/gateway .
#   （docker-compose.yml 已内置该构建参数，无需手动执行）
# ============================================================

# ---- 构建阶段：编译整个 reactor ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# 先拷贝 pom 与全部模块源码，一次 mvn package 产出所有 jar
COPY pom.xml ./
COPY data-khaos-common/       ./data-khaos-common/
COPY data-khaos-auth-api/     ./data-khaos-auth-api/
COPY data-khaos-permission-api/ ./data-khaos-permission-api/
COPY data-khaos-datasource-api/ ./data-khaos-datasource-api/
COPY data-khaos-mart-api/     ./data-khaos-mart-api/
COPY data-khaos-gateway/      ./data-khaos-gateway/
COPY data-khaos-auth/         ./data-khaos-auth/
COPY data-khaos-permission/   ./data-khaos-permission/
COPY data-khaos-approval/     ./data-khaos-approval/
COPY data-khaos-datasource/   ./data-khaos-datasource/
COPY data-khaos-metadata/     ./data-khaos-metadata/
COPY data-khaos-mart/         ./data-khaos-mart/
COPY data-khaos-query/        ./data-khaos-query/
COPY data-khaos-visual/       ./data-khaos-visual/
COPY data-khaos-schedule/     ./data-khaos-schedule/
COPY data-khaos-notification/ ./data-khaos-notification/

RUN mvn -q -B -DskipTests clean package

# ---- 运行阶段：按 MODULE 取对应 jar ----
FROM eclipse-temurin:17-jre-jammy
ARG MODULE

ENV JAVA_OPTS="-Xms256m -Xmx512m -Duser.timezone=Asia/Shanghai" \
    TZ=Asia/Shanghai

WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/${MODULE}-1.0.0-SNAPSHOT.jar /app/app.jar

# 所有服务默认监听 8080 容器端口，主机端口由 compose 映射
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
