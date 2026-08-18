<template>
  <div class="adhoc-wrap">
    <!-- 左侧：数据源 / 库表结构 / 我的模板 -->
    <div class="left-panel">
      <el-select v-model="datasourceId" placeholder="选择数据源" filterable size="small" @change="loadStructure">
        <el-option v-for="ds in datasources" :key="ds.id" :label="ds.dsName" :value="ds.id!" />
      </el-select>

      <el-scrollbar class="tree-box">
        <div class="block-title">
          库表结构
          <span class="tree-actions">
            <el-button link type="primary" size="small" :loading="treeLoading" @click="loadStructure">刷新</el-button>
            <el-button link type="warning" size="small" :loading="syncing" @click="syncMeta">同步元数据</el-button>
          </span>
        </div>
        <el-tree
          v-loading="treeLoading"
          :data="structure"
          :props="{ label: 'label', children: 'children' }"
          node-key="id"
          default-expand-all
          @node-click="onTreeClick"
        />
        <el-empty v-if="!treeLoading && !structure.length" description="暂无表，请先「同步元数据」" :image-size="40" />
      </el-scrollbar>

      <el-scrollbar class="saved-box">
        <div class="block-title">
          我的模板
          <el-button link type="primary" size="small" @click="loadSaved">刷新</el-button>
        </div>
        <div v-for="q in savedList" :key="q.id" class="saved-item" @click="loadSavedQuery(q)">
          <span class="saved-name">{{ q.name }}</span>
          <el-button link type="danger" size="small" @click.stop="removeSaved(q)">删</el-button>
        </div>
        <el-empty v-if="!savedList.length" description="暂无模板" :image-size="50" />
      </el-scrollbar>
    </div>

    <!-- 右侧：编辑器 + 结果 -->
    <div class="right-panel">
      <div class="toolbar">
        <el-button type="primary" size="small" :icon="CaretRight" :loading="executing" @click="run">执行</el-button>
        <el-button size="small" :icon="Collection" @click="openSave">保存</el-button>
        <el-button size="small" :icon="Download" :disabled="!result" @click="exportCsv">导出 CSV</el-button>
        <el-button size="small" :icon="Share" :disabled="!result" @click="openSaveAsItem">存为组件</el-button>
        <el-button size="small" :icon="Clock" @click="historyVisible = true">历史</el-button>
        <span v-if="result" class="meta">
          {{ displayResult?.rows.length }} 行 · 耗时 {{ result.result.costMs }} ms
          <el-tag v-if="result.truncated" type="warning" size="small">已截断(上限{{ maxRows }})</el-tag>
        </span>
      </div>

      <el-input
        ref="sqlInputRef"
        v-model="sqlText"
        type="textarea"
        :rows="10"
        placeholder="SELECT * FROM schema.table WHERE dt = '${dt}' LIMIT 100"
        class="sql-input"
      />

      <!-- 参数面板 -->
      <div v-if="paramKeys.length" class="params-panel">
        <span class="params-label">参数：</span>
        <template v-for="k in paramKeys" :key="k">
          <span class="param-key">{{ k }}</span>
          <el-input v-model="params[k]" size="small" :placeholder="k" class="param-input" />
        </template>
      </div>

      <!-- 图表配置 -->
      <div v-if="result" class="chart-config">
        <el-radio-group v-model="chartType" size="small">
          <el-radio-button v-for="t in chartTypes" :key="t" :value="t">{{ chartLabel(t) }}</el-radio-button>
        </el-radio-group>
        <template v-if="chartType !== 'TABLE' && !pivot">
          <el-select v-model="xCol" placeholder="维度列" size="small" class="ax">
            <el-option v-for="c in colNames" :key="c" :label="c" :value="c" />
          </el-select>
          <el-select v-model="valCol" placeholder="数值列" size="small" class="ax">
            <el-option v-for="c in colNames" :key="c" :label="c" :value="c" />
          </el-select>
          <el-select v-model="seriesCol" placeholder="系列(可选)" size="small" class="ax" clearable>
            <el-option v-for="c in colNames" :key="c" :label="c" :value="c" />
          </el-select>
          <el-button size="small" @click="autoSuggest">自动推荐</el-button>
        </template>
        <div class="adv">
          <el-button size="small" text type="primary" @click="openCalc">+ 计算字段</el-button>
          <el-button size="small" text type="primary" @click="openPivot">透视</el-button>
          <span v-if="calcFields.length" class="calc-tip">已加 {{ calcFields.length }} 个计算列</span>
          <span v-if="pivot" class="calc-tip">透视中（行:{{ pivot.rowDim }} 列:{{ pivot.colDim }}）</span>
        </div>
      </div>

      <!-- 结果 -->
      <div class="result-box">
        <ChartRenderer v-if="displayResult" :item="chartItem" :result="displayResult" :height="420" />
        <el-empty v-else description="执行查询后在此查看结果" />
      </div>
    </div>

    <!-- 保存模板对话框 -->
    <el-dialog v-model="saveVisible" title="保存为模板" width="460px">
      <el-form label-width="80px">
        <el-form-item label="名称" required><el-input v-model="saveForm.name" /></el-form-item>
        <el-form-item label="分组"><el-input v-model="saveForm.folder" placeholder="可选，用于归类" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveVisible = false">取消</el-button>
        <el-button type="primary" @click="doSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 存为组件对话框 -->
    <el-dialog v-model="itemVisible" title="存为仪表板组件" width="520px">
      <el-form label-width="90px">
        <el-form-item label="目标仪表板" required>
          <el-select v-model="itemForm.dashboardId" placeholder="选择仪表板" filterable>
            <el-option v-for="d in dashboards" :key="d.id" :label="d.name" :value="d.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="组件标题" required><el-input v-model="itemForm.title" /></el-form-item>
        <el-form-item label="图表类型">
          <el-select v-model="itemForm.chartType">
            <el-option v-for="t in chartTypes" :key="t" :label="chartLabel(t)" :value="t" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemVisible = false">取消</el-button>
        <el-button type="primary" @click="doSaveAsItem">保存</el-button>
      </template>
    </el-dialog>

    <!-- 计算字段对话框 -->
    <el-dialog v-model="calcVisible" title="添加计算字段" width="460px">
      <el-form label-width="80px">
        <el-form-item label="字段名" required><el-input v-model="calcForm.name" placeholder="如 profit" /></el-form-item>
        <el-form-item label="表达式" required>
          <el-input v-model="calcForm.expr" placeholder="如 amount * 0.1 或 price - cost" />
        </el-form-item>
        <div class="hint">支持列名与数字，运算符 + - * / 及括号，例如 <code>colA + colB</code></div>
      </el-form>
      <template #footer>
        <el-button @click="calcVisible = false">取消</el-button>
        <el-button type="primary" @click="addCalcField">添加</el-button>
      </template>
    </el-dialog>

    <!-- 透视对话框 -->
    <el-dialog v-model="pivotVisible" title="数据透视" width="460px">
      <el-form label-width="80px">
        <el-form-item label="行维度" required>
          <el-select v-model="pivotForm.rowDim"><el-option v-for="c in colNames" :key="c" :label="c" :value="c" /></el-select>
        </el-form-item>
        <el-form-item label="列维度" required>
          <el-select v-model="pivotForm.colDim"><el-option v-for="c in colNames" :key="c" :label="c" :value="c" /></el-select>
        </el-form-item>
        <el-form-item label="聚合数值" required>
          <el-select v-model="pivotForm.measure"><el-option v-for="c in numericCols" :key="c" :label="c" :value="c" /></el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pivotVisible = false">取消</el-button>
        <el-button type="primary" @click="applyPivot">透视</el-button>
      </template>
    </el-dialog>

    <!-- 历史抽屉 -->
    <el-drawer v-model="historyVisible" title="执行历史" size="42%">
      <el-table :data="historyList" size="small" border v-loading="historyLoading">
        <el-table-column label="SQL" min-width="200" show-overflow-tooltip>
          <template #default="{ row }"><el-link type="primary" @click="useHistory(row)">{{ row.sqlText }}</el-link></template>
        </el-table-column>
        <el-table-column label="状态" width="70">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '成功' : '失败' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时" width="80" />
        <el-table-column prop="createTime" label="时间" width="140" />
      </el-table>
      <el-pagination class="pager" small layout="prev,pager,next" v-model:current-page="historyQuery.current"
        :page-size="historyQuery.size" :total="historyTotal" @change="loadHistory" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CaretRight, Clock, Collection, Download, Share } from '@element-plus/icons-vue'
import type { MetaDatasource, QueryResult, VisualAdhocQuery, VisualDashboard } from '@/types'
import ChartRenderer from '@/components/chart/ChartRenderer.vue'
import {
  adhocHistory,
  adhocQuery,
  deleteAdhocQuery,
  exportAdhoc,
  listAdhocQueries,
  pageDashboards,
  saveAdhocAsItem,
  saveAdhocQuery,
} from '@/api/visual'
import { getStructure, syncMetadata } from '@/api/metadata'
import { pageDatasources as pageDs } from '@/api/datasource'

const chartTypes = ['TABLE', 'BAR', 'LINE', 'PIE', 'AREA', 'SCATTER', 'HEATMAP', 'TREEMAP', 'BOXPLOT', 'GAUGE', 'NUMBER'] as const
type ChartType = (typeof chartTypes)[number]
function chartLabel(t: string) {
  return { TABLE: '表格', BAR: '柱状', LINE: '折线', PIE: '饼图', AREA: '面积', SCATTER: '散点', HEATMAP: '热力', TREEMAP: '矩形树', BOXPLOT: '箱线', GAUGE: '仪表', NUMBER: '指标卡' }[t] || t
}

const maxRows = 10000
const datasources = ref<MetaDatasource[]>([])
const datasourceId = ref('')
const sqlText = ref('')
const sqlInputRef = ref<any>()

const params = reactive<Record<string, string>>({})
const executing = ref(false)
const result = ref<{ result: QueryResult; truncated: boolean; originalRowCount: number } | null>(null)
const baseResult = ref<QueryResult | null>(null)

// 图表配置
const chartType = ref<ChartType>('TABLE')
const xCol = ref('')
const valCol = ref('')
const seriesCol = ref('')

// 模板
const savedList = ref<VisualAdhocQuery[]>([])
const saveVisible = ref(false)
const saveForm = reactive({ name: '', folder: '' })

// 存为组件
const itemVisible = ref(false)
const dashboards = ref<VisualDashboard[]>([])
const itemForm = reactive({ dashboardId: '', title: '', chartType: 'TABLE' })

// 历史
const historyVisible = ref(false)
const historyList = ref<any[]>([])
const historyTotal = ref(0)
const historyLoading = ref(false)
const historyQuery = reactive({ current: 1, size: 10 })

// 计算字段 / 透视（P2）
const calcVisible = ref(false)
const calcForm = reactive({ name: '', expr: '' })
const calcFields = ref<{ name: string; expr: string }[]>([])
const pivotVisible = ref(false)
const pivotForm = reactive({ rowDim: '', colDim: '', measure: '' })
const pivot = ref<{ rowDim: string; colDim: string; measure: string } | null>(null)

const structure = ref<any[]>([])
const treeLoading = ref(false)
const syncing = ref(false)

/** 同步元数据后刷新库表结构 */
async function syncMeta() {
  if (!datasourceId.value) return ElMessage.warning('请选择数据源')
  syncing.value = true
  try {
    await syncMetadata(datasourceId.value)
    ElMessage.success('元数据同步完成')
    await loadStructure()
  } catch {
    /* 错误由全局拦截器提示 */
  } finally {
    syncing.value = false
  }
}

/* ============ 派生数据 ============ */
const paramKeys = computed(() => {
  const set = new Set<string>()
  const m = sqlText.value.matchAll(/\$\{([a-zA-Z0-9_]+)}/g)
  for (const x of m) set.add(x[1])
  return [...set]
})
const colNames = computed(() => (baseResult.value?.columns || []).map((c) => c.columnName).filter(Boolean) as string[])
const numericCols = computed(() => {
  const rows = baseResult.value?.rows || []
  return colNames.value.filter((c) => rows.some((r) => typeof r[c] === 'number' || (r[c] != null && !isNaN(Number(r[c])))))
})

/** 经过计算字段、透视处理后的最终结果（供图表渲染） */
const displayResult = computed<QueryResult | null>(() => {
  if (!baseResult.value) return null
  let cols = baseResult.value.columns.map((c) => ({ ...c }))
  let rows = baseResult.value.rows.map((r) => ({ ...r }))

  // 1) 计算字段
  for (const cf of calcFields.value) {
    if (cols.some((c) => c.columnName === cf.name)) continue
    cols.push({ columnName: cf.name } as any)
    for (const row of rows) row[cf.name] = evalExpr(cf.expr, row, colNames.value)
  }

  // 2) 透视
  if (pivot.value) {
    const { rowDim, colDim, measure } = pivot.value
    const colSet = new Map<string, string>()
    const agg = new Map<string, number>()
    for (const row of rows) {
      const rk = String(row[rowDim] ?? '')
      const ck = String(row[colDim] ?? '')
      const mv = Number(row[measure]) || 0
      const key = rk + '||' + ck
      colSet.set(ck, ck)
      agg.set(key, (agg.get(key) || 0) + mv)
    }
    const colKeys = [...colSet.keys()]
    cols = [{ columnName: rowDim } as any, ...colKeys.map((k) => ({ columnName: k } as any))]
    const byRow = new Map<string, any>()
    for (const row of rows) {
      const rk = String(row[rowDim] ?? '')
      if (!byRow.has(rk)) byRow.set(rk, { [rowDim]: rk })
      colKeys.forEach((ck) => {
        byRow.get(rk)[ck] = agg.get(rk + '||' + ck) || 0
      })
    }
    rows = [...byRow.values()]
  }
  return { columns: cols, rows, rowCount: rows.length, costMs: baseResult.value.costMs, update: false }
})

const chartItem = computed(() => ({
  chartType: chartType.value,
  config: JSON.stringify({ xAxisColumn: xCol.value, valueColumn: valCol.value, seriesColumn: seriesCol.value }),
}))

watch(baseResult, () => {
  // 结果变化时重置图表列选择并自动推荐
  calcFields.value = []
  pivot.value = null
  autoSuggest()
})

/* ============ 方法 ============ */
async function run() {
  if (!datasourceId.value) return ElMessage.warning('请选择数据源')
  if (!sqlText.value.trim()) return ElMessage.warning('请输入 SQL')
  executing.value = true
  try {
    const p: Record<string, any> = {}
    paramKeys.value.forEach((k) => (p[k] = params[k]))
    result.value = await adhocQuery({ datasourceId: datasourceId.value, sql: sqlText.value, params: p })
    baseResult.value = result.value.result
    ElMessage.success('查询成功')
  } catch {
    /* 错误已被全局拦截器提示 */
  } finally {
    executing.value = false
  }
}

function autoSuggest() {
  const cols = colNames.value
  if (!cols.length) return
  const nums = numericCols.value
  xCol.value = cols[0]
  valCol.value = nums[0] || cols[1] || ''
  seriesCol.value = ''
  // 默认一律以表格展示，图表由用户主动切换（不一定所有查询都适合柱状/饼图）
  chartType.value = 'TABLE'
}

async function onTreeClick(node: any) {
  // 点击表节点：一键生成并执行 SELECT * FROM 表 LIMIT 100
  if (node.type === 'table') {
    const qualified = node.database && node.database !== node.name ? `${node.database}.${node.name}` : node.name
    sqlText.value = `SELECT * FROM ${qualified} LIMIT 100`
    await run()
    return
  }
  // 其余节点：将名称插入 SQL 编辑器
  const name = node.name || node.label
  if (!name) return
  const input = sqlInputRef.value?.textarea
  if (input && typeof input.selectionStart === 'number') {
    const start = input.selectionStart
    const end = input.selectionEnd
    sqlText.value = sqlText.value.slice(0, start) + name + sqlText.value.slice(end)
    requestAnimationFrame(() => {
      input.focus()
      input.setSelectionRange(start + name.length, start + name.length)
    })
  } else {
    sqlText.value += (sqlText.value.endsWith(' ') || !sqlText.value ? '' : ' ') + name
  }
}

/* 模板 */
async function loadSaved() {
  const data = await listAdhocQueries({ current: 1, size: 100 })
  savedList.value = data.records || []
}
function loadSavedQuery(q: VisualAdhocQuery) {
  sqlText.value = q.sqlText || ''
  // 模板未绑定数据源（内置种子模板）时保留当前选择的数据源
  if (q.datasourceId) {
    datasourceId.value = q.datasourceId
    loadStructure()
  }
  if (q.params) Object.assign(params, q.params)
  ElMessage.info('已载入模板')
}
async function removeSaved(q: VisualAdhocQuery) {
  if (!q.id) return
  await deleteAdhocQuery(q.id)
  ElMessage.success('已删除')
  loadSaved()
}
function openSave() {
  if (!sqlText.value.trim()) return ElMessage.warning('无可保存的 SQL')
  saveForm.name = ''
  saveForm.folder = ''
  saveVisible.value = true
}
async function doSave() {
  if (!saveForm.name.trim()) return ElMessage.warning('请输入名称')
  await saveAdhocQuery({
    name: saveForm.name,
    folder: saveForm.folder,
    datasourceId: datasourceId.value,
    sql: sqlText.value,
    params: { ...params },
  })
  ElMessage.success('已保存')
  saveVisible.value = false
  loadSaved()
}

/* 存为组件 */
async function openSaveAsItem() {
  const data = await pageDashboards({ current: 1, size: 100 })
  dashboards.value = data.records || []
  itemForm.dashboardId = ''
  itemForm.title = '即席查询结果'
  itemForm.chartType = chartType.value
  itemVisible.value = true
}
async function doSaveAsItem() {
  if (!itemForm.dashboardId) return ElMessage.warning('请选择仪表板')
  if (!itemForm.title.trim()) return ElMessage.warning('请输入标题')
  await saveAdhocAsItem({
    dashboardId: itemForm.dashboardId,
    title: itemForm.title,
    chartType: itemForm.chartType,
    datasourceId: datasourceId.value,
    sql: sqlText.value,
    config: JSON.stringify({ xAxisColumn: xCol.value, valueColumn: valCol.value, seriesColumn: seriesCol.value }),
  })
  ElMessage.success('已存为仪表板组件')
  itemVisible.value = false
}

/* 导出 */
async function exportCsv() {
  await exportAdhoc({ datasourceId: datasourceId.value, sql: sqlText.value, params: { ...params } })
  ElMessage.success('已导出 CSV')
}

/* 历史 */
async function loadHistory() {
  historyLoading.value = true
  try {
    const data = await adhocHistory({ ...historyQuery })
    historyList.value = data.records || []
    historyTotal.value = Number(data.total)
  } finally {
    historyLoading.value = false
  }
}
function useHistory(row: any) {
  sqlText.value = row.sqlText || ''
  historyVisible.value = false
}

/* 计算字段 */
function openCalc() {
  calcForm.name = ''
  calcForm.expr = ''
  calcVisible.value = true
}
function addCalcField() {
  if (!calcForm.name.trim()) return ElMessage.warning('请输入字段名')
  if (!calcForm.expr.trim()) return ElMessage.warning('请输入表达式')
  try {
    evalExpr(calcForm.expr, baseResult.value?.rows[0] || {}, colNames.value)
  } catch (e: any) {
    return ElMessage.error('表达式有误：' + e.message)
  }
  calcFields.value.push({ name: calcForm.name.trim(), expr: calcForm.expr.trim() })
  calcVisible.value = false
  ElMessage.success('已添加计算列')
}

/* 透视 */
function openPivot() {
  pivotForm.rowDim = numericCols.value.length ? colNames.value.find((c) => !numericCols.value.includes(c)) || colNames.value[0] : colNames.value[0]
  pivotForm.colDim = colNames.value[1] || colNames.value[0]
  pivotForm.measure = numericCols.value[0] || ''
  pivotVisible.value = true
}
function applyPivot() {
  if (!pivotForm.rowDim || !pivotForm.colDim || !pivotForm.measure) return ElMessage.warning('请选择行/列/聚合列')
  pivot.value = { ...pivotForm }
  pivotVisible.value = false
}

/* 安全表达式求值：仅允许列名/数字与 + - * / () */
function evalExpr(expr: string, row: Record<string, any>, validCols: string[]): number | string {
  const tokens = tokenize(expr)
  const val = parseExpr(tokens, 0, row, validCols)
  return val.value
}

function tokenize(s: string) {
  const re = /\s*(\d+\.?\d*|[a-zA-Z_][\w$]*|[-+*/()]|'.*?'|".*?")\s*/g
  const out: { t: string; v: string }[] = []
  let m: RegExpExecArray | null
  while ((m = re.exec(s))) out.push({ t: m[1], v: m[1] })
  return out
}

function parseExpr(tokens: { t: string; v: string }[], i: number, row: Record<string, any>, cols: string[]): { value: any; next: number } {
  let { value: left, next: ni } = parseTerm(tokens, i, row, cols)
  while (ni < tokens.length && (tokens[ni].v === '+' || tokens[ni].v === '-')) {
    const op = tokens[ni].v
    const r = parseTerm(tokens, ni + 1, row, cols)
    left = op === '+' ? Number(left) + Number(r.value) : Number(left) - Number(r.value)
    ni = r.next
  }
  return { value: left, next: ni }
}
function parseTerm(tokens: { t: string; v: string }[], i: number, row: Record<string, any>, cols: string[]): { value: any; next: number } {
  let { value: left, next: ni } = parseFactor(tokens, i, row, cols)
  while (ni < tokens.length && (tokens[ni].v === '*' || tokens[ni].v === '/')) {
    const op = tokens[ni].v
    const r = parseFactor(tokens, ni + 1, row, cols)
    left = op === '*' ? Number(left) * Number(r.value) : Number(left) / Number(r.value)
    ni = r.next
  }
  return { value: left, next: ni }
}
function parseFactor(tokens: { t: string; v: string }[], i: number, row: Record<string, any>, cols: string[]): { value: any; next: number } {
  if (i >= tokens.length) throw new Error('表达式不完整')
  const tk = tokens[i]
  if (tk.v === '(') {
    const r = parseExpr(tokens, i + 1, row, cols)
    if (tokens[r.next]?.v !== ')') throw new Error('括号不匹配')
    return { value: r.value, next: r.next + 1 }
  }
  if (tk.v === '-' || tk.v === '+') {
    const r = parseFactor(tokens, i + 1, row, cols)
    return { value: tk.v === '-' ? -Number(r.value) : Number(r.value), next: r.next }
  }
  // 数字
  if (/^-?\d+\.?\d*$/.test(tk.v)) return { value: Number(tk.v), next: i + 1 }
  // 列名
  if (cols.includes(tk.v)) {
    const raw = row[tk.v]
    const n = Number(raw)
    if (raw != null && !isNaN(n)) return { value: n, next: i + 1 }
    return { value: raw, next: i + 1 }
  }
  throw new Error('未知标识符: ' + tk.v)
}

/* 初始化 */
async function loadStructure() {
  if (!datasourceId.value) return
  treeLoading.value = true
  try {
    const data = await getStructure(datasourceId.value)
    structure.value = (data || []).map((n: any) => normalizeNode(n))
  } finally {
    treeLoading.value = false
  }
}
/** 后端结构树: [{ database, tables:[{ table, columns:[...] }] }] → 前端 el-tree 数据集 */
function normalizeNode(n: any, dbName?: string): any {
  if (n.database) {
    const db = n.database
    return {
      id: db.id,
      type: 'database',
      label: db.databaseName,
      name: db.databaseName,
      children: (n.tables || []).map((t: any) => normalizeNode(t, db.databaseName)),
    }
  }
  if (n.table) {
    const t = n.table
    return {
      id: t.id,
      type: 'table',
      label: t.tableName,
      name: t.tableName,
      database: dbName,
      children: (n.columns || []).map((c: any) => normalizeNode(c, dbName)),
    }
  }
  // 字段节点：后端字段仅含 columnName/columnType，需映射为 name/label，否则下拉无内容
  if (n.columnName != null) {
    return {
      id: n.id,
      type: 'field',
      name: n.columnName,
      label: n.columnName, // 展开表节点即可见字段名
      columnType: n.columnType,
      database: dbName,
    }
  }
  // 扁平/兜底结构
  if (n.children) {
    return { id: n.id, type: 'node', label: n.name || n.label, name: n.name, children: n.children.map((c: any) => normalizeNode(c, dbName)) }
  }
  return { id: n.id, type: 'node', label: n.name || n.label, name: n.name, database: dbName }
}

onMounted(async () => {
  const ds = await pageDs({ current: 1, size: 100 })
  datasources.value = ds.records || []
  if (datasources.value.length) datasourceId.value = datasources.value[0].id!
  // 首次进入即自动加载选中数据源的库表结构，避免树为空
  loadStructure()
  loadSaved()
})
</script>

<style scoped>
.adhoc-wrap {
  display: flex;
  height: calc(100vh - 120px);
  gap: 12px;
}
.left-panel {
  width: 260px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border: 1px solid var(--card-border, #ebeef5);
  border-radius: 8px;
  padding: 10px;
  box-sizing: border-box;
}
.tree-box {
  flex: 1;
  overflow: auto;
  border-top: 1px solid var(--card-border, #ebeef5);
  padding-top: 6px;
}
.saved-box {
  height: 220px;
  overflow: auto;
  border-top: 1px solid var(--card-border, #ebeef5);
  padding-top: 6px;
}
.block-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-2, #606266);
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.tree-actions {
  display: inline-flex;
  gap: 4px;
  white-space: nowrap;
}
.saved-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 6px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}
.saved-item:hover {
  background: var(--hover-bg, #f5f7fa);
}
.saved-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.meta {
  font-size: 12px;
  color: var(--text-3, #909399);
  margin-left: auto;
}
.sql-input :deep(textarea) {
  font-family: 'JetBrains Mono', Consolas, 'Courier New', monospace;
}
.params-panel {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 13px;
}
.params-label {
  color: var(--text-3, #909399);
}
.param-key {
  font-weight: 600;
}
.param-input {
  width: 140px;
}
.chart-config {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.ax {
  width: 130px;
}
.adv {
  margin-left: auto;
}
.calc-tip {
  font-size: 12px;
  color: var(--text-3, #909399);
  margin-left: 8px;
}
.result-box {
  flex: 1;
  border: 1px solid var(--card-border, #ebeef5);
  border-radius: 8px;
  padding: 8px;
  min-height: 200px;
  overflow: hidden;
}
.pager {
  margin-top: 8px;
  justify-content: flex-end;
}
.hint {
  font-size: 12px;
  color: var(--text-3, #909399);
}
.hint code {
  background: var(--hover-bg, #f5f7fa);
  padding: 1px 4px;
  border-radius: 3px;
}
</style>
