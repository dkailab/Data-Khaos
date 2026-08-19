<template>
  <div class="bi-dashboard-editor" :class="[`theme-${theme}`, { 'is-fullscreen': fullscreen }]">
    <!-- ==================== 顶部栏 Header Bar ==================== -->
    <header class="editor-header">
      <div class="header-left">
        <div class="header-back" @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
        </div>
        <div class="header-logo">
          <el-icon :size="20"><DataAnalysis /></el-icon>
        </div>
        <div class="header-name" @dblclick="editingName = true">
          <el-input
            v-if="editingName"
            ref="nameInputRef"
            v-model="dashboard.name"
            size="small"
            style="width: 240px"
            @blur="editingName = false"
            @keyup.enter="editingName = false"
          />
          <span v-else class="name-text">{{ dashboard.name || '未命名仪表板' }}</span>
          <el-tag v-if="dashboard.status === 2" type="success" size="small" effect="plain">已发布</el-tag>
          <el-tag v-else type="warning" size="small" effect="plain">草稿</el-tag>
        </div>
      </div>

      <!-- 分析板 Tab 栏 -->
      <div class="header-tabs">
        <div class="tabs-wrapper">
          <div
            v-for="(board, idx) in boards"
            :key="board.id"
            class="tab-item"
            :class="{ active: activeBoardId === board.id, editing: editingTabId === board.id }"
            draggable="true"
            @click="activeBoardId = board.id"
            @dblclick.stop="startEditTab(board)"
            @dragstart="onTabDragStart($event, idx)"
            @dragover.prevent="onTabDragOver"
            @drop="onTabDrop($event, idx)"
          >
            <el-icon class="tab-icon"><component :is="board.icon || 'DataAnalysis'" /></el-icon>
            <el-input
              v-if="editingTabId === board.id"
              ref="tabInputRef"
              v-model="board.name"
              size="small"
              style="width: 100px"
              @blur="editingTabId = ''"
              @keyup.enter="editingTabId = ''"
              @click.stop
            />
            <span v-else class="tab-name">{{ board.name }}</span>
            <span class="tab-close" @click.stop="closeBoard(board)">×</span>
          </div>
          <div class="tab-add" @click="addBoard">
            <el-icon><Plus /></el-icon>
          </div>
        </div>
      </div>

      <!-- 右侧操作区 -->
      <div class="header-right">
        <div class="header-zoom">
          <el-button :icon="ZoomOut" circle size="small" @click="zoomOut" />
          <span class="zoom-value">{{ Math.round(zoom * 100) }}%</span>
          <el-button :icon="ZoomIn" circle size="small" @click="zoomIn" />
        </div>
        <el-divider direction="vertical" />
        <el-tooltip content="撤销 (Ctrl+Z)">
          <el-button :icon="RefreshLeft" circle size="small" :disabled="!canUndo" @click="undo" />
        </el-tooltip>
        <el-tooltip content="重做 (Ctrl+Y)">
          <el-button :icon="RefreshRight" circle size="small" :disabled="!canRedo" @click="redo" />
        </el-tooltip>
        <el-tooltip content="保存 (Ctrl+S)">
          <el-button :icon="Document" circle size="small" :loading="saving" @click="saveAll" />
        </el-tooltip>
        <el-divider direction="vertical" />
        <el-tooltip content="切换主题">
          <el-button :icon="theme === 'dark' ? Sunny : Moon" circle size="small" @click="toggleTheme" />
        </el-tooltip>
        <el-tooltip content="全屏">
          <el-button :icon="FullScreen" circle size="small" @click="fullscreen = !fullscreen" />
        </el-tooltip>
        <el-button :icon="View" size="small" @click="preview">预览</el-button>
        <el-button type="primary" size="small" :icon="Check" :loading="saving" @click="saveAll">保存</el-button>
        <el-button v-if="dashboard.status !== 2" type="success" size="small" :icon="Top" @click="publishDialog = true">发布</el-button>
      </div>
    </header>

    <!-- ==================== 主体区域 ==================== -->
    <div class="editor-body">
      <!-- ==================== 左侧面板 ==================== -->
      <aside class="editor-aside left-panel">
        <div class="aside-tabs">
          <div
            v-for="tab in leftTabs"
            :key="tab.key"
            class="aside-tab"
            :class="{ active: leftTab === tab.key }"
            @click="leftTab = tab.key"
          >
            <el-icon><component :is="tab.icon" /></el-icon>
            <span>{{ tab.label }}</span>
          </div>
        </div>

        <!-- 组件库 -->
        <div v-show="leftTab === 'components'" class="panel-content">
          <div class="search-box">
            <el-input v-model="componentSearch" placeholder="搜索图表类型..." size="small" :prefix-icon="Search" />
          </div>
          <div class="component-grid">
            <div
              v-for="comp in filteredComponents"
              :key="comp.type"
              class="comp-card"
              draggable="true"
              @dragstart="onCompDragStart($event, comp)"
              @dragend="dragCompType = ''"
              @click="addComponent(comp.type)"
            >
              <div class="comp-icon-wrap" :style="{ background: comp.color }">
                <el-icon :size="18"><component :is="comp.icon" /></el-icon>
              </div>
              <span class="comp-name">{{ comp.label }}</span>
            </div>
          </div>
        </div>

        <!-- 数据集字段池 -->
        <div v-show="leftTab === 'dataset'" class="panel-content dataset-panel">
          <div class="dataset-selector">
            <el-select
              v-model="selectedDatasetId"
              size="small"
              filterable
              placeholder="选择数据集"
              style="width: 100%"
              @change="onDatasetChange"
            >
              <el-option v-for="ds in datasets" :key="ds.id" :label="ds.name" :value="ds.id" />
            </el-select>
          </div>
          <div v-if="activeDataset" class="field-pool">
            <div class="pool-group">
              <div class="pool-group-title"><el-icon><Menu /></el-icon> 维度</div>
              <div
                v-for="f in dimensionFields"
                :key="f.fieldCode"
                class="field-item dimension"
                draggable="true"
                @dragstart="onFieldDragStart($event, f, 'DIMENSION')"
              >
                <el-icon><Files /></el-icon>
                <span>{{ f.fieldName }}</span>
                <span class="field-code">{{ f.fieldCode }}</span>
              </div>
            </div>
            <div class="pool-group">
              <div class="pool-group-title"><el-icon><TrendCharts /></el-icon> 指标</div>
              <div
                v-for="f in metricFields"
                :key="f.fieldCode"
                class="field-item metric"
                draggable="true"
                @dragstart="onFieldDragStart($event, f, 'METRIC')"
              >
                <el-icon><DataLine /></el-icon>
                <span>{{ f.fieldName }}</span>
                <span class="field-code">{{ f.fieldCode }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="请先选择数据集" :image-size="60" />
        </div>

        <!-- 图层管理 -->
        <div v-show="leftTab === 'layers'" class="panel-content layer-panel">
          <div class="layer-list">
            <div
              v-for="item in reverseBoardItems"
              :key="item.id"
              class="layer-item"
              :class="{ active: selectedIds.includes(item.id!), locked: item.locked }"
              @click="selectItem(item.id!)"
            >
              <el-icon><component :is="getChartIcon(item.chartType)" /></el-icon>
              <span class="layer-name">{{ item.title || '未命名' }}</span>
              <div class="layer-ops">
                <el-button link :icon="item.locked ? Lock : Unlock" size="small" @click.stop="toggleLock(item)" />
                <el-button link :icon="Delete" size="small" @click.stop="removeItem(item)" />
              </div>
            </div>
          </div>
          <el-empty v-if="!boardItems.length" description="暂无组件" :image-size="50" />
        </div>
      </aside>

      <!-- ==================== 中间画布区 ==================== -->
      <main class="editor-canvas-area" ref="canvasAreaEl">
        <div class="canvas-toolbar">
          <div class="toolbar-left">
            <el-checkbox v-model="showGrid" size="small">网格</el-checkbox>
            <el-checkbox v-model="snapToGrid" size="small">吸附</el-checkbox>
            <span class="canvas-size-info">{{ canvasWidth }} × {{ canvasHeight }} px</span>
          </div>
          <div class="toolbar-right">
            <el-button :icon="Plus" size="small" @click="addComponent('BAR')">+ 图表</el-button>
            <el-select v-model="canvasSizePreset" size="small" style="width: 130px" @change="onCanvasSizeChange">
              <el-option v-for="s in CANVAS_PRESETS" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
          </div>
        </div>

        <div class="canvas-wrapper" @wheel.prevent="onWheel">
          <div
            ref="canvasEl"
            class="editor-canvas"
            :style="canvasStyle"
            @mousedown="onCanvasMouseDown"
            @mousemove="onCanvasMouseMove"
            @mouseup="onCanvasMouseUp"
            @dragover.prevent="onCanvasDragOver"
            @drop="onCanvasDrop"
          >
            <!-- 背景网格 -->
            <div v-if="showGrid" class="grid-bg" :style="gridStyle"></div>

            <!-- 组件渲染 -->
            <div
              v-for="item in visibleBoardItems"
              :key="item.id"
              class="canvas-item"
              :class="{ selected: selectedIds.includes(item.id!), editing: selectedIds.length === 1 && selectedIds[0] === item.id, locked: item.locked }"
              :style="getItemStyle(item)"
              @mousedown.stop="onItemMouseDown($event, item)"
              @click.stop="selectItem(item.id!)"
              @dblclick.stop="openItemConfig(item)"
            >
              <!-- 组件头部拖动条 -->
              <div class="item-header" :class="{ 'has-title': item.title }">
                <span class="item-drag-handle">⋮⋮</span>
                <span class="item-title-text">{{ item.title || getDefaultTitle(item.chartType) }}</span>
              </div>
              <!-- 组件内容 -->
              <div class="item-body">
                <ChartRenderer
                  v-if="item.id && results[item.id]"
                  :item="item"
                  :result="results[item.id]"
                  :loading="loadingItems[item.id]"
                  :theme="theme"
                  @drill="onDrill(item, $event)"
                />
                <div v-else class="item-placeholder">
                  <el-icon :size="28"><component :is="getChartIcon(item.chartType)" /></el-icon>
                  <span>拖入维度/指标或<a @click.stop="openItemConfig(item)">配置数据源</a></span>
                </div>
              </div>
              <!-- 选中手柄 -->
              <template v-if="selectedIds.includes(item.id!) && !item.locked">
                <div class="resize-handle n" @mousedown.stop="onResizeStart($event, item, 'n')" />
                <div class="resize-handle s" @mousedown.stop="onResizeStart($event, item, 's')" />
                <div class="resize-handle e" @mousedown.stop="onResizeStart($event, item, 'e')" />
                <div class="resize-handle w" @mousedown.stop="onResizeStart($event, item, 'w')" />
                <div class="resize-handle ne" @mousedown.stop="onResizeStart($event, item, 'ne')" />
                <div class="resize-handle nw" @mousedown.stop="onResizeStart($event, item, 'nw')" />
                <div class="resize-handle se" @mousedown.stop="onResizeStart($event, item, 'se')" />
                <div class="resize-handle sw" @mousedown.stop="onResizeStart($event, item, 'sw')" />
              </template>
              <!-- 悬浮操作 -->
              <div v-if="selectedIds.includes(item.id!)" class="item-actions">
                <el-tooltip content="配置"><el-button :icon="Setting" circle size="small" @click.stop="openItemConfig(item)" /></el-tooltip>
                <el-tooltip content="复制"><el-button :icon="CopyDocument" circle size="small" @click.stop="duplicateItem(item)" /></el-tooltip>
                <el-tooltip content="删除"><el-button :icon="Delete" circle size="small" type="danger" @click.stop="removeItem(item)" /></el-tooltip>
              </div>
            </div>

            <!-- 框选区域 -->
            <div v-if="selectionBox" class="selection-box" :style="selectionBoxStyle" />

            <!-- 对齐辅助线 -->
            <div v-for="(guide, gi) in activeGuides" :key="gi" class="align-guide" :class="guide.type" :style="guide.style" />
          </div>
        </div>
      </main>

      <!-- ==================== 右侧属性面板 ==================== -->
      <aside class="editor-aside right-panel">
        <!-- 无选中显示画布属性 -->
        <template v-if="selectedItems.length === 0">
          <div class="config-tabs">
            <div class="config-tab active">画布</div>
          </div>
          <div class="config-content">
            <ConfigGroup title="画布设置">
              <ConfigRow label="宽度"><el-input-number v-model="canvasWidth" :min="800" :max="7680" size="small" @change="pushHistory" /></ConfigRow>
              <ConfigRow label="高度"><el-input-number v-model="canvasHeight" :min="600" :max="4320" size="small" @change="pushHistory" /></ConfigRow>
              <ConfigRow label="背景色"><el-color-picker v-model="canvasBg" size="small" @change="pushHistory" /></ConfigRow>
              <ConfigRow label="网格吸附"><el-switch v-model="snapToGrid" size="small" /></ConfigRow>
            </ConfigGroup>
            <ConfigGroup title="仪表板信息">
              <ConfigRow label="名称">
                <el-input v-model="dashboard.name" size="small" />
              </ConfigRow>
              <ConfigRow label="描述">
                <el-input v-model="dashboard.description" type="textarea" :rows="2" size="small" />
              </ConfigRow>
              <ConfigRow label="刷新间隔">
                <el-input-number v-model="dashboard.refreshInterval" :min="0" :max="86400" size="small" /> 秒
              </ConfigRow>
            </ConfigGroup>
          </div>
        </template>

        <!-- 单选组件配置 -->
        <template v-if="selectedItems.length === 1">
          <div class="config-tabs">
            <div
              v-for="tab in configTabs"
              :key="tab.key"
              class="config-tab"
              :class="{ active: configTab === tab.key }"
              @click="configTab = tab.key"
            >
              <el-icon><component :is="tab.icon" /></el-icon>
              {{ tab.label }}
            </div>
          </div>

          <!-- 数据配置 -->
          <div v-show="configTab === 'data'" class="config-content">
            <ConfigGroup title="数据源">
              <ConfigRow label="数据集">
                <el-select v-model="dataConfig.datasetId" size="small" filterable style="width: 100%" @change="onDatasetSelectChange">
                  <el-option v-for="ds in datasets" :key="ds.id" :label="ds.name" :value="ds.id" />
                </el-select>
              </ConfigRow>
              <ConfigRow label="自定义SQL">
                <el-switch v-model="dataConfig.useCustomSql" size="small" />
              </ConfigRow>
              <ConfigRow v-if="dataConfig.useCustomSql" label="SQL">
                <el-input v-model="dataConfig.customSql" type="textarea" :rows="4" size="small" placeholder="SELECT ..." />
              </ConfigRow>
              <ConfigRow label="数据限制">
                <el-input-number v-model="dataConfig.limit" :min="1" :max="10000" size="small" /> 条
              </ConfigRow>
            </ConfigGroup>

            <!-- 维度配置 -->
            <ConfigGroup title="维度 (X轴/类别)">
              <div class="field-config-list">
                <div v-for="(dim, di) in dataConfig.dimensions" :key="di" class="field-config-item">
                  <el-icon class="drag-dot">⋮⋮</el-icon>
                  <el-select v-model="dim.fieldCode" size="small" style="flex:1" allow-create>
                    <el-option v-for="f in availableDimensionFields" :key="f.fieldCode" :label="f.fieldName" :value="f.fieldCode" />
                  </el-select>
                  <el-select v-model="dim.sort" size="small" style="width:70px">
                    <el-option label="默认" value="" />
                    <el-option label="升序" value="ASC" />
                    <el-option label="降序" value="DESC" />
                  </el-select>
                  <el-button :icon="Delete" link size="small" type="danger" @click="dataConfig.dimensions!.splice(di, 1)" />
                </div>
              </div>
              <el-button :icon="Plus" size="small" link @click="addDimension">添加维度</el-button>
            </ConfigGroup>

            <!-- 指标配置 -->
            <ConfigGroup title="指标 (Y轴/值)">
              <div class="field-config-list">
                <div v-for="(m, mi) in dataConfig.metrics" :key="mi" class="field-config-item">
                  <el-icon class="drag-dot">⋮⋮</el-icon>
                  <el-select v-model="m.fieldCode" size="small" style="flex:1" allow-create>
                    <el-option v-for="f in availableMetricFields" :key="f.fieldCode" :label="f.fieldName" :value="f.fieldCode" />
                  </el-select>
                  <el-select v-model="m.aggType" size="small" style="width:80px">
                    <el-option label="求和" value="SUM" />
                    <el-option label="平均" value="AVG" />
                    <el-option label="计数" value="COUNT" />
                    <el-option label="去重" value="COUNT_DISTINCT" />
                    <el-option label="最大" value="MAX" />
                    <el-option label="最小" value="MIN" />
                  </el-select>
                  <el-button :icon="Delete" link size="small" type="danger" @click="dataConfig.metrics!.splice(mi, 1)" />
                </div>
              </div>
              <el-button :icon="Plus" size="small" link @click="addMetric">添加指标</el-button>
            </ConfigGroup>

            <!-- 图表类型 -->
            <ConfigGroup title="图表类型">
              <div class="chart-type-grid">
                <div
                  v-for="ct in CHART_TYPES"
                  :key="ct.type"
                  class="chart-type-item"
                  :class="{ active: selectedItems[0].chartType === ct.type }"
                  @click="selectedItems[0].chartType = ct.type"
                >
                  <el-icon :size="16"><component :is="ct.icon" /></el-icon>
                  <span>{{ ct.label }}</span>
                </div>
              </div>
            </ConfigGroup>
          </div>

          <!-- 样式配置 -->
          <div v-show="configTab === 'style'" class="config-content">
            <ConfigGroup title="标题">
              <ConfigRow label="显示标题"><el-switch v-model="styleConfig.title!.show" size="small" /></ConfigRow>
              <ConfigRow label="标题文字"><el-input v-model="styleConfig.title!.text" size="small" /></ConfigRow>
              <ConfigRow label="对齐">
                <el-radio-group v-model="styleConfig.title!.align" size="small">
                  <el-radio-button value="left">左</el-radio-button>
                  <el-radio-button value="center">中</el-radio-button>
                  <el-radio-button value="right">右</el-radio-button>
                </el-radio-group>
              </ConfigRow>
              <ConfigRow label="字号"><el-input-number v-model="styleConfig.title!.fontSize" :min="10" :max="36" size="small" /></ConfigRow>
            </ConfigGroup>
            <ConfigGroup title="配色">
              <ConfigRow label="主题">
                <el-select v-model="styleConfig.colorTheme" size="small" style="width: 100%">
                  <el-option label="默认蓝" value="default" />
                  <el-option label="商务蓝" value="business" />
                  <el-option label="科技感" value="tech" />
                  <el-option label="暖色调" value="warm" />
                  <el-option label="清新绿" value="fresh" />
                </el-select>
              </ConfigRow>
            </ConfigGroup>
            <ConfigGroup title="容器">
              <ConfigRow label="背景色"><el-color-picker v-model="styleConfig.bgColor" size="small" /></ConfigRow>
              <ConfigRow label="圆角"><el-input-number v-model="styleConfig.borderRadius" :min="0" :max="24" size="small" /></ConfigRow>
              <ConfigRow label="边框"><el-input-number v-model="styleConfig.borderWidth" :min="0" :max="4" size="small" /></ConfigRow>
            </ConfigGroup>
            <ConfigGroup title="图例">
              <ConfigRow label="显示"><el-switch v-model="styleConfig.legend!.show" size="small" /></ConfigRow>
              <ConfigRow label="位置">
                <el-select v-model="styleConfig.legend!.position" size="small">
                  <el-option label="顶部" value="top" />
                  <el-option label="底部" value="bottom" />
                  <el-option label="左侧" value="left" />
                  <el-option label="右侧" value="right" />
                </el-select>
              </ConfigRow>
            </ConfigGroup>
            <ConfigGroup title="数值">
              <ConfigRow label="显示标签"><el-switch v-model="styleConfig.labelShow" size="small" /></ConfigRow>
              <ConfigRow label="格式">
                <el-select v-model="styleConfig.valueFormat" size="small">
                  <el-option label="原始" value="NONE" />
                  <el-option label="万" value="WAN" />
                  <el-option label="亿" value="YI" />
                  <el-option label="百分比" value="PERCENT" />
                </el-select>
              </ConfigRow>
              <ConfigRow label="小数位"><el-input-number v-model="styleConfig.decimalDigits" :min="0" :max="6" size="small" /></ConfigRow>
            </ConfigGroup>
          </div>

          <!-- 过滤配置 -->
          <div v-show="configTab === 'filter'" class="config-content">
            <ConfigGroup title="数据过滤">
              <div v-for="(f, fi) in dataConfig.filters" :key="fi" class="filter-item">
                <el-input v-model="f.fieldCode" size="small" placeholder="字段编码" style="width:100px" />
                <el-select v-model="f.operator" size="small" style="width:90px">
                  <el-option label="等于" value="EQ" />
                  <el-option label="不等于" value="NE" />
                  <el-option label="大于" value="GT" />
                  <el-option label="小于" value="LT" />
                  <el-option label="包含" value="IN" />
                  <el-option label="区间" value="BETWEEN" />
                </el-select>
                <el-input v-model="f.values" size="small" placeholder="值(逗号分隔)" style="flex:1" />
                <el-button :icon="Delete" link size="small" type="danger" @click="dataConfig.filters!.splice(fi, 1)" />
              </div>
              <el-button :icon="Plus" size="small" link @click="addFilter">添加过滤条件</el-button>
            </ConfigGroup>
            <ConfigGroup title="范围">
              <ConfigRow label="数据范围">
                <el-radio-group v-model="filterScope" size="small">
                  <el-radio value="current">仅当前组件</el-radio>
                  <el-radio value="board">整个分析板</el-radio>
                </el-radio-group>
              </ConfigRow>
            </ConfigGroup>
          </div>
        </template>

        <!-- 多选批量操作 -->
        <template v-if="selectedItems.length > 1">
          <div class="config-tabs"><div class="config-tab active">批量操作 ({{ selectedItems.length }})</div></div>
          <div class="config-content">
            <ConfigGroup title="对齐">
              <div class="align-grid">
                <el-button size="small" @click="batchAlign('left')">左对齐</el-button>
                <el-button size="small" @click="batchAlign('hcenter')">水平居中</el-button>
                <el-button size="small" @click="batchAlign('right')">右对齐</el-button>
                <el-button size="small" @click="batchAlign('top')">顶部对齐</el-button>
                <el-button size="small" @click="batchAlign('vcenter')">垂直居中</el-button>
                <el-button size="small" @click="batchAlign('bottom')">底部对齐</el-button>
              </div>
            </ConfigGroup>
            <ConfigGroup title="分布">
              <el-button size="small" @click="batchDistribute('horizontal')">水平等距</el-button>
              <el-button size="small" @click="batchDistribute('vertical')">垂直等距</el-button>
            </ConfigGroup>
            <ConfigGroup title="删除">
              <el-button type="danger" size="small" :icon="Delete" @click="batchDelete">批量删餘 ({{ selectedItems.length }})</el-button>
            </ConfigGroup>
          </div>
        </template>
      </aside>
    </div>

    <!-- 下钻弹窗 -->
    <el-dialog v-model="drillDialog" :title="drillTitle" width="800px" top="8vh" destroy-on-close>
      <div v-loading="drillLoading" style="min-height: 200px">
        <el-table v-if="drillResult?.rows?.length" :data="drillResult.rows" border size="small" max-height="400">
          <el-table-column v-for="c in drillColumns" :key="c" :prop="c" :label="c" min-width="120" />
        </el-table>
        <el-empty v-else-if="!drillLoading" description="暂无明细" :image-size="60" />
      </div>
      <template #footer>
        <el-button :icon="Download" @click="exportDrill">导出</el-button>
        <el-button type="primary" @click="drillDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 发布弹窗 -->
    <el-dialog v-model="publishDialog" title="发布仪表板" width="420px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="发布说明"><el-input v-model="publishRemark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doPublish">确认发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { type Component, computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Check, CopyDocument, DataAnalysis, DataLine, Delete, Document, Download, Files, FullScreen,
  Lock, Menu, Moon, Plus, RefreshLeft, RefreshRight, Search, Setting, Sunny, Top, TrendCharts, Unlock, View,
  ZoomIn, ZoomOut, Histogram, PieChart, Grid, Tickets, Odometer,
} from '@element-plus/icons-vue'
import ChartRenderer from '@/components/chart/ChartRenderer.vue'
import {
  createBoard, createDashboard, dashboardVersions, deleteBoard, deleteItem, drillItem, executeItem,
  getDashboard, listBoards, listDashboardItems, publishDashboard, saveItem, updateBoard, updateDashboard,
} from '@/api/visual'
import { pageDatasets as fetchDatasets } from '@/api/dataset'
import type {
  ChartType, DashboardBoard, DatasetField, ItemDataConfig, ItemStyleConfig, QueryResult,
  VisualDashboard, VisualDashboardItem, VisualDataset,
} from '@/types'

/* ============ UI 基础 ============ */
const route = useRoute()
const router = useRouter()
const dashboardId = ref(route.params.id as string)

const theme = ref<'light' | 'dark'>('light')
const fullscreen = ref(false)
const saving = ref(false)
const editingName = ref(false)
const editingTabId = ref('')
const publishDialog = ref(false)
const publishRemark = ref('')

/* ============ 仪表板和分析板 ============ */
const dashboard = ref<VisualDashboard>({})
const boards = ref<DashboardBoard[]>([])
const activeBoardId = ref('')

/** 分析板 Tab 拖拽 */
const tabDragFromIdx = ref(-1)
function onTabDragStart(e: DragEvent, idx: number) { tabDragFromIdx.value = idx; e.dataTransfer!.effectAllowed = 'move' }
function onTabDragOver(e: DragEvent) { e.dataTransfer!.dropEffect = 'move' }
function onTabDrop(e: DragEvent, toIdx: number) {
  const from = tabDragFromIdx.value
  if (from >= 0 && from !== toIdx) {
    const item = boards.value.splice(from, 1)[0]
    boards.value.splice(toIdx, 0, item)
    boards.value.forEach((b, i) => (b.sortOrder = i))
  }
  tabDragFromIdx.value = -1
}

function addBoard() {
  pushHistory()
  const newBoard: DashboardBoard = {
    id: `board_${Date.now()}`,
    dashboardId: dashboardId.value,
    name: `分析板${boards.value.length + 1}`,
    layoutMode: 'CANVAS',
    canvasWidth: 1920,
    canvasHeight: 1080,
    canvasBg: '#f5f7fa',
    sortOrder: boards.value.length,
    status: 1,
  }
  boards.value.push(newBoard)
  activeBoardId.value = newBoard.id
}

function closeBoard(board: DashboardBoard) {
  if (boards.value.length <= 1) { ElMessage.warning('至少保留一个分析板'); return }
  boards.value = boards.value.filter((b) => b.id !== board.id)
  if (activeBoardId.value === board.id) activeBoardId.value = boards.value[0]?.id || ''
}

function startEditTab(board: DashboardBoard) {
  editingTabId.value = board.id
}

/* ============ 组件配置 ============ */
const CHART_TYPES = [
  { type: 'BAR', label: '柱状图', icon: Histogram, color: '#4f9df9' },
  { type: 'LINE', label: '折线图', icon: DataLine, color: '#67c23a' },
  { type: 'PIE', label: '饼图', icon: PieChart, color: '#e6a23c' },
  { type: 'AREA', label: '面积图', icon: TrendCharts, color: '#9254de' },
  { type: 'SCATTER', label: '散点图', icon: Grid, color: '#36cfc9' },
  { type: 'TABLE', label: '表格', icon: Tickets, color: '#597ef7' },
  { type: 'NUMBER', label: '指标卡', icon: Odometer, color: '#ff7a45' },
  { type: 'GAUGE', label: '仪表盘', icon: Odometer, color: '#f759ab' },
  { type: 'FUNNEL', label: '漏斗图', icon: TrendCharts, color: '#d3adf7' },
  { type: 'RADAR', label: '雷达图', icon: Grid, color: '#36cfc9' },
  { type: 'HEATMAP', label: '热力图', icon: Grid, color: '#ff7a45' },
  { type: 'BOXPLOT', label: '箱型图', icon: Histogram, color: '#597ef7' },
  { type: 'TREEMAP', label: '树形图', icon: Grid, color: '#9254de' },
] as { type: ChartType; label: string; icon: Component; color: string }[]

/* ============ 数据集 ============ */
const datasets = ref<VisualDataset[]>([])
const selectedDatasetId = ref('')
const activeDataset = computed(() => datasets.value.find((d) => d.id === selectedDatasetId.value))
const dimensionFields = computed(() => (activeDataset.value?.fields || []).filter((f) => f.fieldType === 'DIMENSION'))
const metricFields = computed(() => (activeDataset.value?.fields || []).filter((f) => f.fieldType === 'METRIC'))

async function loadDatasets() {
  const r = await fetchDatasets({ current: 1, size: 100 })
  datasets.value = r.records || []
}

function onDatasetChange() { /* dataset fields updated via computed */ }

/* ============ 画布 ============ */
const CANVAS_PRESETS = [
  { label: '数据大屏 1920×1080', value: '1920x1080', width: 1920, height: 1080 },
  { label: '指挥大屏 3840×2160', value: '3840x2160', width: 3840, height: 2160 },
  { label: 'PC报表 1600×900', value: '1600x900', width: 1600, height: 900 },
  { label: 'PC宽屏 1920×1200', value: '1920x1200', width: 1920, height: 1200 },
  { label: '移动端 375×667', value: '375x667', width: 375, height: 667 },
]

const canvasSizePreset = ref('1920x1080')
const canvasWidth = ref(1920)
const canvasHeight = ref(1080)
const canvasBg = ref('#f5f7fa')
const zoom = ref(0.8)
const showGrid = ref(true)
const snapToGrid = ref(true)
const gridSize = ref(10)

function onCanvasSizeChange(val: string) {
  const p = CANVAS_PRESETS.find((s) => s.value === val)
  if (p && p.value !== 'custom') { canvasWidth.value = p.width; canvasHeight.value = p.height }
}
function zoomIn() { zoom.value = Math.min(2, +(zoom.value + 0.1).toFixed(2)) }
function zoomOut() { zoom.value = Math.max(0.3, +(zoom.value - 0.1).toFixed(2)) }

const canvasStyle = computed(() => ({
  width: `${canvasWidth.value}px`,
  height: `${canvasHeight.value}px`,
  backgroundColor: canvasBg.value,
  transform: `scale(${zoom.value})`,
  transformOrigin: 'top left',
}))
const gridStyle = computed(() => ({
  backgroundSize: `${gridSize.value}px ${gridSize.value}px`,
  backgroundImage: `linear-gradient(to right, ${theme.value === 'dark' ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.05)'} 1px, transparent 1px), linear-gradient(to bottom, ${theme.value === 'dark' ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.05)'} 1px, transparent 1px)`,
}))

/* ============ 组件(组件) ============ */
const items = ref<(VisualDashboardItem)[]>([])
const selectedIds = ref<string[]>([])
const dragCompType = ref('')
const componentSearch = ref('')
const configTab = ref('data')
const leftTab = ref('components')
const filterScope = ref('current')

const leftTabs = [
  { key: 'components', label: '图表', icon: Grid },
  { key: 'dataset', label: '数据集', icon: Menu },
  { key: 'layers', label: '图层', icon: Files },
]
const configTabs = [
  { key: 'data', label: '数据', icon: DataAnalysis },
  { key: 'style', label: '样式', icon: Setting },
  { key: 'filter', label: '过滤', icon: Files },
]

/* ============ 计算属性 ============ */
const filteredComponents = computed(() => {
  const s = componentSearch.value.toLowerCase()
  if (!s) return CHART_TYPES
  return CHART_TYPES.filter((c) => c.label.toLowerCase().includes(s) || c.type.toLowerCase().includes(s))
})

const boardItems = computed(() => items.value.filter((i) => i.boardId === activeBoardId.value))
const reverseBoardItems = computed(() => [...boardItems.value].reverse())
const visibleBoardItems = computed(() => boardItems.value.filter((i) => i.visible !== 0))

const selectedItems = computed(() => items.value.filter((i) => selectedIds.value.includes(i.id!)))
const selectionBox = ref<{ x: number; y: number; w: number; h: number } | null>(null)
const selectionBoxStyle = computed(() => {
  if (!selectionBox.value) return {}
  const b = selectionBox.value
  return { left: `${Math.min(b.x, b.x + b.w)}px`, top: `${Math.min(b.y, b.y + b.h)}px`, width: `${Math.abs(b.w)}px`, height: `${Math.abs(b.h)}px` }
})

/* ============ 数据配置和样式配置 ============ */
const defaultDataConfig = (): ItemDataConfig => ({ dimensions: [], metrics: [], filters: [], sorts: [], limit: 1000 })
const defaultStyleConfig = (): ItemStyleConfig => ({ title: { show: true, text: '', fontSize: 16, align: 'left' }, legend: { show: true, position: 'top' } })
const dataConfig = reactive<ItemDataConfig>(defaultDataConfig())
const styleConfig = reactive<ItemStyleConfig>(defaultStyleConfig())

const availableDimensionFields = computed(() => dimensionFields.value)
const availableMetricFields = computed(() => metricFields.value)

/* ============ 拖拽状态 ============ */
const dragState = ref<{
  type: 'move' | 'resize' | 'pan'
  item: VisualDashboardItem | null
  dir?: string
  startX: number
  startY: number
  origX: number
  origY: number
  origW: number
  origH: number
} | null>(null)

/* ============ 历史记录 ============ */
let historyStack: string[] = []
let redoStack: string[] = []
const MAX_HISTORY = 50
const canUndo = computed(() => historyStack.length > 0)
const canRedo = computed(() => redoStack.length > 0)

function pushHistory() {
  historyStack.push(JSON.stringify({ items: items.value, boards: boards.value, canvasWidth: canvasWidth.value, canvasHeight: canvasHeight.value }))
  if (historyStack.length > MAX_HISTORY) historyStack.shift()
  redoStack = []
}
function undo() {
  if (!historyStack.length) return
  redoStack.push(JSON.stringify({ items: items.value, boards: boards.value, canvasWidth: canvasWidth.value, canvasHeight: canvasHeight.value }))
  const prev = JSON.parse(historyStack.pop()!)
  items.value = prev.items; boards.value = prev.boards; canvasWidth.value = prev.canvasWidth; canvasHeight.value = prev.canvasHeight
}
function redo() {
  if (!redoStack.length) return
  historyStack.push(JSON.stringify({ items: items.value, boards: boards.value, canvasWidth: canvasWidth.value, canvasHeight: canvasHeight.value }))
  const next = JSON.parse(redoStack.pop()!)
  items.value = next.items; boards.value = next.boards; canvasWidth.value = next.canvasWidth; canvasHeight.value = next.canvasHeight
}

/* ============ 对齐辅助线 ============ */
const activeGuides = computed(() => {
  const guides: any[] = []
  if (dragState.value?.type === 'move' && selectedItems.value.length === 1) {
    const item = selectedItems.value[0]
    const threshold = 8
    for (const other of boardItems.value) {
      if (other.id === item.id) continue
      if (Math.abs((item.posX || 0) - (other.posX || 0)) < threshold)
        guides.push({ type: 'v', style: { left: `${other.posX}px` } })
      if (Math.abs(((item.posX || 0) + (item.width || 300)) - ((other.posX || 0) + (other.width || 300))) < threshold)
        guides.push({ type: 'v', style: { left: `${(other.posX || 0) + (other.width || 300)}px` } })
      if (Math.abs((item.posY || 0) - (other.posY || 0)) < threshold)
        guides.push({ type: 'h', style: { top: `${other.posY}px` } })
      if (Math.abs(((item.posY || 0) + (item.height || 200)) - ((other.posY || 0) + (other.height || 200))) < threshold)
        guides.push({ type: 'h', style: { top: `${(other.posY || 0) + (other.height || 200)}px` } })
    }
  }
  return guides
})

/* ============ 方法 ============ */
function getChartIcon(type?: string) {
  const map: Record<string, any> = {
    BAR: Histogram, LINE: DataLine, PIE: PieChart, AREA: TrendCharts, SCATTER: Grid,
    HEATMAP: Grid, GAUGE: Odometer, TREEMAP: Grid, BOXPLOT: Histogram, MAP: Grid,
    TABLE: Tickets, NUMBER: Odometer, FUNNEL: TrendCharts, RADAR: Grid,
  }
  return map[type || 'TABLE'] || Grid
}

function getDefaultTitle(type?: string) {
  const map: Record<string, string> = {
    BAR: '柱状图', LINE: '折线图', PIE: '饼图', AREA: '面积图', SCATTER: '散点图',
    HEATMAP: '热力图', GAUGE: '仪表盘', TREEMAP: '树形图', BOXPLOT: '箱型图',
    MAP: '地图', TABLE: '表格', NUMBER: '指标卡', FUNNEL: '漏斗图', RADAR: '雷达图',
  }
  return map[type || 'TABLE'] || '组件'
}

function getItemStyle(item: VisualDashboardItem) {
  return {
    left: `${item.posX || 0}px`,
    top: `${item.posY || 0}px`,
    width: `${item.width || 400}px`,
    height: `${item.height || 300}px`,
    backgroundColor: item.bgColor || styleConfig.bgColor || 'var(--card-bg)',
    borderRadius: `${item.borderRadius ?? styleConfig.borderRadius ?? 6}px`,
    border: `${item.borderWidth ?? styleConfig.borderWidth ?? 1}px solid ${item.borderColor || styleConfig.borderColor || 'var(--card-border)'}`,
    zIndex: item.zIndex || 1,
  }
}

/* 组件库拖拽 */
function onCompDragStart(e: DragEvent, comp: any) {
  dragCompType.value = comp.type
  e.dataTransfer?.setData('text/plain', comp.type)
  e.dataTransfer!.effectAllowed = 'copy'
}

function onFieldDragStart(e: DragEvent, field: DatasetField, _fieldType: string) {
  e.dataTransfer?.setData('field-code', field.fieldCode)
  e.dataTransfer?.setData('field-name', field.fieldName)
  e.dataTransfer?.setData('field-type', _fieldType)
  e.dataTransfer!.effectAllowed = 'copy'
}

function onCanvasDragOver(e: DragEvent) { e.dataTransfer!.dropEffect =dragCompType.value ? 'copy' : 'move' }
function onCanvasDrop(e: DragEvent) {
  const compType = e.dataTransfer?.getData('text/plain')
  if (!compType) return
  const rect = canvasEl.value!.getBoundingClientRect()
  const x = (e.clientX - rect.left) / zoom.value
  const y = (e.clientY - rect.top) / zoom.value
  addComponent(compType, x, y)
}

/** 添加组件 */
function addComponent(type: string, x?: number, y?: number) {
  pushHistory()
  const newItem: VisualDashboardItem = {
    id: `item_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
    dashboardId: dashboardId.value,
    boardId: activeBoardId.value,
    title: getDefaultTitle(type),
    chartType: type as ChartType,
    posX: x !== undefined ? Math.round(x) : 50 + boardItems.value.length * 30,
    posY: y !== undefined ? Math.round(y) : 50 + boardItems.value.length * 30,
    width: type === 'NUMBER' ? 240 : type === 'TABLE' ? 440 : 400,
    height: type === 'NUMBER' ? 140 : 300,
    dataConfig: JSON.stringify(defaultDataConfig()),
    styleConfig: JSON.stringify(defaultStyleConfig()),
    zIndex: boardItems.value.length + 1,
  }
  items.value.push(newItem)
  selectedIds.value = [newItem.id!]
  // 加载结果
  refreshItem(newItem)
}

async function refreshItem(item: VisualDashboardItem) {
  if (!item.id) return
  loadingItems[item.id] = true
  try {
    // TODO: 根据数据集和维度指标构建查询
    if (item.datasourceId && item.querySql) {
      results[item.id] = await executeItem(item.id)
    }
  } catch { /* ignore */ } finally {
    loadingItems[item.id] = false
  }
}

function selectItem(id: string) {
  selectedIds.value = [id]
  // 加载选中项的配置
  const item = items.value.find((i) => i.id === id)
  if (item) {
    Object.assign(dataConfig, JSON.parse(item.dataConfig || '{}'))
    Object.assign(styleConfig, JSON.parse(item.styleConfig || '{}'))
  }
}

function removeItem(item: VisualDashboardItem) {
  items.value = items.value.filter((i) => i.id !== item.id)
  selectedIds.value = []
}

function duplicateItem(item: VisualDashboardItem) {
  pushHistory()
  const newItem: VisualDashboardItem = {
    ...item,
    id: `item_${Date.now()}`,
    title: `${item.title} 副本`,
    posX: (item.posX || 0) + 30,
    posY: (item.posY || 0) + 30,
    dataConfig: item.dataConfig,
    styleConfig: item.styleConfig,
  }
  items.value.push(newItem)
  selectedIds.value = [newItem.id!]
}

function openItemConfig(item: VisualDashboardItem) {
  selectedIds.value = [item.id!]
  configTab.value = 'data'
}

function toggleLock(item: VisualDashboardItem) {
  item.locked = item.locked ? 0 : 1
}

/* Canvas 鼠标事件 */
const canvasEl = ref<HTMLDivElement>()
const canvasAreaEl = ref<HTMLDivElement>()

function onCanvasMouseDown(e: MouseEvent) {
  if (e.target === canvasEl.value || (e.target as HTMLElement).classList.contains('editor-canvas') || (e.target as HTMLElement).classList.contains('grid-bg')) {
    selectedIds.value = []
    if (e.shiftKey) {
      selectionBox.value = { x: e.offsetX, y: e.offsetY, w: 0, h: 0 }
    }
  }
}
function onCanvasMouseMove(e: MouseEvent) {
  if (selectionBox.value) {
    selectionBox.value.w = e.offsetX - selectionBox.value.x
    selectionBox.value.h = e.offsetY - selectionBox.value.y
    return
  }
  if (dragState.value?.type === 'move' && dragState.value.item) {
    const dx = (e.clientX - dragState.value.startX) / zoom.value
    const dy = (e.clientY - dragState.value.startY) / zoom.value
    const item = dragState.value.item
    let nx = dragState.value.origX + dx
    let ny = dragState.value.origY + dy
    if (snapToGrid.value) { nx = Math.round(nx / gridSize.value) * gridSize.value; ny = Math.round(ny / gridSize.value) * gridSize.value }
    item.posX = Math.max(0, Math.round(nx))
    item.posY = Math.max(0, Math.round(ny))
    return
  }
  if (dragState.value?.type === 'resize' && dragState.value.item) {
    const dx = (e.clientX - dragState.value.startX) / zoom.value
    const dy = (e.clientY - dragState.value.startY) / zoom.value
    const item = dragState.value.item
    const dir = dragState.value.dir || ''
    if (dir.includes('e')) item.width = Math.max(200, dragState.value.origW + dx)
    if (dir.includes('s')) item.height = Math.max(120, dragState.value.origH + dy)
    if (dir.includes('w')) { const w = dragState.value.origW - dx; if (w >= 200) { item.posX = dragState.value.origX + dx; item.width = w } }
    if (dir.includes('n')) { const h = dragState.value.origH - dy; if (h >= 120) { item.posY = dragState.value.origY + dy; item.height = h } }
    if (snapToGrid.value) { item.width = Math.round((item.width || 0) / gridSize.value) * gridSize.value; item.height = Math.round((item.height || 0) / gridSize.value) * gridSize.value }
  }
}
function onCanvasMouseUp() {
  if (selectionBox.value) {
    const b = selectionBox.value
    const box = { x: b.w < 0 ? b.x + b.w : b.x, y: b.h < 0 ? b.y + b.h : b.y, w: Math.abs(b.w), h: Math.abs(b.h) }
    if (box.w > 5 || box.h > 5) {
      selectedIds.value = boardItems.value
        .filter((item) => {
          const ix = item.posX || 0, iy = item.posY || 0, iw = item.width || 400, ih = item.height || 300
          return ix >= box.x && iy >= box.y && ix + iw <= box.x + box.w && iy + ih <= box.y + box.h
        })
        .map((i) => i.id!)
    }
    selectionBox.value = null
  }
  dragState.value = null
}

function onItemMouseDown(e: MouseEvent, item: VisualDashboardItem) {
  if (item.locked) return
  dragState.value = {
    type: 'move', item, startX: e.clientX, startY: e.clientY,
    origX: item.posX || 0, origY: item.posY || 0, origW: item.width || 400, origH: item.height || 300,
  }
  if (e.shiftKey) {
    if (selectedIds.value.includes(item.id!)) selectedIds.value = selectedIds.value.filter((id) => id !== item.id)
    else selectedIds.value = [...selectedIds.value, item.id!]
  } else if (!selectedIds.value.includes(item.id!)) {
    selectedIds.value = [item.id!]
  }
  e.preventDefault()
}

function onResizeStart(e: MouseEvent, item: VisualDashboardItem, dir: string) {
  dragState.value = {
    type: 'resize', item, dir, startX: e.clientX, startY: e.clientY,
    origX: item.posX || 0, origY: item.posY || 0, origW: item.width || 400, origH: item.height || 300,
  }
  e.preventDefault(); e.stopPropagation()
}

function onWheel(e: WheelEvent) {
  if (e.ctrlKey) { if (e.deltaY < 0) zoomIn(); else zoomOut() }
}

/* 维度/指标/过滤 */
function addDimension() {
  dataConfig.dimensions!.push({ fieldCode: '', fieldName: '', sort: '' })
}
function addMetric() {
  dataConfig.metrics!.push({ fieldCode: '', fieldName: '', aggType: 'SUM' })
}
function addFilter() {
  dataConfig.filters!.push({ fieldCode: '', operator: 'EQ', values: [] })
}
function onDatasetSelectChange() {
  const ds = datasets.value.find((d) => d.id === dataConfig.datasetId)
  if (ds) dataConfig.datasetType = ds.datasetType
}

/* 批量对齐 */
function batchAlign(type: string) {
  if (selectedItems.value.length < 2) return
  pushHistory()
  const list = selectedItems.value
  switch (type) {
    case 'left': { const m = Math.min(...list.map((i) => i.posX || 0)); list.forEach((i) => (i.posX = m)); break }
    case 'right': { const m = Math.max(...list.map((i) => (i.posX || 0) + (i.width || 400))); list.forEach((i) => (i.posX = m - (i.width || 400))); break }
    case 'hcenter': { const a = list.reduce((s, i) => s + (i.posX || 0) + (i.width || 400) / 2, 0) / list.length; list.forEach((i) => (i.posX = Math.round(a - (i.width || 400) / 2))); break }
    case 'top': { const m = Math.min(...list.map((i) => i.posY || 0)); list.forEach((i) => (i.posY = m)); break }
    case 'bottom': { const m = Math.max(...list.map((i) => (i.posY || 0) + (i.height || 300))); list.forEach((i) => (i.posY = m - (i.height || 300))); break }
    case 'vcenter': { const a = list.reduce((s, i) => s + (i.posY || 0) + (i.height || 300) / 2, 0) / list.length; list.forEach((i) => (i.posY = Math.round(a - (i.height || 300) / 2))); break }
  }
}

function batchDistribute(dir: string) {
  if (selectedItems.value.length < 3) return
  pushHistory()
  const sorted = [...selectedItems.value].sort((a, b) => dir === 'horizontal' ? (a.posX || 0) - (b.posX || 0) : (a.posY || 0) - (b.posY || 0))
  if (dir === 'horizontal') {
    const first = sorted[0], last = sorted[sorted.length - 1]
    const totalSpace = (last.posX || 0) - ((first.posX || 0) + (first.width || 400))
    const totalWidth = sorted.slice(1, -1).reduce((s, i) => s + (i.width || 400), 0)
    const gap = (totalSpace - totalWidth) / (sorted.length - 1)
    let cursor = (first.posX || 0) + (first.width || 400) + gap
    sorted.slice(1, -1).forEach((i) => { i.posX = Math.round(cursor); cursor += (i.width || 400) + gap })
  } else {
    const first = sorted[0], last = sorted[sorted.length - 1]
    const totalSpace = (last.posY || 0) - ((first.posY || 0) + (first.height || 300))
    const totalHeight = sorted.slice(1, -1).reduce((s, i) => s + (i.height || 300), 0)
    const gap = (totalSpace - totalHeight) / (sorted.length - 1)
    let cursor = (first.posY || 0) + (first.height || 300) + gap
    sorted.slice(1, -1).forEach((i) => { i.posY = Math.round(cursor); cursor += (i.height || 300) + gap })
  }
}

function batchDelete() {
  ElMessageBox.confirm(`确认删餘选中的 ${selectedItems.value.length} 个组件吗？`, '批量删除', { type: 'warning' })
    .then(() => {
      items.value = items.value.filter((i) => !selectedIds.value.includes(i.id!))
      selectedIds.value = []
    })
    .catch(() => {})
}

/* 下钻 */
const drillDialog = ref(false)
const drillLoading = ref(false)
const drillTitle = ref('')
const drillResult = ref<QueryResult>()
const drillColumns = computed(() => (drillResult.value?.columns || []).map((c) => c.columnName ?? ''))

async function onDrill(item: VisualDashboardItem, payload: { column: string; value: string }) {
  if (!item.id) return
  drillTitle.value = `${item.title} · ${payload.column} = ${payload.value}`
  drillDialog.value = true
  drillLoading.value = true
  drillResult.value = undefined
  try { drillResult.value = await drillItem(item.id, { column: payload.column, value: payload.value }) }
  catch { drillDialog.value = false }
  finally { drillLoading.value = false }
}

function resultToCSV(result: QueryResult): string {
  if (!result?.columns?.length) return ''
  const cols = result.columns.map((c) => c.columnName ?? '')
  const esc = (v: any) => { if (v == null) return ''; const s = String(v); return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s }
  return [cols.join(','), ...result.rows.map((row) => cols.map((c) => esc(row[c])).join(','))].join('\n')
}
function downloadCSV(content: string, filename: string) {
  const blob = new Blob(['\ufeff' + content], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = filename; a.click(); URL.revokeObjectURL(url)
}
function exportDrill() { if (drillResult.value) downloadCSV(resultToCSV(drillResult.value), `${drillTitle.value}.csv`) }

/* 保存/发布 */
async function saveAll() {
  saving.value = true
  try {
    // 保存仪表板
    await updateDashboard({
      id: dashboardId.value, name: dashboard.value.name, description: dashboard.value.description,
      refreshInterval: dashboard.value.refreshInterval, status: dashboard.value.status === 2 ? 2 : 1,
      layout: JSON.stringify({ canvasWidth: canvasWidth.value, canvasHeight: canvasHeight.value, canvasBg: canvasBg.value }),
    })
    // 已存在的分析板 id（用于区分新增/更新）。
    // 注意：不能靠 id 前缀判断，因为后端真实 id（如 board_d1_main）与前端新板临时 id（board_<时间戳>）都以 board_ 开头，
    // 否则已存在的板会被误判为“新增”而走 insert，触发主键冲突返回系统繁忙。
    const existingBoardIds = new Set(((await listBoards(dashboardId.value)) || []).map((b) => b.id))
    for (const board of boards.value) {
      const boardPayload = { ...board, dashboardId: dashboardId.value }
      if (existingBoardIds.has(board.id)) await updateBoard(boardPayload)
      else await createBoard(boardPayload)
    }
    // 保存组件
    for (const item of items.value) {
      const payload: VisualDashboardItem = {
        id: item.id?.startsWith('item_') ? undefined : item.id,
        dashboardId: dashboardId.value, boardId: item.boardId,
        title: item.title, chartType: item.chartType as ChartType,
        datasourceId: item.datasourceId, querySql: item.querySql,
        dataConfig: JSON.stringify(dataConfig), styleConfig: JSON.stringify(styleConfig),
        posX: item.posX, posY: item.posY, width: item.width, height: item.height,
        zIndex: item.zIndex, bgColor: item.bgColor, borderRadius: item.borderRadius,
        borderWidth: item.borderWidth, borderColor: item.borderColor,
        locked: item.locked, visible: item.visible,
      }
      await saveItem(payload)
    }
    ElMessage.success('保存成功')
  } finally { saving.value = false }
}

async function doPublish() {
  saving.value = true
  try {
    const v = await publishDashboard(dashboardId.value, publishRemark.value)
    ElMessage.success(`已发布，版本号 v${v}`)
    publishDialog.value = false
    await loadAll()
  } finally { saving.value = false }
}

function preview() {
  window.open(`${location.origin}/visual/dashboard/view/${dashboardId.value}`, '_blank')
}

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
}

/* 数据加载 */
const loadingItems = reactive<Record<string, boolean>>({})
const results = reactive<Record<string, QueryResult>>({})

async function loadAll() {
  dashboard.value = await getDashboard(dashboardId.value)
  boards.value = ((await listBoards(dashboardId.value)) as unknown as DashboardBoard[]) || []
  if (!boards.value.length) {
    addBoard()
  } else {
    activeBoardId.value = boards.value[0].id
  }
  items.value = (await listDashboardItems(dashboardId.value)) || []
  // 加载结果
  for (const item of items.value) {
    if (item.datasourceId && item.querySql) await refreshItem(item)
  }
}

/* 快捷键 */
function handleKeydown(e: KeyboardEvent) {
  if (e.ctrlKey || e.metaKey) {
    switch (e.key.toLowerCase()) {
      case 's': e.preventDefault(); saveAll(); break
      case 'z': e.preventDefault(); if (e.shiftKey) redo(); else undo(); break
      case 'y': e.preventDefault(); redo(); break
      case 'd': e.preventDefault(); if (selectedItems.value.length === 1) duplicateItem(selectedItems.value[0]); break
      case 'a': e.preventDefault(); selectedIds.value = boardItems.value.map((i) => i.id!); break
    }
  } else if (e.key === 'Delete' || e.key === 'Backspace') {
    if (selectedIds.value.length && document.activeElement?.tagName !== 'INPUT' && document.activeElement?.tagName !== 'TEXTAREA') {
      e.preventDefault()
      selectedIds.value.forEach((id) => {
        const item = items.value.find((i) => i.id === id)
        if (item) removeItem(item)
      })
    }
  } else if (e.key === 'Escape') {
    selectedIds.value = []
  }
}

onMounted(() => {
  loadDatasets()
  loadAll()
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<script lang="ts">
// 配置面板小组件
import { defineComponent, h } from 'vue'

export const ConfigGroup = defineComponent({
  props: { title: String },
  setup(props, { slots }) {
    return () => h('div', { class: 'config-group' }, [
      h('div', { class: 'config-group-title' }, props.title),
      h('div', { class: 'config-group-body' }, slots.default?.()),
    ])
  },
})

export const ConfigRow = defineComponent({
  props: { label: String },
  setup(props, { slots }) {
    return () => h('div', { class: 'config-row' }, [
      h('span', { class: 'config-label' }, props.label),
      h('div', { class: 'config-control' }, slots.default?.()),
    ])
  },
})
</script>

<style scoped>
.bi-dashboard-editor {
  --bg: #f5f7fa;
  --card-bg: #ffffff;
  --card-border: #e4e7ed;
  --text-1: #303133;
  --text-2: #606266;
  --text-3: #909399;
  --header-bg: #ffffff;
  --aside-bg: #fafbfc;
  --accent: #4f9df9;
  --danger: #f56c6c;
  --success: #67c23a;
  --warning: #e6a23c;

  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  color: var(--text-1);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  overflow: hidden;
  position: relative;
}

.bi-dashboard-editor.theme-dark {
  --bg: #0f1117;
  --card-bg: #1a1d27;
  --card-border: #2a2f3a;
  --text-1: #e5eaf3;
  --text-2: #a3abb9;
  --text-3: #6b7280;
  --header-bg: #1a1d27;
  --aside-bg: #151821;
}

.bi-dashboard-editor.is-fullscreen {
  position: fixed; inset: 0; z-index: 9999;
}

/* ============ Header ============ */
.editor-header {
  display: flex;
  align-items: center;
  height: 48px;
  background: var(--header-bg);
  border-bottom: 1px solid var(--card-border);
  padding: 0 12px;
  flex-shrink: 0;
  gap: 12px;
}

.header-left { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.header-back { cursor: pointer; padding: 4px; border-radius: 4px; }
.header-back:hover { background: var(--card-border); }
.header-logo { color: var(--accent); }
.header-name { display: flex; align-items: center; gap: 8px; }
.name-text { font-size: 15px; font-weight: 600; cursor: pointer; }

/* Tabs */
.header-tabs { flex: 1; overflow: hidden; }
.tabs-wrapper { display: flex; align-items: center; gap: 2px; overflow-x: auto; }
.tab-item {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 12px; border-radius: 6px 6px 0 0;
  font-size: 13px; color: var(--text-2); cursor: pointer;
  border-bottom: 2px solid transparent; white-space: nowrap;
  transition: all 0.15s;
}
.tab-item:hover { background: var(--card-border); }
.tab-item.active { color: var(--accent); border-bottom-color: var(--accent); background: var(--bg); }
.tab-item .tab-close { margin-left: 4px; opacity: 0; width: 16px; height: 16px; display: flex; align-items: center; justify-content: center; border-radius: 50%; font-size: 14px; }
.tab-item:hover .tab-close { opacity: 1; }
.tab-item .tab-close:hover { background: var(--danger); color: #fff; }
.tab-add { padding: 4px 8px; border-radius: 4px; cursor: pointer; color: var(--text-3); }
.tab-add:hover { background: var(--card-border); color: var(--accent); }

.header-right { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.header-zoom { display: flex; align-items: center; gap: 4px; }
.zoom-value { font-size: 12px; color: var(--text-2); min-width: 36px; text-align: center; }

/* ============ Body ============ */
.editor-body { flex: 1; display: flex; min-height: 0; overflow: hidden; }

.editor-aside {
  width: 260px;
  background: var(--aside-bg);
  border-right: 1px solid var(--card-border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
}
.right-aside { border-right: none; border-left: 1px solid var(--card-border); width: 300px; }

/* Left panel tabs */
.aside-tabs { display: flex; border-bottom: 1px solid var(--card-border); }
.aside-tab {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 4px;
  padding: 10px; font-size: 12px; color: var(--text-2); cursor: pointer;
  border-bottom: 2px solid transparent; transition: all 0.15s;
}
.aside-tab:hover { color: var(--accent); }
.aside-tab.active { color: var(--accent); border-bottom-color: var(--accent); }

.panel-content { flex: 1; overflow-y: auto; padding: 12px; }

/* Component library */
.search-box { margin-bottom: 12px; }
.component-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.comp-card {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 10px 4px; border-radius: 8px; cursor: grab;
  border: 1px solid var(--card-border); background: var(--card-bg);
  transition: all 0.2s;
}
.comp-card:hover { border-color: var(--accent); box-shadow: 0 2px 8px rgba(79,157,249,0.15); transform: translateY(-1px); }
.comp-icon-wrap { width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #fff; }
.comp-name { font-size: 11px; color: var(--text-2); }

/* Dataset panel */
.dataset-selector { margin-bottom: 12px; }
.field-pool { display: flex; flex-direction: column; gap: 12px; }
.pool-group-title { font-size: 12px; font-weight: 600; color: var(--text-2); margin-bottom: 6px; display: flex; align-items: center; gap: 4px; }
.field-item {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 8px; border-radius: 6px; cursor: grab;
  background: var(--card-bg); border: 1px solid var(--card-border);
  font-size: 12px; transition: all 0.15s;
}
.field-item:hover { border-color: var(--accent); }
.field-item.dimension { border-left: 3px solid #4f9df9; }
.field-item.metric { border-left: 3px solid #67c23a; }
.field-code { margin-left: auto; font-size: 10px; color: var(--text-3); }

/* Layer panel */
.layer-list { display: flex; flex-direction: column; gap: 2px; }
.layer-item {
  display: flex; align-items: center; gap: 8px; padding: 8px 10px;
  border-radius: 6px; cursor: pointer; font-size: 13px;
}
.layer-item:hover { background: var(--card-border); }
.layer-item.active { background: rgba(79,157,249,0.1); color: var(--accent); }
.layer-item.locked { opacity: 0.6; }
.layer-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.layer-ops { display: flex; gap: 2px; }

/* ============ Canvas ============ */
.editor-canvas-area { flex: 1; display: flex; flex-direction: column; min-width: 0; overflow: hidden; }

.canvas-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 12px; background: var(--aside-bg); border-bottom: 1px solid var(--card-border);
}
.toolbar-left { display: flex; align-items: center; gap: 12px; }
.canvas-size-info { font-size: 11px; color: var(--text-3); }
.toolbar-right { display: flex; align-items: center; gap: 8px; }

.canvas-wrapper { flex: 1; overflow: auto; padding: 24px; }

.editor-canvas {
  position: relative;
  box-shadow: 0 4px 20px rgba(0,0,0,0.06);
  border-radius: 4px;
  overflow: visible;
  margin: 0 auto;
}

.grid-bg { position: absolute; inset: 0; pointer-events: none; }

/* Canvas items */
.canvas-item {
  position: absolute;
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  transition: box-shadow 0.2s, border-color 0.2s;
  overflow: hidden;
  cursor: move;
  display: flex;
  flex-direction: column;
}
.canvas-item:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
.canvas-item.selected { border-color: var(--accent); box-shadow: 0 0 0 2px rgba(79,157,249,0.3); }
.canvas-item.locked { cursor: default; }

.item-header {
  display: flex; align-items: center; gap: 6px;
  padding: 4px 8px; background: var(--aside-bg);
  border-bottom: 1px solid var(--card-border);
  flex-shrink: 0; min-height: 28px;
}
.item-drag-handle { color: var(--text-3); cursor: grab; font-size: 12px; }
.item-title-text { font-size: 12px; font-weight: 500; color: var(--text-2); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.item-body { flex: 1; min-height: 0; overflow: hidden; }

.item-placeholder {
  width: 100%; height: 100%;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 8px; color: var(--text-3); font-size: 12px;
}
.item-placeholder a { color: var(--accent); cursor: pointer; }

/* Resize handles */
.resize-handle { position: absolute; width: 8px; height: 8px; background: var(--accent); border: 1px solid #fff; border-radius: 2px; z-index: 10; }
.resize-handle.n { top: -4px; left: 50%; transform: translateX(-50%); cursor: ns-resize; }
.resize-handle.s { bottom: -4px; left: 50%; transform: translateX(-50%); cursor: ns-resize; }
.resize-handle.e { right: -4px; top: 50%; transform: translateY(-50%); cursor: ew-resize; }
.resize-handle.w { left: -4px; top: 50%; transform: translateY(-50%); cursor: ew-resize; }
.resize-handle.ne { top: -4px; right: -4px; cursor: nesw-resize; }
.resize-handle.nw { top: -4px; left: -4px; cursor: nwse-resize; }
.resize-handle.se { bottom: -4px; right: -4px; cursor: nwse-resize; }
.resize-handle.sw { bottom: -4px; left: -4px; cursor: nesw-resize; }

.item-actions {
  position: absolute; top: -36px; right: 4px;
  display: flex; gap: 4px; z-index: 20;
}

.selection-box { position: absolute; background: rgba(79,157,249,0.1); border: 2px dashed var(--accent); pointer-events: none; z-index: 100; }
.align-guide { position: absolute; background: var(--accent); pointer-events: none; z-index: 99; }
.align-guide.v { width: 1px; height: 100%; top: 0; }
.align-guide.h { height: 1px; width: 100%; left: 0; }

/* ============ Right config panel ============ */
.config-tabs { display: flex; border-bottom: 1px solid var(--card-border); background: var(--aside-bg); }
.config-tab {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 4px;
  padding: 10px 8px; font-size: 12px; color: var(--text-2); cursor: pointer;
  border-bottom: 2px solid transparent; transition: all 0.15s;
}
.config-tab:hover { color: var(--accent); }
.config-tab.active { color: var(--accent); border-bottom-color: var(--accent); }

.config-content { flex: 1; overflow-y: auto; padding: 12px; }

.config-group { margin-bottom: 16px; }
.config-group-title { font-size: 12px; font-weight: 600; color: var(--text-1); margin-bottom: 8px; padding-bottom: 6px; border-bottom: 1px solid var(--card-border); }
.config-group-body { display: flex; flex-direction: column; gap: 8px; }

.config-row { display: flex; align-items: center; gap: 8px; }
.config-label { font-size: 12px; color: var(--text-2); min-width: 64px; flex-shrink: 0; }
.config-control { flex: 1; min-width: 0; }

/* Field config list */
.field-config-list { display: flex; flex-direction: column; gap: 6px; margin-bottom: 8px; }
.field-config-item { display: flex; align-items: center; gap: 4px; }
.drag-dot { color: var(--text-3); font-size: 10px; cursor: grab; }

/* Chart type grid */
.chart-type-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; }
.chart-type-item {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  padding: 6px 4px; border-radius: 6px; cursor: pointer;
  border: 1px solid var(--card-border); font-size: 11px; color: var(--text-2);
  transition: all 0.15s;
}
.chart-type-item:hover { border-color: var(--accent); }
.chart-type-item.active { background: rgba(79,157,249,0.1); border-color: var(--accent); color: var(--accent); }

/* Filter */
.filter-item { display: flex; align-items: center; gap: 4px; margin-bottom: 6px; }

/* Align buttons */
.align-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; }

/* ============ Responsive ============ */
@media (max-width: 1400px) {
  .editor-aside { width: 220px; }
  .right-aside { width: 260px; }
}
@media (max-width: 1100px) {
  .editor-aside { display: none; }
}
</style>
