import { del, get, post, put } from './request'
import type {
  MartDimension,
  MartDimLevel,
  MartMetric,
  MartModel,
  MartModelRel,
  PageResult,
  QueryResult,
} from '@/types'

/* ==================== 模型 ==================== */

export function pageMartModels(params: Record<string, any>) {
  return get<PageResult<MartModel>>('/mart/model/page', params)
}

export function martModelDetail(id: string) {
  return get<{ model: MartModel; metrics: MartMetric[]; dimensions: MartDimension[]; rels: MartModelRel[] }>(
    `/mart/model/${id}`,
  )
}

export function createMartModel(data: MartModel) {
  return post<void>('/mart/model', data)
}

export function updateMartModel(data: MartModel) {
  return put<void>('/mart/model', data)
}

export function deleteMartModel(id: string) {
  return del<void>(`/mart/model/${id}`)
}

/** 发布模型 */
export function publishMartModel(id: string) {
  return post<void>(`/mart/model/${id}/publish`)
}

/** 下线模型 */
export function offlineMartModel(id: string) {
  return post<void>(`/mart/model/${id}/offline`)
}

/** 预览模型数据（事实表前 100 行） */
export function previewMartModel(id: string) {
  return get<QueryResult>(`/mart/model/${id}/preview`)
}

/* ==================== 指标 ==================== */

export function pageMartMetrics(params: Record<string, any>) {
  return get<PageResult<MartMetric>>('/mart/metric/page', params)
}

export function createMartMetric(data: MartMetric) {
  return post<void>('/mart/metric', data)
}

export function updateMartMetric(data: MartMetric) {
  return put<void>('/mart/metric', data)
}

export function deleteMartMetric(id: string) {
  return del<void>(`/mart/metric/${id}`)
}

/* ==================== 维度 ==================== */

export function pageMartDimensions(params: Record<string, any>) {
  return get<PageResult<MartDimension>>('/mart/dimension/page', params)
}

export function createMartDimension(data: MartDimension) {
  return post<void>('/mart/dimension', data)
}

export function updateMartDimension(data: MartDimension) {
  return put<void>('/mart/dimension', data)
}

export function deleteMartDimension(id: string) {
  return del<void>(`/mart/dimension/${id}`)
}

/** 维度层级列表 */
export function listDimLevels(dimId: string) {
  return get<MartDimLevel[]>(`/mart/dimension/${dimId}/levels`)
}

/** 保存维度层级（全量替换） */
export function saveDimLevels(dimId: string, levels: MartDimLevel[]) {
  return post<void>(`/mart/dimension/${dimId}/levels`, levels)
}

/* ==================== 模型关联 ==================== */

export function listModelRels(modelId: string) {
  return get<MartModelRel[]>(`/mart/model/${modelId}/rel`)
}

export function saveModelRel(modelId: string, rel: MartModelRel) {
  return post<void>(`/mart/model/${modelId}/rel`, rel)
}

export function deleteModelRel(id: string) {
  return del<void>(`/mart/rel/${id}`)
}
