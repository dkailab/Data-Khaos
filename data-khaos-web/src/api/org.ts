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
