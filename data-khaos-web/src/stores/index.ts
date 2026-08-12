import { createPinia } from 'pinia'

/** 独立的 Pinia 实例，供 main.ts 与路由守卫共用 */
export const pinia = createPinia()
