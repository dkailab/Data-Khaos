<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="目标表">
          <el-input v-model="query.targetTable" placeholder="请输入目标表名" clearable style="width: 200px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增策略</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="policyName" label="策略名称" min-width="140" />
      <el-table-column prop="targetTable" label="目标表" min-width="150" />
      <el-table-column prop="columnName" label="字段" min-width="140" />
      <el-table-column label="脱敏方式" width="110">
        <template #default="{ row }">
          <el-tag>{{ maskTypeText(row.maskType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="maskRule" label="脱敏规则" min-width="130" />
      <el-table-column prop="roleId" label="角色ID" min-width="120" />
      <el-table-column prop="userId" label="用户ID" min-width="120" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑列权限策略' : '新增列权限策略'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="策略名称" prop="policyName">
          <el-input v-model="form.policyName" placeholder="请输入策略名称" />
        </el-form-item>
        <el-form-item label="目标表" prop="targetTable">
          <el-input v-model="form.targetTable" placeholder="如 dwd.dwd_order_detail" />
        </el-form-item>
        <el-form-item label="目标字段" prop="columnName">
          <el-input v-model="form.columnName" placeholder="如 mobile / id_card" />
        </el-form-item>
        <el-form-item label="脱敏方式" prop="maskType">
          <el-select v-model="form.maskType" style="width: 100%">
            <el-option label="掩码 MASK" value="MASK" />
            <el-option label="加密 ENCRYPT" value="ENCRYPT" />
            <el-option label="隐藏 HIDE" value="HIDE" />
            <el-option label="明文 PLAIN" value="PLAIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="脱敏规则" prop="maskRule">
          <el-input v-model="form.maskRule" placeholder="如 left:3,right:4" />
        </el-form-item>
        <el-form-item label="绑定角色ID" prop="roleId">
          <el-input v-model="form.roleId" placeholder="绑定角色ID（可选）" />
        </el-form-item>
        <el-form-item label="绑定用户ID" prop="userId">
          <el-input v-model="form.userId" placeholder="绑定用户ID（可选）" />
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
import { createColumnPolicy, deleteColumnPolicy, pageColumnPolicies, updateColumnPolicy } from '@/api/policy'
import type { SysColumnPolicy } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const list = ref<SysColumnPolicy[]>([])
const total = ref(0)
const query = reactive<Record<string, any>>({ current: 1, size: 10, targetTable: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const form = reactive<SysColumnPolicy>({ status: 1, maskType: 'MASK' })
const formRules: FormRules = {
  policyName: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  targetTable: [{ required: true, message: '请输入目标表', trigger: 'blur' }],
  columnName: [{ required: true, message: '请输入目标字段', trigger: 'blur' }],
  maskType: [{ required: true, message: '请选择脱敏方式', trigger: 'change' }],
}

function maskTypeText(t?: string) {
  return { MASK: '掩码', ENCRYPT: '加密', HIDE: '隐藏', PLAIN: '明文' }[t ?? ''] ?? t ?? '-'
}

async function load() {
  loading.value = true
  try {
    const data = await pageColumnPolicies({ ...query })
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
  query.targetTable = ''
  handleSearch()
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { policyName: '', targetTable: '', columnName: '', maskType: 'MASK', maskRule: '', roleId: '', userId: '', status: 1 })
  dialogVisible.value = true
}

function openEdit(row: SysColumnPolicy) {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateColumnPolicy(form.id!, { ...form })
    } else {
      await createColumnPolicy({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: SysColumnPolicy) {
  await ElMessageBox.confirm(`确认删除列权限策略「${row.policyName}」吗？`, '提示', { type: 'warning' })
  await deleteColumnPolicy(row.id!)
  ElMessage.success('删除成功')
  load()
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
