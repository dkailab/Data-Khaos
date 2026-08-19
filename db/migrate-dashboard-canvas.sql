-- ============================================================
-- 仪表板画布增强迁移脚本
-- 支持：像素级拖拉拽、多页面画布、自定义背景/网格
-- ============================================================

-- 1. 扩展 visual_dashboard 表：新增画布配置字段
ALTER TABLE visual_dashboard ADD COLUMN canvas_width INT DEFAULT 1920 COMMENT '画布宽度(像素)';
ALTER TABLE visual_dashboard ADD COLUMN canvas_height INT DEFAULT 1080 COMMENT '画布高度(像素)';
ALTER TABLE visual_dashboard ADD COLUMN canvas_bg VARCHAR(50) DEFAULT '#f5f7fa' COMMENT '画布背景色';
ALTER TABLE visual_dashboard ADD COLUMN grid_enabled TINYINT DEFAULT 1 COMMENT '显示网格';
ALTER TABLE visual_dashboard ADD COLUMN grid_size INT DEFAULT 20 COMMENT '网格大小(px)';
ALTER TABLE visual_dashboard ADD COLUMN ruler_enabled TINYINT DEFAULT 0 COMMENT '显示标尺';

-- 2. 扩展 visual_dashboard_item 表：新增样式/层级字段
ALTER TABLE visual_dashboard_item ADD COLUMN z_index INT DEFAULT 1 COMMENT '层级(z-index)';
ALTER TABLE visual_dashboard_item ADD COLUMN bg_color VARCHAR(50) DEFAULT '' COMMENT '组件背景色';
ALTER TABLE visual_dashboard_item ADD COLUMN border_radius INT DEFAULT 8 COMMENT '圆角(px)';
ALTER TABLE visual_dashboard_item ADD COLUMN border_width INT DEFAULT 1 COMMENT '边框宽度';
ALTER TABLE visual_dashboard_item ADD COLUMN border_color VARCHAR(50) DEFAULT '' COMMENT '边框颜色';
ALTER TABLE visual_dashboard_item ADD COLUMN locked TINYINT DEFAULT 0 COMMENT '锁定(禁止拖拽)';
ALTER TABLE visual_dashboard_item ADD COLUMN visible TINYINT DEFAULT 1 COMMENT '可见性';

-- 3. 仪表板页面表（支持多页签画布）
CREATE TABLE visual_dashboard_page (
    id              VARCHAR(32) PRIMARY KEY,
    dashboard_id    VARCHAR(32) NOT NULL COMMENT '所属仪表板',
    page_name       VARCHAR(200) NOT NULL COMMENT '页面名称',
    page_code       VARCHAR(100) COMMENT '页面编码',
    sort_order      INT DEFAULT 0 COMMENT '排序',
    canvas_width    INT DEFAULT 1920 COMMENT '画布宽度',
    canvas_height   INT DEFAULT 1080 COMMENT '画布高度',
    canvas_bg       VARCHAR(50) DEFAULT '#f5f7fa' COMMENT '画布背景',
    status          TINYINT DEFAULT 1 COMMENT '1:启用 0:禁用',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP(),
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP(),
    INDEX idx_page_dashboard (dashboard_id)
) COMMENT '仪表板页面表（多页签画布）';

-- 4. 组件添加页面关联
ALTER TABLE visual_dashboard_item ADD COLUMN page_id VARCHAR(32) COMMENT '所属页面ID(多页面模式)';

-- 5. 版本快照扩展：新增页面快照
ALTER TABLE visual_dashboard_version ADD COLUMN pages_json LONGTEXT COMMENT '页面快照(JSON数组)';

-- 6. 仪表板模板表（支持模板导入导出）
CREATE TABLE visual_dashboard_template (
    id              VARCHAR(32) PRIMARY KEY,
    template_name   VARCHAR(200) NOT NULL COMMENT '模板名称',
    template_code   VARCHAR(100) NOT NULL UNIQUE COMMENT '模板编码',
    category        VARCHAR(50) DEFAULT 'COMMON' COMMENT '分类: COMMON/BIGSCREEN/REPORT/MOBILE',
    thumbnail       VARCHAR(500) COMMENT '缩略图URL',
    description     VARCHAR(500) COMMENT '模板描述',
    canvas_width    INT DEFAULT 1920,
    canvas_height   INT DEFAULT 1080,
    canvas_config   TEXT COMMENT '画布配置(JSON)',
    items_json      LONGTEXT COMMENT '组件配置快照(JSON)',
    tags            VARCHAR(300) COMMENT '标签(逗号分隔)',
    usage_count     INT DEFAULT 0 COMMENT '使用次数',
    status          TINYINT DEFAULT 1,
    create_by       VARCHAR(32),
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP()
) COMMENT '仪表板模板表';

-- 7. 插入常用画布模板
INSERT INTO visual_dashboard_template (id, template_name, template_code, category, description, canvas_width, canvas_height, tags) VALUES
('tpl_1', '数据监控大屏', 'BIGSCREEN_1920', 'BIGSCREEN', '1920×1080 数据监控全屏大屏', 1920, 1080, '大屏,监控,数据'),
('tpl_2', '指挥中心大屏', 'BIGSCREEN_4K', 'BIGSCREEN', '3840×2160 4K指挥中心大屏', 3840, 2160, '4K,指挥中心,大屏'),
('tpl_3', 'PC数据分析报表', 'REPORT_1440', 'REPORT', '1440×900 PC端数据分析报表', 1440, 900, 'PC,报表,分析'),
('tpl_4', '移动端数据看板', 'MOBILE_375', 'MOBILE', '375×667 移动端数据看板', 375, 667, '移动,看板,手机'),
('tpl_5', '销售数据分析', 'SALES_DASHBOARD', 'COMMON', '销售数据综合分析看板', 1920, 1080, '销售,分析,业绩'),
('tpl_6', '运营数据看板', 'OPS_DASHBOARD', 'COMMON', '运营数据监控看板', 1920, 1080, '运营,监控,数据');

-- 8. 组件联动配置表
CREATE TABLE visual_item_linkage (
    id              VARCHAR(32) PRIMARY KEY,
    dashboard_id    VARCHAR(32) NOT NULL COMMENT '仪表板ID',
    source_item_id  VARCHAR(32) NOT NULL COMMENT '源组件ID',
    target_item_id  VARCHAR(32) NOT NULL COMMENT '目标组件ID',
    link_type       VARCHAR(50) DEFAULT 'FILTER' COMMENT '联动类型: FILTER/HIGHLIGHT/DRILL',
    link_config     TEXT COMMENT '联动配置(JSON)',
    status          TINYINT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP(),
    INDEX idx_linkage_dashboard (dashboard_id),
    INDEX idx_linkage_source (source_item_id)
) COMMENT '组件联动配置表';

COMMIT;
