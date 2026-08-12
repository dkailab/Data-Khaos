<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-form inline>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="模板名称/编码" clearable style="width: 180px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="query.channel" clearable placeholder="全部" style="width: 120px" @change="handleSearch">
            <el-option v-for="c in channels" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增模板</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="templateCode" label="模板编码" min-width="140" />
      <el-table-column prop="templateName" label="模板名称" min-width="150" />
      <el-table-column label="渠道" width="100">
        <template #default="{ row }">
          <el-tag>{{ channelText(row.channel) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="titleTemplate" label="标题模板" min-width="180" show-overflow-tooltip />
      <el-table-column prop="contentTemplate" label="内容模板" min-width="220" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑模板' : '新增模板'" width="580px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="110px">
        <el-form-item label="模板编码" prop="templateCode">
          <el-input v-model="form.templateCode" placeholder="请输入模板编码" />
        </el-form-item>
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="渠道" prop="channel">
          <el-select v-model="form.channel" style="width: 100%">
            <el-option v-for="c in channels" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题模板" prop="titleTemplate">
          <el-input v-model="form.titleTemplate" type="textarea" :rows="2" placeholder="如 任务 {{jobName}} 执行完成" />
        </el-form-item>
        <el-form-item label="内容模板" prop="contentTemplate">
          <el-input v-model="form.contentTemplate" type="textarea" :rows="4" placeholder="如 任务执行结果：{{message}}" />
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
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createTemplate, deleteTemplate, pageTemplates, updateTemplate } from '@/api/notification'
import type { NotifyTemplate } from '@/types'

const channels = [
  { label: '邮件 MAIL', value: 'MAIL' },
  { label: '站内 SITE', value: 'SITE' },
  { label: '微信 WECHAT', value: 'WECHAT' },
  { label: '短信 SMS', value: 'SMS' },
]

function channelText(c?: string) {
  return channels.find((x) => x.value === c)?.label ?? c ?? '-'
}

const loading = ref(false)
const submitting = ref(false)
const list = ref<NotifyTemplate[]>([])
const total = ref(0)
const query = reactive<Record<string, any>>({ current: 1, size: 10, keyword: '', channel: undefined })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const form = reactive<NotifyTemplate>({ status: 1, channel: 'SITE' })
const formRules: FormRules = {
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  channel: [{ required: true, message: '请选择渠道', trigger: 'change' }],
}

async function load() {
  loading.value = true
  try {
    const data = await pageTemplates({ ...query })
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
  query.keyword = ''
  query.channel = undefined
  handleSearch()
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { templateCode: '', templateName: '', channel: 'SITE', titleTemplate: '', contentTemplate: '', status: 1 })
  dialogVisible.value = true
}

function openEdit(row: NotifyTemplate) {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateTemplate({ ...form })
    } else {
      await createTemplate({ ...form })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: NotifyTemplate) {
  await ElMessageBox.confirm(`确认删除模板「${row.templateName}」吗？`, '提示', { type: 'warning' })
  await deleteTemplate(row.id!)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
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
</style>
