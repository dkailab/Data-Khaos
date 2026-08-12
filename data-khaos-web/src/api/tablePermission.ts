import { del, get, post, put } from './request'
import type { PageResult, SysTablePermission } from '@/types'

/** 分页查询表权限 */
export function pageTablePermissions(params: Record<string, any>) {
  return get<PageResult<SysTablePermission>>('/permission/table/page', params)
}

/** 新增表权限 */
export function createTablePermission(data: SysTablePermission) {
  return post<void>('/permission/table', data)
}

/** 更新表权限 */
export function updateTablePermission(id: string, data: SysTablePermission) {
  return put<void>(`/permission/table/${id}`, data)
}

/** 删除表权限 */
export function deleteTablePermission(id: string) {
  return del<void>(`/permission/table/${id}`)
}

/** 校验用户对库表的操作权限 */
export function checkTablePermission(body: Record<string, string>) {
  return post<boolean>('/permission/table/check', body)
}
