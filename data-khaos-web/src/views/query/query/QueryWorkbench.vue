<template>
  <el-row :gutter="16">
    <el-col :span="18">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <span>SQL 查询工作台</span>
            <el-button type="primary" size="small" :icon="CaretRight" :loading="executing" @click="runQuery">执行</el-button>
            <el-button size="small" :icon="Download" :disabled="!result || result.columns.length === 0" @click="exportCsv">
              导出 CSV
            </el-button>
          </div>
        </template>

        <el-form inline class="sql-toolbar">
          <el-form-item label="数据源">
            <el-select v-model="sqlForm.datasourceId" placeholder="请选择数据源" filterable style="width: 220px">
              <el-option v-for="ds in datasources" :key="ds.id" :label="ds.dsName" :value="ds.id!" />
            </el-select>
          </el-form-item>
          <el-form-item label="数据库">
            <el-input v-model="sqlForm.databaseName" placeholder="可选" style="width: 160px" />
          </el-form-item>
        </el-form>

        <el-input
          v-model="sqlText"
          type="textarea"
          :rows="10"
          placeholder="SELECT * FROM your_table LIMIT 100"
          class="sql-input"
        />

        <div v-if="executing" class="executing-tip">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在执行...</span>
        </div>
      </el-card>

      <el-card shadow="never" class="result-card">
        <template #header>
          <div class="card-header">
            <span>查询结果</span>
            <span v-if="result && result.columns.length" class="result-meta">
              共 {{ result.rows.length }} 行，耗时 {{ result.costMs }} ms
            </span>
          </div>
        </template>
        <el-table :data="result?.rows || []" size="small" border max-height="420" v-loading="executing">
          <el-table-column v-for="col in result?.columns || []" :key="col.columnName" :prop="col.columnName" :label="col.columnName" min-width="120" show-overflow-tooltip />
        </el-table>
        <el-empty v-if="!executing && result && result.columns.length === 0" description="无数据" />
      </el-card>
    </el-col>

    <el-col :span="6">
      <el-card shadow="never">
        <template #header>查询历史</template>
        <el-table v-loading="historyLoading" :data="historyList" size="small" border>
          <el-table-column prop="sqlText" label="SQL" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <el-link type="primary" @click="useHistory(row)">{{ row.sqlText }}</el-link>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="70">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="costMs" label="耗时(ms)" width="80" />
          <el-table-column prop="createTime" label="时间" width="130" />
        </el-table>
        <el-pagination
          class="pager"
          small
          v-model:current-page="historyQuery.current"
          v-model:page-size="historyQuery.size"
          :total="historyTotal"
          layout="prev, pager, next"
          @change="loadHistory"
        />
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CaretRight, Download, Loading } from '@element-plus/icons-vue'
import { executeQuery, exportQueryResult, queryHistory } from '@/api/query'
import { pageDatasources } from '@/api/datasource'
import type { MetaDatasource, QueryHistory, QueryResult } from '@/types'

const executing = ref(false)
const historyLoading = ref(false)
const datasources = ref<MetaDatasource[]>([])
const sqlText = ref('')
const sqlForm = reactive<{ datasourceId: string; databaseName: string }>({ datasourceId: '', databaseName: '' })
const result = ref<QueryResult | null>(null)

const historyList = ref<QueryHistory[]>([])
const historyTotal = ref(0)
const historyQuery = reactive({ current: 1, size: 10 })

async function loadDatasources() {
  const data = await pageDatasources({ current: 1, size: 100 })
  datasources.value = data.records || []
  if (datasources.value.length > 0 && !sqlForm.datasourceId) {
    sqlForm.datasourceId = datasources.value[0].id!
  }
}

async function runQuery() {
  if (!sqlForm.datasourceId) {
    ElMessage.warning('请选择数据源')
    return
  }
  if (!sqlText.value.trim()) {
    ElMessage.warning('请输入 SQL')
    return
  }
  executing.value = true
  try {
    result.value = await executeQuery({
      datasourceId: sqlForm.datasourceId,
      databaseName: sqlForm.databaseName || undefined,
      sql: sqlText.value,
    })
    loadHistory()
  } finally {
    executing.value = false
  }
}

async function exportCsv() {
  await exportQueryResult({
    datasourceId: sqlForm.datasourceId,
    databaseName: sqlForm.databaseName || undefined,
    sql: sqlText.value,
  })
  ElMessage.success('导出成功')
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const data = await queryHistory({ ...historyQuery })
    historyList.value = data.records
    historyTotal.value = Number(data.total)
  } finally {
    historyLoading.value = false
  }
}

function useHistory(row: QueryHistory) {
  sqlText.value = row.sqlText || ''
  sqlForm.datasourceId = row.datasourceId || ''
  sqlForm.databaseName = row.databaseName || ''
}

onMounted(() => {
  loadDatasources()
  loadHistory()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.sql-toolbar {
  margin-bottom: 8px;
}
.sql-input :deep(textarea) {
  font-family: 'JetBrains Mono', Consolas, 'Courier New', monospace;
}
.executing-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #409eff;
  margin-top: 8px;
  font-size: 13px;
}
.result-card {
  margin-top: 16px;
}
.result-meta {
  color: #909399;
  font-size: 12px;
}
.pager {
  margin-top: 8px;
  justify-content: flex-end;
}
</style>
