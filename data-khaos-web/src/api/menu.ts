import { del, get, post, put } from './request'
import type { PageResult, SysMenu } from '@/types'

/** 分页查询菜单 */
export function pageMenus(params: Record<string, any>) {
  return get<PageResult<SysMenu>>('/permission/menu/page', params)
}

/** 查询全部菜单 */
export function listMenus() {
  return get<SysMenu[]>('/permission/menu/list')
}

/** 新增菜单 */
export function createMenu(data: SysMenu) {
  return post<void>('/permission/menu', data)
}

/** 更新菜单 */
export function updateMenu(id: string, data: SysMenu) {
  return put<void>(`/permission/menu/${id}`, data)
}

/** 删除菜单 */
export function deleteMenu(id: string) {
  return del<void>(`/permission/menu/${id}`)
}

/** 全部菜单（权限视图） */
export function allMenus() {
  return get<SysMenu[]>('/permission/menu/all')
}
