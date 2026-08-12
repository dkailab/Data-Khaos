import service, { get, post } from './request'
import type { PageResult, QueryExecuteRequest, QueryHistory, QueryResult } from '@/types'

/** 执行 SQL（自动审核 + 可选表权限校验） */
export function executeQuery(data: QueryExecuteRequest) {
  return post<QueryResult>('/query/execute', data)
}

/** 导出查询结果为 CSV */
export async function exportQueryResult(data: QueryExecuteRequest) {
  const resp = await service.post('/query/export', data, { responseType: 'text' })
  const csv = (resp as any).data as string
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'query_result.csv'
  a.click()
  URL.revokeObjectURL(url)
}

/** 查询历史（分页） */
export function queryHistory(params: Record<string, any>) {
  return get<PageResult<QueryHistory>>('/query/history', params)
}

/** 查询历史详情 */
export function queryHistoryDetail(id: string) {
  return get<QueryHistory>(`/query/history/${id}`)
}
