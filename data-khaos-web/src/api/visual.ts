import { del, get, post, put } from './request'
import type { AdhocQueryRequest, PageResult, QueryResult, VisualDashboard, VisualDashboardItem } from '@/types'

/** 分页查询仪表板 */
export function pageDashboards(params: Record<string, any>) {
  return get<PageResult<VisualDashboard>>('/visual/dashboard/page', params)
}

/** 仪表板详情 */
export function getDashboard(id: string) {
  return get<VisualDashboard>(`/visual/dashboard/${id}`)
}

/** 新增仪表板 */
export function createDashboard(data: VisualDashboard) {
  return post<void>('/visual/dashboard', data)
}

/** 修改仪表板（注意后端为 PUT 无路径ID） */
export function updateDashboard(data: VisualDashboard) {
  return put<void>('/visual/dashboard', data)
}

/** 删除仪表板（级联删除组件） */
export function deleteDashboard(id: string) {
  return del<void>(`/visual/dashboard/${id}`)
}

/** 仪表板组件列表 */
export function listDashboardItems(dashboardId: string) {
  return get<VisualDashboardItem[]>(`/visual/dashboard/${dashboardId}/items`)
}

/** 新增/修改组件（有 id 则更新） */
export function saveItem(data: VisualDashboardItem) {
  return post<void>('/visual/item', data)
}

/** 删除组件 */
export function deleteItem(id: string) {
  return del<void>(`/visual/item/${id}`)
}

/** 即席分析查询 */
export function adhocQuery(data: AdhocQueryRequest) {
  return post<QueryResult>('/visual/analysis/execute', data)
}
