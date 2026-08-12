import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getInfoApi, loginApi, logoutApi } from '@/api/auth'
import type { LoginRequest, LoginResponse, LoginUser } from '@/types'

const TOKEN_KEY = 'dk_token'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref<LoginUser | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  /** 登录：成功后保存 token 到 localStorage */
  async function login(payload: LoginRequest) {
    const data: LoginResponse = await loginApi(payload)
    token.value = data.token
    localStorage.setItem(TOKEN_KEY, data.token)
    userInfo.value = data.user
    roles.value = data.roles || []
    permissions.value = data.permissions || []
    return data
  }

  /** 拉取当前登录用户信息 */
  async function fetchInfo() {
    const data: LoginResponse = await getInfoApi()
    userInfo.value = data.user
    roles.value = data.roles || []
    permissions.value = data.permissions || []
    return data
  }

  /** 登出（后端无状态，清除前端令牌即可） */
  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 忽略登出接口异常，前端本地清除
    }
    reset()
  }

  /** 重置登录态 */
  function reset() {
    token.value = ''
    userInfo.value = null
    roles.value = []
    permissions.value = []
    localStorage.removeItem(TOKEN_KEY)
  }

  return { token, userInfo, roles, permissions, login, fetchInfo, logout, reset }
})
