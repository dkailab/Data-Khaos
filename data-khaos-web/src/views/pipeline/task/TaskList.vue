<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="任务名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="引擎">
          <el-select v-model="query.engine" clearable placeholder="全部" style="width: 160px" @change="handleSearch">
            <el-option v-for="e in engineOptions" :key="e.type" :label="e.name" :value="e.type" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 110px" @change="handleSearch">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增任务</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="taskName" label="任务名称" min-width="150" show-overflow-tooltip />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag>{{ taskTypeText(row.taskType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="引擎" width="150">
        <template #default="{ row }">
          <el-tag :type="engineTagType(row.engine)">{{ engineText(row.engine) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="源 → 目标" min-width="220">
        <template #default="{ row }">
          <span class="arrow">{{ dsName(row.sourceDsId) }}.{{ row.sourceTable }}</span>
          <span class="arrow-sep">→</span>
          <span class="arrow">{{ dsName(row.targetDsId) }}.{{ row.targetTable }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="cronExpr" label="定时表达式" min-width="120">
        <template #default="{ row }">
          <span>{{ row.cronExpr || '仅手动' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" width="170" />
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status !== 1" link type="success" @click="handleEnable(row)">启用</el-button>
          <el-button v-else link type="warning" @click="handleDisable(row)">停用</el-button>
          <el-button link type="info" @click="handleRun(row)">运行</el-button>
          <el-button link type="primary" @click="openInstances(row)">实例</el-button>
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

    <!-- 新增 / 编辑任务 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑任务' : '新增任务'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="任务类型" prop="taskType">
          <el-select v-model="form.taskType" style="width: 100%">
            <el-option label="同步 SYNC" value="SYNC" />
            <el-option label="加工 ETL" value="ETL" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行引擎" prop="engine">
          <el-select v-model="form.engine" style="width: 100%">
            <el-option v-for="e in engineOptions" :key="e.type" :value="e.type" :label="`${e.name}${e.available ? '' : '（未安装）'}`" />
          </el-select>
          <div class="tip">DB-Sync 为内置兜底引擎始终可用；DataX / SeaTunnel 需在引擎环境安装对应 CLI。</div>
        </el-form-item>
        <el-form-item label="源数据源" prop="sourceDsId">
          <el-select v-model="form.sourceDsId" filterable style="width: 100%">
            <el-option v-for="d in dsOptions" :key="d.id" :value="d.id" :label="`${d.dsName}（${d.dsType}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="源表" prop="sourceTable">
          <el-input v-model="form.sourceTable" placeholder="源表名" />
        </el-form-item>
        <el-form-item label="目标数据源" prop="targetDsId">
          <el-select v-model="form.targetDsId" filterable style="width: 100%">
            <el-option v-for="d in dsOptions" :key="d.id" :value="d.id" :label="`${d.dsName}（${d.dsType}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标表" prop="targetTable">
          <el-input v-model="form.targetTable" placeholder="目标表名" />
        </el-form-item>
        <el-form-item label="源查询(SQL)">
          <el-input v-model="form.sourceQuery" type="textarea" :rows="2" placeholder="缺省为 SELECT * FROM 源表（可选）" />
        </el-form-item>
        <el-form-item label="字段映射">
          <el-input v-model="form.fieldMapping" type="textarea" :rows="2" placeholder='JSON，如 {"col_a":"col_b"}（可选）' />
        </el-form-item>
        <el-form-item label="Cron 表达式">
          <el-input v-model="form.cronExpr" placeholder="如 0 0 2 * * ?（空=仅手动）" />
        </el-form-item>
        <el-form-item label="引擎配置">
          <el-input v-model="form.config" type="textarea" :rows="2" placeholder='JSON，如 {"timeoutSeconds":3600}（可选）' />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 执行实例对话框 -->
    <el-dialog v-model="instanceVisible" :title="`执行实例 - ${currentTask?.taskName || ''}`" width="920px">
      <el-table v-loading="instanceLoading" :data="instances" size="small" border max-height="420">
        <el-table-column prop="engine" label="引擎" width="120" />
        <el-table-column label="触发方式" width="90">
          <template #default="{ row }">
            {{ row.triggerType === 'CRON' ? '定时' : '手动' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="instanceTagType(row.status)">{{ instanceText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <el-table-column prop="durationMs" label="耗时(ms)" width="95" />
        <el-table-column prop="rows" label="行数" width="80" />
        <el-table-column prop="errorMessage" label="失败原因" min-width="160" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createPipelineTask,
  deletePipelineTask,
  listPipelineEngines,
  listPipelineInstanceByTask,
  pagePipelineInstances,
  pagePipelineTasks,
  runPipelineTask,
  updatePipelineTask,
} from '@/api/pipeline'
import { pageDatasources } from '@/api/datasource'
import type { MetaDatasource, PipelineEngineInfo, PipelineInstance, PipelineTask } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const list = ref<PipelineTask[]>([])
const total = ref(0)
const query = reactive<Record<string, any>>({ current: 1, size: 10, keyword: '', engine: undefined, status: undefined })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const form = reactive<PipelineTask>({ taskName: '', taskType: 'SYNC', engine: 'DB_SYNC', status: 1 })
const formRules: FormRules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  sourceDsId: [{ required: true, message: '请选择源数据源', trigger: 'change' }],
  sourceTable: [{ required: true, message: '请输入源表', trigger: 'blur' }],
  targetDsId: [{ required: true, message: '请选择目标数据源', trigger: 'change' }],
  targetTable: [{ required: true, message: '请输入目标表', trigger: 'blur' }],
}

const engineOptions = ref<PipelineEngineInfo[]>([])
const dsOptions = ref<MetaDatasource[]>([])

const instanceVisible = ref(false)
const instanceLoading = ref(false)
const instances = ref<PipelineInstance[]>([])
const currentTask = ref<PipelineTask>()

function taskTypeText(t?: string) {
  return { SYNC: '同步', ETL: '加工' }[t ?? ''] ?? t ?? '-'
}

function engineText(t?: string) {
  return engineOptions.value.find((e) => e.type === t)?.name ?? t ?? '-'
}

function engineTagType(t?: string): 'success' | 'warning' | 'info' {
  if (t === 'DB_SYNC') return 'success'
  if (t === 'DATAX' || t === 'SEATUNNEL') return 'warning'
  return 'info'
}

function instanceText(s?: number) {
  return { 0: '运行中', 1: '成功', 2: '失败' }[s ?? -1] ?? '-'
}

function instanceTagType(s?: number): 'success' | 'danger' | 'info' {
  if (s === 1) return 'success'
  if (s === 2) return 'danger'
  return 'info'
}

function dsName(id?: string) {
  return dsOptions.value.find((d) => d.id === id)?.dsName ?? id ?? '-'
}

async function load() {
  loading.value = true
  try {
    const data = await pagePipelineTasks({ ...query })
    list.value = data.records
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  try {
    engineOptions.value = await listPipelineEngines()
  } catch {
    engineOptions.value = []
  }
  try {
    const data = await pageDatasources({ current: 1, size: 200 })
    dsOptions.value = data.records
  } catch {
    dsOptions.value = []
  }
}

function handleSearch() {
  query.current = 1
  load()
}

function handleReset() {
  query.keyword = ''
  query.engine = undefined
  query.status = undefined
  handleSearch()
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { taskName: '', taskType: 'SYNC', engine: 'DB_SYNC', sourceDsId: '', sourceTable: '', targetDsId: '', targetTable: '', sourceQuery: '', fieldMapping: '', config: '', cronExpr: '', status: 1 })
  dialogVisible.value = true
}

function openEdit(row: PipelineTask) {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updatePipelineTask({ ...form })
    } else {
      await createPipelineTask({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: PipelineTask) {
  await ElMessageBox.confirm(`确认删除任务「${row.taskName}」吗？`, '提示', { type: 'warning' })
  await deletePipelineTask(row.id!)
  ElMessage.success('删除成功')
  load()
}

async function handleEnable(row: PipelineTask) {
  await updatePipelineTask({ id: row.id, status: 1 })
  ElMessage.success('任务已启用')
  load()
}

async function handleDisable(row: PipelineTask) {
  await updatePipelineTask({ id: row.id, status: 0 })
  ElMessage.success('任务已停用')
  load()
}

async function handleRun(row: PipelineTask) {
  await ElMessageBox.confirm(`确认立即运行任务「${row.taskName}」吗？`, '提示', { type: 'warning' })
  const instance = await runPipelineTask(row.id!)
  ElMessage.success(`已触发运行，实例ID: ${instance.id}`)
}

async function openInstances(row: PipelineTask) {
  currentTask.value = row
  instanceVisible.value = true
  instanceLoading.value = true
  try {
    const data = await listPipelineInstanceByTask(row.id!)
    instances.value = Array.isArray(data) ? data : await pagePipelineInstances({ taskId: row.id, current: 1, size: 50 }).then((r) => r.records)
  } finally {
    instanceLoading.value = false
  }
}

onMounted(() => {
  loadOptions()
  load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.arrow {
  color: var(--el-text-color-primary);
}
.arrow-sep {
  margin: 0 6px;
  color: var(--el-text-color-secondary);
}
.tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
  margin-top: 4px;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>