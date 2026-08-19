<template>
  <div class="dataset-list-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">数据集管理</h2>
        <p class="page-subtitle">统一管理仪表板的数据源 · 支持SQL查询和模型提取</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Plus" @click="openCreate">新建数据集</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="搜索数据集名称..." clearable style="width: 240px" :prefix-icon="Search" @keyup.enter="load" />
      <el-select v-model="query.datasetType" clearable placeholder="类型" style="width: 120px" @change="load">
        <el-option label="SQL查询" value="SQL" />
        <el-option label="数据模型" value="MODEL" />
      </el-select>
      <el-select v-model="query.status" clearable placeholder="状态" style="width: 120px" @change="load">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="已下线" value="OFFLINE" />
      </el-select>
      <el-button :icon="Refresh" @click="handleReset">重置</el-button>
    </div>

    <div v-loading="loading" class="dataset-grid">
      <div v-for="item in list" :key="item.id" class="dataset-card" @click="openEditor(item)">
        <div class="card-header">
          <div class="card-type-tag" :class="`type-${item.datasetType?.toLowerCase()}`">
            {{ item.datasetType === 'SQL' ? 'SQL' : '模型' }}
          </div>
          <el-dropdown trigger="click" @command="(cmd: any) => handleAction(cmd, item)">
            <el-button :icon="More" circle size="small" class="more-btn" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit"><el-icon><Edit /></el-icon>编辑</el-dropdown-item>
                <el-dropdown-item v-if="item.status !== 'PUBLISHED'" command="publish" divided><el-icon><Top /></el-icon>发布</el-dropdown-item>
                <el-dropdown-item v-else command="unpublish"><el-icon><Bottom /></el-icon>下线</el-dropdown-item>
                <el-dropdown-item command="delete" divided><el-icon><Delete /></el-icon>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="card-body">
          <div class="card-name">{{ item.name }}</div>
          <div class="card-desc">{{ item.description || '暂无描述' }}</div>
          <div class="card-fields">
            <el-tag v-for="f in (item.fields || []).slice(0, 3)" :key="f.fieldCode" size="small" :type="f.fieldType === 'METRIC' ? 'success' : 'info'">
              {{ f.fieldName }}
            </el-tag>
            <span v-if="(item.fields || []).length > 3" class="more-fields">+{{ (item.fields || []).length - 3 }}</span>
          </div>
        </div>
        <div class="card-footer">
          <el-tag v-if="item.status === 'PUBLISHED'" type="success" size="small">已发布</el-tag>
          <el-tag v-else-if="item.status === 'DRAFT'" type="warning" size="small">草稿</el-tag>
          <el-tag v-else type="info" size="small">已下线</el-tag>
          <span class="card-version">v{{ item.version || 1 }}</span>
          <span class="card-time">{{ formatTime(item.updateTime || item.createTime) }}</span>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && !list.length" description="暂无数据集" :image-size="100">
      <el-button type="primary" :icon="Plus" @click="openCreate">新建数据集</el-button>
    </el-empty>

    <el-pagination class="pager" v-model:current-page="query.current" v-model:page-size="query.size" :total="total" :page-sizes="[12, 24, 48]" layout="total, sizes, prev, pager, next, jumper" @change="load" />

    <DatasetEditorDialog v-model:visible="editorVisible" :dataset="editingDataset" @saved="load" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, More, Plus, Refresh, Search, Top, Bottom } from '@element-plus/icons-vue'
import { deleteDataset, pageDatasets, publishDataset, unpublishDataset } from '@/api/dataset'
import type { VisualDataset } from '@/types'
import DatasetEditorDialog from './DatasetEditorDialog.vue'

const loading = ref(false)
const list = ref<VisualDataset[]>([])
const total = ref(0)
const query = reactive<Record<string, any>>({ current: 1, size: 12, keyword: '', datasetType: '', status: '' })

const editorVisible = ref(false)
const editingDataset = ref<VisualDataset | undefined>()

async function load() {
  loading.value = true
  try {
    const data = await pageDatasets({ ...query })
    list.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

function handleReset() {
  Object.assign(query, { current: 1, size: 12, keyword: '', datasetType: '', status: '' })
  load()
}

function openCreate() {
  editingDataset.value = undefined
  editorVisible.value = true
}

function openEditor(item: VisualDataset) {
  editingDataset.value = item
  editorVisible.value = true
}

async function handleAction(cmd: string, item: VisualDataset) {
  switch (cmd) {
    case 'edit': openEditor(item); break
    case 'publish': await publishDataset(item.id!); ElMessage.success('发布成功'); load(); break
    case 'unpublish': await unpublishDataset(item.id!); ElMessage.success('已下线'); load(); break
    case 'delete':
      await ElMessageBox.confirm(`确认删除数据集「${item.name}」吗？`, '提示', { type: 'warning' })
      await deleteDataset(item.id!); ElMessage.success('删除成功'); load(); break
  }
}

function formatTime(t?: string): string {
  if (!t) return '-'
  const d = new Date(t)
  const diff = Date.now() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return d.toLocaleDateString()
}

onMounted(load)
</script>

<style scoped>
.dataset-list-page { padding: 24px; background: #f8fafc; min-height: 100vh; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-title { font-size: 24px; font-weight: 700; color: #1a1d27; margin: 0; }
.page-subtitle { font-size: 13px; color: #6b7280; margin: 4px 0 0 0; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 20px; }
.dataset-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
.dataset-card { background: #fff; border: 1px solid #ebeef5; border-radius: 12px; padding: 16px; cursor: pointer; transition: all 0.25s; }
.dataset-card:hover { border-color: #4f9df9; box-shadow: 0 8px 24px rgba(79,157,249,0.12); transform: translateY(-2px); }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.card-type-tag { padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; }
.type-sql { background: #eff6ff; color: #3b82f6; }
.type-model { background: #f0fdf4; color: #22c55e; }
.more-btn { opacity: 0; }
.dataset-card:hover .more-btn { opacity: 1; }
.card-name { font-size: 16px; font-weight: 600; color: #1a1d27; margin-bottom: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-desc { font-size: 13px; color: #6b7280; margin-bottom: 10px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-fields { display: flex; gap: 4px; flex-wrap: wrap; margin-bottom: 12px; }
.more-fields { font-size: 11px; color: #909399; align-self: center; }
.card-footer { display: flex; align-items: center; gap: 8px; padding-top: 10px; border-top: 1px solid #f0f0f0; }
.card-version, .card-time { font-size: 11px; color: #909399; }
.card-time { margin-left: auto; }
.pager { margin-top: 24px; justify-content: flex-end; }
</style>
