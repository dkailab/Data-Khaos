<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="数据源">
          <el-select v-model="selectedDsId" placeholder="请选择数据源" style="width: 240px" filterable @change="loadStructure">
            <el-option v-for="ds in datasources" :key="ds.id" :label="ds.dsName" :value="ds.id!" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" :loading="loading" :disabled="!selectedDsId" @click="loadStructure">
            加载结构
          </el-button>
          <el-button :icon="Refresh" :disabled="!selectedDsId" @click="syncAll">同步元数据</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-alert title="结构树：数据库 -> 表 -> 字段（来自元数据中心采集结果）" type="info" :closable="false" style="margin-bottom: 12px" />

    <div v-if="datasources.length === 0" class="empty">
      <el-empty description="请先到「数据源管理」中新增并同步数据源" />
    </div>

    <el-tree v-else v-loading="loading" :data="treeData" node-key="id" default-expand-all :props="{ label: 'label', children: 'children' }">
      <template #default="{ data }">
        <span class="tree-node">
          <el-icon v-if="data.nodeType === 'db'"><FolderOpened /></el-icon>
          <el-icon v-else-if="data.nodeType === 'table'"><Grid /></el-icon>
          <el-icon v-else><Minus /></el-icon>
          <span>{{ data.label }}</span>
          <el-tag v-if="data.nodeType === 'table'" size="small" type="info" class="tag">{{ data.rowCount }} 行</el-tag>
          <el-tag v-if="data.nodeType === 'column' && data.isPrimaryKey === 1" size="small" type="warning" class="tag">主键</el-tag>
        </span>
      </template>
    </el-tree>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getStructure, syncMetadata } from '@/api/metadata'
import { pageDatasources } from '@/api/datasource'
import type { MetaColumn, MetaDatabase, MetaTable } from '@/types'

interface StructTable {
  table: MetaTable
  columns: MetaColumn[]
}

interface StructDb {
  database: MetaDatabase
  tables: StructTable[]
}

interface TreeNode {
  id: string
  label: string
  nodeType: 'db' | 'table' | 'column'
  children?: TreeNode[]
  rowCount?: number
  isPrimaryKey?: number
}

const loading = ref(false)
const datasources = ref<any[]>([])
const selectedDsId = ref('')
const treeData = ref<TreeNode[]>([])

async function loadDatasources() {
  const data = await pageDatasources({ current: 1, size: 100 })
  datasources.value = data.records || []
  if (datasources.value.length > 0 && !selectedDsId.value) {
    selectedDsId.value = datasources.value[0].id
    loadStructure()
  }
}

async function loadStructure() {
  if (!selectedDsId.value) return
  loading.value = true
  try {
    const data = await getStructure(selectedDsId.value)
    treeData.value = (data || []).map((dbNode: StructDb) => ({
      id: `db-${dbNode.database.id}`,
      label: dbNode.database.databaseName ?? dbNode.database.id ?? '',
      nodeType: 'db',
      children: (dbNode.tables || []).map((t: StructTable) => ({
        id: `table-${t.table.id}`,
        label: t.table.tableName ?? t.table.id ?? '',
        nodeType: 'table',
        rowCount: t.table.rowCount,
        children: (t.columns || []).map((c: MetaColumn) => ({
          id: `column-${c.id}`,
          label: `${c.columnName} (${c.columnType || '-'})`,
          nodeType: 'column',
          isPrimaryKey: c.isPrimaryKey,
        })),
      })),
    }))
  } finally {
    loading.value = false
  }
}

async function syncAll() {
  await ElMessageBox.confirm('确认全量同步该数据源的元数据吗？', '提示', { type: 'warning' })
  await syncMetadata(selectedDsId.value)
  ElMessage.success('同步完成，正在刷新结构...')
  loadStructure()
}

onMounted(loadDatasources)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
.empty {
  padding: 40px 0;
}
.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}
.tree-node .tag {
  margin-left: 6px;
}
</style>
