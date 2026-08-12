# Data Khaos (数据混沌) — 大数据基建整体规划

> **作者**: pmdream
> **开源协议**: Apache License 2.0
> **项目定位**: 国产化大数据基础设施全栈平台

---

## 一、项目概述

Data Khaos 是一个面向国产化环境的大数据基础设施全栈平台，覆盖从**数据接入 → 权限管控 → 元数据管理 → 数据建模 → 指标查询 → 可视化分析 → 调度推送**的完整链路。

### 核心设计原则

| 原则 | 说明 |
|------|------|
| **国产化优先** | 数据库采用达梦 DM8，消息队列优先 RocketMQ，全链路国产化适配 |
| **模块化** | 各模块独立部署/独立演进，通过 API 网关统一暴露 |
| **可插拔数据源** | 数据源层抽象 SPI 接口，可扩展接入任意数据湖/数据仓库 |
| **统一权限体系** | 一套权限模型贯穿所有模块（SSO + RBAC + 行/列级权限） |
| **前后端分离** | 后端 Spring Cloud + 前端 Vue3 |

---

## 二、技术栈

### 后端

| 组件 | 技术选型 | 说明 |
|------|---------|------|
| 基础框架 | Spring Boot 3.x / Spring Cloud | 微服务架构 |
| 认证授权 | Spring Security + OAuth2.0 + JWT | SSO 单点登录 |
| ORM | MyBatis-Plus | 数据库操作 |
| 数据库 | 达梦 DM8 | 国产化数据库 |
| 缓存 | Redis | 分布式缓存/会话管理 |
| 消息队列 | RocketMQ | 国产消息中间件 |
| 注册中心 | Eureka / Consul | 服务发现与配置 |
| 网关 | Spring Cloud Gateway | API 统一网关 |
| 数据湖连接 | JDBC / Thrift / RESTful SPI | 可插拔数据源 |

### 前端

| 组件 | 技术选型 | 说明 |
|------|---------|------|
| 框架 | Vue 3 + TypeScript | 前端框架 |
| UI 组件库 | Element Plus | 企业级 UI |
| 可视化 | ECharts + AntV | 图表/仪表板渲染 |
| 状态管理 | Pinia | 前端状态管理 |
| 构建工具 | Vite | 前端构建 |

### 部署

- Docker + Docker Compose（开发环境）
- Kubernetes（生产环境）

---

## 三、整体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        应用层 (Application Layer)                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐   │
│  │ 仪表板    │  │ 分析板    │  │ 数据门户  │  │ 系统管理         │   │
│  │ Dashboard│  │ Analysis │  │  Portal  │  │  Admin           │   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └───────┬──────────┘   │
├───────┴──────────────┴────────────┴──────────────────┴────────────┤
│                    服务层 (Service Layer)                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐          │
│  │ 指标服务  │  │ 维度服务  │  │ SQL查询   │  │ 调度系统  │          │
│  │ Metric   │  │ Dim      │  │ SQL Query│  │ Scheduler│          │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐          │
│  │ 推送系统  │  │ 权限服务  │  │ 审批服务  │  │ 模型服务  │          │
│  │ Notify   │  │ Auth/ACL │  │ Approval │  │ Modeling │          │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘          │
├───────┴──────────────┴────────────┴──────────────────┴────────────┤
│                    数据层 (Data Layer)                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐          │
│  │ 数据集市  │  │ 元数据中心 │  │ 数据湖    │  │ 数据建模  │          │
│  │ Data Mart│  │ Meta     │  │ Data Lake│  │ Modeling │          │
│  │          │  │ Center   │  │          │  │ Engine   │          │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘          │
├───────┴──────────────┴────────────┴──────────────────┴────────────┤
│                  数据接入层 (Ingestion Layer)                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐          │
│  │ 星环接入  │  │ Hive接入  │  │ Doris接入 │  │ JDBC通用  │          │
│  │Transwarp │  │  Hive    │  │  Doris   │  │  JDBC    │          │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘          │
├────────────────────────────────────────────────────────────────────┤
│                   基础设施层 (Infrastructure)                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐          │
│  │ 达梦 DM8  │  │  Redis   │  │ RocketMQ │  │  Nacos   │          │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘          │
└────────────────────────────────────────────────────────────────────┘
```

---

## 四、模块划分（共 12 个核心模块）

### 模块清单

| 编号 | 模块名 | 说明 | 优先级 |
|------|--------|------|--------|
| M01 | `data-khaos-common` | 公共工具类、通用模型、常量、异常定义 | P0 |
| M02 | `data-khaos-gateway` | API 网关（路由、鉴权、限流） | P0 |
| M03 | `data-khaos-auth` | 认证中心（SSO 单点登录、OAuth2.0） | P0 |
| M04 | `data-khaos-permission` | 权限系统（RBAC、行/列权限、组织权限、菜单/报表权限） | P0 |
| M05 | `data-khaos-approval` | 权限审批流（权限申请、审批、授予） | P0 |
| M06 | `data-khaos-datasource` | 数据源接入层（星环/Hive/Doris/JDBC 等） | P0 |
| M07 | `data-khaos-metadata` | 元数据中心（库表结构、字段信息、血缘关系） | P0 |
| M08 | `data-khaos-mart` | 数据集市（模型建模、维度/指标定义、集市管理） | P0 |
| M09 | `data-khaos-query` | SQL 查询平台（在线查询、SQL 审核、执行计划） | P1 |
| M10 | `data-khaos-visual` | 可视化引擎（仪表板 + 分析板） | P1 |
| M11 | `data-khaos-schedule` | 调度系统（定时任务、数据同步调度） | P1 |
| M12 | `data-khaos-notification` | 推送系统（消息通知、邮件、站内信） | P1 |

---

## 五、模块详细设计

### M01 — data-khaos-common（公共模块）

**功能**:
- 通用工具类（String、Date、JSON、加密等）
- 统一返回结果封装 `R<T>`
- 统一异常体系 `BusinessException` + 全局异常处理器
- 通用 MyBatis-Plus 基类（BaseEntity、BaseMapper）
- 达梦数据库方言适配
- 分布式 ID 生成器（雪花算法）
- 通用分页模型
- 注解定义（权限校验、操作日志等）

### M02 — data-khaos-gateway（API 网关）

**功能**:
- 基于 Spring Cloud Gateway
- 统一路由转发
- JWT Token 校验与解析
- 接口限流（基于 Redis + Lua）
- 跨域配置
- 统一日志记录
- 请求/响应日志审计

### M03 — data-khaos-auth（认证中心）

**功能**:
- **SSO 单点登录**: 基于 OAuth2.0 + JWT
- 登录方式: 密码登录、短信验证码、LDAP（可选）
- Token 管理: 签发、刷新、吊销
- 会话管理: Redis 分布式会话
- 三方登录对接（如需对接企业微信/钉钉/LDAP）
- 登录审计日志

**核心表设计（达梦）**:

```sql
-- 用户表
CREATE TABLE sys_user (
    id          VARCHAR(32) PRIMARY KEY,
    username    VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    real_name   VARCHAR(100),
    email       VARCHAR(200),
    phone       VARCHAR(20),
    status      TINYINT DEFAULT 1,  -- 1:启用 0:禁用
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP(),
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP()
);

-- 角色表
CREATE TABLE sys_role (
    id          VARCHAR(32) PRIMARY KEY,
    role_code   VARCHAR(100) NOT NULL UNIQUE,
    role_name   VARCHAR(200) NOT NULL,
    status      TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP()
);

-- 用户角色关联表
CREATE TABLE sys_user_role (
    id          VARCHAR(32) PRIMARY KEY,
    user_id     VARCHAR(32) NOT NULL,
    role_id     VARCHAR(32) NOT NULL,
    UNIQUE(user_id, role_id)
);
```

### M04 — data-khaos-permission（权限系统）

**核心概念**:
- **RBAC 模型**: 用户 → 角色 → 权限
- **行权限**: 数据行级过滤（如：部门 A 只能看到本部门数据）
- **列权限**: 数据列级脱敏/隐藏（如：手机号只有管理员可见）
- **组织权限**: 基于组织架构的数据隔离
- **菜单权限**: 控制前端菜单/按钮可见性
- **报表权限**: 控制报表/仪表板的访问
- **表权限**: 控制用户对数据表/视图的访问权限

**功能**:
- 权限定义与管理（菜单、操作、数据权限）
- 角色-权限绑定
- 用户-角色绑定
- 行权限策略配置（支持表达式：`org_id = #{currentOrgId}`）
- 列权限策略配置（字段级脱敏规则：掩码、加密、禁止查看）
- 组织架构管理（树形结构）
- 表权限管理（库表级别的 SELECT/INSERT/UPDATE/DELETE 控制）
- 权限校验拦截器（注解 + 切面）

**核心表设计（达梦）**:

```sql
-- 菜单/资源表
CREATE TABLE sys_menu (
    id          VARCHAR(32) PRIMARY KEY,
    parent_id   VARCHAR(32),       -- 父菜单ID
    name        VARCHAR(200),
    path        VARCHAR(500),
    permission  VARCHAR(200),      -- 权限标识
    type        TINYINT,           -- 0:目录 1:菜单 2:按钮
    sort_order  INT DEFAULT 0,
    status      TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP()
);

-- 角色-权限关联表
CREATE TABLE sys_role_permission (
    id            VARCHAR(32) PRIMARY KEY,
    role_id       VARCHAR(32) NOT NULL,
    permission_id VARCHAR(32) NOT NULL,
    UNIQUE(role_id, permission_id)
);

-- 组织架构表
CREATE TABLE sys_organization (
    id          VARCHAR(32) PRIMARY KEY,
    parent_id   VARCHAR(32),
    org_name    VARCHAR(200) NOT NULL,
    org_code    VARCHAR(100),
    sort_order  INT DEFAULT 0,
    status      TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP()
);

-- 用户-组织关联表
CREATE TABLE sys_user_org (
    id      VARCHAR(32) PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL,
    org_id  VARCHAR(32) NOT NULL,
    UNIQUE(user_id, org_id)
);

-- 行权限策略表
CREATE EXISTS sys_row_policy (
    id              VARCHAR(32) PRIMARY KEY,
    policy_name     VARCHAR(200),
    target_table    VARCHAR(200),       -- 目标表
    expression      VARCHAR(1000),      -- 过滤表达式
    role_id         VARCHAR(32),
    user_id         VARCHAR(32),
    status          TINYINT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP()
);

-- 列权限策略表
CREATE TABLE sys_column_policy (
    id              VARCHAR(32) PRIMARY KEY,
    policy_name     VARCHAR(200),
    target_table    VARCHAR(200),       -- 目标表
    column_name     VARCHAR(200),       -- 目标字段
    mask_type       VARCHAR(50),        -- 脱敏方式: MASK/ENCRYPT/HIDE
    role_id         VARCHAR(32),
    user_id         VARCHAR(32),
    status          TINYINT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP()
);

-- 表权限表
CREATE TABLE sys_table_permission (
    id              VARCHAR(32) PRIMARY KEY,
    datasource_id   VARCHAR(32),
    database_name   VARCHAR(200),
    table_name      VARCHAR(200),
    permission_type VARCHAR(50),        -- SELECT/INSERT/UPDATE/DELETE
    role_id         VARCHAR(32),
    user_id         VARCHAR(32),
    status          TINYINT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP()
);
```

### M05 — data-khaos-approval（权限审批流）

**功能**:
- 权限申请单（用户申请某张表/某个报表的权限）
- 申请类型：表权限申请、报表权限申请、数据源权限申请
- 审批流程定义（单级审批/多级审批）
- 审批人配置（按角色/按组织）
- 审批操作：通过/驳回/转交
- 自动授权：审批通过后自动创建对应的权限记录
- 申请历史与审批记录查询

**核心表设计（达梦）**:

```sql
-- 权限申请表
CREATE TABLE app_apply (
    id              VARCHAR(32) PRIMARY KEY,
    applicant_id    VARCHAR(32) NOT NULL,     -- 申请人
    apply_type      VARCHAR(50) NOT NULL,     -- TABLE/REPORT/DATASOURCE
    target_id       VARCHAR(32),              -- 申请目标ID
    reason          VARCHAR(1000),            -- 申请理由
    status          TINYINT DEFAULT 0,        -- 0:待审批 1:通过 2:驳回
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP()
);

-- 审批记录表
CREATE TABLE app_approval_record (
    id              VARCHAR(32) PRIMARY KEY,
    apply_id        VARCHAR(32) NOT NULL,
    approver_id     VARCHAR(32) NOT NULL,
    action          TINYINT,                  -- 1:通过 2:驳回 3:转交
    comment         VARCHAR(1000),
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP()
);
```

### M06 — data-khaos-datasource（数据源接入层）

**功能**:
- **数据源注册**: 支持配置多种数据源连接信息
- **SPI 扩展机制**: 定义 `DataSourceConnector` 接口，每种数据源实现该接口
- **内置数据源连接器**:
  - 星环 (Transwarp) — JDBC/Thrift
  - Apache Hive — JDBC/Thrift
  - Apache Doris — MySQL 协议
  - ClickHouse — JDBC/Native
  - MySQL / PostgreSQL — JDBC
  - 达梦 DM8 — JDBC（国产化）
- **连接池管理**: 统一管理数据源连接池，支持心跳检测
- **数据源健康检查**: 定时检测数据源可用性
- **数据源元数据同步**: 自动拉取库、表、字段信息
- **SQL 方言适配**: 不同数据源 SQL 语法差异适配

**核心接口设计**:

```java
public interface DataSourceConnector {
    // 数据源类型标识
    String getType();
    
    // 测试连接
    boolean testConnection(DataSourceConfig config);
    
    // 获取数据库列表
    List<String> getDatabases(DataSourceConfig config);
    
    // 获取表列表
    List<String> getTables(DataSourceConfig config, String database);
    
    // 获取表字段信息
    List<ColumnInfo> getColumns(DataSourceConfig config, String database, String table);
    
    // 执行查询
    QueryResult executeQuery(DataSourceConfig config, String sql, Map<String, Object> params);
    
    // 获取表数据量
    long getTableCount(DataSourceConfig config, String database, String table);
}
```

### M07 — data-khaos-metadata（元数据中心）

**功能**:
- **元数据采集**: 定时/实时从数据源拉取元数据
- **库表结构**: 存储所有接入的数据库、表、字段、分区信息
- **字段详情**: 字段名、类型、注释、是否主键、是否可为空
- **血缘关系**: 字段级血缘追踪（ETL 过程中字段的映射关系）
- **元数据版本管理**: 追踪元数据变更历史
- **标签管理**: 给表/字段打标签（如：PII 数据、敏感数据）
- **元数据搜索**: 按表名/字段名/注释搜索

### M08 — data-khaos-mart（数据集市）

**功能**:
- **模型建模**:
  - 星型模型 / 雪花模型
  - 事实表定义（可累加/半累加/不可累加事实）
  - 维度表定义（层级维度、退化维度）
  - 模型关联关系配置
- **指标管理**:
  - 原子指标（如：销售额 = SUM(amount)）
  - 衍生指标（如：客单价 = 销售额 / 订单数）
  - 指标定义：名称、表达式、数据类型、单位
  - 指标分类与标签
- **维度管理**:
  - 维度定义：名称、层级、字段映射
  - 维度值管理
  - 时间维度（自动生成年/季/月/周/日）
- **集市发布**: 将模型发布到物理表或视图
- **数据刷新**: 定时/手动刷新集市数据

**核心表设计（达梦）**:

```sql
-- 模型定义表
CREATE TABLE mart_model (
    id              VARCHAR(32) PRIMARY KEY,
    model_name      VARCHAR(200) NOT NULL,
    model_type      VARCHAR(50),             -- STAR/SNOWFLAKE
    datasource_id   VARCHAR(32),
    status          TINYINT DEFAULT 0,       -- 0:草稿 1:已发布
    version         INT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP()
);

-- 指标定义表
CREATE TABLE mart_metric (
    id              VARCHAR(32) PRIMARY KEY,
    metric_name     VARCHAR(200) NOT NULL,
    metric_code     VARCHAR(100) NOT NULL UNIQUE,
    expression      VARCHAR(1000),           -- 计算表达式
    data_type       VARCHAR(50),             -- BIGINT/DECIMAL/DOUBLE
    unit            VARCHAR(50),             -- 单位
    category_id     VARCHAR(32),
    model_id        VARCHAR(32),
    status          TINYINT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP()
);

-- 维度定义表
CREATE TABLE mart_dimension (
    id              VARCHAR(32) PRIMARY KEY,
    dim_name        VARCHAR(200) NOT NULL,
    dim_code        VARCHAR(100) NOT NULL UNIQUE,
    model_id        VARCHAR(32),
    source_table    VARCHAR(200),
    status          TINYINT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP()
);

-- 维度层级表
CREATE TABLE mart_dim_level (
    id              VARCHAR(32) PRIMARY KEY,
    dim_id          VARCHAR(32) NOT NULL,
    level_name      VARCHAR(200),
    level_column    VARCHAR(200),
    sort_order      INT DEFAULT 0
);
```

### M09 — data-khaos-query（SQL 查询平台）

**功能**:
- **在线 SQL 编辑器**: 语法高亮、自动补全、格式化
- **多数据源查询**: 选择数据源后执行查询
- **SQL 审核**: 查询前进行 SQL 安全检查（禁止 DDL/危险操作）
- **执行计划查看**: EXPLAIN 执行计划展示
- **查询历史**: 保存用户查询历史
- **查询结果导出**: CSV / Excel 导出
- **查询超时控制**: 防止大查询拖垮数据源
- **查询结果缓存**: 相同查询结果缓存
- **权限拦截**: 查询时自动注入行/列权限策略

### M10 — data-khaos-visual（可视化引擎）

**包含两个子模块**:

#### 仪表板 (Dashboard)
- 可视化组件：柱状图、折线图、饼图、表格、数值卡片
- 拖拽式布局
- 定时刷新（分钟级）
- 报表分享
- 数据源绑定指标/维度

#### 分析板 (Analysis Board)
- 所有仪表板功能 +
- **实时分析能力**: 支持即席查询（Ad-hoc Query）
- **交互式筛选**: 联动筛选、下钻分析
- **OLAP 操作**: 上卷（Roll-up）、下钻（Drill-down）、切片（Slice）、切块（Dice）
- **自定义计算字段**: 在分析面板中临时创建计算字段
- **实时数据连接**: 直接连接数据源，不经过缓存/集市
- **导出分析结果**

**核心差异对比**:

| 特性 | 仪表板 | 分析板 |
|------|--------|--------|
| 数据源 | 主要从数据集市读取 | 可直接连接任意数据源 |
| 实时性 | 定时刷新（分钟级） | 实时查询 |
| 交互分析 | 有限（筛选） | 完整 OLAP 操作 |
| 临时计算 | 不支持 | 支持自定义计算字段 |
| 适用场景 | 固定报表、监控大屏 | 数据探索、即席分析 |

### M11 — data-khaos-schedule（调度系统）

**功能**:
- **任务定义**: 定时任务、依赖任务
- **任务类型**:
  - 数据同步任务（数据源 → 集市）
  - 数据刷新任务（集市数据刷新）
  - SQL 任务（定时执行 SQL）
  - 推送任务（定时推送报表）
- **调度引擎**: 基于 Quartz / XXL-Job
- **DAG 依赖**: 任务间依赖关系（有向无环图）
- **任务监控**: 执行状态、日志、耗时
- **失败重试**: 配置重试次数与间隔
- **告警通知**: 任务失败自动告警

### M12 — data-khaos-notification（推送系统）

**功能**:
- **推送渠道**:
  - 站内信
  - 邮件
  - 企业微信/钉钉机器人（可选）
  - 短信（可选）
- **推送模板**: 消息模板管理
- **推送触发**: 手动推送 / 定时推送 / 事件触发
- **推送日志**: 推送记录与状态追踪
- **订阅管理**: 用户订阅感兴趣的报表/指标

---

## 六、数据库设计 — 达梦 DM8 适配说明

### 达梦数据库配置要点

1. **驱动**: `dm.jdbc.driver.DmDriver`
2. **URL 格式**: `jdbc:dm://<host>:<port>/<dbname>`
3. **默认端口**: 5236
4. **MyBatis-Plus 适配**:
   - 配置 `DmDialect` 方言
   - 使用 `DM` 分页插件
5. **主键策略**: 使用雪花算法生成 ID（VARCHAR(32)），避免达梦自增序列性能问题
6. **关键字处理**: 使用 `"` 作为标识符引用符（达梦兼容模式）

### 全局表前缀规范

- `sys_*` — 系统表（用户、角色、权限、组织）
- `app_*` — 业务表（申请、审批）
- `meta_*` — 元数据表
- `mart_*` — 集市表
- `schedule_*` — 调度表
- `notify_*` — 通知表

---

## 七、分阶段实施计划

### 第一阶段：基础架构搭建（P0）
| 任务 | 模块 | 说明 |
|------|------|------|
| 1.1 | 全部 | 创建 Maven 父工程，初始化各模块目录结构 |
| 1.2 | common | 公共工具类、统一返回、异常体系 |
| 1.3 | auth | 用户管理、角色管理、SSO 登录 |
| 1.4 | permission | RBAC 权限模型、菜单权限、权限注解 |
| 1.5 | gateway | 网关搭建、Token 校验 |

### 第二阶段：数据接入与元数据（P0）
| 任务 | 模块 | 说明 |
|------|------|------|
| 2.1 | datasource | 数据源管理、SPI 接口、Hive 连接器 |
| 2.2 | datasource | Doris 连接器、星环连接器、达梦连接器 |
| 2.3 | metadata | 元数据采集、库表结构管理 |
| 2.4 | permission | 行权限、列权限、表权限实现 |

### 第三阶段：权限审批与数据集市（P0）
| 任务 | 模块 | 说明 |
|------|------|------|
| 3.1 | approval | 权限申请、审批流、自动授权 |
| 3.2 | mart | 模型建模、指标管理、维度管理 |
| 3.3 | mart | 集市发布、数据刷新 |
| 3.4 | permission | 组织权限完善 |

### 第四阶段：查询与分析（P1）
| 任务 | 模块 | 说明 |
|------|------|------|
| 4.1 | query | SQL 查询平台、SQL 审核 |
| 4.2 | query | 查询执行、结果导出、权限拦截 |
| 4.3 | visual | 仪表板搭建（拖拽式布局、图表组件） |
| 4.4 | visual | 分析板搭建（实时分析、OLAP 操作） |

### 第五阶段：调度与推送（P1）
| 任务 | 模块 | 说明 |
|------|------|------|
| 5.1 | schedule | 调度引擎、任务定义、DAG 依赖 |
| 5.2 | schedule | 任务监控、失败重试、告警 |
| 5.3 | notification | 推送渠道、模板管理、订阅 |

### 第六阶段：集成与优化
| 任务 | 说明 |
|------|------|
| 6.1 | 全链路集成测试 |
| 6.2 | 性能优化 |
| 6.3 | 安全加固 |
| 6.4 | 部署文档与运维手册 |

---

## 八、项目目录结构

```
data-khaos/
├── README.md
├── LICENSE
├── pom.xml                              # 父 POM
├── docs/                                # 项目文档
│   ├── architecture.md
│   ├── db-design.md
│   └── api-reference.md
├── db/                                  # 数据库脚本
│   └── init.sql                         # 达梦数据库初始化脚本
├── docker/                              # Docker 部署文件
├── data-khaos-common/                   # 公共模块
│   ├── pom.xml
│   └── src/main/java/com/datakhaos/common/
│       ├── constant/
│       ├── exception/
│       ├── model/
│       ├── util/
│       └── config/
├── data-khaos-gateway/                  # API 网关
├── data-khaos-auth/                     # 认证中心
├── data-khaos-permission/               # 权限系统
├── data-khaos-approval/                 # 权限审批
├── data-khaos-datasource/               # 数据源接入
├── data-khaos-metadata/                 # 元数据中心
├── data-khaos-mart/                     # 数据集市
├── data-khaos-query/                    # SQL 查询平台
├── data-khaos-visual/                   # 可视化引擎
├── data-khaos-schedule/                 # 调度系统
└── data-khaos-notification/             # 推送系统
```

---

## 九、开源社区规范

### 分支管理
- `main` — 稳定版本
- `develop` — 开发分支
- `feature/*` — 功能分支
- `release/*` — 发布分支
- `hotfix/*` — 紧急修复分支

### PR 规范
- 每个 PR 关联一个 Issue
- 代码通过 CI 检查
- 包含单元测试

### Issue 模板
- Bug Report
- Feature Request
- Improvement

---

*本文档由 pmdream 创建，Data Khaos 项目 © 2026 pmdream。Licensed under Apache 2.0.*