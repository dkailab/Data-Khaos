<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="模型名称/编码" clearable style="width: 200px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="分层">
          <el-select v-model="query.layerId" clearable placeholder="全部" style="width: 140px" @change="handleSearch">
            <el-option v-for="l in layers" :key="l.id" :label="l.layerName" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 130px" @change="handleSearch">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已下线" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增模型</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="modelName" label="模型名称" min-width="150" />
      <el-table-column label="分层" width="110">
        <template #default="{ row }">
          <el-tag :type="layerTagType(row.layerId)">{{ layerName(row.layerId) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="modelCode" label="模型编码" min-width="140" />
      <el-table-column prop="modelType" label="类型" width="110">
        <template #default="{ row }">
          <el-tag>{{ row.modelType === 'SNOWFLAKE' ? '雪花' : '星型' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="datasourceId" label="数据源ID" min-width="150" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="modelStatusTagType(row.status)">{{ modelStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="version" label="版本" width="70" />
      <el-table-column prop="updateTime" label="更新时间" width="170" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status !== 1" link type="success" @click="handlePublish(row)">发布</el-button>
          <el-button v-if="row.status === 1" link type="warning" @click="handleOffline(row)">下线</el-button>
          <el-button link type="info" @click="showPreview(row)">预览</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      v-model:current-page="query.current"
      v-model:page-size="query.size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @change="load"
    />

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑模型' : '新增模型'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="form.modelName" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="数仓分层" prop="layerId">
          <el-select v-model="form.layerId" placeholder="请选择分层" style="width: 100%">
            <el-option v-for="l in layers" :key="l.id" :label="`${l.layerName} (${l.layerCode})`" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型编码" prop="modelCode">
          <el-input v-model="form.modelCode" placeholder="请输入模型编码" />
        </el-form-item>
        <el-form-item label="模型类型" prop="modelType">
          <el-radio-group v-model="form.modelType">
            <el-radio value="STAR">星型</el-radio>
            <el-radio value="SNOWFLAKE">雪花</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数据源ID" prop="datasourceId">
          <el-input v-model="form.datasourceId" placeholder="请输入数据源ID" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入模型描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 数据预览对话框 -->
    <el-dialog v-model="previewVisible" :title="`模型数据预览 - ${previewTarget?.modelName || ''}`" width="80%">
      <div v-if="previewResult && previewResult.columns.length" class="preview-info">
        <el-tag type="info">共 {{ previewResult.rows.length }} 行</el-tag>
        <el-tag type="success" style="margin-left: 8px">耗时 {{ previewResult.costMs }} ms</el-tag>
      </div>
      <el-table :data="previewResult?.rows || []" size="small" border max-height="480">
        <el-table-column v-for="col in previewResult?.columns || []" :key="col.columnName" :prop="col.columnName" :label="col.columnName" min-width="120" />
      </el-table>
      <el-empty v-if="previewResult && previewResult.columns.length === 0" description="无数据" />
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createMartModel, deleteMartModel, listMartLayers, offlineMartModel, pageMartModels, previewMartModel, publishMartModel, updateMartModel } from '@/api/mart'
import type { MartModel, MartWarehouseLayer, QueryResult } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const list = ref<MartModel[]>([])
const total = ref(0)
const layers = ref<MartWarehouseLayer[]>([])
const query = reactive<Record<string, any>>({ current: 1, size: 10, keyword: '', layerId: undefined, status: undefined })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const form = reactive<MartModel>({ modelType: 'STAR', status: 0, version: 1 })
const formRules: FormRules = {
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  layerId: [{ required: true, message: '请选择数仓分层', trigger: 'change' }],
  modelCode: [{ required: true, message: '请输入模型编码', trigger: 'blur' }],
}

const previewVisible = ref(false)
const previewTarget = ref<MartModel | null>(null)
const previewResult = ref<QueryResult | null>(null)

function layerName(id?: string) {
  return layers.value.find((l) => l.id === id)?.layerName ?? '-'
}

function layerTagType(id?: string): 'primary' | 'warning' | 'success' | 'info' {
  const code = layers.value.find((l) => l.id === id)?.layerCode
  if (code === 'ODS') return 'info'
  if (code === 'DWD') return 'primary'
  if (code === 'DWS') return 'warning'
  return 'success'
}

function modelStatusText(s?: number) {
  return { 0: '草稿', 1: '已发布', 2: '已下线' }[s ?? -1] ?? '-'
}

function modelStatusTagType(s?: number): 'info' | 'success' | 'warning' {
  if (s === 0) return 'info'
  if (s === 1) return 'success'
  return 'warning'
}

async function load() {
  loading.value = true
  try {
    const data = await pageMartModels({ ...query })
    list.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.current = 1
  load()
}

function handleReset() {
  query.keyword = ''
  query.layerId = undefined
  query.status = undefined
  handleSearch()
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { modelName: '', layerId: '', modelCode: '', modelType: 'STAR', datasourceId: '', description: '', status: 0, version: 1 })
  dialogVisible.value = true
}

function openEdit(row: MartModel) {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateMartModel({ ...form })
    } else {
      await createMartModel({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handlePublish(row: MartModel) {
  await ElMessageBox.confirm(`确认发布模型「${row.modelName}」吗？`, '提示', { type: 'warning' })
  await publishMartModel(row.id!)
  ElMessage.success('发布成功')
  load()
}

async function handleOffline(row: MartModel) {
  await ElMessageBox.confirm(`确认下线模型「${row.modelName}」吗？`, '提示', { type: 'warning' })
  await offlineMartModel(row.id!)
  ElMessage.success('已下线')
  load()
}

async function handleDelete(row: MartModel) {
  await ElMessageBox.confirm(`确认删除模型「${row.modelName}」吗？将级联删除指标/维度/关联。`, '提示', { type: 'warning' })
  await deleteMartModel(row.id!)
  ElMessage.success('删除成功')
  load()
}

async function showPreview(row: MartModel) {
  previewTarget.value = row
  previewResult.value = null
  previewVisible.value = true
  try {
    previewResult.value = await previewMartModel(row.id!)
  } catch {
    previewResult.value = null
  }
}

onMounted(async () => {
  try {
    layers.value = await listMartLayers()
  } catch {
    layers.value = []
  }
  load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.preview-info {
  margin-bottom: 12px;
}
</style>
