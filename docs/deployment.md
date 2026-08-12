# Data Khaos 部署运维文档

## 1. 部署架构

Data Khaos 共 11 个可运行服务 + 3 个基础设施组件。开发环境通过 Docker Compose 一键部署。

| 组件 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| MySQL 8 | dk-mysql | 3306 | 业务库（开发默认），自动执行 `db/mysql-init.sql` |
| Redis 7 | dk-redis | 6379 | 网关限流、会话/验证码缓存 |
| Nacos | dk-nacos | 8848 / 9848 | 服务注册与发现（standalone） |
| 网关 | dk-gateway | 8080 | 统一入口 + JWT 鉴权 + 路由 |
| 认证中心 | dk-auth | 8081 | 登录 / SSO / 验证码 / 用户 / 角色 |
| 权限服务 | dk-permission | 8082 | RBAC、行/列/表权限、组织 |
| 审批服务 | dk-approval | 8083 | 权限申请 / 审批流 |
| 数据源服务 | dk-datasource | 8084 | 数据源接入（8 种连接器） |
| 元数据中心 | dk-metadata | 8085 | 元数据采集 / 血缘 / 搜索 |
| 数据集市 | dk-mart | 8086 | 模型 / 指标 / 维度 |
| SQL 查询平台 | dk-query | 8087 | 在线查询 + SQL 审核 |
| 可视化引擎 | dk-visual | 8088 | 仪表板 / 分析板 |
| 调度系统 | dk-schedule | 8089 | 定时任务 / DAG / 重试 |
| 推送系统 | dk-notification | 8090 | 模板 / 站内信 / 订阅 |

## 2. 快速部署（Docker Compose）

### 2.1 环境要求

- Docker 20.10+ / Docker Compose v2
- 主机内存 ≥ 8GB（Nacos + MySQL + 11 个 JVM 服务）

### 2.2 部署步骤

```bash
cd docker
cp .env.example .env          # 按需修改密码/密钥
docker compose up -d --build  # 首次构建约 5~10 分钟
docker compose ps             # 等待所有服务 running
./scripts/smoke-test.sh       # 冒烟测试，全部通过 ✔
```

### 2.3 验证

- 网关就绪：`curl http://localhost:8080/api/auth/captcha` 返回 `"code":0`
- 登录：`curl -X POST -H 'Content-Type: application/json' -d '{"username":"admin","password":"password"}' http://localhost:8080/api/auth/login`
- 服务注册：浏览器打开 `http://localhost:8848/nacos`（默认无鉴权）查看 11 个服务是否全部注册
- 接口文档（网关下发的各服务 Knife4j）：`http://localhost:8080/doc.html`

## 3. 生产部署（达梦 DM8）

### 3.1 差异点

| 维度 | 开发 | 生产 |
|------|------|------|
| 数据库 | MySQL 8（application.yml 默认） | 达梦 DM8 |
| 构建 | `mvn clean install` | `mvn clean install -Pprod`（引入 `DmJdbcDriver18`） |
| 连接配置 | `MYSQL_HOST/...` 环境变量 | 指向 DM8 实例的 JDBC 连接 |
| 密钥 | 默认值 | 必须更换 `JWT_SECRET` / `AES_KEY` |

### 3.2 达梦连接示例

```yaml
spring:
  datasource:
    driver-class-name: dm.jdbc.driver.DmDriver
    url: jdbc:dm://<dm8-host>:5236/DATA_KHAOS
    username: data_khaos
    password: <强密码>
```

> 达梦初始化脚本见 `db/init.sql`；主键采用雪花算法 `VARCHAR(32)`，避免达梦自增序列性能问题。

## 4. 运维手册

### 4.1 常用命令

```bash
# 查看全部服务状态
docker compose ps

# 跟踪某服务日志
docker compose logs -f -t <service>

# 重启单个服务（改配置后）
docker compose up -d <service>

# 全部停止（保留数据）
docker compose down

# 全部停止并重置数据（重建数据库）
docker compose down -v

# 健康检查：服务进程
docker exec dk-gateway sh -c 'jps'
```

### 4.2 故障排查

| 症状 | 排查步骤 |
|------|----------|
| 服务反复重启（restarting） | `docker compose logs <svc>`，多为数据库/Nacos 未就绪，等待 infra 健康后再拉起 |
| 登录失败 / 401 | 确认 MySQL 种子数据已初始化（admin 存在）；确认 `JWT_SECRET` 各服务一致 |
| 服务注册不上 | 确认 `NACOS_ADDR` 指向 `nacos:8848`；`nc -zv nacos 8848` 可通；查看 nacos 控制台 |
| 网关 503 | 目标服务未注册到 Nacos，检查对应服务日志 |
| 限流触发 429 | `/api/query` 默认 10 req/s，调大 `replenishRate` 或确认 Redis 连通 |
| 时区错乱 | 镜像已内置 `-Duser.timezone=Asia/Shanghai`，如需调整改 `docker/Dockerfile` 的 `JAVA_OPTS` |

### 4.3 数据备份

- MySQL：`docker exec dk-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --all-databases' > backup.sql`
- Nacos 配置：Nacos 数据挂载在 `nacos-data` 卷，备份该卷即可。

## 5. 安全加固清单（上线前）

- [ ] 更换 `JWT_SECRET`、`AES_KEY`（`.env` / 环境变量），长度 ≥ 32 字符
- [ ] 修改 MySQL root 密码与默认管理员 `admin` 密码
- [ ] 启用 Nacos 鉴权（`NACOS_AUTH_ENABLE=true` + 配置用户名密码）
- [ ] 网关白名单仅保留 `/api/auth/login`、`/api/auth/captcha`、接口文档
- [ ] 数据源密码字段已 AES 加密落库，确认 `AES_KEY` 托管于密钥管理服务
- [ ] 生产启用达梦 DM8 + `mvn -Pprod` 构建
