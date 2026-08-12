import { del, get, post, put } from './request'
import type { PageResult, SysOrganization } from '@/types'

/** 分页查询组织 */
export function pageOrgs(params: Record<string, any>) {
  return get<PageResult<SysOrganization>>('/permission/org/page', params)
}

/** 查询全部组织 */
export function listOrgs() {
  return get<SysOrganization[]>('/permission/org/list')
}

/** 新增组织 */
export function createOrg(data: SysOrganization) {
  return post<void>('/permission/org', data)
}

/** 更新组织 */
export function updateOrg(id: string, data: SysOrganization) {
  return put<void>(`/permission/org/${id}`, data)
}

/** 删除组织 */
export function deleteOrg(id: string) {
  return del<void>(`/permission/org/${id}`)
}

/** 查询组织树 */
export function orgTree() {
  return get<any[]>('/permission/org/tree')
}

/** 查询组织成员 */
export function orgUsers(id: string) {
  return get<any[]>(`/permission/org/${id}/users`)
}

/** 设置组织成员（全量替换） */
export function assignOrgUsers(id: string, userIds: string[]) {
  return put<void>(`/permission/org/${id}/users`, userIds)
}

/** 查询部门已授予的菜单权限 */
export function orgPermissions(id: string) {
  return get<string[]>(`/permission/org/${id}/permissions`)
}

/** 授予部门菜单权限（全量替换） */
export function assignOrgPermissions(id: string, menuIds: string[]) {
  return put<void>(`/permission/org/${id}/permissions`, menuIds)
}
