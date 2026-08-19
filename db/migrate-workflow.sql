-- ============================================================
-- 工作流编排系统 建表脚本（在 data_khaos 库执行）
-- 迁移方式: source db/migrate-workflow.sql
-- 覆盖：工作流定义 / 节点 / 连线 / 运行实例 / 节点运行实例
-- ============================================================

USE data_khaos;

-- 1. 工作流定义（DAG 图元信息 + 调度配置）
CREATE TABLE IF NOT EXISTS workflow_def (
    id              VARCHAR(32)  NOT NULL PRIMARY KEY COMMENT '主键ID',
    name            VARCHAR(200) NOT NULL COMMENT '工作流名称',
    code            VARCHAR(100) COMMENT '工作流编码',
    cron_expression VARCHAR(100) COMMENT 'Cron 表达式(调度触发,为空仅手动)',
    status          TINYINT      DEFAULT 0 COMMENT '0:禁用/草稿 1:启用',
    description     VARCHAR(500) COMMENT '描述',
    owner           VARCHAR(64)  COMMENT '负责人',
    params          TEXT         COMMENT '运行参数模板(JSON,供节点${param}替换)',
    deleted         TINYINT      DEFAULT 0 COMMENT '逻辑删除 0:正常 1:删除',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB COMMENT='工作流定义表';

-- 2. 工作流节点（DAG 顶点）
CREATE TABLE IF NOT EXISTS workflow_node (
    id             VARCHAR(32)  NOT NULL PRIMARY KEY COMMENT '主键ID',
    wf_id          VARCHAR(32)  NOT NULL COMMENT '所属工作流ID',
    node_code      VARCHAR(100) NOT NULL COMMENT '节点唯一编码(DAG内)',
    node_name      VARCHAR(200) NOT NULL COMMENT '节点名称',
    node_type      VARCHAR(30)  NOT NULL COMMENT '节点类型 SQL/SHELL/PYTHON/DATA_OP',
    config_json    TEXT         COMMENT '节点配置(JSON):datasourceId/sql/script/operator等',
    pos_x          INT          DEFAULT 0 COMMENT '画布X',
    pos_y          INT          DEFAULT 0 COMMENT '画布Y',
    timeout        INT          DEFAULT 0 COMMENT '超时(秒)',
    retry_count    INT          DEFAULT 0 COMMENT '失败重试次数',
    retry_interval INT          DEFAULT 0 COMMENT '重试间隔(秒)',
    deleted        TINYINT      DEFAULT 0 COMMENT '逻辑删除 0:正常 1:删除',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_wf (wf_id)
) ENGINE=InnoDB COMMENT='工作流节点表';

-- 3. 工作流依赖边（DAG 连线，from 完成后触发 to）
CREATE TABLE IF NOT EXISTS workflow_edge (
    id             VARCHAR(32)  NOT NULL PRIMARY KEY COMMENT '主键ID',
    wf_id          VARCHAR(32)  NOT NULL COMMENT '所属工作流ID',
    from_code      VARCHAR(100) NOT NULL COMMENT '前驱节点编码',
    to_code        VARCHAR(100) NOT NULL COMMENT '后继节点编码',
    condition_expr VARCHAR(500) COMMENT '边条件表达式(预留,空表示硬依赖)',
    deleted        TINYINT      DEFAULT 0 COMMENT '逻辑删除 0:正常 1:删除',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_wf (wf_id)
) ENGINE=InnoDB COMMENT='工作流依赖边表';

-- 4. 工作流运行实例
CREATE TABLE IF NOT EXISTS workflow_run (
    id             VARCHAR(32)  NOT NULL PRIMARY KEY COMMENT '主键ID',
    wf_id          VARCHAR(32)  NOT NULL COMMENT '工作流ID',
    wf_name        VARCHAR(200) COMMENT '工作流名称(冗余展示)',
    trigger_type   VARCHAR(20)  DEFAULT 'MANUAL' COMMENT '触发类型 MANUAL/SCHEDULE',
    run_status     VARCHAR(20)  DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED/STOP',
    trigger_params TEXT         COMMENT '触发时注入参数(JSON)',
    start_time     DATETIME     COMMENT '开始时间',
    end_time       DATETIME     COMMENT '结束时间',
    duration_ms    BIGINT       DEFAULT 0 COMMENT '总耗时(毫秒)',
    error_message  VARCHAR(2000) COMMENT '错误信息',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_wf (wf_id),
    KEY idx_status (run_status)
) ENGINE=InnoDB COMMENT='工作流运行实例表';

-- 5. 工作流节点运行实例（含执行日志）
CREATE TABLE IF NOT EXISTS workflow_node_run (
    id            VARCHAR(32)  NOT NULL PRIMARY KEY COMMENT '主键ID',
    run_id        VARCHAR(32)  NOT NULL COMMENT '工作流运行ID',
    wf_id         VARCHAR(32)  COMMENT '工作流ID',
    node_code     VARCHAR(100) NOT NULL COMMENT '节点编码',
    node_name     VARCHAR(200) COMMENT '节点名称',
    node_type     VARCHAR(30)  COMMENT '节点类型',
    status        VARCHAR(30)  DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED/SKIPPED',
    start_time    DATETIME     COMMENT '开始时间',
    end_time      DATETIME     COMMENT '结束时间',
    duration_ms   BIGINT       DEFAULT 0 COMMENT '耗时(毫秒)',
    result_rows   INT          COMMENT '影响/结果行数',
    log_text      TEXT         COMMENT '执行日志',
    error_message VARCHAR(2000) COMMENT '错误信息',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_run (run_id),
    KEY idx_node (wf_id, node_code)
) ENGINE=InnoDB COMMENT='工作流节点运行实例表';