<template>
  <el-card shadow="never">
    <div class="header">
      <div>
        <h3 class="title">门户模块展示配置</h3>
        <p class="desc">管理员可自定义门户展示的功能模块。标注「系统必须」的模块不允许取消展示，其余模块可自由开关。</p>
      </div>
      <div class="actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :loading="saving" :icon="Check" @click="save">保存配置</el-button>
      </div>
    </div>

    <div v-loading="loading">
      <template v-for="cat in grouped" :key="cat.key">
        <div class="cat-head">
          <el-icon :size="18" color="#165dff"><component :is="cat.icon" /></el-icon>
          <span>{{ cat.title }}</span>
          <span class="cat-count">{{ cat.features.length }} 个模块</span>
        </div>
        <el-table :data="cat.features" border stripe class="mod-table">
          <el-table-column label="模块" min-width="200">
            <template #default="{ row }">
              <div class="mod-cell">
                <span class="mod-name">{{ row.moduleName }}</span>
                <el-tag v-if="row.mandatory === 1" size="small" type="danger" effect="dark" class="tag">系统必须</el-tag>
                <el-tag v-if="!row.path" size="small" type="info" class="tag">待建设</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="moduleKey" label="模块标识" min-width="140" />
          <el-table-column prop="path" label="路由路径" min-width="180">
            <template #default="{ row }">{{ row.path || '—' }}</template>
          </el-table-column>
          <el-table-column label="展示" width="120" align="center">
            <template #default="{ row }">
              <el-switch
                v-model="switchModel[row.moduleKey]"
                :disabled="row.mandatory === 1"
                :active-text="row.visible === 1 ? '显示' : '显示'"
              />
            </template>
          </el-table-column>
        </el-table>
      </template>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Refresh } from '@element-plus/icons-vue'
import { getModuleConfigList, saveModuleConfig } from '@/api/moduleConfig'
import { CATEGORIES, SYSTEM_CATEGORY } from '@/modules/registry'
import type { ModuleDisplayConfig } from '@/types'

const loading = ref(false)
const saving = ref(false)
const list = ref<ModuleDisplayConfig[]>([])

/** moduleKey -> 当前开关状态（受控，保存后再由后端校验必须模块） */
const switchModel = reactive<Record<string, boolean>>({})

/** 按注册表分类结构分组渲染（覆盖注册表里全部模块，含隐藏的以支持重新开启） */
const grouped = computed(() => {
  const all = [...CATEGORIES, SYSTEM_CATEGORY]
  return all.map((cat) => ({
    ...cat,
    features: cat.features
      .map((f) => list.value.find((c) => c.moduleKey === f.key))
      .filter((c): c is ModuleDisplayConfig => !!c),
  }))
})

const mandatoryCount = computed(() => list.value.filter((c) => c.mandatory === 1).length)

async function load() {
  loading.value = true
  try {
    list.value = (await getModuleConfigList()) || []
    // 初始化开关状态
    for (const c of list.value) {
      switchModel[c.moduleKey] = c.visible === 1
    }
  } finally {
    loading.value = false
  }
}

async function save() {
  const updates = list.value.map((c) => ({
    moduleKey: c.moduleKey,
    visible: switchModel[c.moduleKey] ? 1 : 0,
  }))
  saving.value = true
  try {
    await saveModuleConfig(updates)
    ElMessage.success('门户模块配置已保存')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}
.title {
  margin: 0 0 4px;
  font-size: 18px;
  color: #1d2129;
}
.desc {
  margin: 0;
  font-size: 13px;
  color: #86909c;
}
.cat-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #1d2129;
  margin: 20px 0 10px;
}
.cat-count {
  font-size: 12px;
  font-weight: normal;
  color: #86909c;
}
.mod-table {
  margin-bottom: 8px;
}
.mod-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mod-name {
  font-weight: 500;
}
.tag {
  flex-shrink: 0;
}
</style>