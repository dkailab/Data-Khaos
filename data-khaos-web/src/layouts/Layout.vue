<template>
  <div class="portal">
    <!-- ================= 顶部通栏全局导航栏 ================= -->
    <header class="nav-bar">
      <div class="nav-inner">
        <div class="nav-logo" @click="router.push('/dashboard')">
          <el-icon :size="24" color="#fff"><DataAnalysis /></el-icon>
          <span class="nav-logo-text">Data-Khaos-Platform</span>
        </div>

        <nav class="nav-menu">
          <div class="nav-item" @click="router.push('/dashboard')" :class="{ active: route.path === '/dashboard' }">
            首页
          </div>

          <!-- 门户地图：悬浮下拉弹窗 -->
          <div
            class="nav-item nav-map"
            :class="{ active: mapOpen }"
            @mouseenter="mapOpen = true"
            @mouseleave="mapOpen = false"
          >
            门户地图
            <el-icon class="nav-arrow"><ArrowDown /></el-icon>
            <transition name="map-fade">
              <div v-show="mapOpen" class="map-panel">
                <div
                  v-for="module in categories"
                  :key="module.key"
                  class="map-module"
                  @mouseenter="hoverModule = module.key"
                >
                  <div class="map-module-head">
                    <el-icon :size="18" color="#165dff"><component :is="module.icon" /></el-icon>
                    <span>{{ module.title }}</span>
                  </div>
                  <div class="map-module-body">
                    <template v-for="feat in module.features" :key="feat.key">
                      <span
                        v-if="feat.path"
                        class="map-feature"
                        @click="go(feat.path)"
                      >
                        {{ feat.title }}
                      </span>
                      <span v-else class="map-feature todo" @click="showTodo(feat.title)">
                        {{ feat.title }}（待建设）
                      </span>
                    </template>
                  </div>
                </div>
              </div>
            </transition>
          </div>

          <!-- 六大分类模块导航（按可插拔配置渲染） -->
          <template v-for="mod in categories" :key="mod.key">
            <div
              class="nav-item"
              :class="{ active: activeModule === mod.key }"
              @mouseenter="activeModule = mod.key"
              @mouseleave="activeModule = ''"
              @click="goModule(mod)"
            >
              {{ mod.title }}
              <el-icon class="nav-arrow"><ArrowDown /></el-icon>
              <transition name="map-fade">
                <div v-show="activeModule === mod.key" class="map-panel mod-panel">
                  <div class="map-module">
                    <div class="map-module-head">
                      <el-icon :size="18" color="#165dff"><component :is="mod.icon" /></el-icon>
                      <span>{{ mod.title }}</span>
                    </div>
                    <div class="map-module-body">
                      <template v-for="feat in mod.features" :key="feat.key">
                        <span v-if="feat.path" class="map-feature" @click="go(feat.path)">{{ feat.title }}</span>
                        <span v-else class="map-feature todo" @click="showTodo(feat.title)">{{ feat.title }}（待建设）</span>
                      </template>
                    </div>
                  </div>
                </div>
              </transition>
            </div>
          </template>
        </nav>

        <div class="nav-right">
          <div class="nav-item nav-extra" @mouseenter="extraOpen = true" @mouseleave="extraOpen = false">
            系统管理
            <el-icon class="nav-arrow"><ArrowDown /></el-icon>
            <transition name="map-fade">
              <div v-show="extraOpen" class="map-panel extra-panel">
                <div class="map-module">
                  <div class="map-module-body">
                    <template v-for="feat in systemCategory.features" :key="feat.key">
                      <span v-if="feat.path" class="map-feature" @click="go(feat.path)">{{ feat.title }}</span>
                    </template>
                    <span v-if="canConfig" class="map-feature accent" @click="go('/system/module-config')">门户模块配置</span>
                  </div>
                </div>
              </div>
            </transition>
          </div>

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
                <el-dropdown-item v-if="canConfig" command="moduleConfig">门户模块配置</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- ================= 内容区 ================= -->
    <main class="portal-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useModuleStore } from '@/stores/module'
import { getUserPermission } from '@/api/permission'
import type { CategoryDef } from '@/modules/registry'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const moduleStore = useModuleStore()

/** 六大分类模块（注册表 + 后端可插拔配置合并后的可见模块） */
const categories = moduleStore.categories
/** 系统管理分类 */
const systemCategory = moduleStore.systemCategory
/** 当前用户是否可配置门户模块 */
const canConfig = moduleStore.canConfig

const mapOpen = ref(false)
const extraOpen = ref(false)
const hoverModule = ref('')
const activeModule = ref('')

function go(path: string) {
  mapOpen.value = false
  extraOpen.value = false
  activeModule.value = ''
  router.push(path)
}

function goModule(mod: CategoryDef) {
  const first = mod.features.find((f) => f.path)
  if (first?.path) {
    go(first.path)
  } else {
    showTodo(mod.title)
  }
}

function showTodo(name: string) {
  ElMessage.info(`「${name}」功能正在建设中，敬请期待`)
}

async function handleCommand(command: string) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确认退出登录吗？', '提示', { type: 'warning' })
    await userStore.logout()
    router.push('/login')
  } else if (command === 'moduleConfig') {
    router.push('/system/module-config')
  }
}

onMounted(async () => {
  // 拉取用户能力位（用于判定 module:config 管理员权限），并加载可插拔模块配置
  let capabilityFlags: string[] | undefined
  try {
    const userId = userStore.userInfo?.id
    if (userId) {
      const perm = await getUserPermission(userId)
      capabilityFlags = perm?.capabilityFlags
    }
  } catch {
    // 权限接口异常不阻塞门户渲染
  }
  await moduleStore.loadConfig(capabilityFlags)
})
</script>

<style scoped>
.portal {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--datakhaos-bg);
}

/* ============ 顶部通栏导航栏 ============ */
.nav-bar {
  flex-shrink: 0;
  height: 60px;
  background: linear-gradient(120deg, #7fb8e6 0%, #165dff 100%);
  box-shadow: 0 2px 12px rgba(22, 93, 255, 0.18);
  position: relative;
  z-index: 100;
}
.nav-inner {
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 24px;
}
.nav-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex-shrink: 0;
}
.nav-logo-text {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.02em;
}
.nav-menu {
  flex: 1;
  display: flex;
  align-items: stretch;
  gap: 4px;
  height: 100%;
}
.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 0 16px;
  height: 100%;
  color: rgba(255, 255, 255, 0.92);
  font-size: 15px;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
  white-space: nowrap;
}
.nav-item:hover,
.nav-item.active {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}
.nav-arrow {
  font-size: 12px;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  height: 100%;
}
.nav-extra {
  padding: 0 12px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #fff;
  outline: none;
  padding: 0 8px;
}
.avatar {
  background: rgba(255, 255, 255, 0.25);
}
.username {
  font-size: 14px;
  color: #fff;
}

/* ============ 门户地图悬浮下拉弹窗 ============ */
.map-panel {
  position: absolute;
  top: calc(100% + 10px);
  left: 0;
  min-width: 420px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(8px);
  border: 1px solid #d3e6f7;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(22, 93, 255, 0.12);
  padding: 16px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.mod-panel {
  grid-template-columns: 1fr;
  min-width: 260px;
}
.extra-panel {
  grid-template-columns: 1fr;
  min-width: 200px;
}
.map-module-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: #1d2129;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eaf3fb;
}
.map-module-body {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.map-feature {
  display: inline-block;
  padding: 5px 12px;
  border-radius: 8px;
  font-size: 13px;
  color: #4e5969;
  background: #f2f8fd;
  border: 1px solid #eaf3fb;
  cursor: pointer;
  transition: all 0.18s;
}
.map-feature:hover {
  background: linear-gradient(120deg, #7fb8e6, #165dff);
  color: #fff;
  border-color: transparent;
}
.map-feature.accent {
  background: linear-gradient(120deg, #7fb8e6, #165dff);
  color: #fff;
  border-color: transparent;
}
.map-feature.todo {
  color: #a9aeb8;
  background: #f7f8fa;
  cursor: not-allowed;
}
.map-feature.todo:hover {
  background: #f7f8fa;
  color: #a9aeb8;
  transform: none;
}

.map-fade-enter-active,
.map-fade-leave-active {
  transition: opacity 0.18s, transform 0.18s;
}
.map-fade-enter-from,
.map-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ============ 内容区 ============ */
.portal-content {
  flex: 1;
  overflow: auto;
  padding: 16px;
}
</style>