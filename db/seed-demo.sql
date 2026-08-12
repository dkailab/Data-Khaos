-- ============================================================
-- Data Khaos - 演示业务数据
-- 电商销售场景：区域/渠道/类目维度 + 订单事实
-- 用于支撑仪表板 10 种图表（条形/折线/饼/散点/热力/面积/仪表/树/箱型/地图）
-- 执行：docker exec -i dk-mysql mysql -uroot -proot123456 data_khaos < seed-demo.sql
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------- 维度表：区域（大区/省/市） ----------
DROP TABLE IF EXISTS demo_dim_region;
CREATE TABLE demo_dim_region (
    id        INT PRIMARY KEY,
    region    VARCHAR(50) COMMENT '大区',
    province  VARCHAR(50) COMMENT '省份',
    city      VARCHAR(50) COMMENT '城市'
) ENGINE=InnoDB COMMENT='区域维度(演示)';

INSERT INTO demo_dim_region (id, region, province, city) VALUES
(1,'华东','上海市','上海市'),(2,'华东','江苏省','南京市'),(3,'华东','江苏省','苏州市'),
(4,'华东','浙江省','杭州市'),(5,'华东','浙江省','宁波市'),(6,'华东','安徽省','合肥市'),
(7,'华南','广东省','广州市'),(8,'华南','广东省','深圳市'),(9,'华南','广东省','东莞市'),
(10,'华南','福建省','厦门市'),(11,'华南','广西壮族自治区','南宁市'),(12,'华南','海南省','海口市'),
(13,'华北','北京市','北京市'),(14,'华北','天津市','天津市'),(15,'华北','河北省','石家庄市'),
(16,'华北','山东省','济南市'),(17,'华北','山东省','青岛市'),(18,'华北','山西省','太原市'),
(19,'华中','湖北省','武汉市'),(20,'华中','湖南省','长沙市'),(21,'华中','河南省','郑州市'),
(22,'华中','江西省','南昌市'),(23,'华中','安徽省','蚌埠市'),
(24,'西南','四川省','成都市'),(25,'西南','重庆市','重庆市'),(26,'西南','云南省','昆明市'),
(27,'西南','贵州省','贵阳市'),(28,'西南','西藏自治区','拉萨市'),
(29,'西北','陕西省','西安市'),(30,'西北','甘肃省','兰州市'),(31,'西北','新疆维吾尔自治区','乌鲁木齐市'),
(32,'西北','宁夏回族自治区','银川市'),(33,'西北','青海省','西宁市'),
(34,'东北','辽宁省','沈阳市'),(35,'东北','辽宁省','大连市'),(36,'东北','吉林省','长春市'),
(37,'东北','黑龙江省','哈尔滨市');

-- ---------- 维度表：渠道 ----------
DROP TABLE IF EXISTS demo_dim_channel;
CREATE TABLE demo_dim_channel (
    id      INT PRIMARY KEY,
    channel VARCHAR(50) COMMENT '渠道'
) ENGINE=InnoDB COMMENT='渠道维度(演示)';
INSERT INTO demo_dim_channel (id, channel) VALUES
(1,'线上商城'),(2,'官网直营'),(3,'线下门店'),(4,'分销代理');

-- ---------- 维度表：类目 ----------
DROP TABLE IF EXISTS demo_dim_category;
CREATE TABLE demo_dim_category (
    id       INT PRIMARY KEY,
    category VARCHAR(50) COMMENT '类目'
) ENGINE=InnoDB COMMENT='类目维度(演示)';
INSERT INTO demo_dim_category (id, category) VALUES
(1,'数码'),(2,'家电'),(3,'服饰'),(4,'食品'),(5,'美妆'),(6,'家居');

-- ---------- 事实表：订单 ----------
DROP TABLE IF EXISTS demo_fact_order;
CREATE TABLE demo_fact_order (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_date  DATE COMMENT '订单日期',
    region_id   INT COMMENT '区域ID',
    province    VARCHAR(50) COMMENT '省份',
    channel_id  INT COMMENT '渠道ID',
    category_id INT COMMENT '类目ID',
    amount      DECIMAL(12,2) COMMENT '订单金额',
    qty         INT COMMENT '数量',
    cost        DECIMAL(12,2) COMMENT '成本',
    profit      DECIMAL(12,2) COMMENT '利润'
) ENGINE=InnoDB COMMENT='订单事实(演示)';

-- ---------- 生成订单数据（2024-01 ~ 2026-08，按月 × 区域 × 渠道 × 类目） ----------
INSERT INTO demo_fact_order (order_date, region_id, province, channel_id, category_id, amount, qty, cost, profit)
WITH RECURSIVE dates AS (
    SELECT '2024-01-15' AS d
    UNION ALL
    SELECT DATE_ADD(d, INTERVAL 1 MONTH) FROM dates WHERE d < '2026-07-15'
),
region_area AS (
    SELECT r.id region_id, r.province, c.id channel_id, c.channel, cat.id category_id, cat.category
    FROM demo_dim_region r, demo_dim_channel c, demo_dim_category cat
)
SELECT
    d.d AS order_date,
    area.region_id,
    area.province,
    area.channel_id,
    area.category_id,
    ROUND(growth * (100 + MOD(MONTH(d.d)*7 + area.region_id*3 + area.category_id*5, 200)) * (1 + IF(area.region_id % 3 = 0, 0.3, 0)), 2) AS amount,
    CAST(growth * 2 + MOD(area.category_id * 3, 8) AS UNSIGNED) AS qty,
    ROUND(growth * (60 + MOD(MONTH(d.d)*5 + area.category_id*9, 120)) * (1 + IF(area.region_id % 3 = 0, 0.25, 0)), 2) AS cost,
    ROUND(growth * (40 + MOD(MONTH(d.d)*3 + area.channel_id*11, 80)) * (1 + IF(area.channel_id = 3, 0.2, 0)), 2) AS profit
FROM dates d
JOIN (
    SELECT region_id, province, channel_id, category_id,
           (1000 + MOD(region_id * 137 + channel_id * 53 + category_id * 29, 500)) AS growth
    FROM (SELECT id region_id, province FROM demo_dim_region) g
    CROSS JOIN (SELECT id channel_id FROM demo_dim_channel) ch
    CROSS JOIN (SELECT id category_id FROM demo_dim_category) cat
) area ON 1=1
ORDER BY d.d, area.region_id;

-- 补充近 30 天逐日订单，供散点图/箱型图/趋势使用
DROP TABLE IF EXISTS demo_daily_order;
CREATE TABLE demo_daily_order (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_date  DATE,
    city        VARCHAR(50),
    channel     VARCHAR(50),
    category    VARCHAR(50),
    amount      DECIMAL(12,2),
    qty         INT,
    profit      DECIMAL(12,2)
) ENGINE=InnoDB COMMENT='逐日订单(演示)';

INSERT INTO demo_daily_order (order_date, city, channel, category, amount, qty, profit)
WITH RECURSIVE dd AS (
    SELECT DATE_SUB(CURDATE(), INTERVAL 29 DAY) AS d
    UNION ALL
    SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM dd WHERE d < CURDATE()
)
SELECT d.d, r.city, c.channel, cat.category,
       ROUND(50 + MOD(DAYOFYEAR(d.d)*13 + r.id*7 + c.id*11 + cat.id*5, 450), 2),
       CAST(1 + MOD(DAYOFYEAR(d.d)*3 + r.id*5, 20) AS UNSIGNED),
       ROUND(10 + MOD(DAYOFYEAR(d.d)*7 + r.id*9 + c.id*3, 120), 2)
FROM dd d
CROSS JOIN demo_dim_region r
CROSS JOIN demo_dim_channel c
CROSS JOIN demo_dim_category cat;

SET FOREIGN_KEY_CHECKS = 1;