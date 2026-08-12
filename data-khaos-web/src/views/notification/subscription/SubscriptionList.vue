<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-alert
        title="订阅管理：当前用户为 {{ userStore.userInfo?.id || userStore.userInfo?.username || '-' }} 的订阅"
        type="info"
        :closable="false"
        class="alert"
      />
      <el-button type="primary" :icon="Plus" @click="openCreate">新增订阅</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="userId" label="用户ID" min-width="150" />
      <el-table-column label="订阅类型" width="120">
        <template #default="{ row }">
          <el-tag type="primary">{{ subscribeTypeText(row.subscribeType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="targetId" label="目标ID" min-width="160" />
      <el-table-column label="渠道" width="110">
        <template #default="{ row }">
          <el-tag>{{ channelText(row.channel) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click="handleDelete(row)">取消订阅</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新增订阅" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="订阅类型" prop="subscribeType">
          <el-select v-model="form.subscribeType" style="width: 100%">
            <el-option label="报表" value="REPORT" />
            <el-option label="指标" value="METRIC" />
            <el-option label="任务" value="JOB" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标ID" prop="targetId">
          <el-input v-model="form.targetId" placeholder="目标对象 ID" />
        </el-form-item>
        <el-form-item label="渠道" prop="channel">
          <el-select v-model="form.channel" style="width: 100%">
            <el-option label="邮件 MAIL" value="MAIL" />
            <el-option label="站内 SITE" value="SITE" />
            <el-option label="微信 WECHAT" value="WECHAT" />
            <el-option label="短信 SMS" value="SMS" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { subscribeNotify, unsubscribeNotify, userSubscriptions } from '@/api/notification'
import { useUserStore } from '@/stores/user'
import type { NotifySubscription } from '@/types'

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const list = ref<NotifySubscription[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<NotifySubscription>({ status: 1, subscribeType: 'JOB', channel: 'SITE' })
const formRules: FormRules = {
  subscribeType: [{ required: true, message: '请选择订阅类型', trigger: 'change' }],
  targetId: [{ required: true, message: '请输入目标ID', trigger: 'blur' }],
  channel: [{ required: true, message: '请选择渠道', trigger: 'change' }],
}

function subscribeTypeText(t?: string) {
  return { REPORT: '报表', METRIC: '指标', JOB: '任务' }[t ?? ''] ?? t ?? '-'
}

function channelText(c?: string) {
  return { MAIL: '邮件', SITE: '站内', WECHAT: '微信', SMS: '短信' }[c ?? ''] ?? c ?? '-'
}

async function load() {
  loading.value = true
  try {
    const userId = userStore.userInfo?.id || ''
    if (!userId) {
      list.value = []
      return
    }
    list.value = (await userSubscriptions(userId)) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { subscribeType: 'JOB', targetId: '', channel: 'SITE', status: 1, userId: userStore.userInfo?.id || '' })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await subscribeNotify({ ...form })
    ElMessage.success('订阅成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: NotifySubscription) {
  await ElMessageBox.confirm('确认取消该订阅吗？', '提示', { type: 'warning' })
  await unsubscribeNotify(row.id!)
  ElMessage.success('已取消订阅')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.alert {
  flex: 1;
  margin-right: 16px;
}
</style>
