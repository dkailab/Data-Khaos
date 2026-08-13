<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="任务">
          <el-select v-model="query.taskId" clearable placeholder="全部任务" style="width: 200px" filterable @change="handleSearch">
            <el-option v-for="t in taskOptions" :key="t.id" :label="t.taskName" :value="t.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="表名">
          <el-input v-model="query.tableName" placeholder="表名模糊搜索" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="taskName" label="任务" min-width="140" />
      <el-table-column label="稽核对象" min-width="180">
        <template #default="{ row }">
          {{ row.databaseName }}.{{ row.tableName }}
        </template>
      </el-table-column>
      <el-table-column label="评分" width="100">
        <template #default="{ row }">
          <el-tag :type="scoreType(row.score)">
            {{ row.score ?? '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="通过率" width="100">
        <template #default="{ row }">
          {{ row.ruleTotal ? Math.round((row.rulePass / row.ruleTotal) * 100) : 0 }}%
        </template>
      </el-table-column>
      <el-table-column label="规则" width="140">
        <template #default="{ row }">
          <span class="pass">{{ row.rulePass }}</span>
          <span class="sep">/</span>
          <span class="fail">{{ row.ruleFail }}</span>
          <span class="sep">/</span>
          <span>{{ row.ruleTotal }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="triggerType" label="触发" width="90">
        <template #default="{ row }">
          <el-tag :type="row.triggerType === 'SCHEDULE' ? 'warning' : 'primary'" size="small">
            {{ row.triggerType === 'SCHEDULE' ? '周期' : '手动' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="costMs" label="耗时" width="90">
        <template #default="{ row }">{{ row.costMs }}ms</template>
      </el-table-column>
      <el-table-column prop="createTime" label="执行时间" min-width="160" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="success" @click="handleExport(row)">导出</el-button>
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

    <el-dialog v-model="detailVisible" title="稽核报告详情" width="760px" destroy-on-close>
      <template v-if="current">
        <el-descriptions :column="3" border size="small" class="desc">
          <el-descriptions-item label="任务">{{ current.taskName }}</el-descriptions-item>
          <el-descriptions-item label="表">{{ current.snapshot.databaseName }}.{{ current.snapshot.tableName }}</el-descriptions-item>
          <el-descriptions-item label="评分">
            <el-tag :type="scoreType(current.snapshot.score)">{{ current.snapshot.score }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="规则数">{{ current.snapshot.ruleTotal }}</el-descriptions-item>
          <el-descriptions-item label="通过">{{ current.snapshot.rulePass }}</el-descriptions-item>
          <el-descriptions-item label="失败">{{ current.snapshot.ruleFail }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ current.snapshot.costMs }}ms</el-descriptions-item>
          <el-descriptions-item label="执行时间">{{ current.snapshot.createTime }}</el-descriptions-item>
        </el-descriptions>

        <el-table :data="current.results" border stripe class="mt">
          <el-table-column label="结果" width="80">
            <template #default="{ row }">
              <el-tag :type="row.passed === 1 ? 'success' : 'danger'" size="small">{{ row.passed === 1 ? '通过' : '失败' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="ruleId" label="规则ID" min-width="200" />
          <el-table-column prop="actualValue" label="实际值" width="110" />
          <el-table-column prop="threshold" label="阈值" width="90" />
          <el-table-column prop="message" label="说明" min-width="200" show-overflow-tooltip />
        </el-table>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { exportSnapshot, pageSnapshots, pageTasks, snapshotDetail } from '@/api/dquality'
import type { DqSnapshot, DqTask } from '@/types'

const loading = ref(false)
const list = ref<DqSnapshot[]>([])
const total = ref(0)
const query = reactive<Record<string, any>>({ current: 1, size: 10, taskId: undefined, tableName: '' })

const taskOptions = ref<DqTask[]>([])
const detailVisible = ref(false)
const current = ref<any>(null)

function scoreType(score?: number) {
  if (score == null) return 'info'
  if (score >= 90) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

async function load() {
  loading.value = true
  try {
    const data = await pageSnapshots({ ...query })
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
  query.taskId = undefined
  query.tableName = ''
  handleSearch()
}

async function loadTasks() {
  const data = await pageTasks({ current: 1, size: 100 })
  taskOptions.value = data.records || []
}

async function openDetail(row: DqSnapshot) {
  current.value = await snapshotDetail(row.id!)
  detailVisible.value = true
}

function handleExport(row: DqSnapshot) {
  exportSnapshot(row.id!).then((resp: any) => {
    const blob = (resp as any).data as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `quality_report_${row.tableName}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  })
}

onMounted(() => {
  loadTasks()
  load()
})
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
.pass {
  color: #67c23a;
}
.fail {
  color: #f56c6c;
}
.sep {
  margin: 0 2px;
  color: #c0c4cc;
}
.desc {
  margin-bottom: 12px;
}
.mt {
  margin-top: 12px;
}
</style>