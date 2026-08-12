import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { R } from '@/types'

/**
 * Axios 实例：
 * - baseURL 取 .env.development 的 VITE_API_BASE（/api），vite dev server 代理到网关 8080
 * - 请求拦截器：注入 Authorization: Bearer <token>（后端 AuthGlobalFilter 校验 JWT）
 * - 响应拦截器：统一解包 R<T>（code === 0 成功），401 跳转登录，统一错误提示
 */
const service = axios.create({
  baseURL: (import.meta.env.VITE_API_BASE as string) || '/api',
  timeout: 60000,
})

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('dk_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

function toLogin() {
  localStorage.removeItem('dk_token')
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

service.interceptors.response.use(
  (response) => {
    // 文件流 / 纯文本（CSV 导出等）直接返回原始内容
    const respType = response.config.responseType
    if (respType === 'blob' || respType === 'text') {
      return response
    }
    const res = response.data as R
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 0) {
        return res.data as any
      }
      ElMessage.error(res.msg || '操作失败')
      if (res.code === 401 || res.code === 4010 || res.code === 4011 || res.code === 403) {
        toLogin()
      }
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  (error) => {
    const status = error.response?.status
    const data = error.response?.data
    if (status === 401) {
      ElMessage.error('登录已过期或未登录')
      toLogin()
    } else {
      ElMessage.error(data?.msg || error.message || '网络异常')
    }
    return Promise.reject(error)
  },
)

/** 统一封装的 GET 请求（已解包 R.data） */
export function get<T>(url: string, params?: Record<string, any>, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, { params, ...config }) as unknown as Promise<T>
}

/** 统一封装的 POST 请求（已解包 R.data） */
export function post<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config) as unknown as Promise<T>
}

/** 统一封装的 PUT 请求（已解包 R.data） */
export function put<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.put(url, data, config) as unknown as Promise<T>
}

/** 统一封装的 DELETE 请求（已解包 R.data） */
export function del<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, config) as unknown as Promise<T>
}

export default service
