package com.datakhaos.pipeline.engine.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.pipeline.engine.EngineUtils;
import com.datakhaos.pipeline.engine.PipelineEngine;
import com.datakhaos.pipeline.entity.PipelineInstance;
import com.datakhaos.pipeline.entity.PipelineTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * DataX 引擎适配器（阿里开源离线同步）。
 *
 * <p>按源/去向类型自动组装 reader/writer，支持三条链路的双向运行：
 * <ul>
 *   <li>HIVE → MYSQL：hdfsreader（读 Hive 表 HDFS 落地文件）→ mysqlwriter</li>
 *   <li>MYSQL → HIVE：mysqlreader → hdfswriter（写 Hive warehouse 目录，并自动建受管 Hive 表）</li>
 *   <li>MYSQL → MYSQL：mysqlreader → mysqlwriter</li>
 * </ul>
 *
 * <p>列信息在运行期通过 JDBC 元数据动态解析（列名 + SQL 类型 → DataX 类型），无需手工配置字段。
 * 通过 <code>python datax.py job.json</code> 执行；引擎未安装（未配置 DATAX_HOME）时明确提示。</p>
 */
@Slf4j
@Component
public class DataXEngine implements PipelineEngine {

    /** 从任务 config 读取的扩展键 */
    private static final String CFG_HDFS_FS = "hdfsDefaultFS";
    private static final String CFG_HIVE_PATH = "hdfsPath";

    /** 默认 HDFS 地址 / Hive warehouse（与 docker/compose Hive 集群一致，可被任务 config 覆盖） */
    private static final String DEFAULT_HDFS_FS = "hdfs://hdfs-namenode:9000";
    private static final String DEFAULT_WAREHOUSE = "/user/hive/warehouse";

    private final JdbcTemplate jdbcTemplate;
    private final String aesKey;

    public DataXEngine(JdbcTemplate jdbcTemplate,
                       @Value("${data-khaos.aes-key:dk-aes-key-16byte}") String aesKey) {
        this.jdbcTemplate = jdbcTemplate;
        this.aesKey = aesKey;
    }

    @Override
    public String type() {
        return "DATAX";
    }

    @Override
    public String name() {
        return "DataX（阿里离线同步）";
    }

    @Override
    public boolean available() {
        String home = System.getenv("DATAX_HOME");
        return StrUtil.isNotBlank(home) && new File(home, "bin/datax.py").exists();
    }

    @Override
    public String buildRunConfig(PipelineTask task) {
        String st = sourceType(task);
        String tt = targetType(task);
        String jobType;
        if ("HIVE".equalsIgnoreCase(st) && "MYSQL".equalsIgnoreCase(tt)) {
            jobType = "HIVE_TO_MYSQL";
        } else if ("MYSQL".equalsIgnoreCase(st) && "HIVE".equalsIgnoreCase(tt)) {
            jobType = "MYSQL_TO_HIVE";
        } else {
            jobType = "MYSQL_TO_MYSQL";
        }
        return JSONUtil.toJsonStr(Map.of(
                "job_type", jobType,
                "source_table", task.getSourceTable(),
                "target_table", task.getTargetTable()));
    }

    @Override
    public int execute(PipelineTask task, PipelineInstance instance) throws Exception {
        String home = System.getenv("DATAX_HOME");
        if (StrUtil.isBlank(home)) {
            throw new BusinessException("DataX 引擎未安装：请设置环境变量 DATAX_HOME 并安装 DataX");
        }
        File script = new File(home, "bin/datax.py");
        if (!script.exists()) {
            throw new BusinessException("DataX 脚本不存在: " + script.getAbsolutePath());
        }
        Map<String, Object> source = EngineUtils.getDs(jdbcTemplate, task.getSourceDsId());
        Map<String, Object> target = EngineUtils.getDs(jdbcTemplate, task.getTargetDsId());
        String st = String.valueOf(source.get("ds_type")).toUpperCase(Locale.ROOT);
        String tt = String.valueOf(target.get("ds_type")).toUpperCase(Locale.ROOT);
        boolean hive2mysql = "HIVE".equals(st) && "MYSQL".equals(tt);
        boolean mysql2hive = "MYSQL".equals(st) && "HIVE".equals(tt);
        boolean mysql2mysql = "MYSQL".equals(st) && "MYSQL".equals(tt);
        if (!hive2mysql && !mysql2hive && !mysql2mysql) {
            throw new BusinessException("DataX 引擎当前支持 MySQL/Hive 之间的组合（"
                    + st + "→" + tt + "），请使用 DB-Sync 或扩展 Reader/Writer 适配器");
        }

        // 1. 解析源列信息（列名 + 类型），供 reader/writer 统一生成列定义
        List<ColumnInfo> columns = resolveColumns(source, task.getSourceTable());

        // 2. MYSQL→HIVE：先确保受管 Hive 表存在（HDFSWriter 只写文件，表结构需由 DDL 保证）
        if (mysql2hive) {
            ensureHiveTable(target, task.getTargetTable(), columns);
        }

        // 3. 渲染 job.json
        String jobJson = renderJobJson(task, source, target, columns, hive2mysql, mysql2hive);
        Path workDir = Files.createTempDirectory("datax-job-");
        Path jobFile = workDir.resolve("job.json");
        Files.write(jobFile, jobJson.getBytes(StandardCharsets.UTF_8));
        log.info("DataX job 已生成: {} \n{}", jobFile, jobJson);

        // 4. 调用 python datax.py job.json 执行（DataX 官方仅支持 JDK8，datax.py 已内置指向 /opt/jdk8）
        List<String> cmd = new ArrayList<>();
        cmd.add("python");
        cmd.add(script.getAbsolutePath());
        cmd.add(jobFile.toFile().getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        long timeoutSeconds = EngineUtils.configTimeout(task.getConfig());
        Process process = pb.start();
        // 实时转发子进程输出到日志，便于排障（DataX 的 stdout 含进度与错误诊断）
        Thread outForwarder = new Thread(() -> {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[DataX] {}", line);
                }
            } catch (Exception ignore) {
                // 子进程结束后退出
            }
        }, "datax-stdout-forwarder");
        outForwarder.setDaemon(true);
        outForwarder.start();
        boolean finished = process.waitFor(timeoutSeconds > 0 ? timeoutSeconds : 3600, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new BusinessException("DataX 执行超时（> " + (timeoutSeconds > 0 ? timeoutSeconds : 3600) + "s）");
        }
        int exit = process.exitValue();
        log.info("DataX 执行完成，exit={}", exit);
        if (exit != 0) {
            throw new BusinessException("DataX 执行失败，退出码=" + exit + "，请查看引擎日志");
        }
        return columns.isEmpty() ? 0 : -1; // DataX 无行数统计，返回 -1 表示成功完成
    }

    // ------------------------------------------------------------------
    // job.json 渲染
    // ------------------------------------------------------------------

    /** 按链路组装 reader/writer 并封装 job.json */
    private String renderJobJson(PipelineTask task, Map<String, Object> source, Map<String, Object> target,
                                 List<ColumnInfo> columns, boolean hive2mysql, boolean mysql2hive) {
        JSONObject reader;
        JSONObject writer;
        if (hive2mysql) {
            reader = buildHiveReader(task, columns);
            writer = buildMysqlWriter(task, target, columns);
        } else if (mysql2hive) {
            reader = buildMysqlReader(task, source, columns);
            writer = buildHiveWriter(String.valueOf(task.getTargetTable()), columns);
        } else {
            reader = buildMysqlReader(task, source, columns);
            writer = buildMysqlWriter(task, target, columns);
        }

        JSONObject content = new JSONObject();
        content.set("reader", reader);
        content.set("writer", writer);

        // DataX 标准格式要求顶层必须有 "job" 包装（job.content / job.setting），缺省会解析不到插件名 -> 插件[null,null]
        JSONObject job = new JSONObject();
        job.set("content", new Object[]{content});
        job.set("setting", new JSONObject()
                .set("speed", new JSONObject().set("channel", 1))
                .set("errorLimit", new JSONObject().set("record", 0)));
        return JSONUtil.toJsonPrettyStr(new JSONObject().set("job", job));
    }

    /** Hive 源 → hdfsreader：按源列顺序生成 index/type，读 Hive 表 HDFS 文本文簇 */
    private JSONObject buildHiveReader(PipelineTask task, List<ColumnInfo> columns) {
        String table = String.valueOf(task.getSourceTable());
        String path = cfg(task).getStr(CFG_HIVE_PATH,
                warehouse() + hiveDir(table) + "/*");
        List<Object> readerColumns = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            JSONObject col = new JSONObject();
            col.set("index", i);
            // hdfsreader 使用 UnstructuredStorageReaderUtil.Type 枚举（大写：LONG/DOUBLE/BOOLEAN/DATE/STRING）
            col.set("type", toUnstructuredType(columns.get(i).type));
            readerColumns.add(col);
        }
        JSONObject reader = new JSONObject();
        reader.set("name", "hdfsreader");
        JSONObject param = new JSONObject();
        param.set("path", path);
        param.set("defaultFS", defaultFs(task));
        param.set("fileType", "text");
        param.set("fieldDelimiter", cfg(task).getStr("fieldDelimiter", "\t"));
        param.set("encoding", "UTF-8");
        param.set("column", readerColumns.toArray());
        reader.set("parameter", param);
        return reader;
    }

    /** MySQL 源 → mysqlreader：全表 / 自定义 SQL，列按解析顺序输出 */
    private JSONObject buildMysqlReader(PipelineTask task, Map<String, Object> source, List<ColumnInfo> columns) {
        String sourceSql = StrUtil.isNotBlank(task.getSourceQuery())
                ? task.getSourceQuery().trim()
                : "select * from " + mysqlTableRef(String.valueOf(source.get("database_name")), task.getSourceTable());
        JSONObject reader = new JSONObject();
        reader.set("name", "mysqlreader");
        JSONObject readerParam = new JSONObject();
        readerParam.set("username", source.get("username"));
        readerParam.set("password", EngineUtils.decryptPwd(String.valueOf(source.get("password")), aesKey));
        readerParam.set("column", columnNamesArr(columns));
        readerParam.set("connection", new Object[]{new JSONObject()
                .set("jdbcUrl", new String[]{EngineUtils.jdbcUrl(source)})
                .set("querySql", new String[]{sourceSql})});
        reader.set("parameter", readerParam);
        return reader;
    }

    /** MySQL 目标 → mysqlwriter：按源列顺序写对应目标列（名称对齐） */
    private JSONObject buildMysqlWriter(PipelineTask task, Map<String, Object> target, List<ColumnInfo> columns) {
        JSONObject writer = new JSONObject();
        writer.set("name", "mysqlwriter");
        JSONObject param = new JSONObject();
        param.set("username", target.get("username"));
        param.set("password", EngineUtils.decryptPwd(String.valueOf(target.get("password")), aesKey));
        param.set("writeMode", "replace");
        param.set("column", columnNamesArr(columns));
        param.set("connection", new Object[]{new JSONObject()
                .set("jdbcUrl", EngineUtils.jdbcUrl(target))
                .set("table", new String[]{String.valueOf(task.getTargetTable())})});
        writer.set("parameter", param);
        return writer;
    }

    /** Hive 目标 → hdfswriter：写 Hive warehouse 目录（受管表），name/type 与 Hive 表列一致 */
    private JSONObject buildHiveWriter(String table, List<ColumnInfo> columns) {
        JSONObject writer = new JSONObject();
        writer.set("name", "hdfswriter");
        JSONObject param = new JSONObject();
        param.set("defaultFS", DEFAULT_HDFS_FS);
        param.set("fileType", "text");
        param.set("path", warehouse() + hiveDir(table));
        // 文件名不可以下划线/点开头，否则 Hadoop 视为隐藏文件，Hive 读表会跳过
        param.set("fileName", "dx_" + sanitize(table));
        param.set("writeMode", "truncate");
        param.set("fieldDelimiter", "\t");
        param.set("encoding", "UTF-8");
        List<Object> writerColumns = new ArrayList<>();
        for (ColumnInfo c : columns) {
            JSONObject col = new JSONObject();
            col.set("name", c.name);
            // hdfswriter 使用 Hive 类型（如 BIGINT/STRING/DOUBLE/DATE/TIMESTAMP），而非 DataX 类型
            col.set("type", toHiveType(c.type));
            writerColumns.add(col);
        }
        param.set("column", writerColumns.toArray());
        writer.set("parameter", param);
        return writer;
    }

    // ------------------------------------------------------------------
    // Hive 受管表 DDL（MYSQL→HIVE 目标保证表存在）
    // ------------------------------------------------------------------

    /** 按 MySQL 源列信息创建 Hive 受管表（IF NOT EXISTS），LOCATION 指向 HDFSWriter 写入路径 */
    private void ensureHiveTable(Map<String, Object> hiveDs, String table, List<ColumnInfo> columns) {
        String hiveUrl = EngineUtils.jdbcUrl(hiveDs);
        String hiveUser = String.valueOf(hiveDs.get("username"));
        String hivePwd = EngineUtils.decryptPwd(String.valueOf(hiveDs.get("password")), aesKey);
        try (Connection conn = DriverManager.getConnection(hiveUrl, hiveUser, hivePwd);
             Statement st = conn.createStatement()) {
            if (StrUtil.isNotBlank(TableRef.parse(table).db)) {
                st.execute("CREATE DATABASE IF NOT EXISTS `" + TableRef.parse(table).db + "`");
            }
            StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS "
                    + TableRef.parse(table).hiveRef() + " (\n");
            for (int i = 0; i < columns.size(); i++) {
                ddl.append("  `").append(columns.get(i).name).append("` ")
                        .append(toHiveType(columns.get(i).type));
                ddl.append(i < columns.size() - 1 ? ",\n" : "\n");
            }
            ddl.append(")\nROW FORMAT DELIMITED FIELDS TERMINATED BY '\\t'\n")
                    .append("STORED AS TEXTFILE\n")
                    .append("LOCATION '").append(DEFAULT_HDFS_FS)
                    .append(warehouse()).append(hiveDir(table)).append("'");
            log.info("确保 Hive 受管表存在:\n{}", ddl);
            st.execute(ddl.toString());
        } catch (Exception e) {
            log.error("创建 Hive 受管表失败: {}", table, e);
            throw new BusinessException("创建 Hive 受管表失败: " + table + "，" + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 列元数据解析
    // ------------------------------------------------------------------

    /** 通过 JDBC 解析源表列（列名 + SQL 类型），WHERE 1=0 只取元数据 */
    private List<ColumnInfo> resolveColumns(Map<String, Object> source, String table) {
        String type = String.valueOf(source.get("ds_type")).toUpperCase(Locale.ROOT);
        String sql;
        if ("HIVE".equals(type)) {
            sql = "select * from " + table + " where 1=0";
        } else {
            sql = "select * from " + mysqlTableRef(String.valueOf(source.get("database_name")), table) + " where 1=0";
        }
        List<ColumnInfo> columns = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(EngineUtils.jdbcUrl(source),
                String.valueOf(source.get("username")),
                EngineUtils.decryptPwd(String.valueOf(source.get("password")), aesKey));
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                String name = md.getColumnLabel(i);
                // Hive 结果集列标签形如 表名.列名，去掉前缀
                int dot = name == null ? -1 : name.lastIndexOf('.');
                String clean = dot > 0 ? name.substring(dot + 1) : name;
                columns.add(new ColumnInfo(clean, normalizeType(md.getColumnTypeName(i))));
            }
        } catch (Exception e) {
            log.error("解析源表列信息失败: {}", table, e);
            throw new BusinessException("解析源表列信息失败: " + table + "，" + e.getMessage());
        }
        if (columns.isEmpty()) {
            throw new BusinessException("源表无可用列: " + table);
        }
        return columns;
    }

    /** SQL 类型 → 统一大写主类型（DataX 映射用） */
    private String normalizeType(String columnTypeName) {
        String t = columnTypeName == null ? "STRING" : columnTypeName.toUpperCase(Locale.ROOT);
        if (t.contains("INT")) return t; // INT/INTEGER/BIGINT/TINYINT/SMALLINT 保留区分
        if (t.startsWith("DECIMAL") || t.startsWith("NUMERIC")) return "DECIMAL";
        if (t.startsWith("CHAR") || t.startsWith("VARCHAR") || t.contains("TEXT")) return "STRING";
        if (t.startsWith("DATETIME") || t.startsWith("TIMESTAMP")) return "DATETIME";
        if (t.equals("REAL")) return "FLOAT";
        String known = switch (t) {
            case "FLOAT", "DOUBLE", "BOOLEAN", "BIT", "DATE", "TIME", "BLOB", "VARBINARY", "BINARY" -> t;
            default -> t;
        };
        return known;
    }

    /** SQL 主类型 → hdfsreader 列类型（UnstructuredStorageReaderUtil.Type 枚举，大写） */
    private String toUnstructuredType(String sqlType) {
        String t = sqlType == null ? "STRING" : sqlType.toUpperCase(Locale.ROOT);
        if (t.contains("INT")) return "LONG";
        if (t.equals("DECIMAL") || t.equals("FLOAT") || t.equals("DOUBLE") || t.equals("REAL")) return "DOUBLE";
        if (t.equals("BOOLEAN") || t.equals("BIT")) return "BOOLEAN";
        if (t.equals("DATE") || t.equals("DATETIME") || t.equals("TIMESTAMP") || t.equals("TIME")) return "DATE";
        return "STRING";
    }

    /** SQL 主类型 → Hive DDL 类型 */
    private String toHiveType(String sqlType) {
        String t = sqlType == null ? "STRING" : sqlType.toUpperCase(Locale.ROOT);
        switch (t) {
            case "BIGINT":
                return "BIGINT";
            case "SMALLINT":
            case "TINYINT":
            case "INT":
            case "INTEGER":
                return "INT";
            case "DECIMAL":
                return "DECIMAL(38,10)";
            case "FLOAT":
            case "REAL":
                return "FLOAT";
            case "DOUBLE":
                return "DOUBLE";
            case "BOOLEAN":
            case "BIT":
                return "BOOLEAN";
            case "DATE":
                return "DATE";
            case "DATETIME":
            case "TIMESTAMP":
            case "TIME":
                return "TIMESTAMP";
            default:
                return "STRING";
        }
    }

    private String[] columnNamesArr(List<ColumnInfo> columns) {
        String[] arr = new String[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            arr[i] = columns.get(i).name;
        }
        return arr;
    }

    // ------------------------------------------------------------------
    // 配置读取 / 路径拼接
    // ------------------------------------------------------------------

    /** 任务 config 的 JSON 视图 */
    private JSONObject cfg(PipelineTask task) {
        return StrUtil.isNotBlank(task.getConfig()) ? JSONUtil.parseObj(task.getConfig()) : new JSONObject();
    }

    private String defaultFs(PipelineTask task) {
        return task == null ? DEFAULT_HDFS_FS : cfg(task).getStr(CFG_HDFS_FS, DEFAULT_HDFS_FS);
    }

    private String warehouse() {
        return DEFAULT_WAREHOUSE;
    }

    /** 根据库.表 计算 Hive warehouse 下的目录：/db/xx.db/yy 或 /default/ 缺省 */
    private String hiveDir(String table) {
        if (StrUtil.isBlank(table)) {
            return "/" + table;
        }
        int dot = table.lastIndexOf('.');
        if (dot > 0) {
            String db = table.substring(0, dot);
            String t = table.substring(dot + 1);
            return "/" + db + ".db/" + t;
        }
        return "/" + table;
    }

    /** MySQL 标识符引用：database_name.表名 */
    private String mysqlTableRef(String databaseName, String table) {
        return "`" + databaseName + "`.`" + table + "`";
    }

    private String sanitize(String table) {
        return table == null ? "hive" : table.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private String sourceType(PipelineTask task) {
        return EngineUtils.getDs(jdbcTemplate, task.getSourceDsId()).get("ds_type").toString();
    }

    private String targetType(PipelineTask task) {
        return EngineUtils.getDs(jdbcTemplate, task.getTargetDsId()).get("ds_type").toString();
    }

    // ------------------------------------------------------------------
    // 内部数据结构
    // ------------------------------------------------------------------

    /** 单列元数据 */
    private static final class ColumnInfo {
        final String name;
        final String type;

        ColumnInfo(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    /** 库.表 引用 */
    private static final class TableRef {
        final String db;
        final String table;

        TableRef(String db, String table) {
            this.db = db;
            this.table = table;
        }

        static TableRef parse(String full) {
            if (StrUtil.isBlank(full)) {
                return new TableRef(null, full);
            }
            int dot = full.lastIndexOf('.');
            return dot > 0
                    ? new TableRef(full.substring(0, dot), full.substring(dot + 1))
                    : new TableRef(null, full);
        }

        String hiveRef() {
            return StrUtil.isBlank(db) ? "`" + table + "`" : "`" + db + "`.`" + table + "`";
        }
    }
}