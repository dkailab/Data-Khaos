<template>
  <div class="portrait-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">用户画像</h2>
        <p class="page-subtitle">标签体系 · 用户画像查询 · 画像分布统计</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- ============ 标签管理 ============ -->
      <el-tab-pane label="标签管理" name="tags">
        <div class="tag-manage">
          <div class="category-panel">
            <div class="panel-title">
              <span>标签分类</span>
              <el-button :icon="Plus" link type="primary" @click="openCategoryDialog()">新增分类</el-button>
            </div>
            <el-scrollbar class="category-list">
              <div
                class="category-item"
                :class="{ active: activeCategoryId === '' }"
                @click="activeCategoryId = ''; loadTags()"
              >
                <el-icon><Files /></el-icon><span>全部标签</span>
              </div>
              <div
                v-for="cat in categories"
                :key="cat.id"
                class="category-item"
                :class="{ active: activeCategoryId === cat.id }"
                @click="activeCategoryId = cat.id; loadTags()"
              >
                <el-icon><Collection /></el-icon>
                <span class="cat-name" :title="cat.name">{{ cat.name }}</span>
                <el-dropdown trigger="click" style="margin-left: auto">
                  <el-icon class="cat-more"><MoreFilled /></el-icon>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="openTagDialog(undefined, cat.id)">新增标签</el-dropdown-item>
                      <el-dropdown-item @click="openCategoryDialog(cat)">编辑分类</el-dropdown-item>
                      <el-dropdown-item divided @click="removeCategory(cat)">删除分类</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </el-scrollbar>
          </div>

          <div class="tag-panel">
            <div class="toolbar">
              <el-input v-model="tagQuery.keyword" placeholder="搜索标签名称/编码" clearable size="small" :prefix-icon="Search" style="width: 240px" @keyup.enter="loadTags" />
              <el-button type="primary" size="small" :icon="Plus" @click="openTagDialog()">新增标签</el-button>
            </div>
            <el-table v-loading="tagLoading" :data="tags" size="small" border>
              <el-table-column prop="name" label="标签名称" min-width="120" />
              <el-table-column prop="code" label="编码" min-width="100" />
              <el-table-column width="90" label="类型">
                <template #default="{ row }">
                  <el-tag size="small">{{ tagTypeLabel(row.tagType) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="unit" label="单位" width="80" />
              <el-table-column prop="description" label="说明" min-width="140" show-overflow-tooltip />
              <el-table-column width="80" label="状态">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column width="110" label="操作" fixed="right">
                <template #default="{ row }">
                  <el-button link size="small" type="primary" @click="openTagDialog(row)">编辑</el-button>
                  <el-button link size="small" type="danger" @click="removeTag(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination class="pager" v-model:current-page="tagQuery.current" v-model:page-size="tagQuery.size" :total="tagTotal" layout="total, prev, pager, next" @change="loadTags" small />
          </div>
        </div>
      </el-tab-pane>

      <!-- ============ 用户画像查询 ============ -->
      <el-tab-pane label="用户画像查询" name="query">
        <div class="portrait-query">
          <div class="query-bar">
            <el-input v-model="queryUserKey" placeholder="输入用户ID" clearable size="small" style="width: 220px" />
            <el-input v-model="queryUserName" placeholder="用户名称(可选)" clearable size="small" style="width: 200px" />
            <el-button type="primary" size="small" :icon="Search" @click="queryUserPortrait">查询画像</el-button>
            <el-button size="small" :icon="Plus" :disabled="!queryUserKey" @click="openUserTagDialog">添加标签值</el-button>
          </div>
          <div v-loading="userTagLoading" class="user-tags">
            <template v-if="userTags.length">
              <el-empty v-if="!groupedUserTags.length" description="该用户暂无画像标签" :image-size="80" />
              <div v-for="g in groupedUserTags" :key="g.categoryId" class="user-tag-group">
                <div class="group-title">
                  <el-icon><Collection /></el-icon><span>{{ g.categoryName }}</span>
                </div>
                <div class="group-body">
                  <div v-for="ut in g.items" :key="ut.id" class="utag-card">
                    <div class="utag-name">{{ ut.tagName }}</div>
                    <div class="utag-value">{{ displayValue(ut) }}</div>
                    <el-button link size="small" type="danger" class="utag-del" :icon="Delete" @click="removeUserTag(ut)" />
                  </div>
                </div>
              </div>
            </template>
            <el-empty v-else-if="!userTagLoading" description="请输入用户ID后查询画像" :image-size="100" />
          </div>
        </div>
      </el-tab-pane>

      <!-- ============ 画像统计 ============ -->
      <el-tab-pane label="画像统计" name="stat">
        <div class="portrait-stat">
          <div class="stat-bar">
            <el-select v-model="statTagId" placeholder="选择标签查看分布" filterable clearable size="small" style="width: 260px" @change="loadDistribution">
              <el-option v-for="t in allTags" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
            <span v-if="statTag" class="stat-tag-desc">{{ statTag.code || statTag.name }}</span>
          </div>
          <div v-if="distribution.length" class="dist-list">
            <div v-for="d in distribution" :key="d.value" class="dist-row">
              <span class="dist-label">{{ d.value }}</span>
              <div class="dist-bar-track">
                <div class="dist-bar" :style="{ width: pct(d) + '%' }"></div>
              </div>
              <span class="dist-count">{{ d.count }} 人</span>
              <span class="dist-ratio">{{ d.ratio }}%</span>
            </div>
          </div>
          <el-empty v-else-if="statTagId" description="暂无该标签的分布数据" :image-size="90" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 分类表单 -->
    <el-dialog v-model="categoryDialog.visible" :title="categoryDialog.isEdit ? '编辑分类' : '新增分类'" width="420px">
      <el-form :model="categoryDialog.form" label-width="80px">
        <el-form-item label="分类名称" required>
          <el-input v-model="categoryDialog.form.name" placeholder="如：基础属性" />
        </el-form-item>
        <el-form-item label="分类编码">
          <el-input v-model="categoryDialog.form.code" placeholder="英文标识" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="categoryDialog.form.sortOrder" :min="0" size="small" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="categoryDialog.visible = false">取消</el-button>
        <el-button size="small" type="primary" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>

    <!-- 标签表单 -->
    <el-dialog v-model="tagDialog.visible" :title="tagDialog.isEdit ? '编辑标签' : '新增标签'" width="520px">
      <el-form :model="tagDialog.form" label-width="90px">
        <el-form-item label="标签名称" required>
          <el-input v-model="tagDialog.form.name" placeholder="如：性别" />
        </el-form-item>
        <el-form-item label="标签编码">
          <el-input v-model="tagDialog.form.code" placeholder="英文标识" />
        </el-form-item>
        <el-form-item label="所属分类">
          <el-select v-model="tagDialog.form.categoryId" size="small" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签类型">
          <el-select v-model="tagDialog.form.tagType" size="small" style="width: 100%">
            <el-option label="布尔 BOOL" value="BOOL" />
            <el-option label="数值 NUMBER" value="NUMBER" />
            <el-option label="字符串 STR" value="STR" />
            <el-option label="枚举 ENUM" value="ENUM" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="tagDialog.form.tagType === 'NUMBER'" label="单位">
          <el-input v-model="tagDialog.form.unit" placeholder="如：元" style="width: 120px" />
        </el-form-item>
        <el-form-item v-if="tagDialog.form.tagType === 'ENUM'" label="可选值">
          <el-input v-model="tagDialog.enumText" type="textarea" :rows="3" placeholder="每行一个可选值" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="tagDialog.form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="tagDialog.visible = false">取消</el-button>
        <el-button size="small" type="primary" @click="saveTag">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加用户标签值 -->
    <el-dialog v-model="userTagDialog.visible" title="添加用户画像标签" width="460px">
      <el-form :model="userTagDialog.form" label-width="80px">
        <el-form-item label="用户ID" required>
          <el-input v-model="userTagDialog.form.userKey" :disabled="!!queryUserKey" />
        </el-form-item>
        <el-form-item label="用户名称">
          <el-input v-model="userTagDialog.form.userName" />
        </el-form-item>
        <el-form-item label="标签" required>
          <el-select v-model="userTagDialog.form.tagId" filterable size="small" style="width: 100%">
            <el-option-group v-for="g in tagGroups" :key="g.categoryId" :label="g.categoryName">
              <el-option v-for="t in g.tags" :key="t.id" :label="t.name" :value="t.id" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="标签值">
          <el-select
            v-if="selectedTagForValue"
            v-model="userTagDialog.form.tagValue"
            size="small"
            allow-create
            filterable
            style="width: 100%"
            :placeholder="selectedTagForValue.tagType === 'BOOL' ? 'true / false' : (selectedTagForValue.tagType === 'NUMBER' ? '请输入数值' : '请输入或选择')"
          >
            <el-option :value="'true'" label="true (是)" />
            <el-option :value="'false'" label="false (否)" />
            <el-option v-for="v in selectedTagEnum" :key="v" :label="v" :value="v" />
          </el-select>
          <el-input v-else v-model="userTagDialog.form.tagValue" placeholder="标签值" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="userTagDialog.visible = false">取消</el-button>
        <el-button size="small" type="primary" @click="saveUserTag">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Collection, Delete, Files, MoreFilled, Plus, Search } from '@element-plus/icons-vue'
import {
  type PortraitCategory, type PortraitDistribution, type PortraitTag, type PortraitUserTag,
  createPortraitCategory, createPortraitTag, deletePortraitCategory, deletePortraitTag,
  deletePortraitUserTag, getPortraitDistribution, getPortraitUserTags, listPortraitCategories,
  listPortraitTags, pagePortraitTags, updatePortraitCategory, updatePortraitTag, upsertPortraitUserTag,
} from '@/api/portrait'

const activeTab = ref('tags')

/* ==================== 分类 ==================== */
const categories = ref<PortraitCategory[]>([])
const activeCategoryId = ref('')

const categoryDialog = reactive<{ visible: boolean; isEdit: boolean; form: Partial<PortraitCategory> }>({
  visible: false, isEdit: false, form: {},
})

function openCategoryDialog(cat?: PortraitCategory) {
  categoryDialog.isEdit = !!cat
  categoryDialog.form = cat ? { ...cat } : { name: '', code: '', sortOrder: 0 }
  categoryDialog.visible = true
}

async function saveCategory() {
  if (!categoryDialog.form.name) return ElMessage.warning('请输入分类名称')
  if (categoryDialog.isEdit) {
    await updatePortraitCategory(categoryDialog.form.id!, categoryDialog.form)
  } else {
    await createPortraitCategory({ ...categoryDialog.form, sortOrder: categoryDialog.form.sortOrder || 0 })
  }
  ElMessage.success('保存成功')
  categoryDialog.visible = false
  loadCategories()
}

async function removeCategory(cat: PortraitCategory) {
  await ElMessageBox.confirm(`确认删除分类「${cat.name}」？其下标签及用户数据将一并删除。`, '提示', { type: 'warning' })
  await deletePortraitCategory(cat.id)
  if (activeCategoryId.value === cat.id) activeCategoryId.value = ''
  ElMessage.success('删除成功')
  loadCategories(); loadTags()
}

async function loadCategories() {
  categories.value = await listPortraitCategories()
}

/* ==================== 标签 ==================== */
const tags = ref<PortraitTag[]>([])
const tagLoading = ref(false)
const tagTotal = ref(0)
const tagQuery = reactive({ current: 1, size: 10, keyword: '' })

const tagDialog = reactive<{ visible: boolean; isEdit: boolean; form: Partial<PortraitTag>; enumText: string }>({
  visible: false, isEdit: false, form: {}, enumText: '',
})

const TAG_TYPES: Record<string, string> = { BOOL: '布尔', NUMBER: '数值', STR: '字符串', ENUM: '枚举' }
function tagTypeLabel(t?: string) { return TAG_TYPES[t || 'STR'] || '字符串' }

function openTagDialog(tag?: PortraitTag, presetCategoryId?: string) {
  tagDialog.isEdit = !!tag
  tagDialog.enumText = tag?.enumOptions ? parseEnum(tag.enumOptions).join('\n') : ''
  tagDialog.form = tag
    ? { ...tag }
    : { categoryId: presetCategoryId || activeCategoryId.value || undefined, tagType: 'STR' }
  if (presetCategoryId) tagDialog.form.categoryId = presetCategoryId
  tagDialog.visible = true
}

async function saveTag() {
  if (!tagDialog.form.name || !tagDialog.form.categoryId) return ElMessage.warning('请填写标签名称与分类')
  const payload: any = { ...tagDialog.form }
  if (tagDialog.form.tagType === 'ENUM') {
    payload.enumOptions = JSON.stringify(tagDialog.enumText.split(/\r?\n/).map(s => s.trim()).filter(Boolean))
  } else if (tagDialog.form.tagType !== 'ENUM') {
    payload.enumOptions = undefined
  }
  if (tagDialog.isEdit) {
    await updatePortraitTag(tagDialog.form.id!, payload)
  } else {
    await createPortraitTag(payload)
  }
  ElMessage.success('保存成功')
  tagDialog.visible = false
  loadTags()
}

async function removeTag(tag: PortraitTag) {
  await ElMessageBox.confirm(`确认删除标签「${tag.name}」？`, '提示', { type: 'warning' })
  await deletePortraitTag(tag.id)
  ElMessage.success('删除成功')
  loadTags()
}

async function loadTags() {
  tagLoading.value = true
  try {
    const data = await pagePortraitTags({
      current: tagQuery.current, size: tagQuery.size, keyword: tagQuery.keyword, categoryId: activeCategoryId.value || undefined,
    })
    tags.value = data.records || []
    tagTotal.value = Number(data.total)
  } finally {
    tagLoading.value = false
  }
}

/* ==================== 画像查询 ==================== */
const queryUserKey = ref('')
const queryUserName = ref('')
const userTags = ref<PortraitUserTag[]>([])
const userTagLoading = ref(false)

const allTags = ref<PortraitTag[]>([])
const tagGroups = computed(() => {
  return categories.value
    .map(c => ({ categoryId: c.id, categoryName: c.name, tags: allTags.value.filter(t => t.categoryId === c.id) }))
    .filter(g => g.tags.length)
})

const tagById = computed(() => {
  const m = new Map<string, PortraitTag>()
  allTags.value.forEach(t => m.set(t.id, t))
  return m
})
const categoryById = computed(() => {
  const m = new Map<string, PortraitCategory>()
  categories.value.forEach(c => m.set(c.id, c))
  return m
})

const groupedUserTags = computed(() => {
  return categories.value
    .map(c => ({
      categoryId: c.id,
      categoryName: c.name,
      items: userTags.value
        .filter(ut => tagById.value.get(ut.tagId)?.categoryId === c.id)
        .map(ut => ({ ...ut, tagName: tagById.value.get(ut.tagId)?.name || ut.tagId, tagType: tagById.value.get(ut.tagId)?.tagType })),
    }))
    .filter(g => g.items.length)
})

function displayValue(ut: any) {
  const tag = tagById.value.get(ut.tagId)
  if (tag?.tagType === 'BOOL') return ut.tagValue === 'true' ? '是' : '否'
  const unit = tag?.unit ? ` ${tag.unit}` : ''
  return `${ut.tagValue ?? ''}${unit}`
}

async function queryUserPortrait() {
  if (!queryUserKey.value) return ElMessage.warning('请输入用户ID')
  userTagLoading.value = true
  try {
    if (!allTags.value.length) allTags.value = await listPortraitTags()
    userTags.value = await getPortraitUserTags(queryUserKey.value) || []
  } finally {
    userTagLoading.value = false
  }
}

async function removeUserTag(ut: PortraitUserTag) {
  await ElMessageBox.confirm('确认删除该标签值？', '提示', { type: 'warning' })
  await deletePortraitUserTag(ut.id!)
  ElMessage.success('删除成功')
  if (queryUserKey.value) userTags.value = await getPortraitUserTags(queryUserKey.value) || []
}

/* ==================== 添加用户标签值 ==================== */
const userTagDialog = reactive<{ visible: boolean; form: PortraitUserTag }>({
  visible: false, form: { userKey: '', userName: '', tagId: '', tagValue: '' },
})

async function openUserTagDialog() {
  if (!allTags.value.length) allTags.value = await listPortraitTags()
  userTagDialog.form = { userKey: queryUserKey.value, userName: queryUserName.value, tagId: '', tagValue: '' }
  userTagDialog.visible = true
}

const selectedTagForValue = computed(() => tagById.value.get(userTagDialog.form.tagId))
const selectedTagEnum = computed(() => {
  const t = selectedTagForValue.value
  if (!t?.enumOptions) return []
  return parseEnum(t.enumOptions)
})

function parseEnum(json?: string): string[] {
  if (!json) return []
  try { return JSON.parse(json) as string[] } catch { return [] }
}

async function saveUserTag() {
  if (!userTagDialog.form.userKey || !userTagDialog.form.tagId) return ElMessage.warning('请填写用户ID与标签')
  await upsertPortraitUserTag(userTagDialog.form)
  ElMessage.success('保存成功')
  userTagDialog.visible = false
  if (userTagDialog.form.userKey === queryUserKey.value) {
    userTags.value = await getPortraitUserTags(queryUserKey.value) || []
  }
}

/* ==================== 画像统计 ==================== */
const statTagId = ref('')
const distribution = ref<PortraitDistribution[]>([])
const statTag = computed(() => allTags.value.find(t => t.id === statTagId.value))
const pct = computed(() => (d: PortraitDistribution) => {
  const max = Math.max(...distribution.value.map(x => x.count), 1)
  return Math.max(4, Math.round(d.count * 100 / max))
})

async function loadDistribution() {
  if (!statTagId.value) { distribution.value = []; return }
  distribution.value = await getPortraitDistribution(statTagId.value) || []
}

/* ==================== init ==================== */
onMounted(async () => {
  await loadCategories()
  await loadTags()
  allTags.value = await listPortraitTags()
})
</script>

<style scoped>
.portrait-page { padding: 16px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-title { margin: 0; font-size: 20px; font-weight: 700; color: #1f2d3d; }
.page-subtitle { margin: 4px 0 0; font-size: 13px; color: #8592a6; }

/* 标签管理 */
.tag-manage { display: flex; gap: 16px; min-height: 480px; }
.category-panel { width: 240px; flex-shrink: 0; border: 1px solid #e4eaf3; border-radius: 8px; display: flex; flex-direction: column; }
.panel-title { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px; font-weight: 600; border-bottom: 1px solid #eef2f8; }
.category-list { flex: 1; padding: 6px; }
.category-item { display: flex; align-items: center; gap: 6px; padding: 8px 10px; border-radius: 6px; cursor: pointer; color: #3d4757; }
.category-item:hover { background: #eef5ff; }
.category-item.active { background: #e7f1ff; color: #165dff; }
.cat-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cat-more { color: #8592a6; }
.tag-panel { flex: 1; display: flex; flex-direction: column; gap: 12px; min-width: 0; }
.toolbar { display: flex; justify-content: space-between; }
.pager { margin-top: 12px; }

/* 画像查询 */
.portrait-query { display: flex; flex-direction: column; gap: 16px; }
.query-bar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.user-tags { display: flex; flex-direction: column; gap: 16px; min-height: 260px; }
.user-tag-group { border: 1px solid #e4eaf3; border-radius: 8px; overflow: hidden; }
.group-title { display: flex; align-items: center; gap: 6px; padding: 10px 12px; font-weight: 600; background: #f7faff; color: #165dff; }
.group-body { display: flex; flex-wrap: wrap; gap: 10px; padding: 12px; }
.utag-card { position: relative; width: 180px; border: 1px solid #e4eaf3; border-radius: 8px; padding: 10px 12px; background: #fff; }
.utag-name { font-size: 12px; color: #8592a6; }
.utag-value { margin-top: 4px; font-size: 16px; font-weight: 600; color: #1f2d3d; }
.utag-del { position: absolute; top: 4px; right: 4px; }

/* 画像统计 */
.portrait-stat { display: flex; flex-direction: column; gap: 16px; min-height: 300px; }
.stat-bar { display: flex; align-items: center; gap: 12px; }
.stat-tag-desc { color: #8592a6; font-size: 13px; }
.dist-list { display: flex; flex-direction: column; gap: 12px; }
.dist-row { display: flex; align-items: center; gap: 12px; }
.dist-label { width: 120px; text-align: right; color: #3d4757; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dist-bar-track { flex: 1; height: 18px; background: #f0f4fb; border-radius: 9px; overflow: hidden; }
.dist-bar { height: 100%; background: linear-gradient(90deg, #7fb8e6, #165dff); border-radius: 9px; transition: width .3s; }
.dist-count { width: 70px; color: #1f2d3d; }
.dist-ratio { width: 60px; color: #8592a6; }
</style>