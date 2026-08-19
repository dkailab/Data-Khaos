<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="isEdit ? '编辑数据集' : '新建数据集'"
    width="900px"
    top="5vh"
    destroy-on-close
  >
    <el-steps :active="step" finish-status="success" simple style="margin-bottom: 20px">
      <el-step title="基本信息" />
      <el-step title="数据源配置" />
      <el-step title="字段定义" />
    </el-steps>

    <!-- Step 1: 基本信息 -->
    <div v-show="step === 0">
      <el-form :model="form" label-width="100px">
        <el-form-item label="数据集名称" required>
          <el-input v-model="form.name" placeholder="请输入数据集名称" />
        </el-form-item>
        <el-form-item label="数据集编码">
          <el-input v-model="form.code" placeholder="英文编码（可选）" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="数据集描述" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-radio-group v-model="form.datasetType">
            <el-radio-button value="SQL">SQL查询</el-radio-button>
            <el-radio-button value="MODEL">数据模型</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="可见范围">
          <el-radio-group v-model="form.visibility">
            <el-radio value="PRIVATE">私有</el-radio>
            <el-radio value="ORG">组织内</el-radio>
            <el-radio value="PUBLIC">公开</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </div>

    <!-- Step 2: 数据源配置 -->
    <div v-show="step === 1">
      <!-- SQL模式 -->
      <template v-if="form.datasetType === 'SQL'">
        <el-form :model="form" label-width="100px">
          <el-form-item label="数据源" required>
            <el-select v-model="form.datasourceId" filterable style="width: 100%" placeholder="选择数据源">
              <el-option v-for="ds in datasources" :key="ds.id" :label="ds.dsName" :value="ds.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="查询SQL" required>
            <el-input v-model="form.querySql" type="textarea" :rows="8" placeholder="SELECT ..." />
          </el-form-item>
        </el-form>
        <div class="step-actions">
          <el-button :loading="testing" @click="testQuery">测试查询</el-button>
          <span v-if="previewResult" class="preview-info">返回 {{ previewResult.rows?.length || 0 }} 行数据</span>
        </div>
        <!-- 预览表格 -->
        <div v-if="previewResult" class="preview-table">
          <el-table :data="(previewResult.rows || []).slice(0, 10)" border size="small" max-height="200">
            <el-table-column v-for="c in (previewResult.columns || [])" :key="c" :prop="c" :label="c" min-width="100" show-overflow-tooltip />
          </el-table>
        </div>
      </template>

      <!-- MODEL模式 -->
      <template v-else>
        <el-form :model="form" label-width="100px">
          <el-form-item label="数据模型" required>
            <el-select v-model="form.modelId" filterable style="width: 100%" placeholder="选择数据集市模型">
              <el-option v-for="m in models" :key="m.id" :label="m.modelName" :value="m.id" />
            </el-select>
          </el-form-item>
        </el-form>
        <el-alert type="info" :closable="false" show-icon>
          选择模型后，系统将自动提取其关联的指标和维度作为字段定义
        </el-alert>
      </template>
    </div>

    <!-- Step 3: 字段定义 -->
    <div v-show="step === 2">
      <div class="fields-toolbar">
        <el-button :icon="Plus" size="small" @click="addField">添加字段</el-button>
        <el-button v-if="form.datasetType === 'SQL'" :icon="Refresh" size="small" @click="autoExtractFields">自动识别</el-button>
      </div>
      <el-table :data="form.fields" border size="small" max-height="360">
        <el-table-column label="字段名" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.fieldName" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="编码" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.fieldCode" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-select v-model="row.fieldType" size="small">
              <el-option label="维度" value="DIMENSION" />
              <el-option label="指标" value="METRIC" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="数据类型" width="120">
          <template #default="{ row }">
            <el-select v-model="row.dataType" size="small">
              <el-option label="文本" value="STRING" />
              <el-option label="整数" value="INTEGER" />
              <el-option label="小数" value="DECIMAL" />
              <el-option label="日期" value="DATE" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="汇总方式" width="120">
          <template #default="{ row }">
            <el-select v-model="row.aggType" size="small" :disabled="row.fieldType !== 'METRIC'">
              <el-option label="求和" value="SUM" />
              <el-option label="平均" value="AVG" />
              <el-option label="计数" value="COUNT" />
              <el-option label="去重计数" value="COUNT_DISTINCT" />
              <el-option label="最大" value="MAX" />
              <el-option label="最小" value="MIN" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="60" fixed="right">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="form.fields.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <el-button v-if="step > 0" @click="step--">上一步</el-button>
      <el-button v-if="step < 2" type="primary" @click="step++">下一步</el-button>
      <el-button v-if="step === 2" type="primary" :loading="saving" @click="save">保存</el-button>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { createDataset, getDataset, previewDataset, updateDataset } from '@/api/dataset'
import { pageDatasources } from '@/api/datasource'
import { pageMartModels } from '@/api/mart'
import type { DatasetField, MartModel, MetaDatasource, VisualDataset } from '@/types'

const props = defineProps<{
  visible: boolean
  dataset?: VisualDataset
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'saved'): void
}>()

const isEdit = computed(() => !!props.dataset?.id)
const step = ref(0)
const saving = ref(false)
const testing = ref(false)
const previewResult = ref<{ columns: string[]; rows: Record<string, any>[] } | null>(null)

const form = reactive<VisualDataset & { fields: DatasetField[] }>({
  name: '',
  code: '',
  description: '',
  datasetType: 'SQL',
  datasourceId: '',
  querySql: '',
  modelId: '',
  visibility: 'PRIVATE',
  refreshInterval: 0,
  fields: [],
})

const datasources = ref<MetaDatasource[]>([])
const models = ref<MartModel[]>([])

watch(() => props.visible, (val) => {
  if (val) {
    step.value = 0
    if (props.dataset) {
      Object.assign(form, props.dataset)
      if (!form.fields) form.fields = []
    } else {
      Object.assign(form, {
        name: '', code: '', description: '', datasetType: 'SQL',
        datasourceId: '', querySql: '', modelId: '', visibility: 'PRIVATE',
        refreshInterval: 0, fields: [],
      })
    }
    loadDatasources()
    loadModels()
  }
})

async function loadDatasources() {
  const r = await pageDatasources({ current: 1, size: 100 })
  datasources.value = r.records || []
}

async function loadModels() {
  const r = await pageMartModels({ current: 1, size: 100 })
  models.value = r.records || []
}

async function testQuery() {
  if (!form.datasourceId || !form.querySql) {
    ElMessage.warning('请先选择数据源并填写SQL')
    return
  }
  testing.value = true
  try {
    const r = await previewDataset(form.datasourceId, form.querySql)
    previewResult.value = r
    ElMessage.success(`查询成功，返回 ${r.rows?.length || 0} 行`)
  } finally {
    testing.value = false
  }
}

function autoExtractFields() {
  if (!previewResult.value?.columns) {
    ElMessage.warning('请先测试查询')
    return
  }
  form.fields = previewResult.value.columns.map((c, i) => ({
    id: `field_${Date.now()}_${i}`,
    fieldName: c,
    fieldCode: c,
    fieldType: i === 0 ? 'DIMENSION' : 'METRIC',
    dataType: 'STRING',
    aggType: i === 0 ? undefined : 'SUM',
    sortOrder: i,
  }))
  ElMessage.success(`已自动识别 ${form.fields.length} 个字段`)
}

function addField() {
  form.fields.push({
    id: `field_${Date.now()}`,
    fieldName: '',
    fieldCode: '',
    fieldType: 'DIMENSION',
    dataType: 'STRING',
    aggType: undefined,
    sortOrder: form.fields.length,
  })
}

async function save() {
  if (!form.name) {
    ElMessage.warning('请输入数据集名称')
    step.value = 0
    return
  }
  saving.value = true
  try {
    if (isEdit.value && form.id) {
      await updateDataset(form.id, form)
    } else {
      await createDataset(form)
    }
    ElMessage.success('保存成功')
    emit('update:visible', false)
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.step-actions { display: flex; align-items: center; gap: 12px; margin: 12px 0; }
.preview-info { font-size: 12px; color: #6b7280; }
.preview-table { margin-top: 12px; }
.fields-toolbar { margin-bottom: 12px; }
</style>
