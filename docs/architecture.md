# Data Khaos 架构设计文档

## 1. 总体架构

Data Khaos 采用微服务架构，基于 Spring Cloud 技术栈。整体分为四层：

### 1.1 基础设施层

提供底层支撑能力，包含数据库、缓存、消息队列、注册中心等。

### 1.2 数据接入层

通过 SPI 机制抽象数据源连接器，支持多种数据湖/仓的接入。

### 1.3 服务层

包含核心业务服务：认证、权限、审批、元数据、集市、查询、可视化、调度、推送。

### 1.4 应用层

提供用户交互界面：仪表板、分析板、数据门户、系统管理。

## 2. 服务划分

| 服务名称 | 端口 | 说明 |
|---------|------|------|
| data-khaos-gateway | 8099 | API 网关 |
| data-khaos-auth | 8081 | 认证中心 |
| data-khaos-permission | 8082 | 权限服务 |
| data-khaos-approval | 8083 | 审批服务 |
| data-khaos-datasource | 8084 | 数据源服务 |
| data-khaos-metadata | 8085 | 元数据服务 |
| data-khaos-mart | 8086 | 集市服务 |
| data-khaos-query | 8087 | 查询服务 |
| data-khaos-visual | 8088 | 可视化服务 |
| data-khaos-schedule | 8089 | 调度服务 |
| data-khaos-notification | 8090 | 推送服务 |

## 3. 通信方式

- **服务间同步调用**: OpenFeign（内部 API）
- **服务间异步通信**: RocketMQ（事件驱动）
- **前端 → 后端**: RESTful API（通过网关）

## 4. 安全架构

```
用户请求 → Gateway(校验Token) → Auth(解析身份) → Permission(校验权限) → 目标服务
```

- Gateway 层负责 JWT Token 的校验
- Auth 服务负责 Token 的签发与刷新
- Permission 服务负责 RBAC + 行/列权限的校验
- 权限校验通过注解 + AOP 切面实现

## 5. 数据架构

```
数据湖 → 数据接入 → 元数据中心 → 数据集市 → 可视化/查询
                                          ↓
                                    调度系统(定时同步)
```

- 数据湖（星环/Hive/Doris）通过数据源接入层统一管理
- 元数据中心采集并存储库表结构信息
- 数据集市基于模型定义进行数据建模
- 调度系统负责定时同步和刷新集市数据