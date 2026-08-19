-- ============================================================
-- 仪表板 V2 迁移脚本
-- 新增：数据集(visual_dataset)、分析板(visual_dashboard_board)
-- 扩展：visual_dashboard / visual_dashboard_item 支持新配置格式
-- ============================================================

-- ==================== 1. 数据集表 ====================
CREATE TABLE visual_dataset (
    id              VARCHAR(32) PRIMARY KEY,
    name            VARCHAR(200) NOT NULL COMMENT '数据集名称',
    code            VARCHAR(100) COMMENT '数据集编码',
    description     VARCHAR(500) COMMENT '描述',
    dataset_type    VARCHAR(20) NOT NULL COMMENT 'SQL / MODEL',
    datasource_id   VARCHAR(32) COMMENT 'SQL模式-数据源ID',
    query_sql       TEXT COMMENT 'SQL模式-查询SQL',
    model_id        VARCHAR(32) COMMENT 'MODEL模式-模型ID',
    fields_json     TEXT COMMENT '字段定义(JSON数组)',
    variables_json  TEXT COMMENT '变量定义(JSON数组)',
    refresh_interval INT DEFAULT 0 COMMENT '刷新间隔(秒)',
    owner_id        VARCHAR(32) COMMENT '所有者ID',
    org_id          VARCHAR(32) COMMENT '组织ID',
    visibility      VARCHAR(20) DEFAULT 'PRIVATE' COMMENT 'PRIVATE/ORG/PUBLIC',
    status          VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/OFFLINE',
    version         INT DEFAULT 1 COMMENT '版本号',
    deleted         TINYINT DEFAULT 0,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP(),
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP(),
    create_by       VARCHAR(32),
    update_by       VARCHAR(32),
    INDEX idx_dataset_org (org_id),
    INDEX idx_dataset_status (status),
    INDEX idx_dataset_type (dataset_type)
) COMMENT '数据集定义表';

-- ==================== 2. 分析板表 ====================
CREATE TABLE visual_dashboard_board (
    id              VARCHAR(32) PRIMARY KEY,
    dashboard_id    VARCHAR(32) NOT NULL COMMENT '所属仪表板',
    name            VARCHAR(200) NOT NULL COMMENT '分析板名称',
    icon            VARCHAR(50) COMMENT '图标',
    layout_mode     VARCHAR(20) DEFAULT 'CANVAS' COMMENT 'CANVAS(自由画布) / GRID(网格)',
    canvas_width    INT DEFAULT 1920 COMMENT '画布宽度(px)',
    canvas_height   INT DEFAULT 1080 COMMENT '画布高度(px)',
    canvas_bg       VARCHAR(50) DEFAULT '#f5f7fa' COMMENT '画布背景色',
    grid_config     VARCHAR(500) COMMENT '网格配置(JSON)',
    filters         TEXT COMMENT '分析板级筛选器(JSON)',
    sort_order      INT DEFAULT 0 COMMENT '排序',
    status          TINYINT DEFAULT 1 COMMENT '1:启用 0:禁用',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP(),
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP(),
    INDEX idx_board_dashboard (dashboard_id)
) COMMENT '仪表板分析板表（多Tab）';

-- ==================== 3. 扩展 visual_dashboard 表 ====================
ALTER TABLE visual_dataset ADD COLUMN canvas_width INT DEFAULT 1920 COMMENT '默认画布宽度';
ALTER TABLE visual_dataset ADD COLUMN canvas_height INT DEFAULT 1080 COMMENT '默认画布高度';
ALTER TABLE visual_dataset ADD COLUMN canvas_bg VARCHAR(50) DEFAULT '#f5f7fa' COMMENT '默认画布背景';

-- ==================== 4. 扩展 visual_dashboard_item 表 ====================
ALTER TABLE visual_dashboard_item ADD COLUMN board_id VARCHAR(32) COMMENT '所属分析板ID';
ALTER TABLE visual_dashboard_item ADD COLUMN data_config TEXT COMMENT '数据配置(JSON): 数据集+维度+指标+过滤';
ALTER TABLE visual_dashboard_item ADD COLUMN style_config TEXT COMMENT '样式配置(JSON): 标题+图例+颜色+边框';
ALTER TABLE visual_dashboard_item ADD COLUMN z_index INT DEFAULT 1 COMMENT '层级';
ALTER TABLE visual_dashboard_item ADD COLUMN bg_color VARCHAR(50) COMMENT '背景色';
ALTER TABLE visual_dashboard_item ADD COLUMN border_radius INT DEFAULT 6 COMMENT '圆角';
ALTER TABLE visual_dashboard_item ADD COLUMN border_width INT DEFAULT 1 COMMENT '边框宽度';
ALTER TABLE visual_dashboard_item ADD COLUMN border_color VARCHAR(50) COMMENT '边框颜色';
ALTER TABLE visual_dashboard_item ADD COLUMN locked TINYINT DEFAULT 0 COMMENT '锁定';
ALTER TABLE visual_dashboard_item ADD COLUMN visible TINYINT DEFAULT 1 COMMENT '可见性';
ALTER TABLE visual_dashboard_item ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP();

ALTER TABLE visual_dashboard_item ADD INDEX idx_item_board (board_id);

-- ==================== 5. 版本快照扩展 ====================
ALTER TABLE visual_dashboard_version ADD COLUMN boards_json LONGTEXT COMMENT '分析板快照(JSON)';

-- ==================== 6. 组件联动配置表 ====================
CREATE TABLE IF NOT EXISTS visual_item_linkage (
    id              VARCHAR(32) PRIMARY KEY,
    dashboard_id    VARCHAR(32) NOT NULL COMMENT '仪表板ID',
    source_item_id  VARCHAR(32) NOT NULL COMMENT '源组件ID',
    target_item_id  VARCHAR(32) NOT NULL COMMENT '目标组件ID',
    link_type       VARCHAR(50) DEFAULT 'FILTER' COMMENT 'FILTER/HIGHLIGHT/DRILL',
    link_config     TEXT COMMENT '联动配置(JSON)',
    status          TINYINT DEFAULT 1,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP(),
    INDEX idx_linkage_dashboard (dashboard_id),
    INDEX idx_linkage_source (source_item_id)
) COMMENT '组件联动配置表';

-- ==================== 7. 仪表板模板表 ====================
CREATE TABLE IF NOT EXISTS visual_dashboard_template (
    id              VARCHAR(32) PRIMARY KEY,
    template_name   VARCHAR(200) NOT NULL COMMENT '模板名称',
    template_code   VARCHAR(100) NOT NULL UNIQUE COMMENT '模板编码',
    category        VARCHAR(50) DEFAULT 'COMMON' COMMENT '分类',
    thumbnail       VARCHAR(500) COMMENT '缩略图URL',
    description     VARCHAR(500) COMMENT '描述',
    canvas_width    INT DEFAULT 1920,
    canvas_height   INT DEFAULT 1080,
    canvas_config   TEXT COMMENT '画布配置(JSON)',
    items_json      LONGTEXT COMMENT '组件配置快照(JSON)',
    boards_json     LONGTEXT COMMENT '分析板配置快照(JSON)',
    tags            VARCHAR(300) COMMENT '标签',
    usage_count     INT DEFAULT 0 COMMENT '使用次数',
    status          TINYINT DEFAULT 1,
    create_by       VARCHAR(32),
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP()
) COMMENT '仪表板模板表';

-- ==================== 8. 插入默认模板 ====================
INSERT IGNORE INTO visual_dashboard_template (id, template_name, template_code, category, description, canvas_width, canvas_height, tags) VALUES
('tpl_1', '数据监控大屏', 'BIGSCREEN_1920', 'BIGSCREEN', '1920×1080 数据监控全屏大屏', 1920, 1080, '大屏,监控,数据'),
('tpl_2', '指挥中心大屏', 'BIGSCREEN_4K', 'BIGSCREEN', '3840×2160 4K指挥中心大屏', 3840, 2160, '4K,指挥中心,大屏'),
('tpl_3', 'PC数据分析报表', 'REPORT_1600', 'REPORT', '1600×900 PC端数据分析报表', 1600, 900, 'PC,报表,分析'),
('tpl_4', '移动端数据看板', 'MOBILE_375', 'MOBILE', '375×667 移动端数据看板', 375, 667, '移动,看板,手机'),
('tpl_5', '销售数据分析', 'SALES_DASHBOARD', 'COMMON', '销售数据综合分析看板', 1920, 1080, '销售,分析,业绩'),
('tpl_6', '运营数据看板', 'OPS_DASHBOARD', 'COMMON', '运营数据监控看板', 1920, 1080, '运营,监控,数据');

COMMIT;
