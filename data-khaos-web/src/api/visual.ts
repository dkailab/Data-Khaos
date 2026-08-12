import { del, get, post, put } from './request'
import type { AdhocQueryRequest, PageResult, QueryResult, VisualBoard, VisualDashboard, VisualDashboardItem, VisualDashboardVersion } from '@/types'

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

/** 执行组件查询（可选传分析板独立筛选 JSON） */
export function executeItem(id: string, filters?: string) {
  const params = filters ? { filters } : undefined
  return post<QueryResult>(`/visual/item/${id}/execute`, undefined, { params })
}

/** 组件下钻查询（点击图表数据点，按维度列=值下钻） */
export function drillItem(id: string, data: { column: string; value: string; filters?: string }) {
  return post<QueryResult>(`/visual/item/${id}/drill`, data)
}

/** 上线仪表板（生成版本快照） */
export function publishDashboard(id: string, remark?: string) {
  return post<number>(`/visual/dashboard/${id}/publish`, { remark })
}

/** 下线仪表板 */
export function unpublishDashboard(id: string) {
  return post<void>(`/visual/dashboard/${id}/unpublish`)
}

/** 版本列表 */
export function dashboardVersions(id: string) {
  return get<VisualDashboardVersion[]>(`/visual/dashboard/${id}/versions`)
}

/** 版本快照详情 */
export function versionDetail(versionId: string) {
  return get<VisualDashboardVersion>(`/visual/version/${versionId}`)
}

/** 回滚到指定版本 */
export function rollbackDashboard(id: string, versionId: string) {
  return post<void>(`/visual/dashboard/${id}/rollback/${versionId}`)
}

/** 即席分析查询 */
export function adhocQuery(data: AdhocQueryRequest) {
  return post<QueryResult>('/visual/analysis/execute', data)
}

/* ==================== 分析板 ==================== */

/** 分析板列表 */
export function listBoards(dashboardId: string) {
  return get<VisualBoard[]>(`/visual/board/${dashboardId}`)
}

/** 新增分析板 */
export function createBoard(data: VisualBoard) {
  return post<void>('/visual/board', data)
}

/** 修改分析板 */
export function updateBoard(data: VisualBoard) {
  return put<void>('/visual/board', data)
}

/** 删除分析板（级联删除组件） */
export function deleteBoard(id: string) {
  return del<void>(`/visual/board/${id}`)
}

/** 复制分析板（含组件） */
export function duplicateBoard(id: string) {
  return post<string>(`/visual/board/${id}/duplicate`)
}
