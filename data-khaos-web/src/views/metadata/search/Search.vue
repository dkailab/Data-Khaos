<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="关键字">
          <el-input v-model="keyword" placeholder="输入表名 / 表注释 / 字段名" clearable style="width: 280px" @keyup.enter="doSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" :loading="loading" @click="doSearch">搜索</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table v-loading="loading" :data="results" border stripe>
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.type === 'TABLE' ? 'primary' : 'success'">{{ row.type === 'TABLE' ? '表' : '字段' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="数据库" min-width="140">
        <template #default="{ row }">{{ row.databaseName || '-' }}</template>
      </el-table-column>
      <el-table-column label="表名" min-width="160">
        <template #default="{ row }">{{ row.table?.tableName || row.tableName || '-' }}</template>
      </el-table-column>
      <el-table-column label="字段名" min-width="140">
        <template #default="{ row }">{{ row.column?.columnName || '-' }}</template>
      </el-table-column>
      <el-table-column label="类型" min-width="110">
        <template #default="{ row }">{{ row.column?.columnType || row.table?.tableType || '-' }}</template>
      </el-table-column>
      <el-table-column label="描述" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.column?.description || row.table?.description || '-' }}</template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && results.length === 0" description="暂无结果，请输入关键字搜索" />
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { searchMetadata } from '@/api/metadata'

const keyword = ref('')
const loading = ref(false)
const results = ref<any[]>([])

async function doSearch() {
  if (!keyword.value.trim()) return
  loading.value = true
  try {
    results.value = (await searchMetadata(keyword.value.trim())) || []
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
</style>
