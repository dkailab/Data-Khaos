<template>
  <el-card v-loading="loading" shadow="never">
    <div v-if="model" class="model-detail">
      <!-- 顶部面包屑 + 操作栏 -->
      <div class="detail-header">
        <div class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item><router-link to="/mart/market">模型市场</router-link></el-breadcrumb-item>
            <el-breadcrumb-item>{{ model.modelName }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-actions">
          <el-button v-if="canSubscribe" type="primary" :icon="Bell" @click="openSubscribe">申请订阅</el-button>
          <el-button :icon="View" @click="openPreview">数据预览</el-button>
        </div>
      </div>

      <!-- 基础信息卡片 -->
      <el-descriptions :column="2" border :label-width="120" title="基础信息">
        <el-descriptions-item label="模型名称">{{ model.modelName }}</el-descriptions-item>
        <el-descriptions-item label="模型编码">{{ model.modelCode }}</el-descriptions-item>
        <el-descriptions-item label="数仓分层">
          <el-tag :type="layerTagType(layerCode)">{{ layerName }} ({{ layerCode }})</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="模型类型">
          <el-tag>{{ model.modelType === 'SNOWFLAKE' ? '雪花模型' : '星型模型' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="数据源ID">{{ model.datasourceId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="版本">v{{ model.version }}</el-descriptions-item>
        <el-descriptions-item label="项目组ID">{{ model.projectGroupId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatTime(model.updateTime) }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ model.description || '暂无描述' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 统计卡片行 -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="sc-title">指标</div>
          <div class="sc-number">{{ metrics.length }}</div>
          <div class="sc-label">已定义指标</div>
        </div>
        <div class="stat-card">
          <div class="sc-title">维度</div>
          <div class="sc-number">{{ dimensions.length }}</div>
          <div class="sc-label">已定义维度</div>
        </div>
        <div class="stat-card">
          <div class="sc-title">关联</div>
          <div class="sc-number">{{ rels.length }}</div>
          <div class="sc-label">事实/维度关联</div>
        </div>
      </div>

      <!-- 指标列表 -->
      <el-divider content-position="left">指标列表</el-divider>
      <el-table :data="metrics" border stripe size="small" max-height="320">
        <el-table-column prop="metricName" label="指标名称" min-width="140" />
        <el-table-column prop="metricCode" label="指标编码" width="140" />
        <el-table-column prop="metricType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.metricType === 'DERIVED' ? '派生' : '原子' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dataType" label="数据类型" width="100" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="expression" label="计算表达式" min-width="180" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
      </el-table>

      <!-- 维度列表 -->
      <el-divider content-position="left">维度列表</el-divider>
      <el-table :data="dimensions" border stripe size="small" max-height="320">
        <el-table-column prop="dimName" label="维度名称" min-width="140" />
        <el-table-column prop="dimCode" label="维度编码" width="140" />
        <el-table-column prop="dimType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ dimTypeText(row.dimType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sourceTable" label="来源表" width="140" />
        <el-table-column prop="sourceColumn" label="来源字段" width="140" />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      </el-table>

      <!-- 关联关系 -->
      <el-divider content-position="left">关联关系</el-divider>
      <el-table :data="rels" border stripe size="small">
        <el-table-column prop="factTable" label="事实表" min-width="140" />
        <el-table-column prop="dimTable" label="维度表" min-width="140" />
        <el-table-column prop="joinKey" label="关联键" min-width="120" />
        <el-table-column prop="joinType" label="关联类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.joinType || 'INNER' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-empty v-else description="模型不存在或无权限访问" />

    <!-- 订阅申请对话框 -->
    <el-dialog v-model="subscribeVisible" title="申请订阅模型" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="applyForm" :rules="formRules" label-width="100px">
        <el-form-item label="申请类型" prop="applyType">
          <el-input v-model="applyForm.applyType" disabled>
            <template #prefix>MART</template>
          </el-input>
        </el-form-item>
        <el-form-item label="模型名称" prop="targetName">
          <el-input v-model="applyForm.targetName" disabled />
        </el-form-item>
        <el-form-item label="申请理由" prop="reason">
          <el-input v-model="applyForm.reason" type="textarea" :rows="3" placeholder="请说明为什么需要订阅这个模型" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="subscribeVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 数据预览对话框 -->
    <el-dialog v-model="previewVisible" title="事实表数据预览（前 100 行）" width="85%" destroy-on-close>
      <div v-if="previewResult && previewResult.columns" class="preview-wrap">
        <div class="preview-info">
          <el-tag type="info">共 {{ previewResult.rows?.length || 0 }} 行</el-tag>
          <el-tag v-if="previewResult.costMs" type="success" style="margin-left: 8px">耗时 {{ previewResult.costMs }} ms</el-tag>
        </div>
        <el-table :data="previewResult.rows || []" border stripe size="small" max-height="480">
          <el-table-column v-for="col in previewResult.columns" :key="col.columnName" :prop="col.columnName" :label="col.columnName" min-width="120" />
        </el-table>
      </div>
      <el-empty v-else description="无数据" />
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Bell, View } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { martModelDetail, previewMartModel, listMartLayers } from '@/api/mart'
import { submitApply } from '@/api/approval'
import type { ApplyRequest } from '@/types'
import type { MartModel, MartMetric, MartDimension, MartModelRel, MartWarehouseLayer, QueryResult } from '@/types'

const route = useRoute()
const userStore = useUserStore()
const modelId = route.params.id as string

const loading = ref(false)
const model = ref<MartModel | null>(null)
const metrics = ref<MartMetric[]>([])
const dimensions = ref<MartDimension[]>([])
const rels = ref<MartModelRel[]>([])
const layers = ref<MartWarehouseLayer[]>([])
const previewVisible = ref(false)
const previewResult = ref<QueryResult | null>(null)

// 订阅申请
const subscribeVisible = ref(false)
const submitting = ref(false)
const applyForm = reactive<ApplyRequest>({
  applyType: 'MART',
  targetId: '',
  targetName: '',
  reason: '',
})
const formRef = ref<FormInstance>()
const formRules: FormRules = {
  applyType: [{ required: true, message: '必选', trigger: 'change' }],
  targetName: [{ required: true, message: '必填', trigger: 'blur' }],
  reason: [{ required: true, message: '请填写申请理由', trigger: 'blur' }],
}

const layerName = computed(() => {
  return layers.value.find(l => l.id === model.value?.layerId)?.layerName || '-'
})

const layerCode = computed(() => {
  return layers.value.find(l => l.id === model.value?.layerId)?.layerCode || '-'
})

function layerTagType(code?: string): 'primary' | 'warning' | 'success' | 'info' {
  if (code === 'ODS') return 'info'
  if (code === 'DWD') return 'primary'
  if (code === 'DWS') return 'warning'
  return 'success'
}

function dimTypeText(t?: string) {
  return { COMMON: '普通', TIME: '时间', ORG: '组织' }[t ?? ''] ?? t ?? '-'
}

function formatTime(t?: string) {
  return t || '-'
}

const canSubscribe = computed(() => {
  const perms = userStore.permissions
  return perms.includes('approval:apply')
})

async function loadDetail() {
  loading.value = true
  try {
    const detail = await martModelDetail(modelId)
    model.value = detail.model as any
    metrics.value = detail.metrics || []
    dimensions.value = detail.dimensions || []
    rels.value = detail.rels || []
    // 预填订阅信息
    applyForm.targetId = modelId
    applyForm.targetName = model.value?.modelName || ''
  } catch {
    model.value = null
  } finally {
    loading.value = false
  }
}

function openSubscribe() {
  subscribeVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await submitApply({ ...applyForm })
    ElMessage.success('订阅申请已提交，请等待审批')
    subscribeVisible.value = false
  } finally {
    submitting.value = false
  }
}

async function openPreview() {
  previewVisible.value = true
  previewResult.value = null
  try {
    previewResult.value = await previewMartModel(modelId)
  } catch {
    previewResult.value = null
  }
}

onMounted(async () => {
  try {
    layers.value = await listMartLayers()
  } catch {
    layers.value = []
  }
  loadDetail()
})
</script>

<style scoped>
.model-detail {
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }
  .header-actions {
    display: flex;
    gap: 8px;
  }
  .stats-row {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
    margin: 20px 0;
  }
  .stat-card {
    padding: 16px 20px;
    border: 1px solid var(--el-border-color);
    border-radius: 8px;
    text-align: center;
    background: linear-gradient(135deg, var(--el-fill-color-lighter) 0%, var(--el-fill-color-lighter) 100%);
  }
  .sc-title {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
  .sc-number {
    margin-top: 4px;
    font-size: 32px;
    font-weight: 700;
    color: #165dff;
  }
  .sc-label {
    margin-top: 2px;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
}
.preview-wrap {
  .preview-info {
    margin-bottom: 12px;
  }
}
</style>