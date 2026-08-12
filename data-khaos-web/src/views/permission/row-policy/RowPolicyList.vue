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
      <el-table-column prop="expression" label="过滤表达式" min-width="200" show-overflow-tooltip />
      <el-table-column prop="expressionDesc" label="说明" min-width="160" show-overflow-tooltip />
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑行权限策略' : '新增行权限策略'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="策略名称" prop="policyName">
          <el-input v-model="form.policyName" placeholder="请输入策略名称" />
        </el-form-item>
        <el-form-item label="目标表" prop="targetTable">
          <el-input v-model="form.targetTable" placeholder="如 dwd.dwd_order_detail" />
        </el-form-item>
        <el-form-item label="过滤表达式" prop="expression">
          <el-input v-model="form.expression" type="textarea" :rows="2" placeholder="如 org_id = #{currentOrgId}" />
        </el-form-item>
        <el-form-item label="表达式说明" prop="expressionDesc">
          <el-input v-model="form.expressionDesc" type="textarea" :rows="2" placeholder="表达式含义说明" />
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
import { createRowPolicy, deleteRowPolicy, pageRowPolicies, updateRowPolicy } from '@/api/policy'
import type { SysRowPolicy } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const list = ref<SysRowPolicy[]>([])
const total = ref(0)
const query = reactive<Record<string, any>>({ current: 1, size: 10, targetTable: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const form = reactive<SysRowPolicy>({ status: 1 })
const formRules: FormRules = {
  policyName: [{ required: true, message: '请输入策略名称', trigger: 'blur' }],
  targetTable: [{ required: true, message: '请输入目标表', trigger: 'blur' }],
  expression: [{ required: true, message: '请输入过滤表达式', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    const data = await pageRowPolicies({ ...query })
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
  Object.assign(form, { policyName: '', targetTable: '', expression: '', expressionDesc: '', roleId: '', userId: '', status: 1 })
  dialogVisible.value = true
}

function openEdit(row: SysRowPolicy) {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateRowPolicy(form.id!, { ...form })
    } else {
      await createRowPolicy({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: SysRowPolicy) {
  await ElMessageBox.confirm(`确认删除行权限策略「${row.policyName}」吗？`, '提示', { type: 'warning' })
  await deleteRowPolicy(row.id!)
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
