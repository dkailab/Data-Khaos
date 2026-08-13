<template>
  <div class="board-page" :class="[themeClass, { 'is-fullscreen': fullscreen }]">
    <!-- ============ 顶部全局导航栏 ============ -->
    <div class="board-header">
      <div class="board-title">
        <el-button :icon="ArrowLeft" circle @click="router.back()" />
        <div>
          <div class="board-name">
            {{ dashboard.name || 'DK实时分析板' }}
            <el-tag v-if="dashboard.status === 2" type="success" size="small">已上线 v{{ dashboard.version }}</el-tag>
            <el-tag v-else type="warning" size="small">草稿 v{{ dashboard.version || 0 }}</el-tag>
          </div>
          <div class="board-desc">{{ dashboard.description || '数据分析仪表板' }} · 含 {{ boards.length }} 个分析板</div>
        </div>
      </div>
      <div class="board-actions">
        <el-tooltip content="切换主题"><el-button :icon="themeClass === 'theme-dark' ? Sunny : Moon" @click="toggleTheme" /></el-tooltip>
        <el-tooltip content="全屏展示"><el-button :icon="FullScreen" @click="fullscreen = !fullscreen" /></el-tooltip>
        <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
        <el-switch v-if="!isPreview" v-model="editMode" active-text="编辑" inactive-text="预览" inline-prompt @change="onEditModeChange" />
        <el-button v-if="!isPreview" :icon="Clock" @click="openVersions">版本</el-button>
        <el-tooltip v-if="!isPreview" content="分享看板"><el-button :icon="Share" @click="openShare">分享</el-button></el-tooltip>
        <el-button v-if="!isPreview && dashboard.status !== 2" type="primary" :icon="Top" @click="openPublish">上线</el-button>
        <el-button v-else-if="!isPreview && dashboard.status === 2" type="warning" :icon="Bottom" @click="handleUnpublish">下线</el-button>
      </div>
    </div>

    <!-- ============ 全局筛选栏 ============ -->
    <div class="filter-bar">
      <div class="filter-item">
        <span class="filter-label">时间</span>
        <el-select v-model="globalFilter.timeRange" size="small" style="width: 150px" @change="onFilterChange">
          <el-option v-for="t in TIME_RANGES" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </div>
      <div class="filter-item">
        <span class="filter-label">维度</span>
        <el-select v-model="globalFilter.dimension" size="small" clearable placeholder="全部维度" style="width: 160px" @change="onFilterChange">
          <el-option label="按品类" value="category" />
          <el-option label="按渠道" value="channel" />
          <el-option label="按地区" value="region" />
          <el-option label="按门店" value="store" />
        </el-select>
      </div>
      <el-button size="small" :icon="RefreshLeft" @click="resetFilter">重置筛选</el-button>
      <span class="filter-tip">全局筛选兜底，分析板内筛选优先</span>
    </div>

    <!-- ============ 编辑模式工具栏 ============ -->
    <div v-if="editMode" class="board-toolbar">
      <el-button type="primary" :icon="Plus" @click="openCreateBoard">新建分析板</el-button>
      <el-button :icon="CopyDocument" @click="duplicateSelectedBoard">复制选中分析板</el-button>
      <el-button :icon="Lock" @click="toggleBoardLock">锁定/解锁布局</el-button>
      <span class="toolbar-hint">拖拽分析板标题可移动，拖拽组件可重排；改动自动保存</span>
      <el-button style="margin-left: auto" type="success" :icon="Check" :loading="saving" @click="saveAll">保存草稿</el-button>
    </div>

    <!-- ============ 主体画布：仪表板 → 分析板 → 组件 ============ -->
    <div v-loading="loading" class="board-canvas">
      <template v-if="boards.length">
        <div
          v-for="(board, bi) in boards"
          :key="board.id"
          class="board-cell"
          :class="{ 'is-edit': editMode, 'is-selected': selectedBoardId === board.id }"
          :style="boardStyle(board)"
          @click="selectedBoardId = board.id || ''"
        >
          <!-- 分析板头 -->
          <div
            class="board-header-bar"
            :draggable="editMode && !board.locked"
            @dragstart="onBoardDragStart($event, bi)"
            @dragover.prevent
            @drop="onBoardDrop($event, bi)"
          >
            <el-icon class="board-drag-handle" :color="accentColor"><Rank /></el-icon>
            <el-icon class="board-icon" :size="18" :color="accentColor"><component :is="boardIcon(board)" /></el-icon>
            <span class="board-name">{{ board.boardName }}</span>
            <el-tag v-if="board.subtitle" size="small" effect="plain" class="board-subtitle">{{ board.subtitle }}</el-tag>
            <el-tag v-if="hasBoardFilter(board)" size="small" type="warning" effect="plain" class="board-filter-tag">已筛选</el-tag>
            <span class="board-ops">
              <template v-if="editMode">
                <el-tooltip content="折叠/展开"><el-button link :icon="board.collapse ? ArrowDown : ArrowUp" @click.stop="toggleCollapse(board)" /></el-tooltip>
                <el-tooltip content="独立筛选"><el-button link :icon="Filter" @click.stop="openBoardFilter(board)" /></el-tooltip>
                <el-tooltip content="添加组件"><el-button link :icon="Plus" @click.stop="openAddItem(board)" /></el-tooltip>
                <el-tooltip content="配置分析板"><el-button link :icon="Setting" @click.stop="openEditBoard(board)" /></el-tooltip>
                <el-tooltip content="复制"><el-button link :icon="CopyDocument" @click.stop="duplicateSingle(board)" /></el-tooltip>
                <el-tooltip content="删除"><el-button link type="danger" :icon="Delete" @click.stop="removeBoard(board)" /></el-tooltip>
              </template>
              <el-tooltip v-else content="刷新分析板"><el-button link :icon="Refresh" @click.stop="refreshBoard(board)" /></el-tooltip>
            </span>
          </div>

          <!-- 分析板体 -->
          <div v-show="!board.collapse" class="board-body">
            <div
              v-for="(item, ii) in boardItems(board.id)"
              :key="item.id"
              class="board-grid-item"
              :class="{ 'is-edit': editMode }"
              :style="itemGridStyle(item)"
              :draggable="editMode && !board.locked"
              @dragstart="onItemDragStart($event, board.id, ii)"
              @dragover.prevent
              @drop="onItemDrop($event, board.id, ii)"
            >
              <div class="item-header">
                <span class="item-title">{{ item.title }}</span>
                <span class="item-ops">
                  <el-tooltip content="导出"><el-button link :icon="Download" @click="exportItem(item)" /></el-tooltip>
                  <el-tooltip content="放大"><el-button link :icon="FullScreen" @click="openFullscreen(item)" /></el-tooltip>
                  <el-tooltip content="刷新"><el-button link :icon="Refresh" @click="refreshItem(item)" /></el-tooltip>
                  <template v-if="editMode">
                    <el-tooltip content="编辑"><el-button link :icon="Edit" @click="openEditItem(item)" /></el-tooltip>
                    <el-tooltip content="删除"><el-button link type="danger" :icon="Delete" @click="removeItem(item)" /></el-tooltip>
                  </template>
                </span>
              </div>
              <div class="item-body">
                <ChartRenderer :item="item" :result="results[item.id!]" :loading="loadingItems[item.id!]" :height="itemHeight(item)" :theme="theme" @drill="onDrill(item, $event)" />
              </div>
            </div>

            <el-empty v-if="!boardItems(board.id).length" :image-size="60" description="该分析板暂无组件，编辑模式下点击 + 添加" />
          </div>
        </div>
      </template>

      <el-empty v-else :description="editMode ? '暂无分析板，点击「新建分析板」开始构建' : '暂无分析板'" />
    </div>

    <!-- ============ 分析板配置弹窗 ============ -->
    <el-dialog v-model="boardDialog" :title="editingBoard?.id ? '配置分析板' : '新建分析板'" width="540px" destroy-on-close>
      <el-form ref="boardFormRef" :model="boardForm" :rules="boardRules" label-width="90px">
        <el-form-item label="分析板标题" prop="boardName">
          <el-input v-model="boardForm.boardName" placeholder="如：用户分析板 / 营收分析板" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="boardForm.subtitle" placeholder="业务描述，如：核心用户增长与活跃分析" />
        </el-form-item>
        <el-form-item label="图标">
          <el-select v-model="boardForm.icon" style="width: 100%">
            <el-option v-for="i in BOARD_ICONS" :key="i.value" :label="i.label" :value="i.value" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="刷新周期">
              <el-select v-model="boardForm.refreshInterval" style="width: 100%">
                <el-option v-for="r in REFRESH_OPTIONS" :key="r.value" :label="r.label" :value="r.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="布局锁定">
          <el-switch v-model="boardForm.locked" active-text="锁定（禁止拖拽）" />
        </el-form-item>
        <el-collapse style="margin-top: 4px">
          <el-collapse-item title="板块样式定制（圆角/阴影/边框/背景/内边距）" name="style">
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item label="圆角">
                  <el-input-number v-model="boardStyleForm.radius" :min="0" :max="40" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="阴影">
                  <el-input-number v-model="boardStyleForm.shadow" :min="0" :max="12" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="内边距">
                  <el-input-number v-model="boardStyleForm.padding" :min="0" :max="40" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item label="边框宽度">
                  <el-input-number v-model="boardStyleForm.borderWidth" :min="0" :max="8" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="16">
                <el-form-item label="边框颜色">
                  <div class="style-color-row">
                    <el-color-picker v-model="boardStyleForm.borderColor" />
                    <el-button size="small" link @click="boardStyleForm.borderColor = ''">清除</el-button>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="背景色">
              <div class="style-color-row">
                <el-color-picker v-model="boardStyleForm.bgColor" />
                <el-button size="small" link @click="boardStyleForm.bgColor = ''">清除(默认)</el-button>
              </div>
            </el-form-item>
          </el-collapse-item>
        </el-collapse>
      </el-form>
      <template #footer>
        <el-button @click="boardDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveBoard">保存</el-button>
      </template>
    </el-dialog>

    <!-- ============ 分析板独立筛选弹窗（单板筛选优先于全局） ============ -->
    <el-dialog v-model="boardFilterDialog" :title="`独立筛选 · ${filterBoard?.boardName || ''}`" width="640px" destroy-on-close>
      <el-form label-width="110px">
        <el-form-item label="联动全局筛选">
          <el-switch v-model="boardFilter.linkGlobal" active-text="联动" inactive-text="独立（覆盖全局）" />
          <div class="filter-tip">关闭后本板仅应用下方独立筛选，忽略全局筛选</div>
        </el-form-item>
        <el-divider content-position="left">时间范围</el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="时间范围">
              <el-select v-model="boardFilter.timeRange" style="width: 100%">
                <el-option v-for="t in TIME_RANGES" :key="t.value" :label="t.label" :value="t.value" />
                <el-option label="不限" value="all" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="日期列">
              <el-input v-model="boardFilter.dateColumn" placeholder="如 order_date / create_time" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">维度条件</el-divider>
        <div v-for="(c, ci) in boardFilter.conditions" :key="ci" class="filter-cond-row">
          <el-input v-model="c.field" placeholder="字段列" style="width: 34%" />
          <el-select v-model="c.op" style="width: 22%">
            <el-option label="=" value="eq" />
            <el-option label="≠" value="ne" />
            <el-option label=">" value="gt" />
            <el-option label="≥" value="gte" />
            <el-option label="<" value="lt" />
            <el-option label="≤" value="lte" />
            <el-option label="包含" value="contains" />
            <el-option label="IN(逗号分隔)" value="in" />
          </el-select>
          <el-input v-model="c.value" placeholder="值" style="width: 32%" />
          <el-button link type="danger" :icon="Delete" @click="boardFilter.conditions.splice(ci, 1)" />
        </div>
        <el-button size="small" :icon="Plus" @click="boardFilter.conditions.push({ field: '', op: 'eq', value: '' })">添加条件</el-button>
      </el-form>
      <template #footer>
        <el-button @click="boardFilterDialog = false">取消</el-button>
        <el-button @click="clearBoardFilter">清除筛选</el-button>
        <el-button type="primary" :loading="saving" @click="saveBoardFilter">应用筛选</el-button>
      </template>
    </el-dialog>

    <!-- ============ 组件配置弹窗 ============ -->
    <el-dialog v-model="itemDialog" :title="editingItem?.id ? '编辑组件' : '添加组件'" width="640px" destroy-on-close>
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="90px">
        <el-form-item label="组件标题" prop="title">
          <el-input v-model="itemForm.title" placeholder="例：销售趋势" />
        </el-form-item>
        <el-form-item label="图表类型" prop="chartType">
          <el-radio-group v-model="itemForm.chartType">
            <el-radio-button v-for="t in CHART_TYPES" :key="t.value" :value="t.value">{{ t.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数据源" prop="datasourceId">
          <el-select v-model="itemForm.datasourceId" placeholder="选择数据源" filterable style="width: 100%">
            <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="查询SQL" prop="querySql">
          <el-input v-model="itemForm.querySql" type="textarea" :rows="4" placeholder="SELECT ..." />
        </el-form-item>
        <el-form-item label="下钻明细SQL">
          <el-input v-model="itemForm.drillSql" type="textarea" :rows="2" placeholder="可选：点击图表下钻时执行的明细/次级SQL，如 SELECT 订单号,订单日期,金额 FROM demo_fact_order" />
          <div class="filter-tip">留空则点击图表使用查询SQL按维度过滤下钻</div>
        </el-form-item>
        <el-form-item label="测试/取列">
          <el-button size="small" :loading="testing" @click="testQuery">测试查询并取列</el-button>
          <span v-if="testColumns.length" class="col-tip">列：{{ testColumns.join(', ') }}</span>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="X轴/名称列">
              <el-select v-model="itemForm.config.xAxisColumn" clearable filterable allow-create placeholder="自动">
                <el-option v-for="c in testColumns" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="系列/维度列">
              <el-select v-model="itemForm.config.seriesColumn" clearable filterable allow-create placeholder="自动">
                <el-option v-for="c in testColumns" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="数值列">
              <el-select v-model="itemForm.config.valueColumn" clearable filterable allow-create placeholder="自动">
                <el-option v-for="c in testColumns" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="地图名称">
              <el-input v-model="itemForm.config.mapName" placeholder="如 china" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 指标卡专属配置（仅 NUMBER） -->
        <el-collapse v-if="itemForm.chartType === 'NUMBER'" style="margin-top: 4px">
          <el-collapse-item title="指标卡配置（单位/千分位/对比/迷你趋势）" name="metric">
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="指标名称">
                  <el-input v-model="itemForm.config.metricName" placeholder="默认用组件标题" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="副标题">
                  <el-input v-model="itemForm.config.metricSubtitle" placeholder="口径/说明" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item label="统计单位">
                  <el-input v-model="itemForm.config.unit" placeholder="如 元 / 件" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="小数位">
                  <el-input-number v-model="itemForm.config.decimals" :min="0" :max="4" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="千分位">
                  <el-switch v-model="itemForm.config.thousand" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item label="指标模式">
                  <el-select v-model="itemForm.config.metricMode" style="width: 100%">
                    <el-option label="基础指标" value="basic" />
                    <el-option label="对比指标" value="compare" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="对比方式">
                  <el-select v-model="itemForm.config.compareMode" style="width: 100%">
                    <el-option label="环比(上一条)" value="prev" />
                    <el-option label="同比(对比列)" value="ref" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="对比标签">
                  <el-input v-model="itemForm.config.compareLabel" placeholder="默认按模式" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item label="涨跌配色">
                  <el-select v-model="itemForm.config.reverseColor" style="width: 100%">
                    <el-option :value="false" label="红跌绿涨" />
                    <el-option :value="true" label="绿跌红涨" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="迷你趋势">
                  <el-switch v-model="itemForm.config.spark" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="趋势数据列">
                  <el-select v-model="itemForm.config.sparkColumn" clearable filterable allow-create placeholder="默认数值列">
                    <el-option v-for="c in testColumns" :key="c" :label="c" :value="c" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </el-collapse-item>
        </el-collapse>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="宽度(格)">
              <el-input-number v-model="itemForm.width" :min="1" :max="12" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="高度(格)">
              <el-input-number v-model="itemForm.height" :min="1" :max="12" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="itemDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveItem">保存</el-button>
      </template>
    </el-dialog>

    <!-- 全屏组件详情 -->
    <el-dialog v-model="fullscreenVisible" :title="fullscreenItem?.title" width="92%" top="4vh" destroy-on-close append-to-body>
      <div v-if="fullscreenItem" style="height: 72vh">
        <ChartRenderer :item="fullscreenItem" :result="results[fullscreenItem.id!]" :loading="loadingItems[fullscreenItem.id!]" />
      </div>
    </el-dialog>

    <!-- 版本历史 -->
    <el-dialog v-model="versionDialog" title="版本历史" width="720px" destroy-on-close>
      <el-table :data="versions" border size="small">
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column prop="remark" label="发布说明" min-width="160" />
        <el-table-column prop="createBy" label="发布人" width="100" />
        <el-table-column prop="createTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="warning" @click="handleRollback(row)">回滚</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 上线弹窗 -->
    <el-dialog v-model="publishDialog" title="上线仪表板" width="420px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="发布说明">
          <el-input v-model="publishRemark" type="textarea" :rows="3" placeholder="本次发布说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doPublish">确认上线</el-button>
      </template>
    </el-dialog>

    <!-- ============ 分享看板弹窗 ============ -->
    <el-dialog v-model="shareDialog" title="分享看板" width="560px" destroy-on-close>
      <div class="share-tip">通过以下链接分享给团队成员，对方登录后即可在只读（预览）模式下查看该看板。</div>
      <el-input v-model="shareLink" readonly>
        <template #append>
          <el-button :icon="CopyDocument" @click="copyShareLink">复制</el-button>
        </template>
      </el-input>
      <div class="share-actions">
        <el-button :icon="Link" @click="openShareInNew">在预览模式打开</el-button>
        <el-button :icon="CopyDocument" @click="copyEmbed">复制嵌入URL</el-button>
      </div>
      <div class="share-hint">提示：预览模式不显示编辑工具栏，适合对外展示。</div>
    </el-dialog>

    <!-- ============ 下钻明细弹窗 ============ -->
    <el-dialog v-model="drillDialog" :title="drillTitle" width="860px" top="6vh" destroy-on-close append-to-body>
      <div v-loading="drillLoading" style="min-height: 200px">
        <el-table v-if="drillResult?.rows?.length" :data="drillResult.rows" border size="small" max-height="480">
          <el-table-column v-for="c in drillColumns" :key="c" :prop="c" :label="c" min-width="120" show-overflow-tooltip />
        </el-table>
        <el-empty v-else-if="!drillLoading" description="暂无下钻明细" :image-size="60" />
      </div>
      <template #footer>
        <el-button :icon="Download" @click="exportCurrentDrill">导出明细</el-button>
        <el-button type="primary" @click="drillDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  ArrowDown, ArrowLeft, ArrowUp, Bottom, Check, Clock, CopyDocument, Delete, Download, Edit, Filter, FullScreen,
  Link, Lock, Moon, Plus, Rank, Refresh, RefreshLeft, Setting, Share, Sunny, Top,
} from '@element-plus/icons-vue'
import ChartRenderer from '@/components/chart/ChartRenderer.vue'
import {
  createBoard, dashboardVersions, deleteBoard, deleteItem, drillItem, duplicateBoard, executeItem,
  getDashboard, listBoards, listDashboardItems, publishDashboard, rollbackDashboard, saveItem,
  unpublishDashboard, updateBoard, updateDashboard,
} from '@/api/visual'
import { pageDatasources, executeSql } from '@/api/datasource'
import type { ChartType, QueryResult, VisualBoard, VisualDashboard, VisualDashboardItem, VisualDashboardVersion } from '@/types'

const route = useRoute()
const router = useRouter()
const dashboardId = route.params.id as string

const CHART_TYPES = [
  { value: 'BAR', label: '条形图' },
  { value: 'LINE', label: '折线图' },
  { value: 'PIE', label: '饼图' },
  { value: 'SCATTER', label: '散点图' },
  { value: 'HEATMAP', label: '热力图' },
  { value: 'AREA', label: '面积图' },
  { value: 'GAUGE', label: '仪表盘' },
  { value: 'TREEMAP', label: '树形图' },
  { value: 'BOXPLOT', label: '箱型图' },
  { value: 'MAP', label: '地图' },
  { value: 'TABLE', label: '表格' },
  { value: 'NUMBER', label: '数字' },
] as { value: ChartType; label: string }[]

const TIME_RANGES = [
  { value: 'today', label: '今日' },
  { value: 'yesterday', label: '昨日' },
  { value: '7d', label: '近7天' },
  { value: '30d', label: '近30天' },
  { value: 'month', label: '本月' },
  { value: 'lastMonth', label: '上月' },
  { value: 'year', label: '本年度' },
]
const REFRESH_OPTIONS = [
  { value: 0, label: '关闭' },
  { value: 30, label: '30秒' },
  { value: 60, label: '1分钟' },
  { value: 300, label: '5分钟' },
  { value: 600, label: '10分钟' },
  { value: 1800, label: '30分钟' },
]
const BOARD_ICONS = [
  { value: 'DataAnalysis', label: '数据分析' },
  { value: 'TrendCharts', label: '趋势' },
  { value: 'PieChart', label: '占比' },
  { value: 'User', label: '用户' },
  { value: 'Money', label: '营收' },
  { value: 'Odometer', label: '监控' },
]

const dashboard = ref<VisualDashboard>({})
const boards = ref<VisualBoard[]>([])
const items = ref<VisualDashboardItem[]>([])
const datasources = ref<any[]>([])
const loading = ref(false)
const loadingItems = reactive<Record<string, boolean>>({})
const results = reactive<Record<string, QueryResult>>({})
const refreshTimers: Record<string, ReturnType<typeof setInterval>> = {}

const editMode = ref(false)
const isPreview = ref(false)
const saving = ref(false)
const fullscreen = ref(false)
const theme = ref<'light' | 'dark'>('light')
const themeClass = computed(() => (theme.value === 'dark' ? 'theme-dark' : 'theme-light'))
const selectedBoardId = ref<string>('')

const globalFilter = reactive<{ timeRange: string; dimension: string }>({ timeRange: '30d', dimension: '' })

/* ==================== 加载 ==================== */

async function loadAll() {
  loading.value = true
  try {
    dashboard.value = await getDashboard(dashboardId)
    boards.value = await listBoards(dashboardId)
    items.value = await listDashboardItems(dashboardId)
    if (!selectedBoardId.value && boards.value.length) selectedBoardId.value = boards.value[0].id!
    clearAllTimers()
    await loadResults()
  } finally {
    loading.value = false
  }
}

function boardItems(boardId?: string) {
  return items.value.filter((i) => (i.boardId || '') === (boardId || ''))
}

async function loadResults() {
  for (const it of items.value) {
    await refreshItem(it)
  }
  // 启动各分析板自动刷新
  for (const b of boards.value) {
    startBoardAutoRefresh(b)
  }
}

async function refreshItem(item: VisualDashboardItem) {
  if (!item.id || !item.datasourceId || !item.querySql) return
  loadingItems[item.id] = true
  try {
    const filterJson = effectiveBoardFilter(item.boardId)
    results[item.id] = await executeItem(item.id, filterJson)
  } catch {
    // 错误已由拦截器提示
  } finally {
    loadingItems[item.id] = false
  }
}

async function refreshBoard(board: VisualBoard) {
  if (!board.id) return
  for (const it of boardItems(board.id)) {
    await refreshItem(it)
  }
  ElMessage.success(`「${board.boardName}」已刷新`)
}

function startBoardAutoRefresh(board: VisualBoard) {
  const sec = board.refreshInterval != null ? board.refreshInterval : dashboard.value.refreshInterval || 60
  if (refreshTimers['board_' + board.id]) clearInterval(refreshTimers['board_' + board.id])
  if (sec <= 0 || !board.id) return
  refreshTimers['board_' + board.id] = setInterval(() => {
    refreshBoard(board)
  }, sec * 1000)
}

function clearAllTimers() {
  Object.values(refreshTimers).forEach((t) => clearInterval(t))
  Object.keys(refreshTimers).forEach((k) => delete refreshTimers[k])
}

/* ==================== 布局 ==================== */

function boardStyle(board: VisualBoard) {
  let cfg: Record<string, any> = {}
  try { cfg = board.layout ? JSON.parse(board.layout) : {} } catch { cfg = {} }
  const style: Record<string, any> = {
    gridColumn: `span ${cfg.cols || 12}`,
    // 高度自适应内容：至少占 rows 行，内容多时自然撑高，避免组件被压缩
    gridRow: 'span 1',
    minHeight: `${Math.max(cfg.rows || 2, 1) * 220}px`,
  }
  // 板块样式定制：圆角/阴影/边框/背景/内边距
  if (cfg.radius != null) style.borderRadius = cfg.radius + 'px'
  if (cfg.shadow != null && Number(cfg.shadow) > 0) style.boxShadow = `0 ${Number(cfg.shadow) * 2}px ${Number(cfg.shadow) * 6}px rgba(0,0,0,0.12)`
  if (cfg.borderColor) style.borderColor = cfg.borderColor
  if (cfg.borderWidth != null) style.borderWidth = cfg.borderWidth + 'px'
  if (cfg.bgColor) style.background = cfg.bgColor
  if (cfg.padding != null) { style.padding = cfg.padding + 'px'; style.boxSizing = 'border-box' }
  return style
}

function itemGridStyle(item: VisualDashboardItem) {
  const w = Math.min(Math.max(item.width || 6, 1), 12)
  const h = Math.min(Math.max(item.height || 3, 1), 12)
  // 【BI-Dashboard 约束】用户自由指定卡片大小(width/height)，Grid 自动流式排布，
  // 默认绝不重叠；不对 posX/posY 做显式定位（避免跨度冲突导致溢出重叠）。
  return {
    gridColumn: `span ${w}`,
    gridRow: `span ${h}`,
    // 固定行高 140px，组件高度 = span 行数 × 140，保证不重叠
    minHeight: `${h * 140}px`,
  }
}

function itemHeight(_item: VisualDashboardItem): number {
  // 图表容器全部 100% 继承父级，无需传固定高度
  return 0
}

/* ==================== 分析板CRUD ==================== */

const boardDialog = ref(false)
const boardFormRef = ref<FormInstance>()
const editingBoard = ref<VisualBoard | null>(null)
const boardForm = reactive<VisualBoard>({ dashboardId, refreshInterval: 60, locked: 0, collapse: 0, boardType: 'ANALYSIS', icon: 'DataAnalysis' })
/** 板块样式定制表单（合并进 layout JSON 持久化） */
const boardStyleForm = reactive<Record<string, any>>({
  cols: 12, rows: 2, radius: 12, shadow: 1, padding: 0, borderWidth: 1, borderColor: '', bgColor: '',
})
const boardRules: FormRules = {
  boardName: [{ required: true, message: '请输入分析板标题', trigger: 'blur' }],
}

function openCreateBoard() {
  editingBoard.value = null
  Object.assign(boardForm, { dashboardId, boardName: `分析板${boards.value.length + 1}`, subtitle: '', icon: 'DataAnalysis', refreshInterval: 60, locked: 0, collapse: 0, boardType: 'ANALYSIS' })
  Object.assign(boardStyleForm, { cols: 12, rows: 2, radius: 12, shadow: 1, padding: 0, borderWidth: 1, borderColor: '', bgColor: '' })
  boardDialog.value = true
}

function openEditBoard(board: VisualBoard) {
  editingBoard.value = board
  Object.assign(boardForm, {
    id: board.id, dashboardId, boardName: board.boardName, subtitle: board.subtitle, icon: board.icon || 'DataAnalysis',
    refreshInterval: board.refreshInterval ?? 60, locked: board.locked ?? 0, collapse: board.collapse ?? 0, boardType: board.boardType,
  })
  let cfg: Record<string, any> = {}
  try { cfg = board.layout ? JSON.parse(board.layout) : {} } catch { cfg = {} }
  Object.assign(boardStyleForm, {
    cols: cfg.cols || 12, rows: cfg.rows || 2,
    radius: cfg.radius ?? 12, shadow: cfg.shadow ?? 1, padding: cfg.padding ?? 0,
    borderWidth: cfg.borderWidth ?? 1, borderColor: cfg.borderColor || '', bgColor: cfg.bgColor || '',
  })
  boardDialog.value = true
}

async function saveBoard() {
  await boardFormRef.value?.validate()
  saving.value = true
  try {
    // 布局+样式 JSON
    const layout = JSON.stringify({
      cols: boardStyleForm.cols, rows: boardStyleForm.rows,
      radius: boardStyleForm.radius, shadow: boardStyleForm.shadow, padding: boardStyleForm.padding,
      borderWidth: boardStyleForm.borderWidth, borderColor: boardStyleForm.borderColor, bgColor: boardStyleForm.bgColor,
    })
    if (editingBoard.value?.id) {
      await updateBoard({ ...boardForm, layout })
    } else {
      await createBoard({ ...boardForm, layout })
    }
    ElMessage.success('分析板已保存')
    boardDialog.value = false
    await loadAll()
  } finally {
    saving.value = false
  }
}

async function removeBoard(board: VisualBoard) {
  await ElMessageBox.confirm(`确认删除分析板「${board.boardName}」？其下组件将一并删除。`, '提示', { type: 'warning' })
  if (board.id) {
    await deleteBoard(board.id)
    if (refreshTimers['board_' + board.id]) { clearInterval(refreshTimers['board_' + board.id]); delete refreshTimers['board_' + board.id] }
  }
  ElMessage.success('删除成功')
  await loadAll()
}

async function toggleCollapse(board: VisualBoard) {
  await updateBoard({ id: board.id, collapse: board.collapse ? 0 : 1 })
  board.collapse = board.collapse ? 0 : 1
}

async function duplicateSingle(board: VisualBoard) {
  if (!board.id) return
  await duplicateBoard(board.id)
  ElMessage.success('已复制分析板')
  await loadAll()
}

async function duplicateSelectedBoard() {
  if (!selectedBoardId.value) { ElMessage.warning('请先选中一个分析板'); return }
  await duplicateBoard(selectedBoardId.value)
  ElMessage.success('已复制选中分析板')
  await loadAll()
}

async function toggleBoardLock() {
  const b = boards.value.find((x) => x.id === selectedBoardId.value)
  if (!b) { ElMessage.warning('请先选中一个分析板'); return }
  await updateBoard({ id: b.id, locked: b.locked ? 0 : 1 })
  b.locked = b.locked ? 0 : 1
  ElMessage.success(b.locked ? '已锁定布局' : '已解锁布局')
}

/* ==================== 分析板独立筛选（单板优先于全局） ==================== */

const boardFilterDialog = ref(false)
const filterBoard = ref<VisualBoard | null>(null)
/** 分析板独立筛选表单：timeRange+dateColumn+conditions，linkGlobal 控制是否联动全局 */
const boardFilter = reactive<{
  linkGlobal: number
  timeRange: string
  dateColumn: string
  conditions: { field: string; op: string; value: string }[]
}>({ linkGlobal: 1, timeRange: 'all', dateColumn: '', conditions: [] })

function parseBoardFilter(board: VisualBoard): { linkGlobal?: number; timeRange?: string; dateColumn?: string; conditions?: { field: string; op: string; value: string }[] } {
  try {
    return board.filters ? JSON.parse(board.filters) : {}
  } catch {
    return {}
  }
}

/** 判断分析板是否设置了独立筛选 */
function hasBoardFilter(board: VisualBoard): boolean {
  const f = parseBoardFilter(board)
  return !!(f.conditions?.length)
}

function openBoardFilter(board: VisualBoard) {
  filterBoard.value = board
  const f = parseBoardFilter(board)
  Object.assign(boardFilter, {
    linkGlobal: f.linkGlobal ?? board.linkGlobal ?? 1,
    timeRange: f.timeRange || 'all',
    dateColumn: f.dateColumn || '',
    conditions: (f.conditions || []).map((c) => ({ field: c.field || '', op: c.op || 'eq', value: String(c.value ?? '') })),
  })
  boardFilterDialog.value = true
}

async function saveBoardFilter() {
  if (!filterBoard.value?.id) return
  saving.value = true
  try {
    const active = boardFilter.conditions.filter((c) => c.field.trim())
    const payload = {
      timeRange: boardFilter.timeRange,
      dateColumn: boardFilter.dateColumn,
      linkGlobal: boardFilter.linkGlobal,
      conditions: active,
    }
    await updateBoard({ id: filterBoard.value.id, filters: JSON.stringify(payload), linkGlobal: boardFilter.linkGlobal })
    filterBoard.value.filters = JSON.stringify(payload)
    filterBoard.value.linkGlobal = boardFilter.linkGlobal
    ElMessage.success('筛选已应用')
    boardFilterDialog.value = false
    await refreshBoard(filterBoard.value)
  } finally {
    saving.value = false
  }
}

async function clearBoardFilter() {
  if (!filterBoard.value?.id) return
  await updateBoard({ id: filterBoard.value.id, filters: '', linkGlobal: boardFilter.linkGlobal })
  filterBoard.value.filters = ''
  ElMessage.success('已清除本板筛选')
  boardFilterDialog.value = false
  await refreshBoard(filterBoard.value)
}

/**
 * 计算组件所属分析板的生效筛选 JSON（字符串）。
 * 规则：单板筛选优先；若单板关闭联动（linkGlobal=0）则完全忽略全局筛选；
 * 否则时间范围取单板优先、全局兜底。
 */
function effectiveBoardFilter(boardId?: string): string | undefined {
  if (!boardId) return undefined
  const board = boards.value.find((b) => b.id === boardId)
  if (!board) return undefined
  const f = parseBoardFilter(board)
  if (f.linkGlobal === 0) {
    if (!f.conditions?.length && (!f.timeRange || f.timeRange === 'all')) return undefined
    return JSON.stringify({ timeRange: f.timeRange, dateColumn: f.dateColumn, conditions: f.conditions || [] })
  }
  // 联动全局：时间范围单板优先、全局兜底；条件仅用单板
  const timeRange = (f.timeRange && f.timeRange !== 'all') ? f.timeRange : (globalFilter.timeRange || undefined)
  const conditions = f.conditions || []
  if (!conditions.length && !timeRange) return undefined
  return JSON.stringify({ timeRange: timeRange || 'all', dateColumn: f.dateColumn, conditions })
}

/* ==================== 拖拽重排 ==================== */

function onBoardDragStart(e: DragEvent, index: number) {
  e.dataTransfer!.setData('text/plain', String(index))
}
function onBoardDrop(e: DragEvent, index: number) {
  const from = Number(e.dataTransfer?.getData('text/plain'))
  if (Number.isNaN(from) || from === index) return
  const arr = [...boards.value]
  const [moved] = arr.splice(from, 1)
  arr.splice(index, 0, moved)
  boards.value = arr
  syncBoardOrder()
}

function onItemDragStart(e: DragEvent, boardId: string | undefined, index: number) {
  e.dataTransfer!.setData('text/plain', `${boardId || ''}|${index}`)
}
function onItemDrop(e: DragEvent, boardId: string | undefined, index: number) {
  const raw = e.dataTransfer?.getData('text/plain') || ''
  const [fromBoard, fromIndex] = raw.split('|')
  const from = Number(fromIndex)
  const bid = boardId || ''
  if (fromBoard !== bid || Number.isNaN(from) || from === index) return
  const arr = [...items.value.filter((i) => (i.boardId || '') === bid)]
  const [moved] = arr.splice(from, 1)
  arr.splice(index, 0, moved)
  // 写回
  const rest = items.value.filter((i) => (i.boardId || '') !== bid)
  items.value = [...rest, ...arr]
  syncItemOrder()
}

function syncBoardOrder() {
  boards.value.forEach((b, i) => updateBoard({ id: b.id, sortOrder: i }).catch(() => {}))
}

function syncItemOrder() {
  items.value.forEach(async (it, i) => {
    if (it.id) await saveItem({ id: it.id, dashboardId, boardId: it.boardId, title: it.title, chartType: it.chartType, datasourceId: it.datasourceId, querySql: it.querySql, config: it.config, width: it.width, height: it.height, posX: i, posY: 0 }).catch(() => {})
  })
  ElMessage.success('组件顺序已保存')
}

/* ==================== 组件增删改 ==================== */

const itemDialog = ref(false)
const itemFormRef = ref<FormInstance>()
const editingItem = ref<VisualDashboardItem | null>(null)
const testColumns = ref<string[]>([])
const testing = ref(false)
const itemForm = reactive<Record<string, any>>({ dashboardId, boardId: '', title: '', chartType: 'BAR', datasourceId: '', querySql: '', drillSql: '', width: 6, height: 3, config: {} })
const itemRules: FormRules = {
  title: [{ required: true, message: '请输入组件标题', trigger: 'blur' }],
  chartType: [{ required: true, message: '请选择图表类型', trigger: 'change' }],
  datasourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  querySql: [{ required: true, message: '请输入查询SQL', trigger: 'blur' }],
}

function openAddItem(board: VisualBoard) {
  editingItem.value = null
  testColumns.value = []
  Object.assign(itemForm, { dashboardId, boardId: board.id, title: '', chartType: 'BAR', datasourceId: '', querySql: '', width: 6, height: 3, config: {} })
  itemDialog.value = true
}

function openEditItem(item: VisualDashboardItem) {
  editingItem.value = item
  let cfg = {}
  try { cfg = item.config ? JSON.parse(item.config) : {} } catch { cfg = {} }
  testColumns.value = []
  Object.assign(itemForm, {
    id: item.id, dashboardId, boardId: item.boardId, title: item.title, chartType: item.chartType || 'BAR',
    datasourceId: item.datasourceId, querySql: item.querySql, width: item.width || 6, height: item.height || 3, config: { ...cfg },
  })
  itemDialog.value = true
}

async function testQuery() {
  if (!itemForm.datasourceId || !itemForm.querySql) { ElMessage.warning('请先选择数据源并填写SQL'); return }
  testing.value = true
  try {
    const r = await executeSql(itemForm.datasourceId, itemForm.querySql)
    testColumns.value = (r.columns || []).map((c) => c.columnName).filter((n): n is string => !!n)
    ElMessage.success(`查询成功，共 ${(r.rows || []).length} 行`)
  } finally {
    testing.value = false
  }
}

async function handleSaveItem() {
  await itemFormRef.value?.validate()
  saving.value = true
  try {
    const payload: VisualDashboardItem = {
      id: itemForm.id, dashboardId, boardId: itemForm.boardId, title: itemForm.title, chartType: itemForm.chartType,
      datasourceId: itemForm.datasourceId, querySql: itemForm.querySql, config: JSON.stringify(itemForm.config),
      width: itemForm.width, height: itemForm.height,
    }
    await saveItem(payload)
    ElMessage.success('组件已保存')
    itemDialog.value = false
    const reloaded = await listDashboardItems(dashboardId)
    items.value = reloaded
    const target = reloaded.find((i) => i.id === (editingItem.value?.id || payload.id))
    if (target) await refreshItem(target)
  } finally {
    saving.value = false
  }
}

async function removeItem(item: VisualDashboardItem) {
  await ElMessageBox.confirm(`确认删除组件「${item.title}」吗？`, '提示', { type: 'warning' })
  if (item.id) {
    await deleteItem(item.id)
    if (refreshTimers[item.id]) { clearInterval(refreshTimers[item.id]); delete refreshTimers[item.id] }
  }
  items.value = items.value.filter((i) => i.id !== item.id)
  ElMessage.success('删除成功')
}

/* ==================== 保存/筛选/主题 ==================== */

async function saveAll() {
  saving.value = true
  try {
    await updateDashboard({
      id: dashboardId, name: dashboard.value.name, description: dashboard.value.description,
      refreshInterval: dashboard.value.refreshInterval, status: dashboard.value.status === 2 ? 2 : 1,
      layout: JSON.stringify({ theme: theme.value, filter: globalFilter, timestamp: Date.now() }),
    })
    ElMessage.success('草稿已保存')
  } finally {
    saving.value = false
  }
}

function onEditModeChange() {
  if (editMode.value) ElMessage.info('已进入编辑模式，改动实时保存')
}

function onFilterChange() {
  ElMessage.info(`全局筛选已应用：${globalFilter.timeRange}${globalFilter.dimension ? ' / ' + globalFilter.dimension : ''}`)
}

function resetFilter() {
  globalFilter.timeRange = '30d'
  globalFilter.dimension = ''
  ElMessage.success('筛选已重置')
}

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
}

/* ==================== 全屏详情 ==================== */

const fullscreenVisible = ref(false)
const fullscreenItem = ref<VisualDashboardItem | null>(null)
function openFullscreen(item: VisualDashboardItem) {
  fullscreenItem.value = item
  fullscreenVisible.value = true
}

/* ==================== 数据导出 ==================== */

function resultToCSV(result: QueryResult | null | undefined): string {
  if (!result || !result.columns?.length) return ''
  const cols = result.columns.map((c) => c.columnName ?? c.description ?? '')
  const esc = (v: any) => {
    if (v == null) return ''
    const s = String(v)
    return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s
  }
  const lines = result.rows.map((row) => cols.map((c) => esc(row[c])).join(','))
  return [cols.join(','), ...lines].join('\n')
}

function downloadCSV(content: string, filename: string) {
  const blob = new Blob(['\ufeff' + content], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

function exportItem(item: VisualDashboardItem) {
  const r = results[item.id!]
  if (!r || !r.rows?.length) { ElMessage.warning('当前组件暂无数据可导出'); return }
  downloadCSV(resultToCSV(r), `${item.title || '组件数据'}.csv`)
}

/* ==================== 下钻明细 ==================== */

const drillDialog = ref(false)
const drillLoading = ref(false)
const drillTitle = ref('')
const drillResult = ref<QueryResult>()
const drillColumns = computed(() => (drillResult.value?.columns || []).map((c) => c.columnName ?? c.description ?? ''))

async function onDrill(item: VisualDashboardItem, payload: { column: string; value: string }) {
  if (!item.id) return
  drillTitle.value = `${item.title || '组件'} · ${payload.column} = ${payload.value}`
  drillDialog.value = true
  drillLoading.value = true
  drillResult.value = undefined
  try {
    const filterJson = effectiveBoardFilter(item.boardId)
    drillResult.value = await drillItem(item.id, { column: payload.column, value: payload.value, filters: filterJson })
    if (!drillResult.value?.rows?.length) ElMessage.info('该数据点暂无下钻明细')
  } catch {
    drillDialog.value = false
  } finally {
    drillLoading.value = false
  }
}

function exportCurrentDrill() {
  downloadCSV(resultToCSV(drillResult.value), `${drillTitle.value || '下钻明细'}.csv`)
}

/* ==================== 看板分享 ==================== */

const shareDialog = ref(false)
const shareLink = ref('')

function openShare() {
  shareLink.value = `${location.origin}/visual/dashboard/edit/${dashboardId}?preview=1`
  shareDialog.value = true
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

function copyShareLink() { copyText(shareLink.value) }
function copyEmbed() { copyText(`<iframe src="${shareLink.value}" width="1200" height="800" frameborder="0"></iframe>`) }
function openShareInNew() { window.open(shareLink.value, '_blank') }

/* ==================== 上线/版本 ==================== */

const publishDialog = ref(false)
const publishRemark = ref('')
function openPublish() { publishRemark.value = ''; publishDialog.value = true }
async function doPublish() {
  saving.value = true
  try {
    const v = await publishDashboard(dashboardId, publishRemark.value)
    ElMessage.success(`已上线，版本号 v${v}`)
    publishDialog.value = false
    await loadAll()
  } finally {
    saving.value = false
  }
}
async function handleUnpublish() {
  await ElMessageBox.confirm('确认下线该仪表板？下线后可继续编辑草稿。', '提示', { type: 'warning' })
  await unpublishDashboard(dashboardId)
  ElMessage.success('已下线')
  await loadAll()
}

const versionDialog = ref(false)
const versions = ref<VisualDashboardVersion[]>([])
async function openVersions() {
  versions.value = await dashboardVersions(dashboardId)
  versionDialog.value = true
}
async function handleRollback(row: VisualDashboardVersion) {
  await ElMessageBox.confirm(`确认回滚到版本 v${row.version}？当前草稿将被覆盖。`, '回滚', { type: 'warning' })
  await rollbackDashboard(dashboardId, row.id!)
  ElMessage.success('回滚成功')
  versionDialog.value = false
  await loadAll()
}

/* ==================== 图标/颜色 ==================== */

const accentColor = computed(() => (theme.value === 'dark' ? '#409eff' : '#4f9df9'))
function boardIcon(board: VisualBoard) {
  const map: Record<string, any> = {
    DataAnalysis: 'DataAnalysis', TrendCharts: 'TrendCharts', PieChart: 'PieChart', User: 'User', Money: 'Money', Odometer: 'Odometer',
  }
  return map[board.icon || 'DataAnalysis'] || 'DataAnalysis'
}

/* ==================== 初始化 ==================== */

async function loadDatasources() {
  const r = await pageDatasources({ current: 1, size: 100 })
  datasources.value = r.records || []
}

onMounted(() => {
  // 分享/预览模式：强制只读，隐藏编辑切换与创作入口
  if (route.query.preview === '1') {
    editMode.value = false
    isPreview.value = true
  }
  loadDatasources()
  loadAll()
})
</script>

<style scoped>
/* ============ 主题变量 ============ */
.board-page {
  --bg: #f5f7fa;
  --card-bg: #ffffff;
  --card-border: #ebeef5;
  --text-1: #303133;
  --text-2: #606266;
  --text-3: #909399;
  --header-bg: #ffffff;
  --toolbar-bg: #f5f7fa;
  --hover-bg: #f0f2f5;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  color: var(--text-1);
  overflow: hidden;
  transition: all 0.3s;
}
.board-page.theme-dark {
  --bg: #14171f;
  --card-bg: #1d2129;
  --card-border: #2a2f3a;
  --text-1: #e5eaf3;
  --text-2: #a3abb9;
  --text-3: #6b7280;
  --header-bg: #1d2129;
  --toolbar-bg: #1a1e26;
  --hover-bg: #262b36;
}
.board-page.is-fullscreen {
  height: 100vh;
}

/* ============ 头部 ============ */
.board-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 8px 16px;
  background: var(--header-bg);
  border-bottom: 1px solid var(--card-border);
}
.board-title { display: flex; align-items: center; gap: 12px; min-width: 0; }
.board-name { font-size: 18px; font-weight: 600; display: flex; align-items: center; gap: 8px; color: var(--text-1); }
.board-desc { color: var(--text-3); font-size: 12px; margin-top: 2px; }
.board-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

/* ============ 全局筛选栏 ============ */
.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  padding: 8px 16px;
  background: var(--header-bg);
  border-bottom: 1px solid var(--card-border);
}
.filter-item { display: flex; align-items: center; gap: 6px; }
.filter-label { color: var(--text-2); font-size: 13px; }
.filter-tip { margin-left: auto; color: var(--text-3); font-size: 12px; }

/* ============ 工具栏 ============ */
.board-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: var(--toolbar-bg);
  border-bottom: 1px solid var(--card-border);
}
.toolbar-hint { color: var(--text-3); font-size: 12px; }

/* ============ 画布 ============ */
.board-canvas {
  flex: 1;
  overflow: auto;
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  /* 行高严格按内容撑开；不得用 min-content/固定值，否则分析板内容溢出压盖后续板 */
  grid-auto-rows: max-content;
  grid-auto-flow: row;
  gap: 16px;
  padding: 16px;
  align-content: start;
  align-items: start;
}

/* ============ 分析板 ============ */
.board-cell {
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: visible;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: border-color 0.2s, box-shadow 0.2s;
}
.board-cell.is-edit { border-color: var(--accent, #4f9df9); border-style: dashed; }
.board-cell.is-selected { box-shadow: 0 0 0 2px var(--accent, #4f9df9); }
.board-header-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 8px;
  padding: 8px 12px;
  background: linear-gradient(90deg, rgba(79, 157, 249, 0.08), transparent);
  border-bottom: 1px solid var(--card-border);
  cursor: grab;
  user-select: none;
}
.board-header-bar:active { cursor: grabbing; }
.board-drag-handle { color: var(--text-3); opacity: 0; transition: opacity 0.2s; }
.board-cell.is-edit .board-drag-handle { opacity: 1; }
.board-name { font-size: 14px; font-weight: 600; color: var(--text-1); }
.board-subtitle { color: var(--text-3); }
.board-ops { margin-left: auto; display: flex; align-items: center; }

/* ============ 分析板体 + 组件 ============ */
.board-body {
  flex: 1 0 auto;
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  /* 【BI-Dashboard 约束】固定行高，行高确定则组件 span 高度确定，绝不重叠；
     组件高度 = span 行数 × 140px */
  grid-auto-rows: 140px;
  /* 普通流式排列，不自动回填空隙(dense)，避免卡片被 Grid 重新排布造成堆叠 */
  grid-auto-flow: row;
  gap: 10px;
  padding: 10px;
  /* 禁止滚动约束：board 随内容增高，行高由 max-content 撑开，卡片不会溢出压盖其他分析板 */
  overflow: visible;
}
.board-grid-item {
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  /* 【BI-Dashboard 约束】卡片容器：overflow hidden + position relative，防止图表互相压盖、z-index 越界 */
  overflow: hidden;
  position: relative;
  isolation: isolate;
  min-height: 0;
}
.board-grid-item.is-edit { border-color: var(--accent, #4f9df9); border-style: dashed; cursor: move; }
.item-header { display: flex; justify-content: space-between; align-items: center; padding: 5px 8px; border-bottom: 1px solid var(--card-border); }
.item-title { font-size: 12px; font-weight: 600; color: var(--text-1); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-ops { display: flex; align-items: center; }
.item-body { flex: 1; min-height: 0; }
.col-tip { margin-left: 8px; color: var(--text-3); font-size: 12px; }
.board-filter-tag { margin-left: 2px; }
.filter-tip { color: var(--text-3); font-size: 12px; margin-top: 4px; }
.filter-cond-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.style-color-row { display: flex; align-items: center; gap: 8px; }
.share-tip { color: var(--text-2); font-size: 13px; margin-bottom: 12px; }
.share-actions { margin-top: 12px; display: flex; gap: 8px; }
.share-hint { margin-top: 12px; color: var(--text-3); font-size: 12px; }

/* ============ 响应式：PC / 平板 / 移动端 ============ */
/* 小屏幕(<1200px)：画布降为 6 列，分析板默认占满 */
@media (max-width: 1200px) {
  .board-canvas { grid-template-columns: repeat(6, 1fr); gap: 12px; padding: 12px; }
  .board-cell { grid-column: span 6 !important; }
  .board-body { grid-template-columns: repeat(6, 1fr); }
  .board-grid-item { grid-column: span 6 !important; }
}
/* 平板(<900px)：分析板与组件占满整行 */
@media (max-width: 900px) {
  .board-canvas { grid-template-columns: 1fr; }
  .board-cell { grid-column: 1 / -1 !important; }
  .board-body { grid-template-columns: repeat(2, 1fr); }
  .board-grid-item { grid-column: span 1 !important; }
  .board-name { font-size: 16px; }
  .board-actions .el-button { padding: 6px 10px; }
}
/* 移动端(<600px)：组件单列堆叠，头部紧凑 */
@media (max-width: 600px) {
  .board-canvas { grid-template-columns: 1fr; padding: 8px; gap: 10px; }
  .board-body { grid-template-columns: 1fr; }
  .board-cell { grid-column: 1 / -1 !important; }
  .board-grid-item { grid-column: 1 / -1 !important; }
  .board-header { padding: 6px 10px; }
  .board-title { gap: 8px; }
  .board-name { font-size: 15px; }
  .board-desc { display: none; }
  .filter-bar { gap: 10px; padding: 6px 10px; }
  .filter-item .el-select { width: 110px !important; }
  .board-header-bar { padding: 6px 8px; }
  .board-subtitle { display: none; }
  .board-ops .el-button { padding: 4px 6px; }
  .item-header { padding: 4px 6px; }
  .metric-card { padding: 4px 10px; }
  .board-actions .el-button { padding: 5px 8px; }
  .board-actions .el-button + .el-button { margin-left: 0; }
}
</style>