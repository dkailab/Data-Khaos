/**
 * 后端统一返回包装 R<T>：
 * 注意：字段为 code / msg / data，成功时 code === 0（非 200）。
 */
export interface R<T = any> {
  code: number
  msg: string
  data: T
  timestamp: number
}

/** 通用分页返回 PageResult */
export interface PageResult<T = any> {
  total: number
  pages: number
  current: number
  size: number
  records: T[]
}

/** 通用分页查询参数 */
export interface PageQuery {
  current: number
  size: number
  [key: string]: any
}

/* ==================== 认证 / 用户 / 角色 ==================== */

export interface LoginRequest {
  username: string
  password: string
  captchaId?: string
  captchaCode?: string
}

export interface CaptchaResponse {
  captchaId: string
  imageBase64: string
}

export interface LoginUser {
  id: string
  username: string
  realName: string
  avatar?: string
  status?: number
  lastLoginTime?: string
}

export interface LoginResponse {
  token: string
  expireIn: number
  user: LoginUser
  roles: string[]
  permissions: string[]
}

export interface SysUser {
  id?: string
  username?: string
  password?: string
  realName?: string
  email?: string
  phone?: string
  avatar?: string
  /** 1:启用 0:禁用 */
  status?: number
  createTime?: string
  updateTime?: string
}

export interface SysRole {
  id?: string
  roleCode?: string
  roleName?: string
  description?: string
  /** 1:启用 0:禁用 */
  status?: number
  createTime?: string
}

/* ==================== 权限（菜单 / 组织 / 策略 / 表权限） ==================== */

export interface SysMenu {
  id?: string
  parentId?: string
  name?: string
  path?: string
  component?: string
  permission?: string
  icon?: string
  /** 0:目录 1:菜单 2:按钮 3:API */
  type?: number
  sortOrder?: number
  status?: number
  createTime?: string
}

export interface SysOrganization {
  id?: string
  parentId?: string
  orgName?: string
  orgCode?: string
  /** DEPT / COMPANY / GROUP */
  orgType?: string
  sortOrder?: number
  status?: number
  createTime?: string
}

export interface SysRowPolicy {
  id?: string
  policyName?: string
  targetTable?: string
  /** 过滤表达式，支持 #{currentUserId}/#{currentOrgId} */
  expression?: string
  expressionDesc?: string
  roleId?: string
  userId?: string
  status?: number
  createTime?: string
}

export interface SysColumnPolicy {
  id?: string
  policyName?: string
  targetTable?: string
  columnName?: string
  /** MASK / ENCRYPT / HIDE / PLAIN */
  maskType?: string
  /** 脱敏规则，如 left:3,right:4 */
  maskRule?: string
  roleId?: string
  userId?: string
  status?: number
  createTime?: string
}

export interface SysTablePermission {
  id?: string
  datasourceId?: string
  databaseName?: string
  tableName?: string
  /** SELECT / INSERT / UPDATE / DELETE / ALL */
  permissionType?: string
  roleId?: string
  userId?: string
  /** ROLE / USER */
  grantType?: string
  status?: number
  createTime?: string
}

export interface UserPermissionDto {
  userId: string
  roles: string[]
  permissions: string[]
  menus: MenuDto[]
}

export interface MenuDto {
  id: string
  parentId?: string
  name: string
  path?: string
  component?: string
  permission?: string
  icon?: string
  /** 0:目录 1:菜单 2:按钮 3:API */
  type?: number
  sortOrder?: number
}

/* ==================== 审批 ==================== */

export interface ApplyRequest {
  /** 申请类型：TABLE / REPORT / DATASOURCE / MENU */
  applyType: string
  /** 申请目标ID（TABLE 时为数据源ID） */
  targetId?: string
  /** 申请目标名称（TABLE 时为 database.table） */
  targetName?: string
  reason?: string
}

export interface AppApply {
  id?: string
  applicantId?: string
  applyType?: string
  targetId?: string
  targetName?: string
  reason?: string
  /** 0:待审批 1:通过 2:驳回 3:已撤销 */
  status?: number
  currentApprover?: string
  createTime?: string
  updateTime?: string
}

export interface ApprovalActionRequest {
  approverId?: string
  comment?: string
}

export interface TransferRequest {
  toApproverId: string
  comment?: string
}

export interface AppApprovalRecord {
  id?: string
  applyId?: string
  approverId?: string
  /** 1:通过 2:驳回 3:转交 */
  action?: number
  comment?: string
  createTime?: string
}

export interface AppApprovalFlow {
  id?: string
  flowName?: string
  applyType?: string
  stepOrder?: number
  approverRole?: string
}

/* ==================== 数据源 ==================== */

export interface MetaDatasource {
  id?: string
  dsName?: string
  /** MYSQL / DM8 / HIVE / DORIS / CLICKHOUSE / POSTGRESQL / ORACLE / TRANSWARP */
  dsType?: string
  host?: string
  port?: number
  databaseName?: string
  username?: string
  /** 密码仅写入，接口不回传 */
  password?: string
  properties?: string
  status?: number
  createTime?: string
  updateTime?: string
}

export interface DsConfig {
  id?: string
  dsName?: string
  dsType?: string
  host?: string
  port?: number
  databaseName?: string
  username?: string
  password?: string
  properties?: string
}

export interface ColumnInfo {
  columnName?: string
  columnType?: string
  columnLength?: number
  columnScale?: number
  nullable?: boolean
  primaryKey?: boolean
  defaultValue?: string
  description?: string
  sortOrder?: number
  sensitiveLevel?: number
}

export interface QueryResult {
  columns: ColumnInfo[]
  rows: Record<string, any>[]
  rowCount?: number
  costMs?: number
  /** 是否写操作（DDL/DML 非查询） */
  update?: boolean
}

/* ==================== 元数据 ==================== */

export interface MetaDatabase {
  id?: string
  datasourceId?: string
  databaseName?: string
  description?: string
  syncTime?: string
}

export interface MetaTable {
  id?: string
  /** 数据库记录ID（meta_database.id） */
  databaseId?: string
  tableName?: string
  /** TABLE / VIEW */
  tableType?: string
  description?: string
  rowCount?: number
  tableSize?: number
  syncTime?: string
  updateTime?: string
}

export interface MetaColumn {
  id?: string
  /** 表记录ID（meta_table.id） */
  tableId?: string
  columnName?: string
  columnType?: string
  columnLength?: number
  columnScale?: number
  /** 是否可空 1:是 0:否 */
  isNullable?: number
  /** 是否主键 1:是 0:否 */
  isPrimaryKey?: number
  defaultValue?: string
  description?: string
  sortOrder?: number
  /** 敏感级别 0:普通 1:敏感 2:高度敏感 */
  sensitiveLevel?: number
}

export interface MetaTableLineage {
  id?: string
  sourceTableId?: string
  targetTableId?: string
  sourceColumn?: string
  targetColumn?: string
  /** ETL / MANUAL */
  relationType?: string
}

/* ==================== 数据集市 ==================== */

export interface MartModel {
  id?: string
  modelName?: string
  modelCode?: string
  /** STAR / SNOWFLAKE */
  modelType?: string
  datasourceId?: string
  description?: string
  /** 0:草稿 1:已发布 2:下线 */
  status?: number
  version?: number
  createTime?: string
  updateTime?: string
}

export interface MartMetric {
  id?: string
  metricName?: string
  metricCode?: string
  /** ATOMIC / DERIVED */
  metricType?: string
  expression?: string
  dataType?: string
  unit?: string
  categoryId?: string
  modelId?: string
  description?: string
  status?: number
  createTime?: string
  updateTime?: string
}

export interface MartDimension {
  id?: string
  dimName?: string
  dimCode?: string
  /** COMMON / TIME / ORG */
  dimType?: string
  modelId?: string
  sourceTable?: string
  sourceColumn?: string
  description?: string
  status?: number
  createTime?: string
}

export interface MartDimLevel {
  id?: string
  dimId?: string
  levelName?: string
  levelColumn?: string
  levelOrder?: number
}

export interface MartModelRel {
  id?: string
  modelId?: string
  factTable?: string
  dimTable?: string
  joinKey?: string
  /** INNER / LEFT / RIGHT */
  joinType?: string
}

/* ==================== SQL 查询 ==================== */

export interface QueryExecuteRequest {
  datasourceId: string
  databaseName?: string
  sql: string
}

export interface QueryHistory {
  id?: string
  userId?: string
  datasourceId?: string
  databaseName?: string
  sqlText?: string
  /** 1:成功 0:失败 */
  status?: number
  costMs?: number
  rowCount?: number
  errorMessage?: string
  createTime?: string
}

/* ==================== 可视化 ==================== */

export interface VisualDashboard {
  id?: string
  name?: string
  description?: string
  layout?: string
  refreshInterval?: number
  /** 0:停用 1:草稿 2:已上线 */
  status?: number
  /** 当前版本号 */
  version?: number
  createBy?: string
  createTime?: string
  updateTime?: string
}

/** 组件图表类型 */
export type ChartType =
  | 'BAR'
  | 'LINE'
  | 'PIE'
  | 'SCATTER'
  | 'HEATMAP'
  | 'AREA'
  | 'GAUGE'
  | 'TREEMAP'
  | 'BOXPLOT'
  | 'MAP'
  | 'TABLE'
  | 'NUMBER'

export interface VisualDashboardItem {
  id?: string
  dashboardId?: string
  /** 所属分析板ID */
  boardId?: string
  title?: string
  /** BAR / LINE / PIE / SCATTER / HEATMAP / AREA / GAUGE / TREEMAP / BOXPLOT / MAP / TABLE / NUMBER */
  chartType?: ChartType
  datasourceId?: string
  querySql?: string
  /** 下钻明细SQL（可选，配置后点击图表下钻） */
  drillSql?: string
  /** 组件配置(JSON)：xAxisColumn / seriesColumn / valueColumn / mapName 等 */
  config?: string
  posX?: number
  posY?: number
  width?: number
  height?: number
  createTime?: string
}

export interface VisualDashboardVersion {
  id?: string
  dashboardId?: string
  version?: number
  name?: string
  description?: string
  layout?: string
  refreshInterval?: number
  /** 组件快照(JSON数组字符串) */
  itemsJson?: string
  /** 分析板快照(JSON数组字符串) */
  boardsJson?: string
  remark?: string
  createBy?: string
  createTime?: string
}

/** 分析板（仪表板内嵌套子业务模块） */
export interface VisualBoard {
  id?: string
  dashboardId?: string
  boardName?: string
  subtitle?: string
  icon?: string
  /** ANALYSIS / CUSTOM */
  boardType?: string
  /** 板块样式与布局配置(JSON) */
  layout?: string
  /** 分析板独立筛选配置(JSON) */
  filters?: string
  /** 是否联动全局筛选 1:联动 0:独立 */
  linkGlobal?: number
  /** 自动刷新周期(秒) */
  refreshInterval?: number
  /** 0:展开 1:折叠 */
  collapse?: number
  /** 布局锁定 */
  locked?: number
  sortOrder?: number
  status?: number
  createTime?: string
  updateTime?: string
}

export interface AdhocQueryRequest {
  datasourceId: string
  sql: string
}

/* ==================== 调度 ==================== */

export interface ScheduleJob {
  id?: string
  jobName?: string
  /** SYNC / SQL / REFRESH / PUSH */
  jobType?: string
  jobGroup?: string
  cronExpression?: string
  datasourceId?: string
  targetSql?: string
  targetTable?: string
  params?: string
  /** 0:停用 1:启用 */
  status?: number
  retryCount?: number
  retryInterval?: number
  timeout?: number
  createTime?: string
  updateTime?: string
}

export interface ScheduleJobLog {
  id?: string
  jobId?: string
  /** 0:运行中 1:成功 2:失败 */
  status?: number
  startTime?: string
  endTime?: string
  durationMs?: number
  errorMessage?: string
  resultRows?: number
}

export interface ScheduleJobDep {
  id?: string
  jobId?: string
  depJobId?: string
  /** HARD / SOFT */
  depType?: string
}

/* ==================== 通知 ==================== */

export interface NotifyTemplate {
  id?: string
  templateCode?: string
  templateName?: string
  /** MAIL / SITE / WECHAT / SMS */
  channel?: string
  titleTemplate?: string
  contentTemplate?: string
  status?: number
  createTime?: string
}

export interface SendRequest {
  templateCode: string
  /** USER 为用户ID；ROLE/ORG 为角色/组织ID */
  receiverId: string
  /** USER / ROLE / ORG */
  receiverType: string
  /** 渠道（缺省取模板渠道） */
  channel?: string
  /** 模板变量（如 title / content / jobName / message） */
  vars?: Record<string, any>
}

export interface NotifyRecord {
  id?: string
  templateId?: string
  receiverId?: string
  receiverType?: string
  channel?: string
  title?: string
  content?: string
  /** 0:待发送 1:已发送 2:发送失败 */
  status?: number
  sendTime?: string
  errorMessage?: string
  createTime?: string
}

export interface NotifySubscription {
  id?: string
  userId?: string
  /** REPORT / METRIC / JOB */
  subscribeType?: string
  targetId?: string
  channel?: string
  status?: number
  createTime?: string
}
