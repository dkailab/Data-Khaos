<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="仪表板名称" clearable style="width: 200px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增仪表板</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="name" label="仪表板名称" min-width="160" />
      <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
      <el-table-column prop="refreshInterval" label="刷新间隔(秒)" width="120" />
      <el-table-column prop="version" label="版本" width="70" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.status === 2" type="success">已上线</el-tag>
          <el-tag v-else-if="row.status === 1" type="warning">草稿</el-tag>
          <el-tag v-else type="info">停用</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createBy" label="创建人" min-width="110" />
      <el-table-column prop="updateTime" label="更新时间" width="170" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditor(row)">查看/编辑</el-button>
          <el-button v-if="row.status !== 2" link type="success" @click="handlePublish(row)">上线</el-button>
          <el-button v-else link type="warning" @click="handleUnpublish(row)">下线</el-button>
          <el-button link @click="openVersions(row)">版本</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑仪表板' : '新增仪表板'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入仪表板名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="刷新间隔(秒)" prop="refreshInterval">
          <el-input-number v-model="form.refreshInterval" :min="0" :max="86400" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
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

    <el-dialog v-model="versionDialog" title="版本历史" width="640px" destroy-on-close>
      <el-table :data="versions" border size="small">
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column prop="remark" label="发布说明" min-width="160" />
        <el-table-column prop="createBy" label="发布人" width="110" />
        <el-table-column prop="createTime" label="发布时间" width="170" />
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createDashboard, dashboardVersions, deleteDashboard, pageDashboards, publishDashboard, unpublishDashboard, updateDashboard } from '@/api/visual'
import type { VisualDashboard, VisualDashboardVersion } from '@/types'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const list = ref<VisualDashboard[]>([])
const total = ref(0)
const query = reactive<Record<string, any>>({ current: 1, size: 10, keyword: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const form = reactive<VisualDashboard>({ status: 1, refreshInterval: 60 })
const formRules: FormRules = {
  name: [{ required: true, message: '请输入仪表板名称', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    const data = await pageDashboards({ ...query })
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
  handleSearch()
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { name: '', description: '', refreshInterval: 60, status: 1 })
  dialogVisible.value = true
}

function openEdit(row: VisualDashboard) {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateDashboard({ ...form })
    } else {
      await createDashboard({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: VisualDashboard) {
  await ElMessageBox.confirm(`确认删除仪表板「${row.name}」吗？将级联删除其组件与版本。`, '提示', { type: 'warning' })
  await deleteDashboard(row.id!)
  ElMessage.success('删除成功')
  load()
}

function openEditor(row: VisualDashboard) {
  router.push({ name: 'VisualDashboardEdit', params: { id: row.id } })
}

async function handlePublish(row: VisualDashboard) {
  const { value } = await ElMessageBox.prompt('请输入发布说明（可选）', '上线仪表板', {
    confirmButtonText: '确认上线',
    cancelButtonText: '取消',
    inputType: 'textarea',
  }).catch(() => ({ value: '' }))
  submitting.value = true
  try {
    const v = await publishDashboard(row.id!, value || undefined)
    ElMessage.success(`已上线，版本号 v${v}`)
    load()
  } finally {
    submitting.value = false
  }
}

async function handleUnpublish(row: VisualDashboard) {
  await ElMessageBox.confirm(`确认下线仪表板「${row.name}」？`, '提示', { type: 'warning' })
  await unpublishDashboard(row.id!)
  ElMessage.success('已下线')
  load()
}

const versionDialog = ref(false)
const versions = ref<VisualDashboardVersion[]>([])
async function openVersions(row: VisualDashboard) {
  versions.value = await dashboardVersions(row.id!)
  versionDialog.value = true
}

onMounted(load)
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
</style>
