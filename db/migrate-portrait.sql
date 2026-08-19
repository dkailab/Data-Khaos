-- ============================================================
-- 用户画像模块 建表脚本（在 data_khaos 库执行）
-- 迁移方式: source db/migrate-portrait.sql
-- ============================================================

USE data_khaos;

-- 1. 画像标签分类
CREATE TABLE IF NOT EXISTS portrait_tag_category (
    id          VARCHAR(32)  NOT NULL PRIMARY KEY COMMENT '主键ID',
    name        VARCHAR(100) NOT NULL COMMENT '分类名称',
    code        VARCHAR(100) COMMENT '分类编码',
    sort_order  INT DEFAULT 0 COMMENT '排序号',
    status      TINYINT DEFAULT 1 COMMENT '状态 1:启用 0:禁用',
    deleted     TINYINT DEFAULT 0 COMMENT '逻辑删除 0:正常 1:删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   VARCHAR(64) COMMENT '创建人',
    update_by   VARCHAR(64) COMMENT '更新人',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB COMMENT='用户画像标签分类表';

-- 2. 画像标签定义
CREATE TABLE IF NOT EXISTS portrait_tag (
    id            VARCHAR(32)  NOT NULL PRIMARY KEY COMMENT '主键ID',
    category_id   VARCHAR(32)  NOT NULL COMMENT '所属分类ID',
    name          VARCHAR(100) NOT NULL COMMENT '标签名称',
    code          VARCHAR(100) COMMENT '标签编码',
    tag_type      VARCHAR(20) DEFAULT 'STR' COMMENT '标签类型 BOOL/NUMBER/STR/ENUM',
    unit          VARCHAR(20) COMMENT '单位(数值类型)',
    enum_options  TEXT COMMENT '枚举可选值(JSON数组)',
    description   VARCHAR(500) COMMENT '标签说明',
    status        TINYINT DEFAULT 1 COMMENT '状态 1:启用 0:禁用',
    deleted       TINYINT DEFAULT 0 COMMENT '逻辑删除 0:正常 1:删除',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     VARCHAR(64) COMMENT '创建人',
    update_by     VARCHAR(64) COMMENT '更新人',
    KEY idx_category (category_id),
    UNIQUE KEY uk_code (category_id, code)
) ENGINE=InnoDB COMMENT='用户画像标签定义表';

-- 3. 用户标签值
CREATE TABLE IF NOT EXISTS portrait_user_tag (
    id          VARCHAR(32) NOT NULL PRIMARY KEY COMMENT '主键ID',
    user_key    VARCHAR(100) NOT NULL COMMENT '用户唯一标识(业务用户ID)',
    user_name   VARCHAR(100) COMMENT '用户名称(冗余展示)',
    tag_id      VARCHAR(32) NOT NULL COMMENT '标签ID',
    tag_value   VARCHAR(200) COMMENT '标签值',
    tag_time    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '标签时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   VARCHAR(64) COMMENT '创建人',
    UNIQUE KEY uk_user_tag (user_key, tag_id),
    KEY idx_tag (tag_id)
) ENGINE=InnoDB COMMENT='用户画像标签值表';

-- 预置演示分类与标签
INSERT INTO portrait_tag_category (id, name, code, sort_order, status, deleted) VALUES
  ('pc_basic', '基础属性', 'basic', 1, 1, 0),
  ('pc_value', '价值分层', 'value', 2, 1, 0),
  ('pc_behavior', '行为偏好', 'behavior', 3, 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO portrait_tag (id, category_id, name, code, tag_type, unit, enum_options, description, status, deleted) VALUES
  ('pt_gender', 'pc_basic', '性别', 'gender', 'ENUM', NULL, '["男","女","未知"]', '用户性别', 1, 0),
  ('pt_age_band', 'pc_basic', '年龄段', 'ageBand', 'ENUM', NULL, '["18以下","18-25","26-35","36-45","46以上"]', '用户年龄段', 1, 0),
  ('pt_level', 'pc_value', '用户层级', 'level', 'ENUM', NULL, '["普通","优质","高潜","VIP"]', '用户价值等级', 1, 0),
  ('pt_consume', 'pc_value', '累计消费额(元)', 'totalConsume', 'NUMBER', '元', NULL, '累计消费金额', 1, 0),
  ('pt_active', 'pc_behavior', '活跃度', 'activeLevel', 'BOOL', NULL, NULL, '是否活跃用户', 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);