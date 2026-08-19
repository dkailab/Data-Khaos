-- ============================================================
-- Data Khaos - 数据集种子数据
-- 依赖：seed-demo.sql 已执行、数据源已配置
-- 执行：docker exec -i dk-mysql mysql -uroot -proot123456 data_khaos < seed-datasets.sql
-- ============================================================
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

-- 默认数据源ID
SET @ds = _utf8mb4'2087424798053343233' COLLATE utf8mb4_general_ci;

-- 清空
DELETE FROM visual_dataset WHERE id IN ('ds_001', 'ds_002', 'ds_003');

-- ============================================================
-- 数据集1：电商订单汇总（SQL模式）
-- ============================================================
INSERT INTO visual_dataset (id, name, code, description, dataset_type, datasource_id, query_sql, fields_json, variables_json, refresh_interval, owner_id, org_id, visibility, status, version, deleted, create_by) VALUES
('ds_001', '电商订单汇总', 'DS_EC_ORDER_SUMMARY', '电商订单多维汇总，含区域、渠道、类目维度及销售额、利润等指标', 'SQL', @ds,
'SELECT DATE_FORMAT(o.order_date, ''%Y-%m'') AS month, r.region, r.province, c.channel, cat.category, ROUND(SUM(o.amount)) AS amount, ROUND(SUM(o.profit)) AS profit, SUM(o.qty) AS qty, COUNT(*) AS order_count FROM demo_fact_order o JOIN demo_dim_region r ON o.region_id = r.id JOIN demo_dim_channel c ON o.channel_id = c.id JOIN demo_dim_category cat ON o.category_id = cat.id GROUP BY month, r.region, r.province, c.channel, cat.category',
'[{"fieldName":"月份","fieldCode":"month","fieldType":"DIMENSION","dataType":"DATE","sortOrder":1},
 {"fieldName":"大区","fieldCode":"region","fieldType":"DIMENSION","dataType":"STRING","sortOrder":2},
 {"fieldName":"省份","fieldCode":"province","fieldType":"DIMENSION","dataType":"STRING","sortOrder":3},
 {"fieldName":"渠道","fieldCode":"channel","fieldType":"DIMENSION","dataType":"STRING","sortOrder":4},
 {"fieldName":"类目","fieldCode":"category","fieldType":"DIMENSION","dataType":"STRING","sortOrder":5},
 {"fieldName":"销售额","fieldCode":"amount","fieldType":"METRIC","dataType":"DECIMAL","aggType":"SUM","sortOrder":6},
 {"fieldName":"利润","fieldCode":"profit","fieldType":"METRIC","dataType":"DECIMAL","aggType":"SUM","sortOrder":7},
 {"fieldName":"数量","fieldCode":"qty","fieldType":"METRIC","dataType":"INT","aggType":"SUM","sortOrder":8},
 {"fieldName":"订单数","fieldCode":"order_count","fieldType":"METRIC","dataType":"INT","aggType":"COUNT","sortOrder":9}]',
'[]', 300, '1', '11', 'ORG', 'PUBLISHED', 1, 0, '1');

-- ============================================================
-- 数据集2：区域销售日报（SQL模式）
-- ============================================================
INSERT INTO visual_dataset (id, name, code, description, dataset_type, datasource_id, query_sql, fields_json, variables_json, refresh_interval, owner_id, org_id, visibility, status, version, deleted, create_by) VALUES
('ds_002', '区域销售日报', 'DS_REGION_DAILY', '近30天分区域逐日销售明细', 'SQL', @ds,
'SELECT d.order_date AS date, d.city, d.channel, d.category, d.amount, d.qty, d.profit FROM demo_daily_order d',
'[{"fieldName":"日期","fieldCode":"date","fieldType":"DIMENSION","dataType":"DATE","sortOrder":1},
 {"fieldName":"城市","fieldCode":"city","fieldType":"DIMENSION","dataType":"STRING","sortOrder":2},
 {"fieldName":"渠道","fieldCode":"channel","fieldType":"DIMENSION","dataType":"STRING","sortOrder":3},
 {"fieldName":"类目","fieldCode":"category","fieldType":"DIMENSION","dataType":"STRING","sortOrder":4},
 {"fieldName":"销售额","fieldCode":"amount","fieldType":"METRIC","dataType":"DECIMAL","aggType":"SUM","sortOrder":5},
 {"fieldName":"数量","fieldCode":"qty","fieldType":"METRIC","dataType":"INT","aggType":"SUM","sortOrder":6},
 {"fieldName":"利润","fieldCode":"profit","fieldType":"METRIC","dataType":"DECIMAL","aggType":"SUM","sortOrder":7}]',
'[]', 600, '1', '11', 'ORG', 'PUBLISHED', 1, 0, '1');

-- ============================================================
-- 数据集3：类目渠道交叉汇总（SQL模式）
-- ============================================================
INSERT INTO visual_dataset (id, name, code, description, dataset_type, datasource_id, query_sql, fields_json, variables_json, refresh_interval, owner_id, org_id, visibility, status, version, deleted, create_by) VALUES
('ds_003', '类目渠道交叉分析', 'DS_CAT_CHANNEL_CROSS', '类目×渠道销售额与利润交叉汇总，适合热力图/堆叠图', 'SQL', @ds,
'SELECT cat.category, c.channel, ROUND(SUM(o.amount)) AS amount, ROUND(SUM(o.profit)) AS profit, COUNT(*) AS order_count FROM demo_fact_order o JOIN demo_dim_category cat ON o.category_id = cat.id JOIN demo_dim_channel c ON o.channel_id = c.id GROUP BY cat.category, c.channel',
'[{"fieldName":"类目","fieldCode":"category","fieldType":"DIMENSION","dataType":"STRING","sortOrder":1},
 {"fieldName":"渠道","fieldCode":"channel","fieldType":"DIMENSION","dataType":"STRING","sortOrder":2},
 {"fieldName":"销售额","fieldCode":"amount","fieldType":"METRIC","dataType":"DECIMAL","aggType":"SUM","sortOrder":3},
 {"fieldName":"利润","fieldCode":"profit","fieldType":"METRIC","dataType":"DECIMAL","aggType":"SUM","sortOrder":4},
 {"fieldName":"订单数","fieldCode":"order_count","fieldType":"METRIC","dataType":"INT","aggType":"COUNT","sortOrder":5}]',
'[]', 300, '1', '11', 'ORG', 'PUBLISHED', 1, 0, '1');

COMMIT;
