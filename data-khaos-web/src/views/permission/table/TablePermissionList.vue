<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="表名">
          <el-input v-model="query.tableName" placeholder="请输入表名" clearable style="width: 200px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增表权限</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="datasourceId" label="数据源ID" min-width="150" />
      <el-table-column prop="databaseName" label="数据库" min-width="130" />
      <el-table-column prop="tableName" label="表名" min-width="150" />
      <el-table-column label="权限类型" width="100">
        <template #default="{ row }">
          <el-tag type="primary">{{ row.permissionType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="授权对象" width="100">
        <template #default="{ row }">
          <el-tag :type="row.grantType === 'ROLE' ? 'warning' : 'info'">{{ row.grantType === 'ROLE' ? '角色' : '用户' }}</el-tag>
        </template>
      </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑表权限' : '新增表权限'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="数据源ID" prop="datasourceId">
          <el-input v-model="form.datasourceId" placeholder="请输入数据源ID" />
        </el-form-item>
        <el-form-item label="数据库" prop="databaseName">
          <el-input v-model="form.databaseName" placeholder="请输入数据库名" />
        </el-form-item>
        <el-form-item label="表名" prop="tableName">
          <el-input v-model="form.tableName" placeholder="请输入表名" />
        </el-form-item>
        <el-form-item label="权限类型" prop="permissionType">
          <el-select v-model="form.permissionType" style="width: 100%">
            <el-option label="查询 SELECT" value="SELECT" />
            <el-option label="插入 INSERT" value="INSERT" />
            <el-option label="更新 UPDATE" value="UPDATE" />
            <el-option label="删除 DELETE" value="DELETE" />
            <el-option label="全部 ALL" value="ALL" />
          </el-select>
        </el-form-item>
        <el-form-item label="授权类型" prop="grantType">
          <el-radio-group v-model="form.grantType">
            <el-radio value="ROLE">按角色</el-radio>
            <el-radio value="USER">按用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.grantType === 'ROLE'" label="角色ID" prop="roleId">
          <el-input v-model="form.roleId" placeholder="请输入角色ID" />
        </el-form-item>
        <el-form-item v-else label="用户ID" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户ID" />
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
import { createTablePermission, deleteTablePermission, pageTablePermissions, updateTablePermission } from '@/api/tablePermission'
import type { SysTablePermission } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const list = ref<SysTablePermission[]>([])
const total = ref(0)
const query = reactive<Record<string, any>>({ current: 1, size: 10, tableName: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const form = reactive<SysTablePermission>({ status: 1, permissionType: 'SELECT', grantType: 'ROLE' })
const formRules: FormRules = {
  datasourceId: [{ required: true, message: '请输入数据源ID', trigger: 'blur' }],
  tableName: [{ required: true, message: '请输入表名', trigger: 'blur' }],
  permissionType: [{ required: true, message: '请选择权限类型', trigger: 'change' }],
}

async function load() {
  loading.value = true
  try {
    const data = await pageTablePermissions({ ...query })
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
  query.tableName = ''
  handleSearch()
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { datasourceId: '', databaseName: '', tableName: '', permissionType: 'SELECT', grantType: 'ROLE', roleId: '', userId: '', status: 1 })
  dialogVisible.value = true
}

function openEdit(row: SysTablePermission) {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateTablePermission(form.id!, { ...form })
    } else {
      await createTablePermission({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: SysTablePermission) {
  await ElMessageBox.confirm(`确认删除表权限记录（${row.tableName}）吗？`, '提示', { type: 'warning' })
  await deleteTablePermission(row.id!)
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
