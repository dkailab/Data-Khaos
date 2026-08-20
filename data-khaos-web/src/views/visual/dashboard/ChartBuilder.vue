<template>
  <div class="bi-chart-builder" :class="[`theme-${theme}`]">
    <!-- ==================== 顶部栏 ==================== -->
    <header class="cb-header">
      <div class="cb-header-left">
        <el-tooltip content="返回仪表板">
          <el-button :icon="ArrowLeft" circle size="small" @click="goBack" />
        </el-tooltip>
        <el-icon class="cb-logo" :size="18"><DataAnalysis /></el-icon>
        <el-input v-model="title" class="cb-title-input" size="small" placeholder="图表标题" />
        <el-tag v-if="primaryDataset" size="small" effect="plain" class="cb-ds-tag">
          <el-icon :size="11"><Coin /></el-icon>
          {{ primaryDataset.name }}
          <span class="cb-ds-type" :class="dsTypeClass(primaryDataset.datasourceType)">
            {{ primaryDataset.datasourceType || 'DATASET' }}
          </span>
        </el-tag>
      </div>
      <div class="cb-header-right">
        <el-tooltip content="切换主题">
          <el-button :icon="theme === 'dark' ? Sunny : Moon" circle size="small" @click="toggleTheme" />
        </el-tooltip>
        <el-tooltip content="查看生成的 SQL">
          <el-button :icon="Document" circle size="small" :disabled="!generatedSql" @click="sqlVisible = !sqlVisible" />
        </el-tooltip>
        <el-button size="small" :icon="Refresh" :loading="loading" @click="runQuery(true)">刷新数据</el-button>
        <el-button type="primary" size="small" :icon="Check" :loading="saving" @click="saveAndBack">保存并返回</el-button>
      </div>
    </header>

    <!-- 跨数据集冲突警示条 -->
    <div v-if="conflictFields.length" class="cb-conflict-bar">
      <el-icon><WarningFilled /></el-icon>
      <span>
        检测到 <b>{{ conflictFields.length }}</b> 个跨数据集字段
        <template v-if="conflictDatasetNames.length">（{{ conflictDatasetNames.join('、') }}）</template>
        ：不同数据集/模型的字段无法联合查询，查询时将被忽略。
      </span>
      <el-button size="small" type="danger" plain @click="removeConflicts">移除冲突字段</el-button>
    </div>

    <!-- ==================== 上方货架区：筛选器 / 维度 / 指标 ==================== -->
    <section class="cb-shelf">
      <!-- 筛选器 -->
      <div
        class="cb-shelf-row"
        :class="{ 'drop-active': dropTarget === 'filter' }"
        @dragover.prevent="dropTarget = 'filter'"
        @dragleave="dropTarget = ''"
        @drop="onShelfDrop($event, 'filter')"
      >
        <div class="cb-shelf-label filter">
          <el-icon><Filter /></el-icon>
          <span>筛选器</span>
        </div>
        <div class="cb-shelf-body">
          <div v-for="(f, i) in filters" :key="f.key" class="cb-chip filter" :class="{ conflict: isConflict(f) }">
            <el-icon class="cb-chip-icon"><Filter /></el-icon>
            <span class="cb-chip-name">{{ f.fieldName }}</span>
            <el-tooltip v-if="isConflict(f)" content="与其他字段不属于同一数据集，无法联合查询">
              <el-icon class="cb-chip-warn"><WarningFilled /></el-icon>
            </el-tooltip>
            <el-select v-model="f.operator" size="small" class="cb-filter-op" @change="f.values = []">
              <el-option v-for="op in FILTER_OPS" :key="op.value" :label="op.label" :value="op.value" />
            </el-select>
            <el-input
              v-model="f.valueInput"
              size="small"
              class="cb-filter-value"
              :placeholder="valuePlaceholder(f.operator)"
              @change="syncFilterValues(f)"
            />
            <el-icon class="cb-chip-remove" @click="filters.splice(i, 1)"><Close /></el-icon>
          </div>
          <div class="cb-shelf-empty" :class="{ hover: dropTarget === 'filter' }">
            {{ filters.length ? '' : '拖入字段作为筛选器' }}
          </div>
        </div>
      </div>

      <!-- 维度 -->
      <div
        class="cb-shelf-row"
        :class="{ 'drop-active': dropTarget === 'dimension' }"
        @dragover.prevent="dropTarget = 'dimension'"
        @dragleave="dropTarget = ''"
        @drop="onShelfDrop($event, 'dimension')"
      >
        <div class="cb-shelf-label dimension">
          <el-icon><Menu /></el-icon>
          <span>维度</span>
        </div>
        <div class="cb-shelf-body">
          <div v-for="(d, i) in selectedDims" :key="d.fieldCode" class="cb-chip dimension" :class="{ conflict: isConflict(d) }">
            <el-icon class="cb-chip-icon"><Files /></el-icon>
            <span class="cb-chip-name">{{ d.fieldName }}</span>
            <el-tooltip v-if="isConflict(d)" content="与其他字段不属于同一数据集，无法联合查询">
              <el-icon class="cb-chip-warn"><WarningFilled /></el-icon>
            </el-tooltip>
            <el-select v-model="d.sort" size="small" class="cb-dim-sort">
              <el-option label="默认" value="" />
              <el-option label="升序" value="ASC" />
              <el-option label="降序" value="DESC" />
            </el-select>
            <el-icon class="cb-chip-remove" @click="selectedDims.splice(i, 1)"><Close /></el-icon>
          </div>
          <div class="cb-shelf-empty" :class="{ hover: dropTarget === 'dimension' }">
            {{ selectedDims.length ? '' : '拖入或点击维度字段' }}
          </div>
        </div>
      </div>

      <!-- 指标 -->
      <div
        class="cb-shelf-row"
        :class="{ 'drop-active': dropTarget === 'metric' }"
        @dragover.prevent="dropTarget = 'metric'"
        @dragleave="dropTarget = ''"
        @drop="onShelfDrop($event, 'metric')"
      >
        <div class="cb-shelf-label metric">
          <el-icon><TrendCharts /></el-icon>
          <span>指标</span>
        </div>
        <div class="cb-shelf-body">
          <div v-for="(m, i) in selectedMetrics" :key="m.fieldCode" class="cb-chip metric" :class="{ conflict: isConflict(m) }">
            <el-icon class="cb-chip-icon"><DataLine /></el-icon>
            <span class="cb-chip-name">{{ m.fieldName }}</span>
            <el-tooltip v-if="isConflict(m)" content="与其他字段不属于同一数据集，无法联合查询">
              <el-icon class="cb-chip-warn"><WarningFilled /></el-icon>
            </el-tooltip>
            <el-select v-model="m.aggType" size="small" class="cb-metric-agg">
              <el-option v-for="a in AGG_TYPES" :key="a.value" :label="a.label" :value="a.value" />
            </el-select>
            <el-icon class="cb-chip-remove" @click="selectedMetrics.splice(i, 1)"><Close /></el-icon>
          </div>
          <div class="cb-shelf-empty" :class="{ hover: dropTarget === 'metric' }">
            {{ selectedMetrics.length ? '' : '拖入或点击指标字段' }}
          </div>
        </div>
      </div>
    </section>

    <!-- ==================== 主体：左侧资产池 + 中间画布 + 右侧样式 ==================== -->
    <div class="cb-main">
      <!-- 左侧：数据集 / 字段资产池 -->
      <aside class="cb-aside">
        <div class="cb-aside-title">
          <el-icon><Coin /></el-icon>
          <span>数据资产池</span>
        </div>
        <div class="cb-search">
          <el-input v-model="searchText" size="small" placeholder="搜索数据集 / 字段" clearable :prefix-icon="Search" />
        </div>

        <div class="cb-dataset-list">
          <div v-if="!filteredDatasets.length" class="cb-pool-empty">无匹配数据集</div>
          <template v-for="ds in filteredDatasets" :key="ds.id">
            <div
              class="cb-dataset-item"
              :class="{ active: expandedDatasetId === ds.id, primary: ds.id === primaryDatasetId }"
              @click="toggleDataset(ds)"
            >
              <el-icon class="cb-ds-expand" :class="{ open: expandedDatasetId === ds.id }"><ArrowRight /></el-icon>
              <span class="cb-ds-name" :title="ds.name">{{ ds.name }}</span>
              <span class="cb-ds-type" :class="dsTypeClass(ds.datasourceType)">{{ ds.datasourceType || 'DS' }}</span>
              <el-tooltip v-if="ds.id !== primaryDatasetId && primaryDatasetId" content="与当前图表主数据集不同，字段无法联合查询" placement="top">
                <el-icon class="cb-ds-warn"><WarningFilled /></el-icon>
              </el-tooltip>
              <el-tooltip v-else-if="ds.id === primaryDatasetId" content="当前图表主数据集" placement="top">
                <el-icon class="cb-ds-primary"><CircleCheckFilled /></el-icon>
              </el-tooltip>
            </div>

            <!-- 字段池 -->
            <div v-if="expandedDatasetId === ds.id" class="cb-field-pool" :class="{ incompatible: ds.id !== primaryDatasetId && !!primaryDatasetId }">
              <div v-if="ds.id !== primaryDatasetId && primaryDatasetId" class="cb-pool-warn">
                <el-icon><WarningFilled /></el-icon>
                该数据集与当前图表字段不来自同一数据集/模型，加入后无法联合查询（将高亮标红）
              </div>
              <div class="cb-pool-group">
                <div class="cb-pool-group-title">
                  <el-icon><Files /></el-icon> 维度
                  <span class="cb-pool-count">{{ poolDims(ds).length }}</span>
                </div>
                <div
                  v-for="f in poolDims(ds)"
                  :key="f.fieldCode"
                  class="cb-field-item dimension"
                  draggable="true"
                  @dragstart="onFieldDragStart($event, ds, f)"
                  @click="addField(ds, f, 'DIMENSION')"
                >
                  <el-icon><Files /></el-icon>
                  <span class="cb-field-name" :title="f.fieldName">{{ f.fieldName }}</span>
                  <span class="cb-field-code">{{ f.fieldCode }}</span>
                  <el-tooltip content="设为筛选器" placement="top">
                    <el-icon class="cb-field-op" @click.stop="addField(ds, f, 'FILTER')"><Filter /></el-icon>
                  </el-tooltip>
                </div>
                <div v-if="!poolDims(ds).length" class="cb-pool-empty">暂无维度字段</div>
              </div>
              <div class="cb-pool-group">
                <div class="cb-pool-group-title">
                  <el-icon><TrendCharts /></el-icon> 指标
                  <span class="cb-pool-count">{{ poolMetrics(ds).length }}</span>
                </div>
                <div
                  v-for="f in poolMetrics(ds)"
                  :key="f.fieldCode"
                  class="cb-field-item metric"
                  draggable="true"
                  @dragstart="onFieldDragStart($event, ds, f)"
                  @click="addField(ds, f, 'METRIC')"
                >
                  <el-icon><DataLine /></el-icon>
                  <span class="cb-field-name" :title="f.fieldName">{{ f.fieldName }}</span>
                  <span class="cb-field-code">{{ f.fieldCode }}</span>
                  <el-tooltip content="设为筛选器" placement="top">
                    <el-icon class="cb-field-op" @click.stop="addField(ds, f, 'FILTER')"><Filter /></el-icon>
                  </el-tooltip>
                </div>
                <div v-if="!poolMetrics(ds).length" class="cb-pool-empty">暂无指标字段</div>
              </div>
            </div>
          </template>
        </div>
      </aside>

      <!-- 中间：图表类型 + 预览画布 -->
      <main class="cb-canvas">
        <!-- 图表类型选择 -->
        <div class="cb-chart-types">
          <div
            v-for="ct in CHART_TYPES"
            :key="ct.type"
            class="cb-chart-type-item"
            :class="{ active: chartType === ct.type }"
            @click="chartType = ct.type"
          >
            <el-icon :size="16"><component :is="ct.icon" /></el-icon>
            <span>{{ ct.label }}</span>
          </div>
        </div>

        <!-- SQL 查看 -->
        <el-collapse-transition>
          <div v-if="sqlVisible && generatedSql" class="cb-sql-box">
            <div class="cb-sql-title">
              <el-icon><Document /></el-icon> 生成的查询 SQL
              <span v-if="result" class="cb-sql-meta">{{ result.rows?.length || 0 }} 行 · {{ costMs }}ms</span>
            </div>
            <pre class="cb-sql-text">{{ generatedSql }}</pre>
          </div>
        </el-collapse-transition>

        <!-- 预览 -->
        <div class="cb-preview" v-loading="loading" element-loading-text="查询中...">
          <ChartRenderer
            v-if="result && (result.rows?.length || result.columns?.length)"
            :item="previewItem"
            :result="result"
            :loading="loading"
            :theme="theme"
          />
          <div v-else-if="!loading" class="cb-preview-empty">
            <el-icon :size="44"><DataLine /></el-icon>
            <template v-if="!primaryDatasetId">
              <p>从左侧资产池选择数据集，点击或拖入维度/指标字段开始绘图</p>
            </template>
            <template v-else-if="!hasQueryFields">
              <p>请至少选择一个维度或指标</p>
            </template>
            <template v-else>
              <p>暂无数据</p>
              <p class="cb-preview-hint">可调整筛选条件或点击「刷新数据」重试</p>
            </template>
          </div>
        </div>
      </main>

      <!-- 右侧：样式配置 -->
      <aside class="cb-style">
        <div class="cb-aside-title">
          <el-icon><Setting /></el-icon>
          <span>图表样式</span>
        </div>
        <div class="cb-style-body">
          <div class="cb-style-group">
            <div class="cb-style-group-title">标题</div>
            <div class="cb-style-row">
              <span>显示标题</span>
              <el-switch v-model="styleCfg.title.show" size="small" />
            </div>
            <div class="cb-style-row">
              <span>对齐</span>
              <el-radio-group v-model="styleCfg.title.align" size="small">
                <el-radio-button value="left">左</el-radio-button>
                <el-radio-button value="center">中</el-radio-button>
              </el-radio-group>
            </div>
            <div class="cb-style-row">
              <span>字号</span>
              <el-input-number v-model="styleCfg.title.fontSize" :min="10" :max="32" size="small" />
            </div>
          </div>

          <div class="cb-style-group">
            <div class="cb-style-group-title">图例 / 标签</div>
            <div class="cb-style-row">
              <span>显示图例</span>
              <el-switch v-model="styleCfg.legend.show" size="small" />
            </div>
            <div class="cb-style-row">
              <span>图例位置</span>
              <el-select v-model="styleCfg.legend.position" size="small" style="width: 90px">
                <el-option label="顶部" value="top" />
                <el-option label="底部" value="bottom" />
                <el-option label="左侧" value="left" />
                <el-option label="右侧" value="right" />
              </el-select>
            </div>
            <div class="cb-style-row">
              <span>数值标签</span>
              <el-switch v-model="styleCfg.labelShow" size="small" />
            </div>
            <div class="cb-style-row">
              <span>小数位</span>
              <el-input-number v-model="styleCfg.decimalDigits" :min="0" :max="6" size="small" />
            </div>
          </div>

          <div class="cb-style-group">
            <div class="cb-style-group-title">配色</div>
            <div class="cb-style-row">
              <span>主题</span>
              <el-select v-model="styleCfg.colorTheme" size="small" style="width: 120px">
                <el-option label="默认蓝" value="default" />
                <el-option label="商务蓝" value="business" />
                <el-option label="科技感" value="tech" />
                <el-option label="暖色调" value="warm" />
                <el-option label="清新绿" value="fresh" />
              </el-select>
            </div>
          </div>

          <div class="cb-style-group">
            <div class="cb-style-group-title">数据</div>
            <div class="cb-style-row">
              <span>行数限制</span>
              <el-input-number v-model="limit" :min="1" :max="10000" :step="100" size="small" />
            </div>
            <div class="cb-style-row">
              <span>自动查询</span>
              <el-switch v-model="autoQuery" size="small" />
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { type Component, computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, ArrowRight, Check, CircleCheckFilled, Close, Coin, DataAnalysis, DataLine, Document, Files,
  Filter, Grid, Histogram, Menu, Moon, Odometer, PieChart, Refresh, Search, Setting, Sunny, Tickets,
  TrendCharts, WarningFilled,
} from '@element-plus/icons-vue'
import ChartRenderer from '@/components/chart/ChartRenderer.vue'
import { listPublishedDatasets, type DatasetField, type VisualDataset } from '@/api/dataset'
import { listDashboardItems, saveItem } from '@/api/visual'
import type { ChartType, QueryResult, VisualDashboardItem } from '@/types'

/* ============ 路由 ============ */
const route = useRoute()
const router = useRouter()
const dashboardId = ref(route.params.dashboardId as string)
const itemId = ref(route.params.itemId as string)

/* ============ UI 状态 ============ */
const theme = ref<'light' | 'dark'>('light')
const saving = ref(false)
const loading = ref(false)
const sqlVisible = ref(false)
const costMs = ref(0)
const title = ref('未命名图表')
const chartType = ref<ChartType>('BAR')
const limit = ref(1000)
const autoQuery = ref(true)
const searchText = ref('')
const dropTarget = ref('')

const CHART_TYPES = [
  { type: 'BAR', label: '柱状图', icon: Histogram },
  { type: 'LINE', label: '折线图', icon: DataLine },
  { type: 'AREA', label: '面积图', icon: TrendCharts },
  { type: 'PIE', label: '饼图', icon: PieChart },
  { type: 'SCATTER', label: '散点图', icon: Grid },
  { type: 'TABLE', label: '表格', icon: Tickets },
  { type: 'NUMBER', label: '指标卡', icon: Odometer },
  { type: 'FUNNEL', label: '漏斗图', icon: TrendCharts },
  { type: 'RADAR', label: '雷达图', icon: Grid },
  { type: 'GAUGE', label: '仪表盘', icon: Odometer },
] as { type: ChartType; label: string; icon: Component }[]

const FILTER_OPS = [
  { value: 'EQ', label: '=' },
  { value: 'NE', label: '≠' },
  { value: 'GT', label: '>' },
  { value: 'GTE', label: '≥' },
  { value: 'LT', label: '<' },
  { value: 'LTE', label: '≤' },
  { value: 'LIKE', label: '包含' },
  { value: 'IN', label: '属于' },
  { value: 'NOT_IN', label: '不属于' },
  { value: 'BETWEEN', label: '区间' },
]

const AGG_TYPES = [
  { value: 'SUM', label: '求和' },
  { value: 'AVG', label: '平均' },
  { value: 'COUNT', label: '计数' },
  { value: 'COUNT_DISTINCT', label: '去重计数' },
  { value: 'MAX', label: '最大' },
  { value: 'MIN', label: '最小' },
]

/* ============ 数据集资产池 ============ */
const datasets = ref<VisualDataset[]>([])
const expandedDatasetId = ref('')

const filteredDatasets = computed(() => {
  const s = searchText.value.trim().toLowerCase()
  if (!s) return datasets.value
  return datasets.value.filter(
    (d) =>
      d.name.toLowerCase().includes(s) ||
      (d.fields || []).some((f) => f.fieldName.toLowerCase().includes(s) || f.fieldCode.toLowerCase().includes(s)),
  )
})

function poolDims(ds: VisualDataset): DatasetField[] {
  const s = searchText.value.trim().toLowerCase()
  const list = (ds.fields || []).filter((f) => f.fieldType === 'DIMENSION')
  return s ? list.filter((f) => f.fieldName.toLowerCase().includes(s) || f.fieldCode.toLowerCase().includes(s)) : list
}

function poolMetrics(ds: VisualDataset): DatasetField[] {
  const s = searchText.value.trim().toLowerCase()
  const list = (ds.fields || []).filter((f) => f.fieldType === 'METRIC')
  return s ? list.filter((f) => f.fieldName.toLowerCase().includes(s) || f.fieldCode.toLowerCase().includes(s)) : list
}

function toggleDataset(ds: VisualDataset) {
  expandedDatasetId.value = expandedDatasetId.value === ds.id ? '' : (ds.id || '')
}

function dsTypeClass(type?: string): string {
  const t = (type || '').toUpperCase()
  if (t === 'DORIS') return 'doris'
  if (t === 'HIVE') return 'hive'
  if (t === 'MYSQL') return 'mysql'
  if (t.includes('DM') || t.includes('ORACLE') || t.includes('POSTGRE') || t.includes('CLICK')) return 'other'
  return 'other'
}

/* ============ 已选字段（维度 / 指标 / 筛选器） ============ */
interface SelField {
  fieldCode: string
  fieldName: string
  datasetId: string
  datasetName: string
  datasourceType?: string
  dataType?: string
}

interface SelDim extends SelField { sort: string }
interface SelMetric extends SelField { aggType: string }
interface SelFilter extends SelField {
  operator: string
  values: string[]
  valueInput: string
  key: string
}

const selectedDims = ref<SelDim[]>([])
const selectedMetrics = ref<SelMetric[]>([])
const filters = ref<SelFilter[]>([])

/** 主数据集：第一个被选择字段所属的数据集（联查兼容性基准） */
const primaryDatasetId = computed(
  () => selectedDims.value[0]?.datasetId || selectedMetrics.value[0]?.datasetId || filters.value[0]?.datasetId || '',
)
const primaryDataset = computed(() => datasets.value.find((d) => d.id === primaryDatasetId.value))

const conflictFields = computed(() => {
  const pid = primaryDatasetId.value
  if (!pid) return [] as SelField[]
  return [
    ...selectedDims.value,
    ...selectedMetrics.value,
    ...filters.value,
  ].filter((f) => f.datasetId !== pid)
})

const conflictDatasetNames = computed(() => {
  const ids = new Set(conflictFields.value.map((f) => f.datasetId))
  return datasets.value.filter((d) => ids.has(d.id || '')).map((d) => d.name)
})

function isConflict(f: SelField): boolean {
  return !!primaryDatasetId.value && f.datasetId !== primaryDatasetId.value
}

function removeConflicts() {
  const pid = primaryDatasetId.value
  selectedDims.value = selectedDims.value.filter((f) => f.datasetId === pid)
  selectedMetrics.value = selectedMetrics.value.filter((f) => f.datasetId === pid)
  filters.value = filters.value.filter((f) => f.datasetId === pid)
  ElMessage.success('已移除冲突字段')
}

/** 添加字段到货架 */
function addField(ds: VisualDataset, f: DatasetField, target: 'DIMENSION' | 'METRIC' | 'FILTER') {
  const base: SelField = {
    fieldCode: f.fieldCode,
    fieldName: f.fieldName,
    datasetId: ds.id || '',
    datasetName: ds.name,
    datasourceType: ds.datasourceType,
    dataType: f.dataType,
  }
  if (isConflict(base)) {
    ElMessage.warning(`「${f.fieldName}」来自 ${ds.name}，与当前字段不属于同一数据集，无法联合查询`)
  }
  if (target === 'DIMENSION') {
    if (selectedDims.value.some((d) => d.fieldCode === f.fieldCode && d.datasetId === ds.id)) return
    selectedDims.value.push({ ...base, sort: '' })
  } else if (target === 'METRIC') {
    if (selectedMetrics.value.some((m) => m.fieldCode === f.fieldCode && m.datasetId === ds.id)) return
    selectedMetrics.value.push({ ...base, aggType: f.aggType || 'SUM' })
  } else {
    if (filters.value.some((x) => x.fieldCode === f.fieldCode && x.datasetId === ds.id)) return
    filters.value.push({ ...base, operator: 'EQ', values: [], valueInput: '', key: `f_${Date.now()}_${Math.random().toString(36).slice(2, 6)}` })
  }
}

/* 拖拽 */
function onFieldDragStart(e: DragEvent, ds: VisualDataset, f: DatasetField) {
  e.dataTransfer?.setData('application/json', JSON.stringify({ datasetId: ds.id, fieldCode: f.fieldCode }))
  e.dataTransfer!.effectAllowed = 'copy'
}

function onShelfDrop(e: DragEvent, target: 'dimension' | 'metric' | 'filter') {
  dropTarget.value = ''
  const raw = e.dataTransfer?.getData('application/json')
  if (!raw) return
  try {
    const { datasetId, fieldCode } = JSON.parse(raw)
    const ds = datasets.value.find((d) => d.id === datasetId)
    const f = ds?.fields?.find((x) => x.fieldCode === fieldCode)
    if (!ds || !f) return
    const t = target === 'filter' ? 'FILTER' : f.fieldType === 'METRIC' ? 'METRIC' : 'DIMENSION'
    addField(ds, f, t)
  } catch { /* ignore */ }
}

/* 筛选器值 */
function valuePlaceholder(op: string): string {
  if (op === 'BETWEEN') return '两值逗号分隔，如 100,200'
  if (op === 'IN' || op === 'NOT_IN') return '多值逗号分隔'
  return '值'
}

function syncFilterValues(f: SelFilter) {
  f.values = f.valueInput.split(',').map((v) => v.trim()).filter((v) => v.length > 0)
}

/* ============ 查询 ============ */
const result = ref<QueryResult | null>(null)
const generatedSql = ref('')

const hasQueryFields = computed(
  () => selectedDims.value.some((d) => d.datasetId === primaryDatasetId.value) || selectedMetrics.value.some((m) => m.datasetId === primaryDatasetId.value),
)

const previewItem = computed<VisualDashboardItem>(() => ({
  id: 'preview',
  title: title.value,
  chartType: chartType.value,
  dataConfig: JSON.stringify({
    datasetId: primaryDatasetId.value,
    dimensions: selectedDims.value.filter((d) => d.datasetId === primaryDatasetId.value).map((d) => ({ fieldCode: d.fieldCode, fieldName: d.fieldName })),
    metrics: selectedMetrics.value.filter((m) => m.datasetId === primaryDatasetId.value).map((m) => ({ fieldCode: m.fieldCode, fieldName: m.fieldName, aggType: m.aggType })),
    filters: filters.value.map((f) => ({ fieldCode: f.fieldCode, operator: f.operator, values: f.values })),
    limit: limit.value,
  }),
  styleConfig: JSON.stringify(styleCfg),
}))

async function runQuery(manual = false) {
  if (!primaryDatasetId.value) {
    if (manual) ElMessage.warning('请先从左侧资产池选择字段')
    return
  }
  if (!hasQueryFields.value) {
    if (manual) ElMessage.warning('请至少选择一个维度或指标')
    return
  }
  loading.value = true
  const start = performance.now()
  try {
    const { queryDatasetChart } = await import('@/api/dataset')
    const dims = selectedDims.value.filter((d) => d.datasetId === primaryDatasetId.value)
    const mets = selectedMetrics.value.filter((m) => m.datasetId === primaryDatasetId.value)
    const fts = filters.value.filter((f) => f.datasetId === primaryDatasetId.value && f.values.length > 0)
    const r = await queryDatasetChart({
      datasetId: primaryDatasetId.value,
      dimensions: dims.map((d) => ({ fieldCode: d.fieldCode, sort: d.sort || undefined })),
      metrics: mets.map((m) => ({ fieldCode: m.fieldCode, aggType: m.aggType })),
      filters: fts.map((f) => ({ fieldCode: f.fieldCode, operator: f.operator, values: f.values })),
      sorts: dims.filter((d) => d.sort).map((d) => ({ fieldCode: d.fieldCode, direction: d.sort })),
      limit: limit.value,
    })
    result.value = r.result
    generatedSql.value = r.sql
    costMs.value = Math.round(performance.now() - start)
    if (conflictFields.value.length) {
      ElMessage.warning(`已忽略 ${conflictFields.value.length} 个跨数据集冲突字段`)
    }
  } catch (e: any) {
    result.value = null
    if (manual) ElMessage.error(e?.message || '查询失败')
  } finally {
    loading.value = false
  }
}

/* 选择变化自动查询（去抖 800ms） */
let autoTimer: ReturnType<typeof setTimeout> | null = null
watch(
  () => [
    selectedDims.value.map((d) => `${d.datasetId}:${d.fieldCode}:${d.sort}`).join(','),
    selectedMetrics.value.map((m) => `${m.datasetId}:${m.fieldCode}:${m.aggType}`).join(','),
    filters.value.map((f) => `${f.datasetId}:${f.fieldCode}:${f.operator}:${f.values.join('|')}`).join(','),
    limit.value,
  ],
  () => {
    if (!autoQuery.value) return
    if (autoTimer) clearTimeout(autoTimer)
    autoTimer = setTimeout(() => runQuery(false), 800)
  },
)

/* ============ 样式配置 ============ */
interface BuilderStyleCfg {
  title: { show: boolean; text: string; fontSize: number; align: string }
  legend: { show: boolean; position: string }
  colorTheme: string
  labelShow: boolean
  decimalDigits: number
}

const styleCfg = reactive<BuilderStyleCfg>({
  title: { show: true, text: '', fontSize: 15, align: 'left' },
  legend: { show: true, position: 'top' },
  colorTheme: 'default',
  labelShow: false,
  decimalDigits: 2,
})

/* ============ 保存 ============ */
async function saveAndBack() {
  if (!primaryDatasetId.value) {
    ElMessage.warning('请先配置图表数据')
    return
  }
  if (!generatedSql.value) {
    await runQuery(true)
    if (!generatedSql.value) return
  }
  const ds = primaryDataset.value
  if (!ds?.datasourceId) {
    ElMessage.error('数据集未关联数据源，无法保存')
    return
  }
  saving.value = true
  try {
    const payload: VisualDashboardItem = {
      id: itemId.value === 'new' ? undefined : itemId.value,
      dashboardId: dashboardId.value,
      boardId: (route.query.boardId as string) || existingItem.value?.boardId,
      title: title.value,
      chartType: chartType.value,
      datasourceId: ds.datasourceId,
      querySql: generatedSql.value,
      dataConfig: JSON.stringify({
        datasetId: primaryDatasetId.value,
        datasetType: ds.datasetType,
        dimensions: selectedDims.value.filter((d) => d.datasetId === primaryDatasetId.value).map((d) => ({ fieldCode: d.fieldCode, fieldName: d.fieldName, sort: d.sort })),
        metrics: selectedMetrics.value.filter((m) => m.datasetId === primaryDatasetId.value).map((m) => ({ fieldCode: m.fieldCode, fieldName: m.fieldName, aggType: m.aggType })),
        filters: filters.value.filter((f) => f.datasetId === primaryDatasetId.value).map((f) => ({ fieldCode: f.fieldCode, operator: f.operator, values: f.values })),
        limit: limit.value,
      }),
      styleConfig: JSON.stringify(styleCfg),
      posX: existingItem.value?.posX ?? 60,
      posY: existingItem.value?.posY ?? 60,
      width: existingItem.value?.width ?? 420,
      height: existingItem.value?.height ?? 300,
      zIndex: existingItem.value?.zIndex ?? 1,
    }
    await saveItem(payload)
    ElMessage.success('图表已保存')
    router.push(`/visual/dashboard/edit/${dashboardId.value}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push(`/visual/dashboard/edit/${dashboardId.value}`)
}

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
}

/* ============ 加载已有组件配置 ============ */
const existingItem = ref<VisualDashboardItem | null>(null)

async function init() {
  try {
    datasets.value = (await listPublishedDatasets()) || []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载数据集失败')
    return
  }
  if (itemId.value === 'new') {
    title.value = '未命名图表'
    // 恢复编辑器暂存的新组件草稿（标题/图表类型/位置尺寸/样式）
    try {
      const raw = sessionStorage.getItem(`cb_draft_${dashboardId.value}`)
      if (raw) {
        const draft = JSON.parse(raw)
        if (draft.title) title.value = draft.title
        if (draft.chartType) chartType.value = draft.chartType as ChartType
        existingItem.value = {
          boardId: draft.boardId,
          posX: draft.posX ?? 60,
          posY: draft.posY ?? 60,
          width: draft.width ?? 420,
          height: draft.height ?? 300,
          styleConfig: draft.styleConfig,
        } as VisualDashboardItem
        if (draft.styleConfig) {
          try {
            const sc = JSON.parse(draft.styleConfig)
            Object.assign(styleCfg, sc)
            styleCfg.title = { show: true, fontSize: 15, align: 'left', ...(sc.title || {}) }
            styleCfg.legend = { show: true, position: 'top', ...(sc.legend || {}) }
          } catch { /* ignore */ }
        }
        sessionStorage.removeItem(`cb_draft_${dashboardId.value}`)
      }
    } catch { /* ignore */ }
    if (datasets.value.length) expandedDatasetId.value = datasets.value[0].id || ''
    return
  }
  // 编辑已有组件：恢复配置
  try {
    const items = (await listDashboardItems(dashboardId.value)) || []
    const item = items.find((i) => i.id === itemId.value)
    if (!item) {
      ElMessage.warning('组件不存在，将创建新图表')
      itemId.value = 'new'
      return
    }
    existingItem.value = item
    title.value = item.title || '未命名图表'
    chartType.value = (item.chartType as ChartType) || 'BAR'
    if (item.styleConfig) {
      try {
        const sc = JSON.parse(item.styleConfig)
        Object.assign(styleCfg, sc)
        styleCfg.title = { show: true, fontSize: 15, align: 'left', ...(sc.title || {}) }
        styleCfg.legend = { show: true, position: 'top', ...(sc.legend || {}) }
      } catch { /* ignore */ }
    }
    if (item.dataConfig) {
      try {
        const dc = JSON.parse(item.dataConfig)
        limit.value = dc.limit || 1000
        const dsId: string = dc.datasetId || ''
        const ds = datasets.value.find((d) => d.id === dsId)
        if (ds) expandedDatasetId.value = dsId
        const toSel = (code: string, name?: string): SelField | null => {
          const f = ds?.fields?.find((x) => x.fieldCode === code)
          return {
            fieldCode: code,
            fieldName: name || f?.fieldName || code,
            datasetId: dsId,
            datasetName: ds?.name || '',
            datasourceType: ds?.datasourceType,
            dataType: f?.dataType,
          }
        }
        selectedDims.value = (dc.dimensions || [])
          .map((d: any) => ({ ...(toSel(d.fieldCode, d.fieldName) || {} as SelField), sort: d.sort || '' }))
          .filter((d: SelDim) => !!d.fieldCode)
        selectedMetrics.value = (dc.metrics || [])
          .map((m: any) => ({ ...(toSel(m.fieldCode, m.fieldName) || {} as SelField), aggType: m.aggType || 'SUM' }))
          .filter((m: SelMetric) => !!m.fieldCode)
        filters.value = (dc.filters || [])
          .map((f: any) => ({
            ...(toSel(f.fieldCode) || {} as SelField),
            operator: f.operator || 'EQ',
            values: f.values || [],
            valueInput: (f.values || []).join(','),
            key: `f_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
          }))
          .filter((f: SelFilter) => !!f.fieldCode)
      } catch { /* ignore */ }
    }
    if (hasQueryFields.value) runQuery(false)
  } catch (e: any) {
    ElMessage.error(e?.message || '加载组件配置失败')
  }
}

onMounted(init)
onBeforeUnmount(() => {
  if (autoTimer) clearTimeout(autoTimer)
})
</script>

<style scoped>
.bi-chart-builder {
  --bg: #f5f7fa;
  --card-bg: #ffffff;
  --card-border: #e4e7ed;
  --text-1: #303133;
  --text-2: #606266;
  --text-3: #909399;
  --header-bg: #ffffff;
  --aside-bg: #fafbfc;
  --shelf-bg: #ffffff;
  --accent: #4f9df9;
  --dim-color: #4f9df9;
  --metric-color: #13c2c2;
  --filter-color: #9254de;
  --danger: #f56c6c;
  --warning: #e6a23c;

  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  color: var(--text-1);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  overflow: hidden;
}

.bi-chart-builder.theme-dark {
  --bg: #0f1117;
  --card-bg: #1a1d27;
  --card-border: #2a2f3a;
  --text-1: #e5eaf3;
  --text-2: #a3abb9;
  --text-3: #6b7280;
  --header-bg: #1a1d27;
  --aside-bg: #151821;
  --shelf-bg: #1a1d27;
}

/* ============ 顶部栏 ============ */
.cb-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 12px;
  background: var(--header-bg);
  border-bottom: 1px solid var(--card-border);
  flex-shrink: 0;
}

.cb-header-left { display: flex; align-items: center; gap: 10px; min-width: 0; }
.cb-logo { color: var(--accent); }
.cb-title-input { width: 220px; }
.cb-title-input :deep(.el-input__wrapper) { background: transparent; box-shadow: none; font-weight: 600; font-size: 14px; }
.cb-ds-tag { display: inline-flex; align-items: center; gap: 4px; max-width: 260px; }
.cb-header-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }

/* ============ 冲突警示条 ============ */
.cb-conflict-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: rgba(230, 162, 60, 0.12);
  color: var(--warning);
  border-bottom: 1px solid rgba(230, 162, 60, 0.3);
  font-size: 13px;
  flex-shrink: 0;
}
.cb-conflict-bar b { color: var(--danger); }
.cb-conflict-bar .el-button { margin-left: auto; }

/* ============ 货架区 ============ */
.cb-shelf {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 12px;
  background: var(--shelf-bg);
  border-bottom: 1px solid var(--card-border);
  flex-shrink: 0;
}

.cb-shelf-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-height: 34px;
  border-radius: 6px;
  padding: 2px 4px;
  transition: background 0.15s, box-shadow 0.15s;
}
.cb-shelf-row.drop-active {
  background: rgba(79, 157, 249, 0.08);
  box-shadow: inset 0 0 0 1.5px var(--accent);
}

.cb-shelf-label {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 76px;
  height: 28px;
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
  border-radius: 4px;
  padding: 0 8px;
}
.cb-shelf-label.dimension { color: var(--dim-color); background: rgba(79, 157, 249, 0.1); }
.cb-shelf-label.metric { color: var(--metric-color); background: rgba(19, 194, 194, 0.1); }
.cb-shelf-label.filter { color: var(--filter-color); background: rgba(146, 84, 222, 0.1); }

.cb-shelf-body {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  flex: 1;
  min-height: 28px;
}

.cb-shelf-empty {
  font-size: 12px;
  color: var(--text-3);
  padding: 4px 10px;
  border: 1px dashed var(--card-border);
  border-radius: 4px;
}
.cb-shelf-empty.hover { border-color: var(--accent); color: var(--accent); }

/* 字段 chip */
.cb-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 28px;
  padding: 0 8px;
  border-radius: 4px;
  font-size: 12.5px;
  cursor: default;
  border: 1px solid transparent;
  transition: all 0.15s;
}
.cb-chip.dimension { background: rgba(79, 157, 249, 0.12); color: var(--dim-color); }
.cb-chip.metric { background: rgba(19, 194, 194, 0.12); color: var(--metric-color); }
.cb-chip.filter { background: rgba(146, 84, 222, 0.12); color: var(--filter-color); }
.cb-chip.conflict {
  border-color: var(--danger);
  background: rgba(245, 108, 108, 0.14);
  color: var(--danger);
  animation: cb-pulse 1.6s infinite;
}
@keyframes cb-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.35); }
  50% { box-shadow: 0 0 0 4px rgba(245, 108, 108, 0); }
}

.cb-chip-icon { font-size: 12px; }
.cb-chip-name { font-weight: 500; white-space: nowrap; }
.cb-chip-warn { color: var(--danger); font-size: 13px; }
.cb-chip-remove { cursor: pointer; font-size: 13px; opacity: 0.55; }
.cb-chip-remove:hover { opacity: 1; color: var(--danger); }

.cb-dim-sort { width: 68px; }
.cb-metric-agg { width: 92px; }
.cb-filter-op { width: 78px; }
.cb-filter-value { width: 170px; }
.cb-chip .el-select :deep(.el-input__wrapper),
.cb-chip .el-select :deep(.el-input__inner) { background: transparent; }

/* ============ 主体 ============ */
.cb-main { display: flex; flex: 1; min-height: 0; }

/* 左侧资产池 */
.cb-aside {
  width: 262px;
  flex-shrink: 0;
  background: var(--aside-bg);
  border-right: 1px solid var(--card-border);
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.cb-aside-title {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-2);
  flex-shrink: 0;
}

.cb-search { padding: 0 12px 8px; flex-shrink: 0; }

.cb-dataset-list { flex: 1; overflow-y: auto; padding: 0 8px 12px; }

.cb-dataset-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 8px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-2);
  transition: background 0.12s;
}
.cb-dataset-item:hover { background: var(--card-border); }
.cb-dataset-item.active { background: rgba(79, 157, 249, 0.12); color: var(--accent); }
.cb-dataset-item.primary .cb-ds-name { color: var(--accent); font-weight: 600; }

.cb-ds-expand { font-size: 12px; color: var(--text-3); transition: transform 0.15s; }
.cb-ds-expand.open { transform: rotate(90deg); }
.cb-ds-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.cb-ds-type {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 3px;
  letter-spacing: 0.4px;
  flex-shrink: 0;
}
.cb-ds-type.doris { background: #ff7a4522; color: #ff7a45; }
.cb-ds-type.hive { background: #e6a23c22; color: #e6a23c; }
.cb-ds-type.mysql { background: #4f9df922; color: #4f9df9; }
.cb-ds-type.other { background: #90939922; color: #909399; }

.cb-ds-warn { color: var(--warning); font-size: 13px; flex-shrink: 0; }
.cb-ds-primary { color: var(--accent); font-size: 13px; flex-shrink: 0; }

/* 字段池 */
.cb-field-pool {
  padding: 2px 4px 8px 22px;
  animation: cb-slide-in 0.18s ease;
}
@keyframes cb-slide-in { from { opacity: 0; transform: translateY(-4px); } to { opacity: 1; transform: none; } }

.cb-pool-warn {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 4px 0 8px;
  padding: 6px 8px;
  border-radius: 4px;
  background: rgba(245, 108, 108, 0.1);
  border: 1px solid rgba(245, 108, 108, 0.35);
  color: var(--danger);
  font-size: 12px;
  line-height: 1.5;
}

.cb-pool-group { margin-bottom: 6px; }
.cb-pool-group-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-3);
  padding: 6px 4px 4px;
}
.cb-pool-count {
  background: var(--card-border);
  border-radius: 8px;
  padding: 0 6px;
  font-size: 10px;
}

.cb-field-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  border-radius: 4px;
  font-size: 12.5px;
  cursor: pointer;
  color: var(--text-2);
  transition: background 0.12s, transform 0.12s;
}
.cb-field-item:hover { background: var(--card-border); transform: translateX(2px); }
.cb-field-item.dimension:hover { color: var(--dim-color); }
.cb-field-item.metric:hover { color: var(--metric-color); }
.cb-field-item.dimension .el-icon { color: var(--dim-color); font-size: 12px; }
.cb-field-item.metric .el-icon { color: var(--metric-color); font-size: 12px; }

.cb-field-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cb-field-code { font-size: 10.5px; color: var(--text-3); font-family: 'SF Mono', Menlo, monospace; }
.cb-field-op { font-size: 12px; color: var(--filter-color); opacity: 0; flex-shrink: 0; }
.cb-field-item:hover .cb-field-op { opacity: 1; }

.cb-pool-empty { padding: 8px; font-size: 12px; color: var(--text-3); text-align: center; }

/* ============ 中间画布 ============ */
.cb-canvas {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.cb-chart-types {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  background: var(--shelf-bg);
  border-bottom: 1px solid var(--card-border);
  overflow-x: auto;
  flex-shrink: 0;
}

.cb-chart-type-item {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 10px;
  border-radius: 5px;
  font-size: 12.5px;
  color: var(--text-2);
  cursor: pointer;
  white-space: nowrap;
  border: 1px solid transparent;
  transition: all 0.15s;
}
.cb-chart-type-item:hover { background: var(--card-border); }
.cb-chart-type-item.active {
  color: var(--accent);
  background: rgba(79, 157, 249, 0.12);
  border-color: rgba(79, 157, 249, 0.45);
}

.cb-sql-box {
  margin: 8px 12px 0;
  border: 1px solid var(--card-border);
  border-radius: 6px;
  background: var(--card-bg);
  overflow: hidden;
  flex-shrink: 0;
}
.cb-sql-title {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-2);
  border-bottom: 1px solid var(--card-border);
}
.cb-sql-meta { margin-left: auto; color: var(--text-3); font-weight: 400; }
.cb-sql-text {
  margin: 0;
  padding: 10px;
  font-family: 'SF Mono', Menlo, monospace;
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--text-2);
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 160px;
  overflow-y: auto;
}

.cb-preview {
  flex: 1;
  margin: 10px 12px 12px;
  border: 1px solid var(--card-border);
  border-radius: 8px;
  background: var(--card-bg);
  min-height: 0;
  overflow: hidden;
  position: relative;
  isolation: isolate;
}

.cb-preview-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-3);
  font-size: 13px;
}
.cb-preview-empty .el-icon { color: var(--card-border); }
.cb-preview-hint { font-size: 12px; opacity: 0.8; }

/* ============ 右侧样式面板 ============ */
.cb-style {
  width: 252px;
  flex-shrink: 0;
  background: var(--aside-bg);
  border-left: 1px solid var(--card-border);
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.cb-style-body { flex: 1; overflow-y: auto; padding: 4px 12px 16px; }

.cb-style-group { margin-bottom: 14px; }
.cb-style-group-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-3);
  padding: 8px 0 6px;
  border-bottom: 1px solid var(--card-border);
  margin-bottom: 6px;
}
.cb-style-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 0;
  font-size: 12.5px;
  color: var(--text-2);
}

/* 滚动条 */
.cb-dataset-list::-webkit-scrollbar,
.cb-style-body::-webkit-scrollbar,
.cb-sql-text::-webkit-scrollbar,
.cb-chart-types::-webkit-scrollbar { width: 5px; height: 5px; }
.cb-dataset-list::-webkit-scrollbar-thumb,
.cb-style-body::-webkit-scrollbar-thumb,
.cb-sql-text::-webkit-scrollbar-thumb,
.cb-chart-types::-webkit-scrollbar-thumb { background: var(--card-border); border-radius: 3px; }
</style>
