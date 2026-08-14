# Data-Khaos 数据管道服务（Data Pipeline）架构与实现规划

> 版本：1.0.0
> 适用模块：`data-khaos-pipeline`（管理面）+ `data-khaos-pipeline-worker`（执行面）
> 状态：规划 + 最小闭环实现

- [1. 概述](#1-概述)
- [2. 双层架构](#2-双层架构平台--引擎)
- [3. 引擎可扩展设计](#3-引擎可扩展设计spi-适配器)
- [4. 数据模型设计](#4-数据模型设计)
- [5. API 设计](#5-api-设计)
- [6. 权限模型](#6-权限模型)
- [7. 部署与运维](#7-部署与运维)
- [8. 路线图](#8-路线图)

---

## 1. 概述

数据管道服务负责**异构数据源之间的数据搬运、同步与加工**，是数据治理平台「数据接入」能力落地的核心执行枢纽。

核心目标：

- **统一任务管理**：任务定义、手动/定时触发、执行实例、运行日志、状态监控，全部纳入平台统一门户与权限体系。
- **引擎可扩展**：底层执行引擎通过 SPI 适配器模式抽象，当前内置 **SeaTunnel（Waterdrop）**、**DataX** 两个开源引擎，并提供 **DB-Sync** 内置兜底引擎保证最小闭环可用；后续可平滑扩展 Flink / Spark / Debezium CDC 等。
- **平台与执行解耦**：管理面（pipeline 服务）负责任务编排与状态入库；执行面（worker / 引擎进程）只做数据搬运，崩溃自愈，不影响主平台。

---

## 2. 双层架构（平台 + 引擎）

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        管理面 · 现有平台微服务群                            │
│                                                                          │
│   ┌──────────┐    ┌──────────────┐    ┌─────────────┐    ┌──────────┐    │
│   │ 网关/门户 │──▶│  pipeline 服务 │──▶│  datasource  │    │  schedule │    │
│   │  (8099)  │    │    (8092)    │    │   (8084)    │    │  (8089)  │    │
│   └──────────┘    └──────┬───────┘    └─────────────┘    └──────────┘    │
│                          │ 任务/实例/日志/worker 注册（MySQL）              │
│                          ▼                                                │
│                 权限能力位 pipeline:manage / browse / run                   │
└──────────────────────────────────────────┬───────────────────────────────┘
                                            │ 派发执行（HTTP / 进程调用）
┌──────────────────────────────────────────▼───────────────────────────────┐
│                        执行面 · 独立 worker 容器                           │
│                                                                          │
│   ┌─────────────────────────────┐                                        │
│   │  pipeline-worker（Python）   │                                        │
│   │  ┌────────────┐ ┌─────────┐ │                                        │
│   │  │ SeaTunnel   │ │  DataX  │ │   （引擎适配器：配置生成 + 进程执行）      │
│   │  │  Engine     │ │  Engine │ │                                        │
│   │  └────────────┘ └─────────┘ │                                        │
│   └─────────────────────────────┘                                        │
└───────────────────────────────────────────────────────────┬──────────────┘
                                                            │ 读写
                                            ┌───────────────▼──────────────┐
                                            │  异构数据源：MySQL/达梦/        │
                                            │  Hive/Doris/ClickHouse/文件    │
                                            └──────────────────────────────┘
```

### 职责边界

| 层 | 职责 | 技术选型 |
|---|---|---|
| **管理面** pipeline 服务 | 任务 CRUD、实例管理、日志、worker 注册、引擎配置生成、调度触发、权限 | Spring Cloud + MyBatis-Plus（复用现有微服务模板，端口 8092） |
| **执行面** pipeline-worker | 拉取任务、执行引擎、回传状态/进度/日志 | Python + 引擎 CLI（SeaTunnel / DataX），独立容器 |
| **兜底引擎** DB-Sync | 不依赖引擎二进制的 JDBC 直连同步，保证最小闭环可用 | Java（pipeline 服务内，通过 datasource API） |

### 为什么这样分

- **复用**：pipeline 服务复用统一权限、统一数据源管理（datasource API）、统一网关与门户，门户 `数据同步任务` 入口直接挂载。
- **隔离**：引擎执行重 IO、易崩溃，独立进程/容器不影响主平台；任务超时、OOM 自愈。
- **可扩展**：新增引擎只需实现 SPI 适配器 + worker 内对应 CLI，无侵入。

---

## 3. 引擎可扩展设计（SPI 适配器）

### 引擎抽象

```java
/**
 * 管道执行引擎 SPI。新增引擎：实现本接口 + 在 EngineFactory 注册即可。
 */
public interface PipelineEngine {
    /** 引擎标识：DB_SYNC / DATAX / SEATUNNEL（新增在此扩展） */
    String type();

    /** 引擎是否可用（例如引擎 CLI 是否安装） */
    boolean available();

    /** 由任务定义生成引擎运行配置（JSON/命令参数） */
    String buildRunConfig(PipelineTask task);

    /** 同步执行一次任务，返回影响行数；抛异常表示失败 */
    int execute(PipelineTask task, PipelineInstance instance) throws Exception;
}
```

### 内置引擎

| 引擎 | type | 说明 | 可用性 |
|---|---|---|---|
| **DB-Sync（兜底）** | `DB_SYNC` | pipeline 服务内通过 datasource API 读源表 → 写目标表，真实可跑，不依赖引擎二进制 | 始终可用 |
| **DataX** | `DATAX` | 阿里开源离线同步，生成 `job.json`，通过 ProcessBuilder 调用 `python datax.py` | 需 worker/宿主机安装 DataX |
| **SeaTunnel（Waterdrop）** | `SEATUNNEL` | Apache 开源，支持流/批，生成 conf 文件并调用 `start-seatunnel.sh` | 需 worker/宿主机安装 SeaTunnel |

> **扩展点**：新增 Flink / Spark / CDC 引擎时，只需添加对应 `PipelineEngine` 实现并在 `EngineFactory` 注册，前端 `引擎列表` 接口自动返回新引擎，无需改动主流程。

#### DB-Sync（兜底，最小闭环真实可跑）
通过 JDBC 直连源/目标数据源完成同步，不依赖任何引擎二进制，**pipeline 服务内即可运行**（真实可跑，已通过端到端验证）。

#### DataX / SeaTunnel（可插拔引擎）
pipeline 服务内的引擎适配器会**生成标准的 DataX job.json / SeaTunnel conf 文件**，并通过 `ProcessBuilder` 调用引擎 CLI（`python datax.py` / `start-seatunnel.sh`）以独立子进程执行。引擎二进制只需安装在宿主机/pipeline 容器并配置 `DATAX_HOME` / `SEATUNNEL_HOME` 环境变量即可无缝启用；未安装时 `available()` 返回 `false`，前端「引擎列表」明确标识「未安装」。

> **执行面形态**：既支持「引擎子进程」方式（进程级隔离，崩溃自愈），也支持后续演进为独立 worker 容器（Python/引擎 CLI）。当前实现采用前者，简化部署、复用服务内数据源凭证。

### 执行流程（DB-Sync 兜底，最小闭环）

```
触发运行（手动/定时）
   │
   ▼
创建 PipelineInstance（status=0 运行中）
   │
   ▼
PipelineRunner 提交到线程池
   │
   ├─▶ engine = EngineFactory.get(task.engine)
   │
   ├─▶ DataX/SeaTunnel: 生成配置 → 派发 worker 执行 → 回传状态
   │
   └─▶ DB_SYNC: datasource API 读源表 → 按字段映射写目标表 → 更新实例
   │
   ▼
更新 PipelineInstance（status=1 成功 / 2 失败，rows / error / duration）
```

---

## 4. 数据模型设计

### pipeline_task（管道任务定义）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | varchar(32) PK | 任务 ID |
| task_name | varchar(128) | 任务名称 |
| task_type | varchar(32) | 任务类型：SYNC（同步）/ ETL（加工） |
| engine | varchar(32) | 引擎：DB_SYNC / DATAX / SEATUNNEL |
| source_ds_id | varchar(32) | 源数据源 ID |
| source_table | varchar(128) | 源表 |
| target_ds_id | varchar(32) | 目标数据源 ID |
| target_table | varchar(128) | 目标表 |
| source_query | text | 源查询（可选，自定义 SQL） |
| field_mapping | text | 字段映射（JSON） |
| config | text | 引擎扩展配置（JSON） |
| cron_expr | varchar(64) | 定时表达式（空=仅手动） |
| status | tinyint | 1=启用 0=停用 |
| create_time / update_time | datetime | 时间戳 |

### pipeline_instance（执行实例）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | varchar(32) PK | 实例 ID |
| task_id | varchar(32) | 关联任务 |
| engine | varchar(32) | 实际执行引擎 |
| trigger_type | varchar(16) | MANUAL / CRON |
| status | tinyint | 0=运行中 1=成功 2=失败 |
| start_time / end_time | datetime | 起止时间 |
| duration_ms | bigint | 耗时（毫秒） |
| rows | bigint | 影响行数 |
| error_message | text | 失败原因 |
| worker | varchar(64) | 执行 worker 标识 |

### pipeline_worker（worker 注册表，可扩展）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | varchar(32) PK | worker ID |
| worker_name | varchar(128) | worker 名称/地址 |
| engines | varchar(128) | 支持的引擎，逗号分隔 |
| status | tinyint | 1=在线 0=离线 |
| last_heartbeat | datetime | 最近心跳 |
| create_time | datetime | 注册时间 |

---

## 5. API 设计

前缀 `/api/pipeline`，经网关转发到 `data-khaos-pipeline`。

### PipelineTaskController

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/pipeline/task/page` | 分页查询任务 |
| GET | `/api/pipeline/task/{id}` | 任务详情 |
| POST | `/api/pipeline/task` | 新增任务 |
| PUT | `/api/pipeline/task` | 修改任务 |
| DELETE | `/api/pipeline/task/{id}` | 删除任务 |
| POST | `/api/pipeline/task/{id}/run` | 手动触发运行 |
| GET | `/api/pipeline/task/engines` | 引擎列表（可扩展） |

### PipelineInstanceController

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/pipeline/instance/page` | 分页查询实例 |
| GET | `/api/pipeline/instance/{id}` | 实例详情 |
| GET | `/api/pipeline/instance/task/{taskId}` | 某任务的历史实例 |

### 交互示例（触发运行）

```bash
POST /api/pipeline/task/{id}/run
# 响应：{ code:0, data:{ instanceId:"..." } }
```

---

## 6. 权限模型

复用平台「组织 → 项目组 → 人」三级权限 + 能力位。新增能力位位定义于
`com.datakhaos.permission.api.service.PermissionConstants`：

| 能力位 | 说明 |
|---|---|
| `pipeline:manage` | 管道任务管理（增删改） |
| `pipeline:browse` | 管道任务 / 实例查看 |
| `pipeline:run` | 触发管道执行 |

超级管理员天然具备全部能力位。pipeline 服务通过 `MetadataHolder` + 能力位校验实现鉴权（参考 dquality 的 `DqAuthContext` 模式）。

---

## 7. 部署与运维

- **pipeline 服务**：作为独立微服务容器 `dk-pipeline`，主机端口 `8092`，复用通用 Dockerfile。
- **执行引擎**：DataX / SeaTunnel 以子进程方式在 pipeline 容器内执行（需在容器内安装引擎并配置 `DATAX_HOME` / `SEATUNNEL_HOME`）；DB-Sync 走 JDBC 直连，无需引擎。后续可演进为独立 worker 容器 `dk-pipeline-worker`。
- **数据库**：`pipeline_task` / `pipeline_instance` / `pipeline_worker` 三张表，随 `db/mysql-init.sql` 初始化。
- **网关**：`/api/pipeline/**` 路由到 pipeline 服务。

---

## 8. 路线图

- [x] 架构规划与引擎 SPI 设计
- [x] 数据模型与 API 契约
- [x] pipeline 服务最小闭环（DB-Sync 兜底引擎真实可跑，已端到端验证）
- [x] DataX / SeaTunnel 引擎适配器（生成 job.json / conf 并调 CLI 子进程执行）
- [x] 前端「数据同步任务」页面（任务列表/创建/运行/实例）
- [ ] 独立 worker 容器（Python + 引擎 CLI）接入
- [ ] 定时触发（对接 schedule 服务）
- [ ] 任务 DAG 编排 / 依赖
- [ ] 引擎监控看板（写入 ops 模块）