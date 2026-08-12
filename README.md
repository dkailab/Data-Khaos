# Data Khaos (数据工程)

> 国产化大数据基础设施全栈平台
>
> **作者**: dkailab | **开源协议**: Apache License 2.0

---

## 项目简介

Data Khaos 是一个面向国产化环境的大数据基础设施全栈平台，覆盖从**数据接入 → 权限管控 → 元数据管理 → 数据建模 → 指标查询 → 可视化分析 → 调度推送**的完整链路。

### 核心特性

- **国产化优先**：基于达梦 DM8 数据库，RocketMQ 消息队列，全链路国产化适配
- **统一权限体系**：SSO 单点登录 + RBAC + 行级/列级权限 + 组织权限 + 权限审批流
- **可插拔数据源**：SPI 机制支持星环、Hive、Doris、ClickHouse 等多种数据湖/仓
- **数据集市与建模**：星型/雪花模型建模，原子指标与衍生指标管理
- **可视化分析**：仪表板（定时刷新）+ 分析板（实时 OLAP 分析）
- **调度与推送**：DAG 任务调度 + 多渠道消息推送

## 技术栈

| 组件 | 选型 |
|------|------|
| 后端框架 | Spring Boot 3.x + Spring Cloud |
| 数据库 | 达梦 DM8 |
| 缓存 | Redis |
| 消息队列 | RocketMQ |
| 注册中心 | Eureka / Consul |
| ORM | MyBatis-Plus |
| 网关 | Spring Cloud Gateway |
| 前端 | Vue 3 + TypeScript + Element Plus |
| 可视化 | ECharts + AntV |
| 部署 | Docker / Kubernetes |

## 模块架构

```
data-khaos/
├── data-khaos-common          # 公共模块（工具类、异常、通用模型）
├── data-khaos-gateway         # API 网关
├── data-khaos-auth            # 认证中心（SSO、OAuth2.0、JWT）
├── data-khaos-permission      # 权限系统（RBAC、行/列权限、组织权限）
├── data-khaos-approval        # 权限审批流
├── data-khaos-datasource      # 数据源接入层（SPI 扩展）
├── data-khaos-metadata        # 元数据中心
├── data-khaos-mart            # 数据集市（模型建模、指标/维度）
├── data-khaos-query           # SQL 查询平台
├── data-khaos-visual          # 可视化引擎（仪表板 + 分析板）
├── data-khaos-schedule        # 调度系统
├── data-khaos-notification    # 推送系统
├── data-khaos-web             # Vue3 + Element Plus 前端工程
├── docker/                    # Docker 部署（compose / Dockerfile / 冒烟脚本）
├── db/                        # 数据库初始化脚本（MySQL / 达梦 DM8）
└── docs/                      # 架构、数据库、部署运维文档
```

## 分阶段实施

| 阶段 | 内容 | 状态 |
|------|------|------|
| 第一阶段 | 基础架构搭建（common、auth、permission、gateway） | ✅ 已完成 |
| 第二阶段 | 数据接入与元数据（datasource、metadata、行/列/表权限） | ✅ 已完成 |
| 第三阶段 | 权限审批与数据集市（approval、mart） | ✅ 已完成 |
| 第四阶段 | 查询与分析（query、visual） | ✅ 已完成 |
| 第五阶段 | 调度与推送（schedule、notification） | ✅ 已完成 |
| 第六阶段 | 集成测试与优化（Docker 部署、冒烟测试、文档） | ✅ 已完成 |
| 前端 | Vue3 + Element Plus 工程（data-khaos-web） | ✅ 已完成 |

## 快速开始

> 详细文档请参考 [docs/](docs/) 目录

```bash
# 1. 一键拉起全部服务（详见 docker/README.md）
cd docker && docker compose up -d --build

# 2. 冒烟测试
./scripts/smoke-test.sh

# 3. 启动前端（开发模式，代理到网关 8080）
cd ../data-khaos-web && npm install && npm run dev
```

### 环境要求

- JDK 17+ / Maven 3.8+ / Node 18+
- 开发：MySQL 8 + Redis 6+ + Nacos 2.x（可用 Docker Compose 一键拉起）
- 生产：达梦 DM8（`mvn clean install -Pprod`）

### 构建

```bash
mvn clean install -DskipTests
```

## 开源协议

Data Khaos 基于 [Apache License 2.0](LICENSE) 开源协议。

---

*Copyright [2026] [dkailab]*