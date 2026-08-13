-- ============================================================
-- 项目组权限 种子数据与测试用例
-- 说明：依赖 mysql-init.sql 已执行（含 sg_* 四表、sys_table_permission.project_group_id）
-- 覆盖：组织(业务线) → 项目组 → 人 三级模型；组长/开发者/使用者 三档角色与能力位；
--       表权限按「个人 + 项目组」两种主体并存取并集，以及当前项目组上下文。
-- ============================================================

-- 1) 测试用户（依赖 sys_user 存在；密码为占位，实际登录走 auth 服务）
INSERT INTO sys_user (id, username, password, real_name, status) VALUES
('pg_user_001', 'zhangsan', 'x', '张三', 1),
('pg_user_002', 'lisi',     'x', '李四', 1),
('pg_user_003', 'wangwu',   'x', '王五', 1),
('pg_user_004', 'zhaoliu',  'x', '赵六', 1)
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name);

-- 2) 数据源（表权限授权目标，需与 datasource 服务建的数据源一致；占位 ID）
INSERT INTO meta_datasource (id, ds_name, ds_type, host, port, database_name, status) VALUES
('ds_dw', '数仓 DWD', 'MYSQL', '127.0.0.1', 3306, 'data_khaos', 1),
('ds_app', '应用库',  'MYSQL', '127.0.0.1', 3306, 'data_khaos', 1)
ON DUPLICATE KEY UPDATE ds_name = VALUES(ds_name);

-- 3) 项目组（归属于组织 11「数据平台部」）
INSERT INTO sg_project_group (id, org_id, project_name, project_code, leader_id, status, sort_order) VALUES
('pg_001', '11', '零售数仓项目组',      'PG_RETAIL', 'pg_user_001', 1, 1),
('pg_002', '11', '营销分析项目组',      'PG_MARKET', 'pg_user_002', 1, 2),
('pg_003', '11', '风险管控项目组',      'PG_RISK',   'pg_user_003', 1, 3)
ON DUPLICATE KEY UPDATE project_name = VALUES(project_name);

-- 4) 项目组角色：三档全局模板（project_group_id 为空）+ 组内自定义示例
INSERT INTO sg_project_role (id, org_id, project_group_id, role_name, role_code, capability_flags, status, sort_order) VALUES
-- 全局模板
('pg_role_leader',   '11', NULL, '组长',   'PG_LEADER',   '["meta:browse","query:execute","table:manage","model:develop","model:publish","model:browse","report:develop","report:publish","report:browse","task:develop","task:schedule","approval:apply","approval:approve","pg:manage","quality:manage","quality:browse","quality:run"]', 1, 1),
('pg_role_dev',      '11', NULL, '开发者', 'PG_DEV',      '["meta:browse","query:execute","model:develop","model:browse","report:develop","report:browse","task:develop","approval:apply","quality:manage","quality:browse","quality:run"]', 1, 2),
('pg_role_user',     '11', NULL, '使用者', 'PG_USER',     '["meta:browse","model:browse","report:browse","approval:apply","quality:browse"]', 1, 3),
-- 零售组自定义：开发者扩展了表管理权限
('pg_role_retail_dev', '11', 'pg_001', '零售-高级开发者', 'PG_RETAIL_DEV', '["meta:browse","query:execute","table:manage","model:develop","model:browse","report:develop","report:browse","task:develop","approval:apply","quality:manage","quality:browse","quality:run"]', 1, 1)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- 5) 项目组成员（含组内角色 + 主组标记，用于当前项目组上下文）
INSERT INTO sg_project_group_member (id, project_group_id, user_id, project_role_id, is_primary) VALUES
-- 零售组：张三组长(主组)、李四开发者
('pgm_001', 'pg_001', 'pg_user_001', 'pg_role_leader', 1),
('pgm_002', 'pg_001', 'pg_user_002', 'pg_role_retail_dev', 0),
-- 营销组：李四组长(主组)、王五使用者
('pgm_003', 'pg_002', 'pg_user_002', 'pg_role_leader', 1),
('pgm_004', 'pg_002', 'pg_user_003', 'pg_role_user', 0),
-- 风险组：王五组长(主组)、赵六使用者
('pgm_005', 'pg_003', 'pg_user_003', 'pg_role_leader', 1),
('pgm_006', 'pg_003', 'pg_user_004', 'pg_role_user', 0)
ON DUPLICATE KEY UPDATE project_role_id = VALUES(project_role_id);

-- 6) 项目组资源（组下绑定的报表/任务/表）
INSERT INTO sg_project_group_resource (id, project_group_id, resource_type, resource_id) VALUES
('pgr_001', 'pg_001', 'REPORT', 'dash_retail_001'),
('pgr_002', 'pg_001', 'TASK',   'task_retail_001'),
('pgr_003', 'pg_002', 'REPORT', 'dash_market_001')
ON DUPLICATE KEY UPDATE resource_id = VALUES(resource_id);

-- 7) 表权限：个人 + 项目组 两种主体并存（取并集）
INSERT INTO sys_table_permission (id, datasource_id, database_name, table_name, permission_type, role_id, user_id, project_group_id, grant_type, status) VALUES
-- 项目组授权：零售组获取 ds_dw 下 DWD 层全表 SELECT
('tp_001', 'ds_dw', 'data_khaos', 'dwd_order',    'SELECT', NULL, NULL, 'pg_001', 'PROJECT_GROUP', 1),
('tp_002', 'ds_dw', 'data_khaos', 'dwd_customer', 'SELECT', NULL, NULL, 'pg_001', 'PROJECT_GROUP', 1),
-- 项目组授权：营销组获取 app 库订单表 SELECT + INSERT
('tp_003', 'ds_app', 'data_khaos', 'app_order',   'SELECT', NULL, NULL, 'pg_002', 'PROJECT_GROUP', 1),
('tp_004', 'ds_app', 'data_khaos', 'app_order',   'INSERT', NULL, NULL, 'pg_002', 'PROJECT_GROUP', 1),
-- 个人授权：仅张三（组长）额外拥有 dwd_order 的 UPDATE（个人优先级高于项目组，用于收敛/放行）
('tp_005', 'ds_dw', 'data_khaos', 'dwd_order',    'UPDATE', NULL, 'pg_user_001', NULL, 'USER', 1)
ON DUPLICATE KEY UPDATE permission_type = VALUES(permission_type);

-- 8) 行/列策略：按项目组绑定（风险组行策略 + 零售组列脱敏）
INSERT INTO sys_row_policy (id, policy_name, target_table, expression, role_id, user_id, project_group_id, status) VALUES
('rp_001', '风险组-仅本人数据', 'risk_order', 'risk_level <> ''高''', NULL, NULL, 'pg_003', 1)
ON DUPLICATE KEY UPDATE policy_name = VALUES(policy_name);

INSERT INTO sys_column_policy (id, policy_name, target_table, column_name, mask_type, role_id, user_id, project_group_id, status) VALUES
('cp_001', '零售组-手机号脱敏', 'dwd_customer', 'phone', 'MASK', NULL, NULL, 'pg_001', 1)
ON DUPLICATE KEY UPDATE policy_name = VALUES(policy_name);