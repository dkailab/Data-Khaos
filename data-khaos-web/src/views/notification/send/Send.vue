<template>
  <el-row :gutter="16">
    <el-col :span="12">
      <el-card shadow="never">
        <template #header>发送通知</template>
        <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
          <el-form-item label="模板编码" prop="templateCode">
            <el-select v-model="form.templateCode" placeholder="请选择模板" filterable style="width: 100%">
              <el-option v-for="t in templates" :key="t.id" :label="`${t.templateCode}（${t.templateName}）`" :value="t.templateCode!" />
            </el-select>
          </el-form-item>
          <el-form-item label="接收人类型" prop="receiverType">
            <el-radio-group v-model="form.receiverType">
              <el-radio value="USER">用户</el-radio>
              <el-radio value="ROLE">角色</el-radio>
              <el-radio value="ORG">组织</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="接收人ID" prop="receiverId">
            <el-input v-model="form.receiverId" placeholder="用户/角色/组织 ID" />
          </el-form-item>
          <el-form-item label="渠道" prop="channel">
            <el-select v-model="form.channel" clearable placeholder="缺省取模板渠道" style="width: 100%">
              <el-option v-for="c in channels" :key="c.value" :label="c.label" :value="c.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="模板变量">
            <div class="vars">
              <div v-for="(v, k) in form.vars" :key="k" class="var-row">
                <el-input :model-value="k" placeholder="变量名" style="width: 180px" disabled />
                <el-input :model-value="String(v)" placeholder="变量值" style="width: 200px" @update:model-value="(val: string) => updateVar(k, val)" />
                <el-button link type="danger" @click="removeVar(k)">删除</el-button>
              </div>
              <el-button link type="primary" @click="addVar">+ 添加变量</el-button>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="sending" @click="submit">发送</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>

    <el-col :span="12">
      <el-card shadow="never">
        <template #header>发送记录</template>
        <el-table v-loading="recordLoading" :data="records" size="small" border max-height="520">
          <el-table-column prop="templateId" label="模板ID" min-width="130" />
          <el-table-column prop="receiverId" label="接收人ID" min-width="110" />
          <el-table-column prop="receiverType" label="类型" width="80" />
          <el-table-column prop="channel" label="渠道" width="80" />
          <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="recordStatusTagType(row.status)">{{ recordStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sendTime" label="发送时间" width="160" />
        </el-table>
        <el-pagination
          class="pager"
          v-model:current-page="recordQuery.current"
          v-model:page-size="recordQuery.size"
          :total="recordTotal"
          layout="total, prev, pager, next"
          @change="loadRecords"
        />
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { pageRecords, pageTemplates, sendNotify } from '@/api/notification'
import type { NotifyRecord, NotifyTemplate, SendRequest } from '@/types'

const channels = [
  { label: '邮件 MAIL', value: 'MAIL' },
  { label: '站内 SITE', value: 'SITE' },
  { label: '微信 WECHAT', value: 'WECHAT' },
  { label: '短信 SMS', value: 'SMS' },
]

const templates = ref<NotifyTemplate[]>([])
const formRef = ref<FormInstance>()
const sending = ref(false)

interface SendForm {
  templateCode: string
  receiverId: string
  receiverType: string
  channel: string
  vars: Record<string, any>
}

const form = reactive<SendForm>({ templateCode: '', receiverId: '', receiverType: 'USER', channel: '', vars: {} })
const formRules: FormRules = {
  templateCode: [{ required: true, message: '请选择模板', trigger: 'change' }],
  receiverId: [{ required: true, message: '请输入接收人ID', trigger: 'blur' }],
  receiverType: [{ required: true, message: '请选择接收人类型', trigger: 'change' }],
}

const recordLoading = ref(false)
const records = ref<NotifyRecord[]>([])
const recordTotal = ref(0)
const recordQuery = reactive({ current: 1, size: 10 })

function recordStatusText(s?: number) {
  return { 0: '待发送', 1: '已发送', 2: '发送失败' }[s ?? -1] ?? '-'
}

function recordStatusTagType(s?: number): 'info' | 'success' | 'danger' {
  if (s === 0) return 'info'
  if (s === 1) return 'success'
  return 'danger'
}

async function loadTemplates() {
  const data = await pageTemplates({ current: 1, size: 100 })
  templates.value = data.records || []
}

async function loadRecords() {
  recordLoading.value = true
  try {
    const data = await pageRecords({ ...recordQuery })
    records.value = data.records
    recordTotal.value = Number(data.total)
  } finally {
    recordLoading.value = false
  }
}

function addVar() {
  const idx = Object.keys(form.vars).length
  form.vars[`var${idx + 1}`] = ''
}

function updateVar(key: string, val: string) {
  form.vars[key] = val
}

function removeVar(key: string) {
  delete form.vars[key]
}

async function submit() {
  await formRef.value?.validate()
  sending.value = true
  try {
    await sendNotify({
      ...form,
      channel: form.channel || undefined,
      vars: Object.keys(form.vars).length ? form.vars : undefined,
    })
    ElMessage.success('发送成功')
    loadRecords()
  } finally {
    sending.value = false
  }
}

onMounted(() => {
  loadTemplates()
  loadRecords()
})
</script>

<style scoped>
.var-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.vars {
  width: 100%;
}
.pager {
  margin-top: 8px;
  justify-content: flex-end;
}
</style>
