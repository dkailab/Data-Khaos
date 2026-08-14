<template>
  <div>
    <el-card shadow="never" class="welcome-card">
      <div class="welcome">
        <div>
          <h2>欢迎回来，{{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}</h2>
          <p>这里是 Data-Khaos-Platform 数据治理平台，一站式管理数据源、元数据、数据集市与数据权限。</p>
        </div>
        <el-button type="primary" @click="router.push('/query/query')">前往 SQL 查询</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="card in statCards" :key="card.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" :style="{ background: card.color }">
              <el-icon :size="26" color="#fff"><component :is="card.icon" /></el-icon>
            </div>
            <div>
              <div class="stat-title">{{ card.title }}</div>
              <div class="stat-value">{{ card.value }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>平台能力</template>
      <el-row :gutter="16">
        <el-col :span="8" v-for="feat in features" :key="feat.title">
          <div class="feature-item">
            <el-icon :size="20" color="#409eff"><component :is="feat.icon" /></el-icon>
            <div>
              <div class="feature-title">{{ feat.title }}</div>
              <div class="feature-desc">{{ feat.desc }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const statCards = ref([
  { title: '数据源数量', value: 8, icon: 'Connection', color: '#409eff' },
  { title: '已采集表数量', value: 126, icon: 'Collection', color: '#67c23a' },
  { title: '发布模型', value: 32, icon: 'DataBoard', color: '#e6a23c' },
  { title: '调度任务', value: 15, icon: 'AlarmClock', color: '#f56c6c' },
])

const features = ref([
  { title: '多源接入', desc: '支持 MySQL、达梦、Hive、Doris、ClickHouse、PostgreSQL、Oracle 等数据源', icon: 'Connection' },
  { title: '元数据管理', desc: '自动采集库表结构、字段信息、血缘关系，支持检索', icon: 'Collection' },
  { title: '数据权限', desc: '行列级权限策略、表级权限、审批流程一体化管控', icon: 'Lock' },
  { title: '数据集市', desc: '模型 / 指标 / 维度统一管理，支持发布与预览', icon: 'DataBoard' },
  { title: 'SQL 查询', desc: '在线 SQL 查询工作台，自动 SQL 审核与权限校验', icon: 'EditPen' },
  { title: '可视化与调度', desc: '仪表板管理、定时任务调度、消息通知推送', icon: 'PieChart' },
])
</script>

<style scoped>
.welcome-card {
  margin-bottom: 16px;
}
.welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.welcome h2 {
  margin: 0 0 8px;
}
.welcome p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}
.stat-row {
  margin-bottom: 16px;
}
.stat-card :deep(.el-card__body) {
  padding: 18px;
}
.stat-item {
  display: flex;
  align-items: center;
  gap: 14px;
}
.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stat-title {
  color: #909399;
  font-size: 13px;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}
.feature-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
}
.feature-title {
  font-weight: 600;
}
.feature-desc {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}
</style>
