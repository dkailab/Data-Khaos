import { del, get, post, put } from './request'
import type { PageResult, SysUser } from '@/types'

/** 分页查询用户 */
export function pageUsers(params: Record<string, any>) {
  return get<PageResult<SysUser>>('/auth/user/page', params)
}

/** 查询用户详情 */
export function getUser(id: string) {
  return get<SysUser>(`/auth/user/${id}`)
}

/** 新增用户 */
export function createUser(data: SysUser) {
  return post<void>('/auth/user', data)
}

/** 更新用户 */
export function updateUser(id: string, data: SysUser) {
  return put<void>(`/auth/user/${id}`, data)
}

/** 删除用户 */
export function deleteUser(id: string) {
  return del<void>(`/auth/user/${id}`)
}

/** 重置密码 */
export function resetPassword(id: string, password: string) {
  return put<void>(`/auth/user/${id}/password`, { password })
}

/** 分配角色 */
export function assignRoles(id: string, roleIds: string[]) {
  return put<void>(`/auth/user/${id}/roles`, roleIds)
}
