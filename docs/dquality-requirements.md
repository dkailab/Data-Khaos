# Data-Khaos 数据质量稽核模块需求文档（v1.0.0）

> 模块：`data-khaos-dquality`（数据质量）+ `data-khaos-dquality-api`（契约）
> 定位：数据治理核心一环，为企业版（Enterprise）首要卖点，遵循 `permission-handbook.md` 权限模型与 `backend-development-guide.md` 开发规范。
> 状态：本MD为**需求能力文档**，据此进入开发。

---

## 1. 模块目标与价值

### 1.1 业务目标
- 对表 / 字段提供**质量稽核**，识别数据完整性、准确性、一致性、唯一性、空值率等问题。
- 通过**质量规则**（模板 + 自定义）周期性或手动执行，产出**质量报告**与**稽核评分**。
- 与**调度（schedule）**、**元数据（metadata）**、**通知（notification）**联动，形成"规则 → 调度 → 执行 → 报告 → 告警"闭环。

### 1.2 商业价值（企业版）
- 数据质量是**数据治理**的核心，也是企业付费刚需（合规、数据可信）。
- 此模块建议归入**企业版（私有仓库）**，作为 Open-Core 防白嫖的差异化卖点之一。

### 1.3 用户价值
- 数据团队能持续监控数据可信度，降低"脏数据"导致的决策风险。
- 通过评分与趋势，量化展示数据质量改善成果。

---

## 2. 权限模型（对接现有体系）

完全复用现有三级权限：**组织 → 项目组 → 人**，能力位 + 数据权限双约束。

### 2.1 新增能力位（写入 `PermissionConstants`）
| 能力位 | 说明 | 默认组长 | 默认开发者 | 默认使用者 |
|---|---|---|---|---|
| `quality:manage` | 质量规则/任务 CRUD、启停 | ✔ | ✔ | ✘ |
| `quality:browse` | 质量规则/报告/评分只读浏览 | ✔ | ✔ | ✔ |
| `quality:run` | 手动触发稽核执行 | ✔ | ✔ | ✘ |

> 对照 permission-handbook §4.2 能力位清单，新增「数据治理」能力域。

### 2.2 数据隔离（资源级）
- 质量**规则、任务、报告**均带 `project_group_id`，按**当前项目组上下文**过滤，互不可见。
- 质量稽核针对的**表**必须校验：当前用户对该表有 `SELECT` 权限（复用 `PermissionApiClient.checkTablePermission`，超级管理员跳过）。
- 执行稽核 SQL 前复用 `SqlAuditUtil.audit` 防注入（仅允许 SELECT）。

### 2.3 权限校验要点
| 操作 | 能力位 | 数据约束 |
|---|---|---|
| 规则列表/详情 | `quality:browse` | 仅当前项目组 |
| 规则 CRUD | `quality:manage` | 仅当前项目组 |
| 手动执行 | `quality:run` | 目标表需 SELECT 权限 |
| 报告/评分查看 | `quality:browse` | 仅当前项目组 |

---

## 3. 功能范围

### 3.1 质量规则（Rule）
- 规则模板：**非空校验、唯一性校验、值域校验、自定义 SQL 校验、自定义 SQL 探查**。
- 规则可绑定到：整表 / 单字段。
- 规则分**模板规则**（全局预置）与**用户规则**（项目组内创建，含 project_group_id）。
- 规则含**告警阈值**（如空值率 > 5% 告警）。

### 3.2 质量任务（Task）
- 一次性 / 周期执行（复用 schedule 触发，或模块内定时）。
- 手动触发：`quality:run`。
- 任务绑定规则集，执行顺序可定义。

### 3.3 稽核执行（Snapshot / Report）
- 每次执行生成质量**快照**：各规则通过/失败、空值率、唯一性、违规行数、耗时。
- 生成**质量评分**（0-100），按规则权重加权。
- 生成**质量报告**（结构化 JSON，供前端展示与导出）。

### 3.4 质量趋势与总览
- 按表/项目组查看质量评分趋势（历史快照）。
- 待办：失败规则列表、告警。

### 3.5 告警通知（联动 notification）
- 失败规则 / 评分跌破阈值 → 触发通知（复用现有通知模块或预留扩展点）。

---

## 4. 数据模型（表结构规划）

> 表前缀 `dquality_`，主键雪花 `VARCHAR(32)`，通用字段 `create_time`（BaseEntity），`update_time` 可选。

### 4.1 `dquality_rule` 质量规则
| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | VARCHAR(32) PK | 主键 |
| `project_group_id` | VARCHAR(32) | 项目组（权限隔离）；空=全局模板 |
| `rule_code` | VARCHAR(64) | 规则编码 |
| `rule_name` | VARCHAR(128) | 规则名称 |
| `rule_type` | VARCHAR(32) | 规则模板类型（见 §3.1）|
| `datasource_id` | VARCHAR(32) | 数据源ID |
| `database_name` | VARCHAR(128) | 库 |
| `table_name` | VARCHAR(128) | 表 |
| `column_name` | VARCHAR(128) | 字段（表级规则可空）|
| `rule_config` | TEXT | 规则配置 JSON（阈值/表达式/自定义SQL）|
| `weight` | INT | 权重（评分用，默认 1）|
| `alert_threshold` | DECIMAL(5,2) | 告警阈值（如空值率 0.05）|
| `status` | TINYINT | 0停用 1启用 |
| `create_by` | VARCHAR(32) | 创建人 |
| `create_time` | DATETIME | 创建时间 |

### 4.2 `dquality_task` 质量任务
| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | VARCHAR(32) PK | 主键 |
| `project_group_id` | VARCHAR(32) | 项目组隔离 |
| `task_name` | VARCHAR(128) | 任务名称 |
| `rule_ids` | TEXT | 关联规则ID集合（JSON数组）|
| `cron_expr` | VARCHAR(64) | 周期表达式（空=一次性/手动）|
| `status` | TINYINT | 0停用 1启用 |
| `create_by` / `create_time` | | 通用 |

### 4.3 `dquality_snapshot` 稽核快照（每次执行）
| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | VARCHAR(32) PK | 主键 |
| `project_group_id` | VARCHAR(32) | 项目组隔离 |
| `task_id` | VARCHAR(32) | 关联任务 |
| `datasource_id` / `database_name` / `table_name` | | 稽核对象 |
| `score` | DECIMAL(5,2) | 质量评分 0-100 |
| `rule_total` | INT | 规则总数 |
| `rule_pass` | INT | 通过数 |
| `rule_fail` | INT | 失败数 |
| `detail` | TEXT | 明细 JSON（各规则结果）|
| `cost_ms` | BIGINT | 耗时 |
| `trigger_type` | VARCHAR(16) | MANUAL / SCHEDULE |
| `create_by` / `create_time` | | 通用 |

### 4.4 `dquality_rule_result` 规则执行结果（可选，明细）
| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | VARCHAR(32) PK | 主键 |
| `snapshot_id` | VARCHAR(32) | 关联快照 |
| `rule_id` | VARCHAR(32) | 规则 |
| `passed` | TINYINT | 0失败 1通过 |
| `actual_value` | DECIMAL(20,4) | 实际值（如空值率）|
| `threshold` | DECIMAL(5,2) | 阈值 |
| `sample_rows` | TEXT | 违规样本（前 N 行 JSON）|
| `message` | VARCHAR(500) | 结果说明 |

---

## 5. 接口规划（REST，前缀 `/api/dquality`）

> 遵循开发规范：返回 `R<T>`、分页 `PageResult<T>`、`@Operation` 注解、构造器注入。

### 5.1 规则（Rule）
| 方法 | 路径 | 说明 | 能力位 |
|---|---|---|---|
| GET | `/rule/page` | 规则分页（project_group 过滤）| `quality:browse` |
| GET | `/rule/{id}` | 规则详情 | `quality:browse` |
| POST | `/rule` | 创建规则 | `quality:manage` |
| PUT | `/rule/{id}` | 更新规则 | `quality:manage` |
| DELETE | `/rule/{id}` | 删除规则 | `quality:manage` |
| GET | `/rule/template/options` | 规则模板下拉列表 | `quality:browse` |

### 5.2 任务（Task）
| 方法 | 路径 | 说明 | 能力位 |
|---|---|---|---|
| GET | `/task/page` | 任务分页 | `quality:browse` |
| POST | `/task` | 创建任务 | `quality:manage` |
| PUT | `/task/{id}` | 更新任务 | `quality:manage` |
| DELETE | `/task/{id}` | 删除任务 | `quality:manage` |
| POST | `/task/{id}/enable` | 启用/停用 | `quality:manage` |
| POST | `/task/{id}/run` | 手动执行 | `quality:run` |

### 5.3 快照 / 报告（Snapshot）
| 方法 | 路径 | 说明 | 能力位 |
|---|---|---|---|
| GET | `/snapshot/page` | 快照分页（按表/任务）| `quality:browse` |
| GET | `/snapshot/{id}` | 快照详情（含规则明细）| `quality:browse` |
| GET | `/snapshot/trend` | 评分趋势（按表）| `quality:browse` |
| GET | `/snapshot/overview` | 总览（评分、通过率、最差表 Top）| `quality:browse` |
| GET | `/snapshot/{id}/export` | 导出报告（CSV/JSON）| `quality:browse` |

---

## 6. 核心稽核逻辑（规则模板）

> 稽核 SQL 均通过 `DatasourceApiClient.executeRaw` 在目标数据源执行，先 `SqlAuditUtil.audit` 校验。

| 规则类型 | 逻辑 | 生成 SQL 示例 |
|---|---|---|
| 非空校验（NOT_NULL） | 统计某字段空值率 | `SELECT COUNT(*) total, COUNT(col) not_null FROM tbl` |
| 唯一性校验（UNIQUE） | 统计重复组数 | `SELECT COUNT(*) FROM (SELECT col FROM tbl GROUP BY col HAVING COUNT(*)>1) t` |
| 值域校验（VALUE_RANGE） | 统计越界行数 | `SELECT COUNT(*) total, COUNT(CASE WHEN col<min OR col>max THEN 1 END) bad FROM tbl` |
| 自定义 SQL（CUSTOM_SQL） | 用户提供返回"违规行"的 SQL，稽核行数 | 执行用户 SQL，取返回行数 |
| 自定义探查（CUSTOM_PROBE） | 用户提供统计 SQL，取首个数值结果 | 执行用户 SQL，取第一行第一列 |

**评分算法**：`score = Σ(pass_rule_weight) / Σ(all_rule_weight) × 100`
**告警判断**：`actual_value > threshold`（如空值率、重复率）→ 标记失败并可告警。

### 防注入与安全
- 规则配置中的 SQL 字段必须过 `SqlAuditUtil.audit`。
- 动态拼 WHERE 的列名做白名单校验（`safeColumn`），值单引号转义。
- 稽核只允许 SELECT，禁止写操作。

---

## 7. 前端交互（web）

### 7.1 页面结构（新菜单「数据治理 → 数据质量」）
| 页面 | 路由 | 说明 |
|---|---|---|
| 质量总览 | `/dquality/overview` | 评分、通过率、最差表 Top、趋势图 |
| 规则管理 | `/dquality/rule` | 规则列表 + 新建/编辑对话框（模板选择、绑定表/字段、阈值）|
| 任务管理 | `/dquality/task` | 任务列表 + 手动运行、启停 |
| 稽核报告 | `/dquality/snapshot` | 快照列表 + 详情（规则通过/失败、趋势）|

### 7.2 交互要点
- 规则创建时通过现有数据源接口拉取 库 → 表 → 字段（复用 `DatasourceApiClient` 对应 API）。
- 按 `quality:browse` / `quality:manage` / `quality:run` 显隐菜单与按钮。
- 报告页支持导出 CSV（带 UTF-8 BOM，防中文乱码）。

### 7.3 前端 API（`api/dquality.ts`）
- `pageRules / saveRule / deleteRule / ruleTemplateOptions`
- `pageTasks / saveTask / deleteTask / enableTask / runTask`
- `pageSnapshots / snapshotDetail / snapshotTrend / snapshotOverview / exportSnapshot`

---

## 8. 模块工程结构

### 8.1 `data-khaos-dquality`（业务服务）
```
com.datakhaos.dquality
├── DQualityApplication.java
├── controller/   DQualityRuleController / TaskController / SnapshotController
├── service/      RuleService / QualityTaskService / SnapshotService / QualityEngine(稽核引擎)
├── mapper/       DqRuleMapper / DqTaskMapper / DqSnapshotMapper / DqRuleResultMapper
├── entity/       DqRule / DqTask / DqSnapshot / DqRuleResult
├── dto/          RuleDto / TaskDto / SnapshotDto / OverviewDto / TrendDto
└── config/
```

### 8.2 `data-khaos-dquality-api`（契约模块，公开）
- 放 `DqRuleDto` 等 DTO + 可选 `DQualityApiClient`（供其他服务调用，如通知联动）。
- 不建表、不引 mybatis。

### 8.3 依赖
- `data-khaos-common`（必）
- `data-khaos-datasource-api`（执行稽核 SQL）
- `data-khaos-permission-api`（能力位 + 表权限校验）
- `data-khaos-schedule-api`（可选，接入调度）—— 若 schedule 无 api 模块则预留扩展

### 8.4 网关
- 新增路由 `/api/dquality/**` → `lb://data-khaos-dquality`
- 端口规划：内部 8080（docker-compose 映射宿主机新端口，避开 8099/8088 占用）

---

## 9. 与现有模块的联动

| 联动 | 说明 |
|---|---|
| metadata | 复用表/字段元数据，规则绑定目标表 |
| datasource | 执行稽核 SQL、拉取库表字段 |
| permission | 能力位校验 + 目标表 SELECT 权限校验 |
| schedule | 质量任务周期执行（可选，先做手动+简单定时）|
| notification | 失败告警通知（预留扩展点，可后续接入）|

---

## 10. 验收标准（Checklist）

- [ ] 权限：不同项目组看不到彼此的质量规则/任务/报告；无 `quality:manage` 无法 CRUD
- [ ] 表权限：无目标表 SELECT 权限用户执行稽核被拒
- [ ] 规则：5 类模板均可创建并正确稽核
- [ ] 评分：按权重加权计算正确
- [ ] 趋势：历史快照趋势正确
- [ ] 导出：CSV 带 UTF-8 BOM 无乱码
- [ ] 防注入：恶意 SQL 被 `SqlAuditUtil.audit` 拦截
- [ ] 规范：全接口 `R<T>` / `@Operation` / 构造器注入 / 实体继承 BaseEntity

---

## 11. 待评审 / 后续扩展

- [ ] 是否接入 schedule 做真正的 cron 周期执行（当前先手动 + 简单定时）
- [ ] 告警通知联动 notification 的具体实现
- [ ] 列级脱敏字段是否参与稽核（与列策略冲突如何处理）
- [ ] 是否支持"数据对比"（两个表/时段一致性校验）
- [ ] 规则模板是否做成可插拔 SPI（便于企业版扩展更多模板）