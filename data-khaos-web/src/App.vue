<template>
  <router-view />
  <!-- 全站常驻水印：Data khaos 半透明倾斜平铺 -->
  <div class="global-watermark" aria-hidden="true">
    <div v-for="i in watermarkRows" :key="i" class="watermark-row">
      <span v-for="j in watermarkCols" :key="j" class="watermark-item">Data khaos</span>
    </div>
  </div>
</template>

<script setup lang="ts">
// 依据屏幕尺寸平铺水印密度（1920x1080 基准约 6 行 x 1 列）
const watermarkRows = 8
const watermarkCols = 1
</script>

<style>
/* ============ 全局基础 ============ */
html,
body,
#app {
  height: 100%;
  margin: 0;
  padding: 0;
  font-family: 'Microsoft YaHei', 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
}

/* ============ 浅蓝数据风主题（覆盖 Element Plus 主色变量） ============ */
:root {
  --datakhaos-blue: #7fb8e6; /* 低饱和浅天蓝 */
  --datakhaos-deep: #165dff; /* 深海蓝 */
  --datakhaos-grayblue: #a9c4dd; /* 浅灰蓝 */
  --datakhaos-bg: #f7fafc; /* 页面极浅灰蓝背景 */

  --el-color-primary: #165dff;
  --el-color-primary-light-3: #4590ff;
  --el-color-primary-light-5: #7fb8e6;
  --el-color-primary-light-7: #b8d6f2;
  --el-color-primary-light-8: #d3e6f7;
  --el-color-primary-light-9: #eaf3fb;
  --el-color-primary-dark-2: #0e4ad2;
}

/* ============ 全站水印 ============ */
.global-watermark {
  position: fixed;
  inset: 0;
  z-index: 9999;
  pointer-events: none;
  overflow: hidden;
}
.watermark-row {
  display: flex;
  justify-content: space-around;
  opacity: 0.5;
}
.watermark-item {
  padding: 46px 0;
  font-size: 20px;
  font-weight: 600;
  color: rgba(120, 150, 180, 0.16);
  white-space: nowrap;
  transform: rotate(-24deg);
  letter-spacing: 2px;
  user-select: none;
}
</style>