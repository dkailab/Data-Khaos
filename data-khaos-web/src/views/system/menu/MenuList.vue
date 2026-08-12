<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="菜单名称">
          <el-input v-model="query.name" placeholder="请输入菜单名称" clearable style="width: 200px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增菜单</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="name" label="菜单名称" min-width="150" />
      <el-table-column prop="type" label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="typeTagType(row.type)">{{ typeText(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路由路径" min-width="160" />
      <el-table-column prop="component" label="组件路径" min-width="180" />
      <el-table-column prop="permission" label="权限标识" min-width="140" />
      <el-table-column prop="icon" label="图标" width="80" />
      <el-table-column prop="sortOrder" label="排序" width="70" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
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
      :page-sizes="[20, 50, 100, 200]"
      layout="total, sizes, prev, pager, next, jumper"
      @change="load"
    />

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑菜单' : '新增菜单'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="不选则为顶级菜单"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="菜单类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="目录" :value="0" />
            <el-option label="菜单" :value="1" />
            <el-option label="按钮" :value="2" />
            <el-option label="API" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="路由路径" prop="path">
          <el-input v-model="form.path" placeholder="如 /system/user" />
        </el-form-item>
        <el-form-item label="组件路径" prop="component">
          <el-input v-model="form.component" placeholder="如 system/user/UserList" />
        </el-form-item>
        <el-form-item label="权限标识" prop="permission">
          <el-input v-model="form.permission" placeholder="如 system:user:list" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="如 User" />
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
import { createMenu, deleteMenu, listMenus, pageMenus, updateMenu } from '@/api/menu'
import type { SysMenu } from '@/types'

interface MenuNode extends SysMenu {
  children?: MenuNode[]
}

const loading = ref(false)
const submitting = ref(false)
const list = ref<SysMenu[]>([])
const total = ref(0)
const query = reactive<Record<string, any>>({ current: 1, size: 20, name: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const form = reactive<SysMenu>({ status: 1, type: 1, sortOrder: 0, parentId: '' })
const formRules: FormRules = {
  name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
}

const parentOptions = ref<MenuNode[]>([])

function buildTree(menus: SysMenu[]): MenuNode[] {
  const map = new Map<string, MenuNode>()
  const roots: MenuNode[] = []
  menus.forEach((m) => map.set(m.id!, { ...m, children: [] }))
  map.forEach((node) => {
    if (node.parentId && map.has(node.parentId) && node.id !== form.id) {
      map.get(node.parentId)!.children!.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

function typeText(t?: number) {
  return { 0: '目录', 1: '菜单', 2: '按钮', 3: 'API' }[t ?? -1] ?? '未知'
}

function typeTagType(t?: number): 'info' | 'primary' | 'warning' | 'danger' {
  if (t === 0) return 'info'
  if (t === 1) return 'primary'
  if (t === 2) return 'warning'
  return 'danger'
}

async function load() {
  loading.value = true
  try {
    const data = await pageMenus({ ...query })
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
  query.name = ''
  handleSearch()
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { parentId: '', name: '', path: '', component: '', permission: '', icon: '', type: 1, sortOrder: 0, status: 1 })
  loadParentOptions()
  dialogVisible.value = true
}

function openEdit(row: SysMenu) {
  isEdit.value = true
  Object.assign(form, { ...row })
  loadParentOptions()
  dialogVisible.value = true
}

async function loadParentOptions() {
  const menus = (await listMenus()) || []
  parentOptions.value = buildTree(menus)
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateMenu(form.id!, { ...form })
    } else {
      await createMenu({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: SysMenu) {
  await ElMessageBox.confirm(`确认删除菜单「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteMenu(row.id!)
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
