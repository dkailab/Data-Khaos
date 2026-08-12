import { get, post } from './request'
import type { AppApply, AppApprovalFlow, ApplyRequest, ApprovalActionRequest, PageResult, TransferRequest } from '@/types'

/** 提交权限申请 */
export function submitApply(data: ApplyRequest, applicantId?: string) {
  return post<AppApply>('/approval/apply', data, {
    params: applicantId ? { applicantId } : undefined,
  })
}

/** 我的申请 */
export function pageMyApplies(params: Record<string, any>) {
  return get<PageResult<AppApply>>('/approval/apply/page', params)
}

/** 待审批列表 */
export function pagePendingApplies(params: Record<string, any>) {
  return get<PageResult<AppApply>>('/approval/apply/pending', params)
}

/** 全部申请（审批管理） */
export function pageAllApplies(params: Record<string, any>) {
  return get<PageResult<AppApply>>('/approval/apply/all', params)
}

/** 申请详情（含审批记录） */
export function applyDetail(id: string) {
  return get<{ apply: AppApply; records: any[] }>(`/approval/apply/${id}`)
}

/** 通过 */
export function approveApply(id: string, data?: ApprovalActionRequest) {
  return post<void>(`/approval/apply/${id}/approve`, data)
}

/** 驳回 */
export function rejectApply(id: string, data?: ApprovalActionRequest) {
  return post<void>(`/approval/apply/${id}/reject`, data)
}

/** 转交 */
export function transferApply(id: string, data: TransferRequest) {
  return post<void>(`/approval/apply/${id}/transfer`, data)
}

/** 撤销申请 */
export function cancelApply(id: string) {
  return post<void>(`/approval/apply/${id}/cancel`)
}

/** 审批流程定义列表 */
export function listFlows() {
  return get<AppApprovalFlow[]>('/approval/flow/list')
}
