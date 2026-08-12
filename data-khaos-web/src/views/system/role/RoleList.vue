<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="角色名称">
          <el-input v-model="query.roleName" placeholder="请输入角色名称" clearable style="width: 200px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增角色</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="roleCode" label="角色编码" min-width="140" />
      <el-table-column prop="roleName" label="角色名称" min-width="140" />
      <el-table-column prop="description" label="描述" min-width="200" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="openBindMenu(row)">绑定权限</el-button>
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

    <!-- 新增 / 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="如 admin / analyst" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入角色描述" />
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

    <!-- 绑定权限菜单对话框 -->
    <el-dialog v-model="menuDialogVisible" :title="`绑定权限菜单 - ${menuTarget?.roleName || ''}`" width="460px" destroy-on-close>
      <el-scrollbar max-height="420px">
        <el-tree
          ref="menuTreeRef"
          :data="menuTree"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          show-checkbox
          default-expand-all
        />
      </el-scrollbar>
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitMenus">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ElTree, FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createRole, deleteRole, pageRoles, updateRole } from '@/api/role'
import { allMenus } from '@/api/menu'
import { assignRolePermissions, getRolePermissions } from '@/api/permission'
import type { SysMenu, SysRole } from '@/types'

interface MenuNode extends SysMenu {
  children?: MenuNode[]
}

const loading = ref(false)
const submitting = ref(false)
const list = ref<SysRole[]>([])
const total = ref(0)
const query = reactive<Record<string, any>>({ current: 1, size: 10, roleName: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const form = reactive<SysRole>({ status: 1 })
const formRules: FormRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
}

const menuDialogVisible = ref(false)
const menuTreeRef = ref<InstanceType<typeof ElTree>>()
const menuTarget = ref<SysRole | null>(null)
const menuTree = ref<MenuNode[]>([])

/** 扁平菜单转树 */
function buildMenuTree(menus: SysMenu[]): MenuNode[] {
  const map = new Map<string, MenuNode>()
  const roots: MenuNode[] = []
  menus.forEach((m) => map.set(m.id!, { ...m, children: [] }))
  map.forEach((node) => {
    if (node.parentId && map.has(node.parentId)) {
      map.get(node.parentId)!.children!.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

async function load() {
  loading.value = true
  try {
    const data = await pageRoles({ ...query })
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
  query.roleName = ''
  handleSearch()
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { roleCode: '', roleName: '', description: '', status: 1 })
  dialogVisible.value = true
}

function openEdit(row: SysRole) {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateRole(form.id!, { ...form })
    } else {
      await createRole({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: SysRole) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」吗？`, '提示', { type: 'warning' })
  await deleteRole(row.id!)
  ElMessage.success('删除成功')
  load()
}

async function openBindMenu(row: SysRole) {
  menuTarget.value = row
  if (menuTree.value.length === 0) {
    const menus = (await allMenus()) || []
    menuTree.value = buildMenuTree(menus)
  }
  menuDialogVisible.value = true
  // 等待树渲染后设置选中
  requestAnimationFrame(async () => {
    const checked = (await getRolePermissions(row.id!)) || []
    menuTreeRef.value?.setCheckedKeys(checked)
  })
}

async function submitMenus() {
  submitting.value = true
  try {
    const checked = menuTreeRef.value?.getCheckedKeys(false) || []
    const halfChecked = menuTreeRef.value?.getHalfCheckedKeys() || []
    const menuIds = [...checked, ...halfChecked].filter((id): id is string => id != null && id !== '')
    await assignRolePermissions(menuTarget.value!.id!, menuIds)
    ElMessage.success('权限绑定成功')
    menuDialogVisible.value = false
  } finally {
    submitting.value = false
  }
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
