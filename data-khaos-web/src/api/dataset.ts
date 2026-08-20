import { del, get, post, put } from './request'
import type { PageResult } from '@/types'

/* ==================== 数据集字段定义 ==================== */

export interface DatasetField {
  id?: string
  fieldName: string
  fieldCode: string
  /** DIMENSION / METRIC */
  fieldType: string
  /** STRING / INTEGER / DECIMAL / DATE */
  dataType?: string
  /** SUM / AVG / COUNT / COUNT_DISTINCT / MAX / MIN */
  aggType?: string
  format?: string
  sortOrder?: number
}

export interface DatasetVariable {
  varName: string
  varType: string
  defaultValue: string
}

/* ==================== 数据集 ==================== */

export interface VisualDataset {
  id?: string
  name: string
  code: string
  description?: string
  /** SQL / MODEL */
  datasetType: string
  datasourceId?: string
  /** 数据源类型（MYSQL/HIVE/DORIS 等，联查兼容性判断用） */
  datasourceType?: string
  querySql?: string
  modelId?: string
  refreshInterval?: number
  visibility?: string
  status?: string
  version?: number
  createBy?: string
  createTime?: string
  updateTime?: string
  fields?: DatasetField[]
  variables?: DatasetVariable[]
}

export interface DatasetPreviewResult {
  columns: string[]
  rows: Record<string, any>[]
}

/* ==================== 图表绘制页（BI Chart Builder） ==================== */

export interface ChartQueryFieldRef {
  fieldCode: string
  sort?: string
}

export interface ChartQueryMetricRef {
  fieldCode: string
  aggType?: string
}

export interface ChartQueryFilterRef {
  fieldCode: string
  /** EQ / NE / GT / GTE / LT / LTE / IN / NOT_IN / LIKE / BETWEEN */
  operator: string
  values?: string[]
}

export interface ChartQuerySortRef {
  fieldCode: string
  direction: string
}

export interface DatasetChartQueryRequest {
  datasetId: string
  dimensions?: ChartQueryFieldRef[]
  metrics?: ChartQueryMetricRef[]
  filters?: ChartQueryFilterRef[]
  sorts?: ChartQuerySortRef[]
  limit?: number
}

export interface DatasetChartQueryResult {
  sql: string
  result: import('@/types').QueryResult
  truncated?: boolean
  originalRowCount?: number
}

/* ==================== API ==================== */

/** 分页查询数据集 */
export function pageDatasets(params: Record<string, any>) {
  return get<PageResult<VisualDataset>>('/visual/dataset/page', params)
}

/** 数据集详情 */
export function getDataset(id: string) {
  return get<VisualDataset>(`/visual/dataset/${id}`)
}

/** 创建数据集 */
export function createDataset(data: VisualDataset) {
  return post<string>('/visual/dataset', data)
}

/** 更新数据集 */
export function updateDataset(id: string, data: VisualDataset) {
  return put<void>(`/visual/dataset/${id}`, data)
}

/** 删除数据集 */
export function deleteDataset(id: string) {
  return del<void>(`/visual/dataset/${id}`)
}

/** 发布数据集 */
export function publishDataset(id: string, remark?: string) {
  return post<number>(`/visual/dataset/${id}/publish`, null, { params: { remark } })
}

/** 下线数据集 */
export function unpublishDataset(id: string) {
  return post<void>(`/visual/dataset/${id}/unpublish`)
}

/** 测试SQL查询(返回字段+前100数据) */
export function previewDataset(datasourceId: string, querySql: string) {
  return post<DatasetPreviewResult>('/visual/dataset/preview', { datasourceId, querySql })
}

/** 根据模型提取字段 */
export function extractFieldsFromModel(modelId: string) {
  return get<DatasetField[]>('/visual/dataset/extract-fields', { modelId })
}

/** 已发布数据集列表（图表绘制页资产池，含字段定义与数据源类型） */
export function listPublishedDatasets() {
  return get<VisualDataset[]>('/visual/dataset/list')
}

/** 图表聚合查询：数据集 + 维度/指标/筛选/排序 → 服务端生成 SQL 并执行 */
export function queryDatasetChart(data: DatasetChartQueryRequest) {
  return post<DatasetChartQueryResult>('/visual/dataset/query', data)
}
