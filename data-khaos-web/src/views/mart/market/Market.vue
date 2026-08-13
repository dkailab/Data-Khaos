<template>
  <el-card shadow="never">
    <!-- 顶部概览 -->
    <div class="market-hero">
      <div class="hero-text">
        <div class="hero-title">模型市场</div>
        <div class="hero-sub">检索并订阅已发布的数仓语义模型，能力位：model:browse</div>
      </div>
      <div class="hero-stats">
        <div class="stat">
          <div class="stat-num">{{ total }}</div>
          <div class="stat-label">已发布模型</div>
        </div>
        <div class="stat">
          <div class="stat-num">{{ totalMetrics }}</div>
          <div class="stat-label">指标</div>
        </div>
        <div class="stat">
          <div class="stat-num">{{ totalDimensions }}</div>
          <div class="stat-label">维度</div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="模型名称/编码" clearable style="width: 220px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="分层">
          <el-select v-model="query.layerId" clearable placeholder="全部分层" style="width: 150px" @change="handleSearch">
            <el-option v-for="l in layers" :key="l.id" :label="l.layerName" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 卡片网格 -->
    <div v-loading="loading">
      <div v-if="list.length" class="card-grid">
        <div v-for="m in list" :key="m.id" class="model-card" @click="openDetail(m)">
          <div class="card-head">
            <el-tag :type="layerTagType(m.layerCode)" size="small">{{ m.layerName || m.layerCode || '未分层' }}</el-tag>
            <el-tag size="small" type="info">v{{ m.version }}</el-tag>
          </div>
          <div class="card-title">{{ m.modelName }}</div>
          <div class="card-code">{{ m.modelCode }}</div>
          <div class="card-desc">{{ m.description || '暂无描述' }}</div>
          <div class="card-stats">
            <div class="stat-item">
              <span class="num">{{ m.metricCount ?? 0 }}</span>
              <span class="label">指标</span>
            </div>
            <div class="stat-item">
              <span class="num">{{ m.dimensionCount ?? 0 }}</span>
              <span class="label">维度</span>
            </div>
            <div class="stat-item">
              <span class="num">{{ m.relCount ?? 0 }}</span>
              <span class="label">关联</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无已发布模型" />
    </div>

    <el-pagination
      class="pager"
      v-model:current-page="query.current"
      v-model:page-size="query.size"
      :total="total"
      :page-sizes="[12, 24, 48]"
      layout="total, sizes, prev, pager, next, jumper"
      @change="load"
    />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import { listMartLayers, pageMartMarket } from '@/api/mart'
import type { MarketModelDto, MartWarehouseLayer } from '@/types'

const router = useRouter()
const loading = ref(false)
const list = ref<MarketModelDto[]>([])
const total = ref(0)
const totalMetrics = ref(0)
const totalDimensions = ref(0)
const layers = ref<MartWarehouseLayer[]>([])
const query = reactive<Record<string, any>>({ current: 1, size: 12, keyword: '', layerId: undefined })

function layerTagType(code?: string): 'primary' | 'warning' | 'success' | 'info' {
  if (code === 'ODS') return 'info'
  if (code === 'DWD') return 'primary'
  if (code === 'DWS') return 'warning'
  return 'success'
}

async function load() {
  loading.value = true
  try {
    const data = await pageMartMarket({ ...query })
    list.value = data.records
    total.value = Number(data.total)
    totalMetrics.value = list.value.reduce((s, m) => s + (m.metricCount ?? 0), 0)
    totalDimensions.value = list.value.reduce((s, m) => s + (m.dimensionCount ?? 0), 0)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.current = 1
  load()
}

function handleReset() {
  query.keyword = ''
  query.layerId = undefined
  handleSearch()
}

function openDetail(m: MarketModelDto) {
  router.push({ path: `/mart/market/${m.id}` })
}

onMounted(async () => {
  try {
    layers.value = await listMartLayers()
  } catch {
    layers.value = []
  }
  load()
})
</script>

<style scoped>
.market-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  margin-bottom: 16px;
  border-radius: 8px;
  background: linear-gradient(135deg, #165dff19 0%, #165dff05 100%);
  border: 1px solid #165dff1f;
}
.hero-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.hero-sub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.hero-stats {
  display: flex;
  gap: 32px;
}
.stat {
  text-align: center;
}
.stat-num {
  font-size: 24px;
  font-weight: 600;
  color: #165dff;
}
.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.toolbar {
  margin-bottom: 16px;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}
.model-card {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--el-bg-color);
}
.model-card:hover {
  border-color: #165dff;
  box-shadow: 0 4px 16px #165dff1f;
  transform: translateY(-2px);
}
.card-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.card-code {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-family: monospace;
}
.card-desc {
  margin-top: 10px;
  min-height: 40px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-stats {
  display: flex;
  gap: 24px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.stat-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.stat-item .num {
  font-size: 18px;
  font-weight: 600;
  color: #165dff;
}
.stat-item .label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>