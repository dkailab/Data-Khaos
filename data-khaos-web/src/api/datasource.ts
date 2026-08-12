import { del, get, post, put } from './request'
import type { ColumnInfo, DsConfig, MetaDatasource, PageResult, QueryResult } from '@/types'

/** 分页查询数据源 */
export function pageDatasources(params: Record<string, any>) {
  return get<PageResult<MetaDatasource>>('/ds/page', params)
}

/** 数据源详情 */
export function getDatasource(id: string) {
  return get<MetaDatasource>(`/ds/${id}`)
}

/** 新增数据源 */
export function createDatasource(data: MetaDatasource) {
  return post<void>('/ds', data)
}

/** 修改数据源（密码留空表示不修改；注意后端为 PUT 无路径ID） */
export function updateDatasource(data: MetaDatasource) {
  return put<void>('/ds', data)
}

/** 删除数据源 */
export function deleteDatasource(id: string) {
  return del<void>(`/ds/${id}`)
}

/** 测试连接（未保存的配置） */
export function testDatasourceConfig(config: DsConfig) {
  return post<boolean>('/ds/test', config)
}

/** 测试连接（已保存的数据源） */
export function testDatasourceById(id: string) {
  return post<boolean>(`/ds/${id}/test`)
}

/** 获取数据库列表 */
export function listDatabases(id: string) {
  return get<string[]>(`/ds/${id}/databases`)
}

/** 获取表列表 */
export function listTables(id: string, database: string) {
  return get<string[]>(`/ds/${id}/tables/${database}`)
}

/** 获取字段名列表 */
export function listColumns(id: string, database: string, table: string) {
  return get<string[]>(`/ds/${id}/columns/${database}/${table}`)
}

/** 获取字段详情列表 */
export function listColumnInfos(id: string, database: string, table: string) {
  return get<ColumnInfo[]>(`/ds/${id}/column-info/${database}/${table}`)
}

/** 执行 SQL（自动 SQL 审核） */
export function executeSql(id: string, sql: string, params?: Record<string, any>) {
  return post<QueryResult>(`/ds/${id}/execute`, { sql, params })
}

/** 统计表行数 */
export function tableCount(id: string, database: string, table: string) {
  return get<number>(`/ds/${id}/count/${database}/${table}`)
}
