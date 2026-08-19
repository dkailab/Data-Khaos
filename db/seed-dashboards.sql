-- ============================================================
-- Data Khaos - 测试仪表板种子数据（V2格式：数据集+分析板+dataConfig）
-- 依赖：seed-datasets.sql、seed-demo.sql 已执行
-- 执行：docker exec -i dk-mysql mysql -uroot -proot123456 data_khaos < seed-dashboards.sql
-- ============================================================
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

-- 数据源ID
SET @ds = _utf8mb4'2087424798053343233' COLLATE utf8mb4_general_ci;

-- ============================================================
-- 仪表板1：销售总览
-- ============================================================
SET @d1 = _utf8mb4'2087425001321390081' COLLATE utf8mb4_general_ci;
DELETE FROM visual_dashboard_item WHERE dashboard_id = @d1;
DELETE FROM visual_dashboard_board WHERE dashboard_id = @d1;

INSERT INTO visual_dashboard (id, name, description, refresh_interval, status, version, create_by) VALUES
(@d1, '销售总览', '电商核心指标一览，含订单/利润/趋势', 60, 1, 0, '1') ON DUPLICATE KEY UPDATE name = '销售总览';

-- 默认分析板
SET @b1 = _utf8mb4'board_d1_main' COLLATE utf8mb4_general_ci;
INSERT INTO visual_dashboard_board (id, dashboard_id, name, icon, layout_mode, canvas_width, canvas_height, sort_order, status) VALUES
(@b1, @d1, '概览', 'DataAnalysis', 'CANVAS', 1920, 1080, 0, 1) ON DUPLICATE KEY UPDATE name = '概览';

-- KPI 卡片：订单总额
INSERT INTO visual_dashboard_item (id, dashboard_id, board_id, title, chart_type, datasource_id, query_sql, config, data_config, style_config, pos_x, pos_y, width, height, z_index, bg_color, border_radius, border_width, border_color, locked, visible) VALUES
('it_d1_01', @d1, @b1, '订单总额', 'NUMBER', @ds,
 'SELECT ROUND(SUM(amount)) AS amount FROM demo_fact_order',
 '{"valueColumn":"amount","unit":"元","thousand":true,"decimals":0}',
 '{"datasetId":"ds_001","dimensions":[],"metrics":[{"fieldCode":"amount","aggType":"SUM","alias":"订单总额"}],"filters":[],"sorts":[]}',
 '{"title":{"show":true,"text":"订单总额"},"legend":{"show":false},"colors":["#5470c6"]}',
 0, 0, 240, 120, 1, '#fff', 8, 1, '#e4e7ed', 0, 1);

-- KPI 卡片：订单量
INSERT INTO visual_dashboard_item (id, dashboard_id, board_id, title, chart_type, datasource_id, query_sql, config, data_config, style_config, pos_x, pos_y, width, height, z_index, bg_color, border_radius, border_width, border_color, locked, visible) VALUES
('it_d1_02', @d1, @b1, '订单量', 'NUMBER', @ds,
 'SELECT COUNT(*) AS order_count FROM demo_fact_order',
 '{"valueColumn":"order_count","unit":"件","thousand":true,"decimals":0}',
 '{"datasetId":"ds_001","dimensions":[],"metrics":[{"fieldCode":"order_count","aggType":"SUM","alias":"订单量"}],"filters":[],"sorts":[]}',
 '{"title":{"show":true,"text":"订单量"},"legend":{"show":false},"colors":["#91cc75"]}',
 260, 0, 240, 120, 1, '#fff', 8, 1, '#e4e7ed', 0, 1);

-- KPI 卡片：销售利润
INSERT INTO visual_dashboard_item (id, dashboard_id, board_id, title, chart_type, datasource_id, query_sql, config, data_config, style_config, pos_x, pos_y, width, height, z_index, bg_color, border_radius, border_width, border_color, locked, visible) VALUES
('it_d1_03', @d1, @b1, '销售利润', 'NUMBER', @ds,
 'SELECT ROUND(SUM(profit)) AS profit FROM demo_fact_order',
 '{"valueColumn":"profit","unit":"元","thousand":true,"decimals":0}',
 '{"datasetId":"ds_001","dimensions":[],"metrics":[{"fieldCode":"profit","aggType":"SUM","alias":"销售利润"}],"filters":[],"sorts":[]}',
 '{"title":{"show":true,"text":"销售利润"},"legend":{"show":false},"colors":["#fac858"]}',
 520, 0, 240, 120, 1, '#fff', 8, 1, '#e4e7ed', 0, 1);

-- 月度趋势折线图
INSERT INTO visual_dashboard_item (id, dashboard_id, board_id, title, chart_type, datasource_id, query_sql, config, data_config, style_config, pos_x, pos_y, width, height, z_index, bg_color, border_radius, border_width, border_color, locked, visible) VALUES
('it_d1_04', @d1, @b1, '月度销售趋势', 'LINE', @ds,
 'SELECT DATE_FORMAT(order_date,"%Y-%m") AS month, ROUND(SUM(amount)) AS amount FROM demo_fact_order GROUP BY month ORDER BY month',
 '{"xAxisColumn":"month","valueColumn":"amount"}',
 '{"datasetId":"ds_001","dimensions":[{"fieldCode":"month","alias":"月份"}],"metrics":[{"fieldCode":"amount","aggType":"SUM","alias":"销售额"}],"filters":[],"sorts":[{"field":"month","order":"asc"}]}',
 '{"title":{"show":true,"text":"月度销售趋势"},"legend":{"show":true,"position":"top"}}',
 0, 140, 760, 340, 1, '#fff', 8, 1, '#e4e7ed', 0, 1);

-- 渠道销售占比饼图
INSERT INTO visual_dashboard_item (id, dashboard_id, board_id, title, chart_type, datasource_id, query_sql, config, data_config, style_config, pos_x, pos_y, width, height, z_index, bg_color, border_radius, border_width, border_color, locked, visible) VALUES
('it_d1_05', @d1, @b1, '渠道销售占比', 'PIE', @ds,
 'SELECT c.channel, ROUND(SUM(o.amount)) AS amount FROM demo_fact_order o JOIN demo_dim_channel c ON o.channel_id=c.id GROUP BY c.channel',
 '{"xAxisColumn":"channel","valueColumn":"amount"}',
 '{"datasetId":"ds_001","dimensions":[{"fieldCode":"channel","alias":"渠道"}],"metrics":[{"fieldCode":"amount","aggType":"SUM","alias":"销售额"}],"filters":[],"sorts":[]}',
 '{"title":{"show":true,"text":"渠道销售占比"},"legend":{"show":true,"position":"right"}}',
 780, 140, 400, 340, 1, '#fff', 8, 1, '#e4e7ed', 0, 1);

-- 销售明细表
INSERT INTO visual_dashboard_item (id, dashboard_id, board_id, title, chart_type, datasource_id, query_sql, config, data_config, style_config, pos_x, pos_y, width, height, z_index, bg_color, border_radius, border_width, border_color, locked, visible) VALUES
('it_d1_06', @d1, @b1, '销售明细TOP10', 'TABLE', @ds,
 'SELECT r.region, r.province, c.channel, cat.category, ROUND(SUM(o.amount)) AS amount FROM demo_fact_order o JOIN demo_dim_region r ON o.region_id=r.id JOIN demo_dim_channel c ON o.channel_id=c.id JOIN demo_dim_category cat ON o.category_id=cat.id GROUP BY r.region, r.province, c.channel, cat.category ORDER BY amount DESC LIMIT 10',
 '{"xAxisColumn":"province","valueColumn":"amount"}',
 '{"datasetId":"ds_001","dimensions":[{"fieldCode":"region"},{"fieldCode":"province"},{"fieldCode":"channel"},{"fieldCode":"category"}],"metrics":[{"fieldCode":"amount","aggType":"SUM"}],"filters":[],"sorts":[{"field":"amount","order":"desc"}]}',
 '{"title":{"show":true,"text":"销售明细 TOP10"}}',
 0, 500, 1180, 320, 1, '#fff', 8, 1, '#e4e7ed', 0, 1);

-- ============================================================
-- 仪表板2：区域与渠道分析
-- ============================================================
SET @d2 = _utf8mb4'2087425001413664770' COLLATE utf8mb4_general_ci;
DELETE FROM visual_dashboard_item WHERE dashboard_id = @d2;
DELETE FROM visual_dashboard_board WHERE dashboard_id = @d2;

INSERT INTO visual_dashboard (id, name, description, refresh_interval, status, version, create_by) VALUES
(@d2, '区域与渠道分析', '分区域、分渠道销售数据多维分析', 60, 1, 0, '1') ON DUPLICATE KEY UPDATE name = '区域与渠道分析';

SET @b2 = _utf8mb4'board_d2_main' COLLATE utf8mb4_general_ci;
INSERT INTO visual_dashboard_board (id, dashboard_id, name, icon, layout_mode, canvas_width, canvas_height, sort_order, status) VALUES
(@b2, @d2, '分析', 'DataLine', 'CANVAS', 1920, 1080, 0, 1) ON DUPLICATE KEY UPDATE name = '分析';

-- 区域销售额条形图
INSERT INTO visual_dashboard_item (id, dashboard_id, board_id, title, chart_type, datasource_id, query_sql, config, data_config, style_config, pos_x, pos_y, width, height, z_index, bg_color, border_radius, border_width, border_color, locked, visible) VALUES
('it_d2_01', @d2, @b2, '区域销售额条形图', 'BAR', @ds,
 'SELECT r.region, ROUND(SUM(o.amount)) AS amount FROM demo_fact_order o JOIN demo_dim_region r ON o.region_id=r.id GROUP BY r.region ORDER BY amount DESC',
 '{"xAxisColumn":"region","valueColumn":"amount"}',
 '{"datasetId":"ds_001","dimensions":[{"fieldCode":"region","alias":"大区"}],"metrics":[{"fieldCode":"amount","aggType":"SUM","alias":"销售额"}],"filters":[],"sorts":[{"field":"amount","order":"desc"}]}',
 '{"title":{"show":true,"text":"区域销售额"}}',
 0, 0, 580, 320, 1, '#fff', 8, 1, '#e4e7ed', 0, 1);

-- 渠道×类目堆叠条形图
INSERT INTO visual_dashboard_item (id, dashboard_id, board_id, title, chart_type, datasource_id, query_sql, config, data_config, style_config, pos_x, pos_y, width, height, z_index, bg_color, border_radius, border_width, border_color, locked, visible) VALUES
('it_d2_02', @d2, @b2, '渠道×类目销售额', 'BAR', @ds,
 'SELECT c.channel, cat.category, ROUND(SUM(o.amount)) AS amount FROM demo_fact_order o JOIN demo_dim_channel c ON o.channel_id=c.id JOIN demo_dim_category cat ON o.category_id=cat.id GROUP BY c.channel, cat.category',
 '{"xAxisColumn":"channel","seriesColumn":"category","valueColumn":"amount"}',
 '{"datasetId":"ds_001","dimensions":[{"fieldCode":"channel","alias":"渠道"},{"fieldCode":"category","alias":"类目"}],"metrics":[{"fieldCode":"amount","aggType":"SUM","alias":"销售额"}],"filters":[],"sorts":[]}',
 '{"title":{"show":true,"text":"渠道×类目销售额"},"legend":{"show":true,"position":"top"}}',
 600, 0, 580, 320, 1, '#fff', 8, 1, '#e4e7ed', 0, 1);

-- 类目渠道热力图
INSERT INTO visual_dashboard_item (id, dashboard_id, board_id, title, chart_type, datasource_id, query_sql, config, data_config, style_config, pos_x, pos_y, width, height, z_index, bg_color, border_radius, border_width, border_color, locked, visible) VALUES
('it_d2_03', @d2, @b2, '类目×渠道热力图', 'HEATMAP', @ds,
 'SELECT cat.category, c.channel, ROUND(SUM(o.amount)) AS amount FROM demo_fact_order o JOIN demo_dim_category cat ON o.category_id=cat.id JOIN demo_dim_channel c ON o.channel_id=c.id GROUP BY cat.category, c.channel',
 '{"xAxisColumn":"category","seriesColumn":"channel","valueColumn":"amount"}',
 '{"datasetId":"ds_003","dimensions":[{"fieldCode":"category"},{"fieldCode":"channel"}],"metrics":[{"fieldCode":"amount","aggType":"SUM"}],"filters":[],"sorts":[]}',
 '{"title":{"show":true,"text":"类目×渠道销售额热力图"}}',
 0, 340, 580, 320, 1, '#fff', 8, 1, '#e4e7ed', 0, 1);

-- 类目销售饼图
INSERT INTO visual_dashboard_item (id, dashboard_id, board_id, title, chart_type, datasource_id, query_sql, config, data_config, style_config, pos_x, pos_y, width, height, z_index, bg_color, border_radius, border_width, border_color, locked, visible) VALUES
('it_d2_04', @d2, @b2, '类目销售占比', 'PIE', @ds,
 'SELECT cat.category, ROUND(SUM(o.amount)) AS amount FROM demo_fact_order o JOIN demo_dim_category cat ON o.category_id=cat.id GROUP BY cat.category',
 '{"xAxisColumn":"category","valueColumn":"amount"}',
 '{"datasetId":"ds_001","dimensions":[{"fieldCode":"category","alias":"类目"}],"metrics":[{"fieldCode":"amount","aggType":"SUM","alias":"销售额"}],"filters":[],"sorts":[]}',
 '{"title":{"show":true,"text":"类目销售占比"},"legend":{"show":true,"position":"right"}}',
 600, 340, 580, 320, 1, '#fff', 8, 1, '#e4e7ed', 0, 1);

-- ============================================================
-- 仪表板3：商品类目分析
-- ============================================================
SET @d3 = _utf8mb4'2087425001501745154' COLLATE utf8mb4_general_ci;
DELETE FROM visual_dashboard_item WHERE dashboard_id = @d3;
DELETE FROM visual_dashboard_board WHERE dashboard_id = @d3;

INSERT INTO visual_dashboard (id, name, description, refresh_interval, status, version, create_by) VALUES
(@d3, '商品类目分析', '商品类目销售额与利润对比分析', 60, 1, 0, '1') ON DUPLICATE KEY UPDATE name = '商品类目分析';

SET @b3 = _utf8mb4'board_d3_main' COLLATE utf8mb4_general_ci;
INSERT INTO visual_dashboard_board (id, dashboard_id, name, icon, layout_mode, canvas_width, canvas_height, sort_order, status) VALUES
(@b3, @d3, '类目分析', 'PieChart', 'CANVAS', 1920, 1080, 0, 1) ON DUPLICATE KEY UPDATE name = '类目分析';

-- 类目销售额条形图
INSERT INTO visual_dashboard_item (id, dashboard_id, board_id, title, chart_type, datasource_id, query_sql, config, data_config, style_config, pos_x, pos_y, width, height, z_index, bg_color, border_radius, border_width, border_color, locked, visible) VALUES
('it_d3_01', @d3, @b3, '类目销售额对比', 'BAR', @ds,
 'SELECT cat.category, ROUND(SUM(o.amount)) AS amount FROM demo_fact_order o JOIN demo_dim_category cat ON o.category_id=cat.id GROUP BY cat.category ORDER BY amount DESC',
 '{"xAxisColumn":"category","valueColumn":"amount"}',
 '{"datasetId":"ds_001","dimensions":[{"fieldCode":"category","alias":"类目"}],"metrics":[{"fieldCode":"amount","aggType":"SUM","alias":"销售额"}],"filters":[],"sorts":[{"field":"amount","order":"desc"}]}',
 '{"title":{"show":true,"text":"类目销售额对比"}}',
 0, 0, 580, 320, 1, '#fff', 8, 1, '#e4e7ed', 0, 1);

-- 类目利润条形图
INSERT INTO visual_dashboard_item (id, dashboard_id, board_id, title, chart_type, datasource_id, query_sql, config, data_config, style_config, pos_x, pos_y, width, height, z_index, bg_color, border_radius, border_width, border_color, locked, visible) VALUES
('it_d3_02', @d3, @b3, '类目利润对比', 'BAR', @ds,
 'SELECT cat.category, ROUND(SUM(o.profit)) AS profit FROM demo_fact_order o JOIN demo_dim_category cat ON o.category_id=cat.id GROUP BY cat.category ORDER BY profit DESC',
 '{"xAxisColumn":"category","valueColumn":"profit"}',
 '{"datasetId":"ds_001","dimensions":[{"fieldCode":"category","alias":"类目"}],"metrics":[{"fieldCode":"profit","aggType":"SUM","alias":"利润"}],"filters":[],"sorts":[{"field":"profit","order":"desc"}]}',
 '{"title":{"show":true,"text":"类目利润对比"}}',
 600, 0, 580, 320, 1, '#fff', 8, 1, '#e4e7ed', 0, 1);

-- 各类目月度趋势
INSERT INTO visual_dashboard_item (id, dashboard_id, board_id, title, chart_type, datasource_id, query_sql, config, data_config, style_config, pos_x, pos_y, width, height, z_index, bg_color, border_radius, border_width, border_color, locked, visible) VALUES
('it_d3_03', @d3, @b3, '类目月度销售趋势', 'LINE', @ds,
 'SELECT DATE_FORMAT(o.order_date,"%Y-%m") AS month, cat.category, ROUND(SUM(o.amount)) AS amount FROM demo_fact_order o JOIN demo_dim_category cat ON o.category_id=cat.id GROUP BY month, cat.category ORDER BY month',
 '{"xAxisColumn":"month","seriesColumn":"category","valueColumn":"amount"}',
 '{"datasetId":"ds_001","dimensions":[{"fieldCode":"month","alias":"月份"},{"fieldCode":"category","alias":"类目"}],"metrics":[{"fieldCode":"amount","aggType":"SUM","alias":"销售额"}],"filters":[],"sorts":[{"field":"month","order":"asc"}]}',
 '{"title":{"show":true,"text":"类目月度趋势"},"legend":{"show":true,"position":"top"}}',
 0, 340, 1180, 320, 1, '#fff', 8, 1, '#e4e7ed', 0, 1);

COMMIT;
