import { del, get, post, put } from './request'
import type { DqRule, DqSnapshot, DqTask, PageResult } from '@/types'

/* ==================== 规则 ==================== */

export function pageRules(params: Record<string, any>) {
  return get<PageResult<DqRule>>('/dquality/rule/page', params)
}

export function getRule(id: string) {
  return get<DqRule>(`/dquality/rule/${id}`)
}

export function createRule(data: DqRule) {
  return post<void>('/dquality/rule', data)
}

export function updateRule(id: string, data: DqRule) {
  return put<void>(`/dquality/rule/${id}`, data)
}

export function deleteRule(id: string) {
  return del<void>(`/dquality/rule/${id}`)
}

export function ruleTemplateOptions() {
  return get<{ type: string; name: string; desc: string }[]>('/dquality/rule/template/options')
}

/* ==================== 任务 ==================== */

export function pageTasks(params: Record<string, any>) {
  return get<PageResult<DqTask>>('/dquality/task/page', params)
}

export function createTask(data: DqTask) {
  return post<void>('/dquality/task', data)
}

export function updateTask(id: string, data: DqTask) {
  return put<void>(`/dquality/task/${id}`, data)
}

export function deleteTask(id: string) {
  return del<void>(`/dquality/task/${id}`)
}

export function enableTask(id: string, enabled: boolean) {
  return post<void>(`/dquality/task/${id}/enable?enabled=${enabled}`)
}

export function runTask(id: string) {
  return post<DqSnapshot>(`/dquality/task/${id}/run`)
}

/* ==================== 快照 / 报告 ==================== */

export function pageSnapshots(params: Record<string, any>) {
  return get<PageResult<DqSnapshot>>('/dquality/snapshot/page', params)
}

export function snapshotDetail(id: string) {
  return get<{ snapshot: DqSnapshot; results: DqRuleResult[]; taskName: string }>(`/dquality/snapshot/${id}`)
}

export function snapshotTrend(params: Record<string, any>) {
  return get<{ snapshotTime: string; score: number; passRate: number }[]>('/dquality/snapshot/trend', params)
}

export function snapshotOverview() {
  return get<any>('/dquality/snapshot/overview')
}

export function exportSnapshot(id: string) {
  return get<Blob>(`/dquality/snapshot/${id}/export`, {}, { responseType: 'blob' })
}

export interface DqRuleResult {
  id?: string
  snapshotId?: string
  ruleId?: string
  ruleName?: string
  ruleType?: string
  passed?: number
  actualValue?: number
  threshold?: number
  sampleRows?: string
  message?: string
}