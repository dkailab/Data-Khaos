import { del, get, post, put } from './request'
import type { PageResult, SysRole } from '@/types'

/** 分页查询角色 */
export function pageRoles(params: Record<string, any>) {
  return get<PageResult<SysRole>>('/auth/role/page', params)
}

/** 查询全部启用角色 */
export function listRoles() {
  return get<SysRole[]>('/auth/role/list')
}

/** 新增角色 */
export function createRole(data: SysRole) {
  return post<void>('/auth/role', data)
}

/** 更新角色 */
export function updateRole(id: string, data: SysRole) {
  return put<void>(`/auth/role/${id}`, data)
}

/** 删除角色 */
export function deleteRole(id: string) {
  return del<void>(`/auth/role/${id}`)
}
