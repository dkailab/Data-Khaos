import { get, put } from './request'
import type { ModuleDisplayConfig } from '@/types'

/** 全部模块配置（含必须标识与可见性，管理员查看/编辑用） */
export function getModuleConfigList() {
  return get<ModuleDisplayConfig[]>('/permission/module-config/list')
}

/** 仅当前应展示的模块清单（普通用户门户渲染用） */
export function getVisibleModules() {
  return get<ModuleDisplayConfig[]>('/permission/module-config/visible')
}

/** 批量保存模块可见性（需 module:config 能力位；必须模块后端强校验不可隐藏） */
export function saveModuleConfig(updates: Partial<ModuleDisplayConfig>[]) {
  return put<void>('/permission/module-config', updates)
}