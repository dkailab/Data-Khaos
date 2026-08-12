-- ============================================================
-- Data Khaos - 测试仪表板组件种子数据
-- 依赖：seed-demo.sql 已执行、数据源/仪表板已通过 API 创建
-- 执行：docker exec -i dk-mysql mysql -uroot -proot123456 data_khaos < seed-dashboards.sql
-- ============================================================
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

-- 数据源ID
SET @ds = _utf8mb4'2087424798053343233' COLLATE utf8mb4_general_ci;

-- ============================================================
-- 仪表板1：销售总览  (id=2087425001321390081)
-- ============================================================
SET @d1 = _utf8mb4'2087425001321390081' COLLATE utf8mb4_general_ci;
DELETE FROM visual_dashboard_item WHERE dashboard_id = @d1;

-- 数字：订单总额
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d1, '_01'), @d1, '订单总额', 'NUMBER', @ds,
 'SELECT ROUND(SUM(amount)) AS 订单总额 FROM demo_fact_order',
 '{"valueColumn":"订单总额","xAxisColumn":"订单总额","unit":"元","thousand":true,"decimals":0}', 0, 0, 3, 2);

-- 数字：订单量
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d1, '_02'), @d1, '订单量', 'NUMBER', @ds,
 'SELECT COUNT(*) AS 订单量 FROM demo_fact_order',
 '{"valueColumn":"订单量","xAxisColumn":"订单量","unit":"件","thousand":true,"decimals":0}', 3, 0, 3, 2);

-- 数字：销售利润
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d1, '_03'), @d1, '销售利润', 'NUMBER', @ds,
 'SELECT ROUND(SUM(profit)) AS 销售利润 FROM demo_fact_order',
 '{"valueColumn":"销售利润","xAxisColumn":"销售利润","unit":"元","thousand":true,"decimals":0}', 6, 0, 3, 2);

-- 数字：利润率
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d1, '_04'), @d1, '销售利润率', 'NUMBER', @ds,
 'SELECT ROUND(SUM(profit)/SUM(amount)*100,2) AS 利润率 FROM demo_fact_order',
 '{"valueColumn":"利润率","xAxisColumn":"利润率","unit":"%","thousand":false,"decimals":2}', 9, 0, 3, 2);

-- 折线图：月度销售趋势
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d1, '_05'), @d1, '月度销售趋势', 'LINE', @ds,
 'SELECT DATE_FORMAT(order_date,"%Y-%m") AS 月份, ROUND(SUM(amount)) AS 销售额, ROUND(SUM(profit)) AS 利润 FROM demo_fact_order GROUP BY 月份 ORDER BY 月份',
 '{"xAxisColumn":"月份","valueColumn":"销售额","seriesColumn":"利润"}', 0, 2, 6, 4);

-- 面积图：月度累计销售额
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d1, '_06'), @d1, '月度销售额面积图', 'AREA', @ds,
 'SELECT DATE_FORMAT(order_date,"%Y-%m") AS 月份, ROUND(SUM(amount)) AS 销售额 FROM demo_fact_order GROUP BY 月份 ORDER BY 月份',
 '{"xAxisColumn":"月份","valueColumn":"销售额"}', 6, 2, 6, 4);

-- 饼图：渠道销售占比
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d1, '_07'), @d1, '渠道销售占比', 'PIE', @ds,
 'SELECT c.channel AS 渠道, ROUND(SUM(o.amount)) AS 销售额 FROM demo_fact_order o JOIN demo_dim_channel c ON o.channel_id=c.id GROUP BY c.channel',
 '{"xAxisColumn":"渠道","valueColumn":"销售额"}', 0, 6, 4, 4);

-- 仪表盘：目标完成率
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d1, '_08'), @d1, '年度目标完成率', 'GAUGE', @ds,
 'SELECT ROUND(SUM(amount)/1000000000*100,1) AS 完成率 FROM demo_fact_order WHERE YEAR(order_date)=2025',
 '{"valueColumn":"完成率","valueColumn2":"完成率","xAxisColumn":"完成率"}', 4, 6, 4, 4);

-- 表格：销售明细TOP10
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d1, '_09'), @d1, '销售明细', 'TABLE', @ds,
 'SELECT r.region AS 区域, r.province AS 省份, c.channel AS 渠道, cat.category AS 类目, ROUND(SUM(o.amount)) AS 销售额, ROUND(SUM(o.profit)) AS 利润 FROM demo_fact_order o JOIN demo_dim_region r ON o.region_id=r.id JOIN demo_dim_channel c ON o.channel_id=c.id JOIN demo_dim_category cat ON o.category_id=cat.id GROUP BY r.region, r.province, c.channel, cat.category ORDER BY 销售额 DESC LIMIT 10',
 '{"xAxisColumn":"省份","valueColumn":"销售额"}', 8, 6, 4, 4);

-- ============================================================
-- 仪表板2：区域与渠道分析  (id=2087425001413664770)
-- ============================================================
SET @d2 = _utf8mb4'2087425001413664770' COLLATE utf8mb4_general_ci;
DELETE FROM visual_dashboard_item WHERE dashboard_id = @d2;

-- 条形图：各区域销售额
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d2, '_01'), @d2, '区域销售额条形图', 'BAR', @ds,
 'SELECT r.region AS 区域, ROUND(SUM(o.amount)) AS 销售额 FROM demo_fact_order o JOIN demo_dim_region r ON o.region_id=r.id GROUP BY r.region ORDER BY 销售额 DESC',
 '{"xAxisColumn":"区域","valueColumn":"销售额"}', 0, 0, 6, 4);

-- 条形图：各渠道销售额（多系列）
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d2, '_02'), @d2, '渠道×类目销售额', 'BAR', @ds,
 'SELECT c.channel AS 渠道, cat.category AS 类目, ROUND(SUM(o.amount)) AS 销售额 FROM demo_fact_order o JOIN demo_dim_channel c ON o.channel_id=c.id JOIN demo_dim_category cat ON o.category_id=cat.id GROUP BY c.channel, cat.category',
 '{"xAxisColumn":"渠道","seriesColumn":"类目","valueColumn":"销售额"}', 6, 0, 6, 4);

-- 散点图：客单价 vs 数量
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d2, '_03'), @d2, '各省销售散点图', 'SCATTER', @ds,
 'SELECT r.province AS 省份, ROUND(SUM(o.amount)) AS 销售额, SUM(o.qty) AS 数量 FROM demo_fact_order o JOIN demo_dim_region r ON o.region_id=r.id GROUP BY r.province',
 '{"xAxisColumn":"销售额","valueColumn":"数量","seriesColumn":"省份"}', 0, 4, 6, 4);

-- 热力图：区域×类目
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d2, '_04'), @d2, '区域×类目热力图', 'HEATMAP', @ds,
 'SELECT r.region AS 区域, cat.category AS 类目, ROUND(SUM(o.amount)) AS 销售额 FROM demo_fact_order o JOIN demo_dim_region r ON o.region_id=r.id JOIN demo_dim_category cat ON o.category_id=cat.id GROUP BY r.region, cat.category',
 '{"xAxisColumn":"区域","seriesColumn":"类目","valueColumn":"销售额"}', 6, 4, 6, 4);

-- 地图：各省销售额
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d2, '_05'), @d2, '全国销售地图', 'MAP', @ds,
 'SELECT r.province AS 省份, ROUND(SUM(o.amount)) AS 销售额 FROM demo_fact_order o JOIN demo_dim_region r ON o.region_id=r.id GROUP BY r.province ORDER BY 销售额 DESC',
 '{"xAxisColumn":"省份","valueColumn":"销售额","mapName":"china"}', 0, 8, 12, 5);

-- 条形图：各省销售额排行
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d2, '_06'), @d2, '省份销售排行', 'BAR', @ds,
 'SELECT r.province AS 省份, ROUND(SUM(o.amount)) AS 销售额 FROM demo_fact_order o JOIN demo_dim_region r ON o.region_id=r.id GROUP BY r.province ORDER BY 销售额 DESC LIMIT 12',
 '{"xAxisColumn":"省份","valueColumn":"销售额"}', 0, 13, 12, 4);

-- ============================================================
-- 仪表板3：商品类目分析  (id=2087425001501745154)
-- ============================================================
SET @d3 = _utf8mb4'2087425001501745154' COLLATE utf8mb4_general_ci;
DELETE FROM visual_dashboard_item WHERE dashboard_id = @d3;

-- 饼图：类目销售占比
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d3, '_01'), @d3, '类目销售占比', 'PIE', @ds,
 'SELECT cat.category AS 类目, ROUND(SUM(o.amount)) AS 销售额 FROM demo_fact_order o JOIN demo_dim_category cat ON o.category_id=cat.id GROUP BY cat.category ORDER BY 销售额 DESC',
 '{"xAxisColumn":"类目","valueColumn":"销售额"}', 0, 0, 4, 4);

-- 树形图：类目销售额
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d3, '_02'), @d3, '类目销售额树形图', 'TREEMAP', @ds,
 'SELECT cat.category AS 类目, ROUND(SUM(o.amount)) AS 销售额 FROM demo_fact_order o JOIN demo_dim_category cat ON o.category_id=cat.id GROUP BY cat.category',
 '{"xAxisColumn":"类目","valueColumn":"销售额"}', 4, 0, 4, 4);

-- 箱型图：各渠道订单金额分布
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d3, '_03'), @d3, '渠道订单金额箱型图', 'BOXPLOT', @ds,
 'SELECT c.channel AS 渠道, o.amount AS 金额 FROM demo_fact_order o JOIN demo_dim_channel c ON o.channel_id=c.id WHERE o.id % 5 = 0',
 '{"xAxisColumn":"渠道","valueColumn":"金额"}', 8, 0, 4, 4);

-- 折线图：各类目月度趋势
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d3, '_04'), @d3, '类目月度销售趋势', 'LINE', @ds,
 'SELECT DATE_FORMAT(o.order_date,"%Y-%m") AS 月份, cat.category AS 类目, ROUND(SUM(o.amount)) AS 销售额 FROM demo_fact_order o JOIN demo_dim_category cat ON o.category_id=cat.id GROUP BY 月份, cat.category ORDER BY 月份',
 '{"xAxisColumn":"月份","seriesColumn":"类目","valueColumn":"销售额"}', 0, 4, 6, 4);

-- 条形图：类目利润
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d3, '_05'), @d3, '类目利润对比', 'BAR', @ds,
 'SELECT cat.category AS 类目, ROUND(SUM(o.amount)) AS 销售额, ROUND(SUM(o.profit)) AS 利润 FROM demo_fact_order o JOIN demo_dim_category cat ON o.category_id=cat.id GROUP BY cat.category',
 '{"xAxisColumn":"类目","valueColumn":"利润","seriesColumn":"销售额"}', 6, 4, 6, 4);

-- 表格：类目明细
INSERT INTO visual_dashboard_item (id, dashboard_id, title, chart_type, datasource_id, query_sql, config, pos_x, pos_y, width, height) VALUES
(CONCAT('it_', @d3, '_06'), @d3, '类目销售明细表', 'TABLE', @ds,
 'SELECT cat.category AS 类目, COUNT(*) AS 订单数, SUM(o.qty) AS 商品数, ROUND(SUM(o.amount)) AS 销售额, ROUND(SUM(o.profit)) AS 利润, ROUND(SUM(o.profit)/SUM(o.amount)*100,2) AS 利润率 FROM demo_fact_order o JOIN demo_dim_category cat ON o.category_id=cat.id GROUP BY cat.category',
 '{"xAxisColumn":"类目","valueColumn":"销售额"}', 0, 8, 12, 4);