import { del, get, post, put } from './request'
import type { PageResult } from '@/types'

/* ==================== 用户画像 类型定义 ==================== */

/** 画像标签分类 */
export interface PortraitCategory {
  id: string
  name: string
  code?: string
  sortOrder?: number
  status?: number
  createTime?: string
  updateTime?: string
}

/** 画像标签定义 */
export interface PortraitTag {
  id: string
  categoryId: string
  name: string
  code?: string
  /** BOOL布尔 / NUMBER数值 / STR字符串 / ENUM枚举 */
  tagType?: string
  unit?: string
  enumOptions?: string
  description?: string
  status?: number
  createTime?: string
  updateTime?: string
}

/** 用户标签值 */
export interface PortraitUserTag {
  id?: string
  userKey: string
  userName?: string
  tagId: string
  tagValue?: string
  tagTime?: string
  createTime?: string
}

/** 标签分布统计项 */
export interface PortraitDistribution {
  value: string
  count: number
  ratio: number
}

/* ==================== 分类 API ==================== */

export function listPortraitCategories() {
  return get<PortraitCategory[]>('/visual/portrait/category/list')
}

export function createPortraitCategory(data: Partial<PortraitCategory>) {
  return post<string>('/visual/portrait/category', data)
}

export function updatePortraitCategory(id: string, data: Partial<PortraitCategory>) {
  return put<void>(`/visual/portrait/category/${id}`, data)
}

export function deletePortraitCategory(id: string) {
  return del<void>(`/visual/portrait/category/${id}`)
}

/* ==================== 标签 API ==================== */

export function pagePortraitTags(params: Record<string, any>) {
  return get<PageResult<PortraitTag>>('/visual/portrait/tag/page', params)
}

export function listPortraitTags(categoryId?: string) {
  return get<PortraitTag[]>('/visual/portrait/tag/list', categoryId ? { categoryId } : undefined)
}

export function getPortraitTag(id: string) {
  return get<PortraitTag>(`/visual/portrait/tag/${id}`)
}

export function createPortraitTag(data: Partial<PortraitTag>) {
  return post<string>('/visual/portrait/tag', data)
}

export function updatePortraitTag(id: string, data: Partial<PortraitTag>) {
  return put<void>(`/visual/portrait/tag/${id}`, data)
}

export function deletePortraitTag(id: string) {
  return del<void>(`/visual/portrait/tag/${id}`)
}

/* ==================== 用户标签值 API ==================== */

export function upsertPortraitUserTag(data: PortraitUserTag) {
  return post<void>('/visual/portrait/user-tag', data)
}

export function deletePortraitUserTag(id: string) {
  return del<void>(`/visual/portrait/user-tag/${id}`)
}

export function getPortraitUserTags(userKey: string) {
  return get<PortraitUserTag[]>(`/visual/portrait/user/${userKey}`)
}

/* ==================== 统计 API ==================== */

export function getPortraitDistribution(tagId: string) {
  return get<PortraitDistribution[]>('/visual/portrait/tag/distribution', { tagId })
}