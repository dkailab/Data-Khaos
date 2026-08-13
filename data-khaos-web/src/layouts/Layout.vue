<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="aside">
      <div class="logo">
        <el-icon :size="26" color="#409eff"><DataAnalysis /></el-icon>
        <span v-show="!collapsed" class="logo-title">Data Khaos</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        background-color="#001529"
        text-color="rgba(255,255,255,0.72)"
        active-text-color="#ffffff"
        router
      >
        <template v-for="item in menus" :key="item.path">
          <el-menu-item v-if="!item.children || item.children.length === 0" :index="item.path!">
            <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
          <el-sub-menu v-else :index="item.title">
            <template #title>
              <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path!">
              <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
              <template #title>{{ child.title }}</template>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-aside>

    <el-container class="main">
      <el-header class="header" height="56px">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <Expand v-if="collapsed" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="30" class="avatar">
                <el-icon><UserFilled /></el-icon>
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.realName || userStore.userInfo?.username || '未登录' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

interface MenuItem {
  title: string
  path?: string
  icon?: string
  children?: MenuItem[]
}

const menus: MenuItem[] = [
  { title: '首页', path: '/dashboard', icon: 'HomeFilled' },
  {
    title: '系统管理',
    icon: 'Setting',
    children: [
      { title: '用户管理', path: '/system/user' },
      { title: '角色管理', path: '/system/role' },
      { title: '菜单管理', path: '/system/menu' },
      { title: '组织管理', path: '/system/org' },
    ],
  },
  {
    title: '权限管理',
    icon: 'Lock',
    children: [
      { title: '行权限策略', path: '/permission/policy-row' },
      { title: '列权限策略', path: '/permission/policy-column' },
      { title: '表权限', path: '/permission/table' },
    ],
  },
  {
    title: '审批中心',
    icon: 'DocumentChecked',
    children: [
      { title: '我的申请', path: '/approval/apply' },
      { title: '待办审批', path: '/approval/todo' },
    ],
  },
  {
    title: '数据源管理',
    icon: 'Connection',
    children: [{ title: '数据源列表', path: '/datasource/list' }],
  },
  {
    title: '元数据中心',
    icon: 'Collection',
    children: [
      { title: '库表结构', path: '/metadata/structure' },
      { title: '血缘关系', path: '/metadata/lineage' },
      { title: '元数据搜索', path: '/metadata/search' },
    ],
  },
  {
    title: '数据集市',
    icon: 'DataBoard',
    children: [
      { title: '模型市场', path: '/mart/market' },
      { title: '模型管理', path: '/mart/model' },
      { title: '指标管理', path: '/mart/metric' },
      { title: '维度管理', path: '/mart/dimension' },
    ],
  },
  {
    title: 'SQL 查询',
    icon: 'EditPen',
    children: [
      { title: '查询工作台', path: '/query/query' },
      { title: '即席分析', path: '/visual/adhoc' },
    ],
  },
  {
    title: '可视化',
    icon: 'PieChart',
    children: [{ title: '仪表板管理', path: '/visual/dashboard' }],
  },
  {
    title: '数据质量',
    icon: 'Odometer',
    children: [
      { title: '质量规则', path: '/dquality/rule' },
      { title: '质量任务', path: '/dquality/task' },
      { title: '稽核报告', path: '/dquality/snapshot' },
    ],
  },
  {
    title: '调度中心',
    icon: 'AlarmClock',
    children: [{ title: '调度任务', path: '/schedule/job' }],
  },
  {
    title: '通知中心',
    icon: 'Bell',
    children: [
      { title: '通知模板', path: '/notification/template' },
      { title: '发送通知', path: '/notification/send' },
      { title: '订阅管理', path: '/notification/subscription' },
    ],
  },
]

const collapsed = ref(false)
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title as string | undefined)

async function handleCommand(command: string) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出登录吗？', '提示', { type: 'warning' })
    await userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}
.aside {
  background-color: #001529;
  transition: width 0.2s;
  overflow-x: hidden;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
}
.logo-title {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}
.aside :deep(.el-menu) {
  border-right: none;
}
.main {
  min-width: 0;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 1;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #303133;
  outline: none;
}
.avatar {
  background: #409eff;
}
.username {
  font-size: 14px;
}
.content {
  background: #f0f2f5;
  overflow: auto;
}
</style>
