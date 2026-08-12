import { get, post } from './request'
import type { CaptchaResponse, LoginRequest, LoginResponse } from '@/types'

/** 登录 */
export function loginApi(data: LoginRequest) {
  return post<LoginResponse>('/auth/login', data)
}

/** 登出（后端无状态，仅清除前端令牌） */
export function logoutApi() {
  return post<void>('/auth/logout')
}

/** 获取图形验证码 */
export function getCaptcha() {
  return get<CaptchaResponse>('/auth/captcha')
}

/** 查询当前登录用户信息 */
export function getInfoApi() {
  return get<LoginResponse>('/auth/info')
}
