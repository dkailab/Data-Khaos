<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="所属模型">
          <el-select v-model="query.modelId" clearable placeholder="全部模型" style="width: 200px" filterable @change="handleSearch">
            <el-option v-for="m in modelOptions" :key="m.id" :label="m.modelName" :value="m.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="指标名称/编码" clearable style="width: 180px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增指标</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="metricName" label="指标名称" min-width="140" />
      <el-table-column prop="metricCode" label="指标编码" min-width="140" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag>{{ row.metricType === 'DERIVED' ? '派生' : '原子' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="expression" label="计算表达式" min-width="200" show-overflow-tooltip />
      <el-table-column prop="dataType" label="数据类型" min-width="100" />
      <el-table-column prop="unit" label="单位" width="80" />
      <el-table-column prop="modelId" label="模型ID" min-width="150" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑指标' : '新增指标'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="指标名称" prop="metricName">
          <el-input v-model="form.metricName" placeholder="请输入指标名称" />
        </el-form-item>
        <el-form-item label="指标编码" prop="metricCode">
          <el-input v-model="form.metricCode" placeholder="请输入指标编码" />
        </el-form-item>
        <el-form-item label="指标类型" prop="metricType">
          <el-radio-group v-model="form.metricType">
            <el-radio value="ATOMIC">原子</el-radio>
            <el-radio value="DERIVED">派生</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="所属模型" prop="modelId">
          <el-select v-model="form.modelId" style="width: 100%" filterable>
            <el-option v-for="m in modelOptions" :key="m.id" :label="m.modelName" :value="m.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="计算表达式" prop="expression">
          <el-input v-model="form.expression" type="textarea" :rows="2" placeholder="如 SUM(amount)" />
        </el-form-item>
        <el-form-item label="数据类型" prop="dataType">
          <el-input v-model="form.dataType" placeholder="如 BIGINT / DECIMAL" />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="如 元 / 件" />
        </el-form-item>
        <el-form-item label="分类ID" prop="categoryId">
          <el-input v-model="form.categoryId" placeholder="指标分类ID（可选）" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
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
import { createMartMetric, deleteMartMetric, pageMartMetrics, updateMartMetric } from '@/api/mart'
import { pageMartModels } from '@/api/mart'
import type { MartMetric, MartModel } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const list = ref<MartMetric[]>([])
const total = ref(0)
const query = reactive<Record<string, any>>({ current: 1, size: 10, modelId: undefined, keyword: '' })

const modelOptions = ref<MartModel[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const form = reactive<MartMetric>({ metricType: 'ATOMIC', status: 1 })
const formRules: FormRules = {
  metricName: [{ required: true, message: '请输入指标名称', trigger: 'blur' }],
  metricCode: [{ required: true, message: '请输入指标编码', trigger: 'blur' }],
  modelId: [{ required: true, message: '请选择所属模型', trigger: 'change' }],
}

async function loadModels() {
  const data = await pageMartModels({ current: 1, size: 100 })
  modelOptions.value = data.records || []
}

async function load() {
  loading.value = true
  try {
    const data = await pageMartMetrics({ ...query })
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
  query.modelId = undefined
  query.keyword = ''
  handleSearch()
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { metricName: '', metricCode: '', metricType: 'ATOMIC', expression: '', dataType: '', unit: '', categoryId: '', modelId: query.modelId || '', description: '', status: 1 })
  dialogVisible.value = true
}

function openEdit(row: MartMetric) {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateMartMetric({ ...form })
    } else {
      await createMartMetric({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: MartMetric) {
  await ElMessageBox.confirm(`确认删除指标「${row.metricName}」吗？`, '提示', { type: 'warning' })
  await deleteMartMetric(row.id!)
  ElMessage.success('删除成功')
  load()
}

onMounted(() => {
  loadModels()
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
</style>
