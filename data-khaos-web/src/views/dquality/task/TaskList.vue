<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="任务名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增任务</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="taskName" label="任务名称" min-width="160" />
      <el-table-column label="绑定规则数" width="120">
        <template #default="{ row }">
          {{ ruleCount(row.ruleIds) }} 条
        </template>
      </el-table-column>
      <el-table-column label="关联调度任务" min-width="220">
        <template #default="{ row }">
          <template v-if="(scheduleMap[row.id] || []).length">
            <el-tag
              v-for="job in scheduleMap[row.id]"
              :key="job.jobId"
              class="job-tag"
              :type="job.status === 1 ? 'success' : 'info'"
              effect="plain"
              @click="goSchedule(job)"
            >
              {{ job.jobName }}<span v-if="job.cronExpression" class="job-cron">{{ job.cronExpression }}</span>
            </el-tag>
          </template>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="cronExpr" label="周期表达式" min-width="140">
        <template #default="{ row }">
          <span v-if="row.cronExpr">{{ row.cronExpr }}</span>
          <el-tag v-else size="small" type="info">手动</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleRun(row)">执行</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="handleEnable(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑任务' : '新增任务'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="绑定规则" prop="ruleIds">
          <el-select v-model="selectedRules" multiple filterable style="width: 100%" placeholder="选择要执行的规则">
            <el-option v-for="r in ruleOptions" :key="r.id" :label="r.ruleName" :value="r.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="周期表达式">
          <el-input v-model="form.cronExpr" placeholder="如 0 0 2 * * ?（留空=手动执行）" />
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
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { createTask, deleteTask, enableTask, pageTasks, runTask, taskScheduleMap, updateTask } from '@/api/dquality'
import { pageRules } from '@/api/dquality'
import type { DqRule, DqTask, ScheduleJobBrief } from '@/types'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const runningId = ref('')
const list = ref<DqTask[]>([])
const total = ref(0)
const query = reactive<Record<string, any>>({ current: 1, size: 10, keyword: '' })

/** 质量任务ID -> 关联的调度任务列表 */
const scheduleMap = ref<Record<string, ScheduleJobBrief[]>>({})

const ruleOptions = ref<DqRule[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const selectedRules = ref<string[]>([])
const form = reactive<any>({ taskName: '', cronExpr: '', status: 1 })
const formRules: FormRules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
}

function ruleCount(ruleIds?: string) {
  if (!ruleIds) return 0
  try {
    return JSON.parse(ruleIds).length
  } catch {
    return 0
  }
}

async function load() {
  loading.value = true
  try {
    const [pageData, scheduleData] = await Promise.all([
      pageTasks({ ...query }),
      taskScheduleMap(),
    ])
    list.value = pageData.records
    total.value = Number(pageData.total)
    scheduleMap.value = scheduleData || {}
  } finally {
    loading.value = false
  }
}

/** 跳转到调度中心（可按任务ID定位） */
function goSchedule(job: ScheduleJobBrief) {
  router.push({ path: '/schedule/job', query: job.jobId ? { keyword: job.jobName } : {} })
}

function handleSearch() {
  query.current = 1
  load()
}

function handleReset() {
  query.keyword = ''
  handleSearch()
}

async function loadRules() {
  const data = await pageRules({ current: 1, size: 100 })
  ruleOptions.value = data.records || []
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { taskName: '', cronExpr: '', status: 1 })
  selectedRules.value = []
  dialogVisible.value = true
}

function openEdit(row: DqTask) {
  isEdit.value = true
  Object.assign(form, { ...row })
  try {
    selectedRules.value = JSON.parse(row.ruleIds || '[]')
  } catch {
    selectedRules.value = []
  }
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  if (selectedRules.value.length === 0) {
    ElMessage.warning('请至少绑定一个规则')
    return
  }
  submitting.value = true
  try {
    const payload = { ...form, ruleIds: JSON.stringify(selectedRules.value) }
    if (isEdit.value) {
      await updateTask(form.id, payload)
    } else {
      await createTask(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleRun(row: DqTask) {
  await ElMessageBox.confirm(`确认立即执行任务「${row.taskName}」吗？`, '提示', { type: 'warning' })
  const rt = await runTask(row.id!)
  ElMessage.success(`执行完成，评分 ${rt.score ?? '-'}`)
}

async function handleEnable(row: DqTask) {
  const next = !(row.status === 1)
  await enableTask(row.id!, next)
  ElMessage.success(next ? '已启用' : '已停用')
  load()
}

async function handleDelete(row: DqTask) {
  await ElMessageBox.confirm(`确认删除任务「${row.taskName}」吗？`, '提示', { type: 'warning' })
  await deleteTask(row.id!)
  ElMessage.success('删除成功')
  load()
}

onMounted(() => {
  loadRules()
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
.job-tag {
  margin-right: 6px;
  cursor: pointer;
}
.job-tag .job-cron {
  margin-left: 4px;
  color: #909399;
  font-family: monospace;
}
.muted {
  color: #c0c4cc;
}
</style>