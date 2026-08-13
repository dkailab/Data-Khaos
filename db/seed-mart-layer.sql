-- ============================================================
-- 数仓分层 种子数据（一线大厂标准分层：ODS/DWD/DWS/ADS）
-- 说明：依赖 mysql-init.sql 已执行（含 mart_warehouse_layer 表）
-- ============================================================

INSERT INTO mart_warehouse_layer (id, layer_code, layer_name, layer_desc, sort_order, status) VALUES
('layer_ods', 'ODS', '操作数据层', 'Original Data Store，原始数据落地层，与源系统保持一致', 1, 1),
('layer_dwd', 'DWD', '明细数据层', 'Data Warehouse Detail，清洗加工后的明细数据，遵循维度建模', 2, 1),
('layer_dws', 'DWS', '汇总数据层', 'Data Warehouse Summary，按主题/粒度汇总的轻度汇总数据', 3, 1),
('layer_ads', 'ADS', '应用数据层', 'Application Data Store，面向应用/报表的指标与宽表', 4, 1)
ON DUPLICATE KEY UPDATE layer_name = VALUES(layer_name), layer_desc = VALUES(layer_desc);