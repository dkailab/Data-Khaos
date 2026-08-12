import { get, post } from './request'
import type { UserPermissionDto } from '@/types'

/** 查询用户权限视图（角色/权限/菜单） */
export function getUserPermission(userId: string) {
  return get<UserPermissionDto>(`/permission/user/${userId}`)
}

/** 查询用户权限标识集合 */
export function getUserPermissions(userId: string) {
  return get<string[]>(`/permission/user/${userId}/permissions`)
}

/** 绑定角色菜单权限 */
export function assignRolePermissions(roleId: string, menuIds: string[]) {
  return post<void>(`/permission/role/${roleId}/permissions`, menuIds)
}

/** 查询角色已绑定的菜单权限 */
export function getRolePermissions(roleId: string) {
  return get<string[]>(`/permission/role/${roleId}/permissions`)
}
