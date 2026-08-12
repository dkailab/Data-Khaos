<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="组织名称">
          <el-input v-model="query.orgName" placeholder="请输入组织名称" clearable style="width: 200px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增组织</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="orgName" label="组织名称" min-width="160" />
      <el-table-column prop="orgCode" label="组织编码" min-width="140" />
      <el-table-column prop="orgType" label="类型" width="110">
        <template #default="{ row }">
          <el-tag>{{ orgTypeText(row.orgType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" />
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
      :page-sizes="[20, 50, 100, 200]"
      layout="total, sizes, prev, pager, next, jumper"
      @change="load"
    />

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑组织' : '新增组织'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="上级组织" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'orgName', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="不选则为顶级组织"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="组织名称" prop="orgName">
          <el-input v-model="form.orgName" placeholder="请输入组织名称" />
        </el-form-item>
        <el-form-item label="组织编码" prop="orgCode">
          <el-input v-model="form.orgCode" placeholder="请输入组织编码" />
        </el-form-item>
        <el-form-item label="组织类型" prop="orgType">
          <el-select v-model="form.orgType" style="width: 100%">
            <el-option label="部门" value="DEPT" />
            <el-option label="公司" value="COMPANY" />
            <el-option label="集团" value="GROUP" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
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
import { createOrg, deleteOrg, listOrgs, pageOrgs, updateOrg } from '@/api/org'
import type { SysOrganization } from '@/types'

interface OrgNode extends SysOrganization {
  children?: OrgNode[]
}

const loading = ref(false)
const submitting = ref(false)
const list = ref<SysOrganization[]>([])
const total = ref(0)
const query = reactive<Record<string, any>>({ current: 1, size: 20, orgName: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const form = reactive<SysOrganization>({ status: 1, sortOrder: 0, parentId: '', orgType: 'DEPT' })
const formRules: FormRules = {
  orgName: [{ required: true, message: '请输入组织名称', trigger: 'blur' }],
  orgCode: [{ required: true, message: '请输入组织编码', trigger: 'blur' }],
}

const parentOptions = ref<OrgNode[]>([])

function buildTree(orgs: SysOrganization[]): OrgNode[] {
  const map = new Map<string, OrgNode>()
  const roots: OrgNode[] = []
  orgs.forEach((o) => map.set(o.id!, { ...o, children: [] }))
  map.forEach((node) => {
    if (node.parentId && map.has(node.parentId) && node.id !== form.id) {
      map.get(node.parentId)!.children!.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

function orgTypeText(t?: string) {
  return { DEPT: '部门', COMPANY: '公司', GROUP: '集团' }[t ?? ''] ?? t ?? '-'
}

async function load() {
  loading.value = true
  try {
    const data = await pageOrgs({ ...query })
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
  query.orgName = ''
  handleSearch()
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { parentId: '', orgName: '', orgCode: '', orgType: 'DEPT', sortOrder: 0, status: 1 })
  loadParentOptions()
  dialogVisible.value = true
}

function openEdit(row: SysOrganization) {
  isEdit.value = true
  Object.assign(form, { ...row })
  loadParentOptions()
  dialogVisible.value = true
}

async function loadParentOptions() {
  const orgs = (await listOrgs()) || []
  parentOptions.value = buildTree(orgs)
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateOrg(form.id!, { ...form })
    } else {
      await createOrg({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: SysOrganization) {
  await ElMessageBox.confirm(`确认删除组织「${row.orgName}」吗？`, '提示', { type: 'warning' })
  await deleteOrg(row.id!)
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
