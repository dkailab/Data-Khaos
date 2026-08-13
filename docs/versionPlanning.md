# Data Khaos 版本规划

> 国产化大数据基础设施全栈平台
> 版本：**v1.0.0**（当前基线）
> 说明：本文档记录当前版本的系统架构与功能基线，供后续版本规划与回溯。

---

## 1. 版本信息

| 项 | 值 |
|----|----|
| 版本号 | v1.0.0 |
| 项目名 | Data Khaos（数据工程） |
| 定位 | 国产化大数据基础设施全栈平台 |
| 覆盖链路 | 数据接入 → 权限管控 → 元数据管理 → 数据建模 → 指标查询 → 可视化分析 → 调度推送 |
| 开源协议 | Apache License 2.0 |
| 仓库 | https://github.com/dkailab/Data-Khaos |

---

## 2. 总体架构

系统采用微服务架构，基于 Spring Cloud 技术栈，整体分五层：

```
┌──────────────────────────────────────────────────────────────┐
│ 应用层：仪表板 | 分析板 | 数据门户 | 系统管理（Vue3 前端）      │
├──────────────────────────────────────────────────────────────┤
│ 服务层：auth | permission | approval | datasource | metadata │
│         mart | query | visual | schedule | notification      │
├──────────────────────────────────────────────────────────────┤
│ 数据层：数据集市 | 元数据中心 | 数据湖 | 数据建模              │
├──────────────────────────────────────────────────────────────┤
│ 数据接入层：星环 | Hive | Doris | ClickHouse | MySQL | PG ...│
├──────────────────────────────────────────────────────────────┤
│ 基础设施层：达梦 DM8 | MySQL | Redis | Nacos | RocketMQ      │
└──────────────────────────────────────────────────────────────┘
```

- 前端 → 后端：RESTful API（经网关统一暴露）
- 服务间同步调用：OpenFeign（各 `*-api` 共享契约模块）
- 服务间异步：RocketMQ（事件驱动）
- 安全：Gateway 校验 JWT → Auth 解析身份 → Permission 校验权限

---

## 3. 模块划分与功能基线

### 3.1 公共与基础设施

| 模块 | 说明 |
|------|------|
| `data-khaos-common` | 公共模块：统一返回 `R<T>`、异常体系、通用模型、JWT 工具、元数据上下文过滤器、SQL 审计工具、加解密工具 |
| `data-khaos-gateway` | API 网关：路由转发、JWT Token 校验、白名单、接口限流 |

### 3.2 认证与权限

| 模块 | 功能基线 |
|------|---------|
| `data-khaos-auth` | 登录（验证码 + JWT 签发）、用户管理、角色管理 |
| `data-khaos-permission` | 菜单管理、组织架构管理（树形）、用户权限聚合（`/permission/user-permissions`）、行级策略、列级脱敏策略、表权限（SELECT/INSERT/UPDATE/DELETE） |
| `data-khaos-approval` | 权限申请、审批（通过 / 驳回 / 转交）、申请列表 |

> 当前权限体系：RBAC（用户-角色）+ 组织权限 + 表/行/列级数据权限。

### 3.3 数据接入与元数据

| 模块 | 功能基线 |
|------|---------|
| `data-khaos-datasource` | 数据源 CRUD、测试连接、元数据同步、SPI 可插拔连接器（星环 / Hive / Doris / ClickHouse / MySQL / PostgreSQL / Oracle / 达梦） |
| `data-khaos-metadata` | 元数据管理：库、表、字段、血缘关系查询 |

### 3.4 建模与查询

| 模块 | 功能基线 |
|------|---------|
| `data-khaos-mart` | 数据集市：模型 CRUD、指标 CRUD、维度 CRUD、模型关联关系管理 |
| `data-khaos-query` | SQL 查询平台：SQL 执行、查询历史、历史详情 |

### 3.5 可视化

| 模块 | 功能基线 |
|------|---------|
| `data-khaos-visual` | 仪表板 CRUD、发布（生成只读预览链接）、版本管理与回滚；分析板 CRUD、即席查询（Ad-hoc）、下钻分析（Drill） |

### 3.6 调度与推送

| 模块 | 功能基线 |
|------|---------|
| `data-khaos-schedule` | 调度任务 CRUD、手动运行、执行日志、任务依赖（DAG）管理 |
| `data-khaos-notification` | 推送模板、订阅管理、发送、推送记录 |

### 3.7 前端工程

| 工程 | 说明 |
|------|------|
| `data-khaos-web` | Vue3 + TypeScript + Element Plus + ECharts + Pinia + Vite 单页应用 |

前端功能模块：
- 登录 / 系统管理（用户、角色、菜单、组织）
- 权限管理（表权限、行策略、列策略）
- 数据源、元数据（搜索 / 结构 / 血缘）
- 数据集市（模型 / 指标 / 维度）
- 查询工作台
- 可视化（仪表板 / 分析板，含拖拽编辑、发布、版本、下钻）
- 调度任务、通知推送、审批流

---

## 4. 技术栈

| 分类 | 选型 |
|------|------|
| 后端框架 | Spring Boot 3.2.0 / Spring Cloud 2023.0.0 / Spring Cloud Alibaba 2023.0.3.3 |
| 语言 / 构建 | Java 17 / Maven 多模块 |
| ORM | MyBatis-Plus 3.5.5（Spring Boot 3 starter） |
| 数据库 | 达梦 DM8（生产 `-Pprod`）/ MySQL 8（开发） |
| 注册中心 | Nacos |
| 网关 | Spring Cloud Gateway |
| 工具 / 文档 | Hutool 5.8.25 / Knife4j 4.4.0 |
| 前端 | Vue 3.4 + TypeScript + Element Plus 2.7 + ECharts 5.5 + Pinia + Vite 5 |
| 部署 | Docker / Docker Compose |

---

## 5. 服务端口

| 服务 | 端口 |
|------|------|
| data-khaos-gateway | 8099 |
| data-khaos-auth | 8081 |
| data-khaos-permission | 8082 |
| data-khaos-approval | 8083 |
| data-khaos-datasource | 8084 |
| data-khaos-metadata | 8085 |
| data-khaos-mart | 8086 |
| data-khaos-query | 8087 |
| data-khaos-visual | 8088 |
| data-khaos-schedule | 8089 |
| data-khaos-notification | 8090 |
| data-khaos-web（Vite 开发） | 5173 |

---

## 6. 部署与运维

- 开发：`cd docker && docker compose up -d --build` 一键拉起；`smoke-test.sh` 冒烟测试
- 前端：`cd data-khaos-web && npm install && npm run dev`（代理到网关 8099）
- 生产：`mvn clean install -Pprod`（引入达梦驱动并激活 dm8 配置）
- 数据库脚本：`db/init.sql`、`db/mysql-init.sql`、`db/seed-dashboards.sql`、`db/seed-demo.sql`

---

## 7. 当前能力边界与后续规划方向

- 当前权限为 RBAC + 组织 + 表/行/列级，已经完成「组织 → 项目组 → 人」三级模型落地，实现**项目组角色+能力位+数据隔离**。
- 后续规划：数据治理（数据质量稽核，见 `docs/dquality-requirements.md`）、高级数据源、AI-Agent 深度分析、企业版能力沉淀。

---

## 8. 版本变更记录

### v1.0.0-20260813（本轮更新）

**核心新增：**
1. **数仓模型市场**：按一线大厂标准，引入 ODS/DWD/DWS/ADS 标准分层，新增 `mart_warehouse_layer` 表 + 增量迁移脚本。
2. **项目组权限隔离**：`mart_model/metric/dimension/rel` 增加 `project_group_id`，所有查询按当前项目组过滤，符合 `permission-handbook.md` 规范。
3. **能力位校验**：新增 `model:browse/model:develop/model:publish` 能力位，CRUD/发布按能力位校验。
4. **模型市场前端**：新增模型市场浏览页（卡片式网格）、模型详情页，支持"申请订阅"和数据预览。
5. **审批扩展**：审批模块新增 `MART`（模型订阅）类型，支持数据订阅审批流。
6. **文档新增**：
   - `docs/open-core-plan.md`：Open-Core 开放核心模式规划（社区版/企业版双版本、代码治理、防白嫖机制）
   - `docs/dquality-requirements.md`：数据质量稽核模块需求文档

**变更统计：**
- 修改文件：17 个
- 新增文件：9 个（模块实体 + 前端页面 + 文档 + 迁移脚本）
- 代码行数：+376 / -24

**涉及模块：**
- `data-khaos-mart` / `data-khaos-mart-api` / `data-khaos-permission-api` / `data-khaos-approval`
- `data-khaos-web`（前端：views + api + types + router + layout）
- `db`（mysql-init + migration + seed）
- `docs`

---

*Data Khaos © 2026 dkailab · Licensed under Apache 2.0*