<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="所属模型">
          <el-select v-model="query.modelId" clearable placeholder="全部模型" style="width: 200px" filterable @change="handleSearch">
            <el-option v-for="m in modelOptions" :key="m.id" :label="m.modelName" :value="m.id!" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增维度</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="dimName" label="维度名称" min-width="140" />
      <el-table-column prop="dimCode" label="维度编码" min-width="140" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag>{{ dimTypeText(row.dimType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sourceTable" label="来源表" min-width="140" />
      <el-table-column prop="sourceColumn" label="来源字段" min-width="120" />
      <el-table-column prop="modelId" label="模型ID" min-width="150" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="openLevels(row)">层级</el-button>
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

    <!-- 新增 / 编辑维度 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑维度' : '新增维度'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="维度名称" prop="dimName">
          <el-input v-model="form.dimName" placeholder="请输入维度名称" />
        </el-form-item>
        <el-form-item label="维度编码" prop="dimCode">
          <el-input v-model="form.dimCode" placeholder="请输入维度编码" />
        </el-form-item>
        <el-form-item label="维度类型" prop="dimType">
          <el-select v-model="form.dimType" style="width: 100%">
            <el-option label="通用" value="COMMON" />
            <el-option label="时间" value="TIME" />
            <el-option label="组织" value="ORG" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属模型" prop="modelId">
          <el-select v-model="form.modelId" style="width: 100%" filterable>
            <el-option v-for="m in modelOptions" :key="m.id" :label="m.modelName" :value="m.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源表" prop="sourceTable">
          <el-input v-model="form.sourceTable" placeholder="请输入来源表" />
        </el-form-item>
        <el-form-item label="来源字段" prop="sourceColumn">
          <el-input v-model="form.sourceColumn" placeholder="请输入来源字段" />
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

    <!-- 层级管理对话框 -->
    <el-dialog v-model="levelsVisible" :title="`维度层级 - ${levelsTarget?.dimName || ''}`" width="640px">
      <el-table :data="levels" size="small" border>
        <el-table-column prop="levelName" label="层级名称" min-width="150" />
        <el-table-column prop="levelColumn" label="层级字段" min-width="150" />
        <el-table-column prop="levelOrder" label="层级顺序" width="100" />
        <el-table-column label="操作" width="90">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeLevel($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-divider />
      <el-form inline>
        <el-form-item label="层级名称">
          <el-input v-model="levelForm.levelName" placeholder="如 省 / 市 / 区" style="width: 150px" />
        </el-form-item>
        <el-form-item label="层级字段">
          <el-input v-model="levelForm.levelColumn" placeholder="如 province" style="width: 150px" />
        </el-form-item>
        <el-form-item label="顺序">
          <el-input-number v-model="levelForm.levelOrder" :min="0" :max="99" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="addLevel">添加</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="levelsVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="saveLevels">保存层级</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createMartDimension, deleteMartDimension, listDimLevels, pageMartDimensions, saveDimLevels, updateMartDimension } from '@/api/mart'
import { pageMartModels } from '@/api/mart'
import type { MartDimension, MartDimLevel, MartModel } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const list = ref<MartDimension[]>([])
const total = ref(0)
const query = reactive<Record<string, any>>({ current: 1, size: 10, modelId: undefined })

const modelOptions = ref<MartModel[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const form = reactive<MartDimension>({ dimType: 'COMMON', status: 1 })
const formRules: FormRules = {
  dimName: [{ required: true, message: '请输入维度名称', trigger: 'blur' }],
  dimCode: [{ required: true, message: '请输入维度编码', trigger: 'blur' }],
  modelId: [{ required: true, message: '请选择所属模型', trigger: 'change' }],
}

const levelsVisible = ref(false)
const levelsTarget = ref<MartDimension | null>(null)
const levels = ref<MartDimLevel[]>([])
const levelForm = reactive<MartDimLevel>({ levelName: '', levelColumn: '', levelOrder: 0 })

function dimTypeText(t?: string) {
  return { COMMON: '通用', TIME: '时间', ORG: '组织' }[t ?? ''] ?? t ?? '-'
}

async function loadModels() {
  const data = await pageMartModels({ current: 1, size: 100 })
  modelOptions.value = data.records || []
}

async function load() {
  loading.value = true
  try {
    const data = await pageMartDimensions({ ...query })
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
  handleSearch()
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { dimName: '', dimCode: '', dimType: 'COMMON', modelId: query.modelId || '', sourceTable: '', sourceColumn: '', description: '', status: 1 })
  dialogVisible.value = true
}

function openEdit(row: MartDimension) {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateMartDimension({ ...form })
    } else {
      await createMartDimension({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: MartDimension) {
  await ElMessageBox.confirm(`确认删除维度「${row.dimName}」吗？将级联删除其层级。`, '提示', { type: 'warning' })
  await deleteMartDimension(row.id!)
  ElMessage.success('删除成功')
  load()
}

async function openLevels(row: MartDimension) {
  levelsTarget.value = row
  levels.value = (await listDimLevels(row.id!)) || []
  levelsVisible.value = true
}

function addLevel() {
  if (!levelForm.levelName || !levelForm.levelColumn) {
    ElMessage.warning('请填写层级名称和层级字段')
    return
  }
  levels.value.push({ ...levelForm, dimId: levelsTarget.value!.id })
  Object.assign(levelForm, { levelName: '', levelColumn: '', levelOrder: levels.value.length })
}

function removeLevel(index: number) {
  levels.value.splice(index, 1)
}

async function saveLevels() {
  submitting.value = true
  try {
    await saveDimLevels(levelsTarget.value!.id!, levels.value)
    ElMessage.success('层级保存成功')
    levelsVisible.value = false
  } finally {
    submitting.value = false
  }
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
