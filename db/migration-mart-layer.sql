-- ============================================================
-- 数仓模型权限与分层 增量迁移（对已初始化库执行）
-- 目标库：data_khaos
-- ============================================================

-- 条件加列存储过程
DROP PROCEDURE IF EXISTS add_col_if_missing;
DELIMITER ;;
CREATE PROCEDURE add_col_if_missing(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl VARCHAR(255))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = tbl AND column_name = col
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN ', ddl);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END ;;
DELIMITER ;

CALL add_col_if_missing('mart_model',     'project_group_id', '`project_group_id` VARCHAR(32) COMMENT ''项目组ID（权限隔离）'' AFTER `id`');
CALL add_col_if_missing('mart_model',     'layer_id',         '`layer_id` VARCHAR(32) COMMENT ''数仓分层ID'' AFTER `project_group_id`');
CALL add_col_if_missing('mart_metric',    'project_group_id', '`project_group_id` VARCHAR(32) COMMENT ''项目组ID（权限隔离）'' AFTER `id`');
CALL add_col_if_missing('mart_dimension', 'project_group_id', '`project_group_id` VARCHAR(32) COMMENT ''项目组ID（权限隔离）'' AFTER `id`');
CALL add_col_if_missing('mart_model_rel', 'project_group_id', '`project_group_id` VARCHAR(32) COMMENT ''项目组ID（权限隔离）'' AFTER `id`');

DROP PROCEDURE IF EXISTS add_col_if_missing;

-- 数仓分层表
CREATE TABLE IF NOT EXISTS mart_warehouse_layer (
    id          VARCHAR(32) NOT NULL PRIMARY KEY COMMENT '主键ID',
    layer_code  VARCHAR(32) NOT NULL COMMENT '分层编码 ODS/DWD/DWS/ADS',
    layer_name  VARCHAR(100) NOT NULL COMMENT '分层名称',
    layer_desc  VARCHAR(500) COMMENT '分层说明',
    sort_order  INT DEFAULT 0 COMMENT '排序',
    status      TINYINT DEFAULT 1 COMMENT '状态 0:停用 1:启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_layer_code (layer_code)
) ENGINE=InnoDB COMMENT='数仓分层表（一线大厂标准：ODS/DWD/DWS/ADS）';

-- 分层种子数据
INSERT INTO mart_warehouse_layer (id, layer_code, layer_name, layer_desc, sort_order, status) VALUES
('layer_ods', 'ODS', '操作数据层', 'Original Data Store，原始数据落地层，与源系统保持一致', 1, 1),
('layer_dwd', 'DWD', '明细数据层', 'Data Warehouse Detail，清洗加工后的明细数据，遵循维度建模', 2, 1),
('layer_dws', 'DWS', '汇总数据层', 'Data Warehouse Summary，按主题/粒度汇总的轻度汇总数据', 3, 1),
('layer_ads', 'ADS', '应用数据层', 'Application Data Store，面向应用/报表的指标与宽表', 4, 1)
ON DUPLICATE KEY UPDATE layer_name = VALUES(layer_name), layer_desc = VALUES(layer_desc);