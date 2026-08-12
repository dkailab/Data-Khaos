import { del, get, post, put } from './request'
import type { PageResult, ScheduleJob, ScheduleJobDep, ScheduleJobLog } from '@/types'

/** 分页查询任务 */
export function pageJobs(params: Record<string, any>) {
  return get<PageResult<ScheduleJob>>('/schedule/job/page', params)
}

/** 任务详情 */
export function getJob(id: string) {
  return get<ScheduleJob>(`/schedule/job/${id}`)
}

/** 新增任务 */
export function createJob(data: ScheduleJob) {
  return post<void>('/schedule/job', data)
}

/** 修改任务（注意后端为 PUT 无路径ID） */
export function updateJob(data: ScheduleJob) {
  return put<void>('/schedule/job', data)
}

/** 删除任务（级联删除日志与依赖） */
export function deleteJob(id: string) {
  return del<void>(`/schedule/job/${id}`)
}

/** 启用任务 */
export function startJob(id: string) {
  return post<void>(`/schedule/job/${id}/start`)
}

/** 停用任务 */
export function stopJob(id: string) {
  return post<void>(`/schedule/job/${id}/stop`)
}

/** 手动触发任务 */
export function runJob(id: string) {
  return post<void>(`/schedule/job/${id}/run`)
}

/** 分页查询执行日志 */
export function pageJobLogs(params: Record<string, any>) {
  return get<PageResult<ScheduleJobLog>>('/schedule/log/page', params)
}

/** 任务依赖列表 */
export function listJobDeps(jobId: string) {
  return get<ScheduleJobDep[]>(`/schedule/job/${jobId}/dep`)
}

/** 新增任务依赖 */
export function saveJobDep(jobId: string, dep: ScheduleJobDep) {
  return post<void>(`/schedule/job/${jobId}/dep`, dep)
}

/** 删除任务依赖 */
export function deleteJobDep(id: string) {
  return del<void>(`/schedule/dep/${id}`)
}
