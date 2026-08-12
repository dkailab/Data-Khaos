import { del, get, post, put } from './request'
import type { PageResult, SysColumnPolicy, SysRowPolicy } from '@/types'

/** 分页查询行权限策略 */
export function pageRowPolicies(params: Record<string, any>) {
  return get<PageResult<SysRowPolicy>>('/permission/policy/row/page', params)
}

/** 新增行权限策略 */
export function createRowPolicy(data: SysRowPolicy) {
  return post<void>('/permission/policy/row', data)
}

/** 更新行权限策略 */
export function updateRowPolicy(id: string, data: SysRowPolicy) {
  return put<void>(`/permission/policy/row/${id}`, data)
}

/** 删除行权限策略 */
export function deleteRowPolicy(id: string) {
  return del<void>(`/permission/policy/row/${id}`)
}

/** 分页查询列权限策略 */
export function pageColumnPolicies(params: Record<string, any>) {
  return get<PageResult<SysColumnPolicy>>('/permission/policy/column/page', params)
}

/** 新增列权限策略 */
export function createColumnPolicy(data: SysColumnPolicy) {
  return post<void>('/permission/policy/column', data)
}

/** 更新列权限策略 */
export function updateColumnPolicy(id: string, data: SysColumnPolicy) {
  return put<void>(`/permission/policy/column/${id}`, data)
}

/** 删除列权限策略 */
export function deleteColumnPolicy(id: string) {
  return del<void>(`/permission/policy/column/${id}`)
}
