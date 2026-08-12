import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { pinia } from '@/stores'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layouts/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '首页' },
      },
      // 系统管理
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/user/UserList.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/role/RoleList.vue'),
        meta: { title: '角色管理' },
      },
      {
        path: 'system/menu',
        name: 'SystemMenu',
        component: () => import('@/views/system/menu/MenuList.vue'),
        meta: { title: '菜单管理' },
      },
      {
        path: 'system/org',
        name: 'SystemOrg',
        component: () => import('@/views/system/org/OrgList.vue'),
        meta: { title: '组织管理' },
      },
      // 权限管理
      {
        path: 'permission/policy-row',
        name: 'RowPolicy',
        component: () => import('@/views/permission/row-policy/RowPolicyList.vue'),
        meta: { title: '行权限策略' },
      },
      {
        path: 'permission/policy-column',
        name: 'ColumnPolicy',
        component: () => import('@/views/permission/column-policy/ColumnPolicyList.vue'),
        meta: { title: '列权限策略' },
      },
      {
        path: 'permission/table',
        name: 'TablePermission',
        component: () => import('@/views/permission/table/TablePermissionList.vue'),
        meta: { title: '表权限' },
      },
      // 审批中心
      {
        path: 'approval/apply',
        name: 'ApprovalApply',
        component: () => import('@/views/approval/apply/ApplyList.vue'),
        meta: { title: '我的申请' },
      },
      {
        path: 'approval/todo',
        name: 'ApprovalTodo',
        component: () => import('@/views/approval/todo/TodoList.vue'),
        meta: { title: '待办审批' },
      },
      // 数据源管理
      {
        path: 'datasource/list',
        name: 'DatasourceList',
        component: () => import('@/views/datasource/list/DatasourceList.vue'),
        meta: { title: '数据源列表' },
      },
      // 元数据中心
      {
        path: 'metadata/structure',
        name: 'MetadataStructure',
        component: () => import('@/views/metadata/structure/Structure.vue'),
        meta: { title: '库表结构' },
      },
      {
        path: 'metadata/lineage',
        name: 'MetadataLineage',
        component: () => import('@/views/metadata/lineage/Lineage.vue'),
        meta: { title: '血缘关系' },
      },
      {
        path: 'metadata/search',
        name: 'MetadataSearch',
        component: () => import('@/views/metadata/search/Search.vue'),
        meta: { title: '元数据搜索' },
      },
      // 数据集市
      {
        path: 'mart/model',
        name: 'MartModel',
        component: () => import('@/views/mart/model/ModelList.vue'),
        meta: { title: '模型管理' },
      },
      {
        path: 'mart/metric',
        name: 'MartMetric',
        component: () => import('@/views/mart/metric/MetricList.vue'),
        meta: { title: '指标管理' },
      },
      {
        path: 'mart/dimension',
        name: 'MartDimension',
        component: () => import('@/views/mart/dimension/DimensionList.vue'),
        meta: { title: '维度管理' },
      },
      // SQL 查询
      {
        path: 'query/query',
        name: 'QueryWorkbench',
        component: () => import('@/views/query/query/QueryWorkbench.vue'),
        meta: { title: '查询工作台' },
      },
      // 可视化
      {
        path: 'visual/dashboard',
        name: 'VisualDashboard',
        component: () => import('@/views/visual/dashboard/DashboardList.vue'),
        meta: { title: '仪表板管理' },
      },
      {
        path: 'visual/dashboard/edit/:id',
        name: 'VisualDashboardEdit',
        component: () => import('@/views/visual/dashboard/DashboardEditor.vue'),
        meta: { title: 'DK实时分析板' },
      },
      // 调度中心
      {
        path: 'schedule/job',
        name: 'ScheduleJob',
        component: () => import('@/views/schedule/job/JobList.vue'),
        meta: { title: '调度任务' },
      },
      // 通知中心
      {
        path: 'notification/template',
        name: 'NotifyTemplate',
        component: () => import('@/views/notification/template/TemplateList.vue'),
        meta: { title: '通知模板' },
      },
      {
        path: 'notification/send',
        name: 'NotifySend',
        component: () => import('@/views/notification/send/Send.vue'),
        meta: { title: '发送通知' },
      },
      {
        path: 'notification/subscription',
        name: 'NotifySubscription',
        component: () => import('@/views/notification/subscription/SubscriptionList.vue'),
        meta: { title: '订阅管理' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 登录守卫：无 token 跳转登录页
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore(pinia)
  if (to.path !== '/login' && !userStore.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

router.afterEach((to) => {
  const title = to.meta?.title as string | undefined
  document.title = title ? `${title} - Data Khaos 数据治理平台` : 'Data Khaos 数据治理平台'
})

export default router
