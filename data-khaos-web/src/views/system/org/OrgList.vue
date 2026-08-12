<template>
  <div class="org-container">
    <!-- 左侧：组织树 -->
    <el-card shadow="never" class="left-panel">
      <template #header>
        <div class="panel-header">
          <span>组织架构</span>
          <el-button type="primary" :icon="Plus" size="small" @click="openCreate">新增组织</el-button>
        </div>
      </template>
      <el-scrollbar class="tree-scroll">
        <el-tree
          :data="tree"
          :props="{ label: 'orgName', children: 'children' }"
          node-key="id"
          highlight-current
          default-expand-all
          :current-node-key="selectedOrg?.id"
          @node-click="handleNodeClick"
          @node-contextmenu="handleContextMenu"
        >
          <template #default="{ data }">
            <div class="tree-node">
              <el-icon :color="orgTypeColor(data.orgType)"><OfficeBuilding /></el-icon>
              <span class="node-name">{{ data.orgName }}</span>
              <el-tag size="small" :type="data.status === 1 ? 'success' : 'danger'" effect="plain">
                {{ orgTypeText(data.orgType) }}
              </el-tag>
            </div>
          </template>
        </el-tree>
      </el-scrollbar>
    </el-card>

    <!-- 右侧：部门详情（成员 / 权限） -->
    <el-card shadow="never" class="right-panel">
      <template #header>
        <div class="panel-header">
          <span>部门权限管理</span>
          <el-button-group v-if="selectedOrg">
            <el-button size="small" :icon="Edit" @click="openEdit(selectedOrg)">编辑</el-button>
            <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(selectedOrg)">删除</el-button>
          </el-button-group>
        </div>
      </template>

      <el-empty v-if="!selectedOrg" description="请在左侧选择组织部门" />

      <template v-else>
        <el-alert
          class="org-info"
          type="info"
          :closable="false"
          show-icon
          :title="`当前部门：${selectedOrg.orgName}（${selectedOrg.orgCode || '-'}）`"
          :description="`部门成员将自动继承以下授予的菜单权限。`"
        />

        <el-tabs v-model="activeTab" @tab-change="onTabChange">
          <!-- 成员管理 -->
          <el-tab-pane label="部门成员" name="members">
            <div class="toolbar">
              <el-button type="primary" :icon="User" @click="openMemberDialog">添加成员</el-button>
              <span class="member-count">共 {{ members.length }} 名成员</span>
            </div>
            <el-table v-loading="loadingUsers" :data="members" border stripe>
              <el-table-column prop="username" label="用户名" min-width="120" />
              <el-table-column prop="realName" label="姓名" min-width="120" />
              <el-table-column prop="email" label="邮箱" min-width="180" />
              <el-table-column label="主部门" width="100" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.isPrimary === 1" type="warning" size="small">主部门</el-tag>
                  <span v-else>—</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" fixed="right">
                <template #default="{ row }">
                  <el-button link type="danger" @click="removeMember(row)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 部门菜单权限 -->
          <el-tab-pane label="部门菜单权限" name="permissions">
            <div class="perm-summary">
              <span>为「{{ selectedOrg.orgName }}」授予菜单权限，成员将自动继承。</span>
            </div>
            <el-scrollbar max-height="440px" class="perm-tree-wrap">
              <el-tree
                ref="menuTreeRef"
                :data="menuTree"
                :props="{ label: 'name', children: 'children' }"
                node-key="id"
                show-checkbox
                default-expand-all
                v-loading="loadingPerms"
              />
            </el-scrollbar>
            <div class="perm-footer">
              <el-button type="primary" :loading="savingPerms" @click="savePermissions">保存菜单权限</el-button>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-card>

    <!-- 新增 / 编辑组织 -->
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

    <!-- 添加成员 -->
    <el-dialog v-model="memberDialogVisible" :title="`添加成员 - ${selectedOrg?.orgName || ''}`" width="560px" destroy-on-close>
      <el-select
        v-model="memberSelections"
        multiple
        filterable
        remote
        :remote-method="searchUsers"
        :loading="searchingUser"
        placeholder="搜索并选择用户（可多选）"
        style="width: 100%"
      >
        <el-option v-for="u in userOptions" :key="u.id" :label="`${u.realName || u.username}（${u.username}）`" :value="u.id" />
      </el-select>
      <div class="member-tip">提示：添加后，该用户的主部门将自动切换为当前部门。</div>
      <template #footer>
        <el-button @click="memberDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingMembers" @click="saveMembers">保存成员</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ElTree, FormInstance, FormRules } from 'element-plus'
import { Delete, Edit, OfficeBuilding, Plus, User } from '@element-plus/icons-vue'
import { createOrg, deleteOrg, orgTree, orgUsers, assignOrgUsers, orgPermissions, assignOrgPermissions, updateOrg } from '@/api/org'
import { allMenus } from '@/api/menu'
import { pageUsers } from '@/api/user'
import type { SysMenu, SysOrganization, SysUser } from '@/types'

interface OrgNode {
  id: string
  parentId?: string
  orgName: string
  orgCode?: string
  orgType?: string
  status?: number
  children?: OrgNode[]
}
interface MenuNode extends SysMenu {
  children?: MenuNode[]
}

const tree = ref<OrgNode[]>([])
const selectedOrg = ref<OrgNode | null>(null)
const activeTab = ref('members')

const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const form = reactive<SysOrganization>({ status: 1, sortOrder: 0, parentId: '', orgType: 'DEPT' })
const formRules: FormRules = {
  orgName: [{ required: true, message: '请输入组织名称', trigger: 'blur' }],
  orgCode: [{ required: true, message: '请输入组织编码', trigger: 'blur' }],
}
const parentOptions = ref<OrgNode[]>([])

// 成员
const members = ref<any[]>([])
const loadingUsers = ref(false)
const memberDialogVisible = ref(false)
const memberSelections = ref<string[]>([])
const savingMembers = ref(false)
const userOptions = ref<SysUser[]>([])
const searchingUser = ref(false)

// 菜单权限
const menuTreeRef = ref<InstanceType<typeof ElTree>>()
const menuTree = ref<MenuNode[]>([])
const loadingPerms = ref(false)
const savingPerms = ref(false)

function orgTypeText(t?: string) {
  return { DEPT: '部门', COMPANY: '公司', GROUP: '集团' }[t ?? ''] ?? t ?? '-'
}
function orgTypeColor(t?: string) {
  return { DEPT: '#409eff', COMPANY: '#67c23a', GROUP: '#e6a23c' }[t ?? ''] ?? '#909399'
}

function buildOrgTree(orgs: OrgNode[]): OrgNode[] {
  const map = new Map<string, OrgNode>()
  const roots: OrgNode[] = []
  orgs.forEach((o) => map.set(o.id, { ...o, children: [] }))
  map.forEach((node) => {
    if (node.parentId && map.has(node.parentId)) {
      map.get(node.parentId)!.children!.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

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

async function loadTree() {
  const orgs = (await orgTree()) || []
  tree.value = buildOrgTree(orgs)
  if (!selectedOrg.value && tree.value.length > 0) {
    selectNode(tree.value[0])
  } else if (selectedOrg.value) {
    const found = findNode(tree.value, selectedOrg.value.id)
    if (!found) {
      selectedOrg.value = null
      if (tree.value.length > 0) selectNode(tree.value[0])
    }
  }
}

function findNode(nodes: OrgNode[], id: string): OrgNode | null {
  for (const n of nodes) {
    if (n.id === id) return n
    const child = findNode(n.children || [], id)
    if (child) return child
  }
  return null
}

function handleNodeClick(data: OrgNode) {
  selectNode(data)
}

function selectNode(node: OrgNode) {
  selectedOrg.value = node
  activeTab.value = 'members'
  loadMembers()
}

/** 右键菜单：简化为直接弹出操作（编辑/新增子组织/删除） */
function handleContextMenu(event: MouseEvent, data: OrgNode) {
  event.preventDefault()
  ElMessageBox.confirm(`选择要对「${data.orgName}」执行的操作：`, '组织操作', {
    confirmButtonText: '新增下级',
    cancelButtonText: '编辑',
    distinguishCancelAndClose: true,
    type: 'info',
  })
    .then(() => {
      openCreate(data.id)
    })
    .catch((action: string) => {
      if (action === 'cancel') {
        openEdit(data)
      }
    })
}

async function loadMembers() {
  if (!selectedOrg.value) return
  loadingUsers.value = true
  try {
    members.value = (await orgUsers(selectedOrg.value.id)) || []
  } finally {
    loadingUsers.value = false
  }
}

async function openMemberDialog() {
  memberSelections.value = []
  userOptions.value = []
  memberDialogVisible.value = true
  // 预加载部分用户
  await searchUsers('')
}

async function searchUsers(keyword: string) {
  searchingUser.value = true
  try {
    const data = await pageUsers({ current: 1, size: 50, username: keyword, realName: keyword })
    userOptions.value = data.records || []
  } finally {
    searchingUser.value = false
  }
}

async function saveMembers() {
  if (!selectedOrg.value) return
  savingMembers.value = true
  try {
    await assignOrgUsers(selectedOrg.value.id, memberSelections.value)
    ElMessage.success('成员已更新')
    memberDialogVisible.value = false
    loadMembers()
  } finally {
    savingMembers.value = false
  }
}

async function removeMember(row: any) {
  if (!selectedOrg.value) return
  await ElMessageBox.confirm(`确认将成员「${row.realName || row.username}」移出该部门吗？`, '提示', { type: 'warning' })
  const remaining = members.value.filter((m) => m.userId !== row.userId).map((m) => m.userId)
  await assignOrgUsers(selectedOrg.value.id, remaining)
  ElMessage.success('已移除')
  loadMembers()
}

async function loadMenuPermissions() {
  if (!selectedOrg.value) return
  loadingPerms.value = true
  try {
    if (menuTree.value.length === 0) {
      const menus = (await allMenus()) || []
      menuTree.value = buildMenuTree(menus)
    }
    const checked = (await orgPermissions(selectedOrg.value.id)) || []
    requestAnimationFrame(() => {
      menuTreeRef.value?.setCheckedKeys(checked)
    })
  } finally {
    loadingPerms.value = false
  }
}

function onTabChange(name: string | number) {
  if (name === 'permissions') {
    loadMenuPermissions()
  }
}

async function savePermissions() {
  if (!selectedOrg.value) return
  savingPerms.value = true
  try {
    const checked = menuTreeRef.value?.getCheckedKeys(false) || []
    const halfChecked = menuTreeRef.value?.getHalfCheckedKeys() || []
    const menuIds = [...checked, ...halfChecked].filter((id): id is string => id != null && id !== '')
    await assignOrgPermissions(selectedOrg.value.id, menuIds)
    ElMessage.success('部门菜单权限已保存')
  } finally {
    savingPerms.value = false
  }
}

function openCreate(parentId?: string) {
  isEdit.value = false
  Object.assign(form, { parentId: parentId || '', orgName: '', orgCode: '', orgType: 'DEPT', sortOrder: 0, status: 1 })
  loadParentOptions()
  dialogVisible.value = true
}

function openEdit(node: OrgNode) {
  isEdit.value = true
  Object.assign(form, { ...node })
  loadParentOptions()
  dialogVisible.value = true
}

async function loadParentOptions() {
  const orgs = (await orgTree()) || []
  parentOptions.value = buildOrgTree(orgs)
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
    await loadTree()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(node: OrgNode) {
  await ElMessageBox.confirm(`确认删除组织「${node.orgName}」吗？`, '提示', { type: 'warning' })
  await deleteOrg(node.id)
  ElMessage.success('删除成功')
  selectedOrg.value = null
  await loadTree()
}

onMounted(loadTree)
</script>

<style scoped>
.org-container {
  display: flex;
  gap: 16px;
  align-items: stretch;
  height: calc(100vh - 120px);
}
.left-panel {
  width: 320px;
  flex-shrink: 0;
}
.right-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.tree-scroll {
  height: calc(100vh - 200px);
}
.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding-right: 8px;
}
.node-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.org-info {
  margin-bottom: 12px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.member-count {
  color: #909399;
  font-size: 13px;
}
.perm-summary {
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
}
.perm-tree-wrap {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 8px;
}
.perm-footer {
  margin-top: 12px;
  text-align: right;
}
.member-tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}
</style>