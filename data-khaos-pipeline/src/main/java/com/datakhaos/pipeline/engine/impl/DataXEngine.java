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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * DataX 引擎适配器（阿里开源离线同步）。
 *
 * <p>生成 DataX job.json（MySQL → MySQL）并调用 <code>python datax.py job.json</code> 执行。
 * 引擎未安装（未配置 DATAX_HOME）时明确提示；安装后无缝启用，属「可插拔引擎」扩展实现。
 */
@Slf4j
@Component
public class DataXEngine implements PipelineEngine {

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
        return JSONUtil.toJsonStr(Map.of(
                "job_type", "HIVE".equalsIgnoreCase(st) ? "HIVE_TO_MYSQL" : "MYSQL_TO_MYSQL",
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
        String st = String.valueOf(source.get("ds_type")).toUpperCase();
        String tt = String.valueOf(target.get("ds_type")).toUpperCase();
        if (!"MYSQL".equals(tt)) {
            throw new BusinessException("DataX hive2mysql 目标当前仅支持 MySQL（" + st + "→" + tt + "）");
        }
        if (!"MYSQL".equals(st) && !"HIVE".equals(st)) {
            throw new BusinessException("DataX 引擎当前支持 MySQL/Hive 源（" + st + "→" + tt + "），其他类型请扩展 DataX Reader 适配器");
        }

        // 1. 渲染 job.json
        String jobJson = renderJobJson(task, source, target);
        Path workDir = Files.createTempDirectory("datax-job-");
        Path jobFile = workDir.resolve("job.json");
        Files.write(jobFile, jobJson.getBytes(StandardCharsets.UTF_8));
        log.info("DataX job 已生成: {} \n{}", jobFile, jobJson);

        // 2. 调用 python datax.py job.json 执行
        List<String> cmd = new ArrayList<>();
        cmd.add("python");
        cmd.add(script.getAbsolutePath());
        cmd.add(jobFile.toFile().getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        long timeoutSeconds = EngineUtils.configTimeout(task.getConfig());
        Process process = pb.start();
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
        return 0;
    }

    private String sourceType(PipelineTask task) {
        return EngineUtils.getDs(jdbcTemplate, task.getSourceDsId()).get("ds_type").toString();
    }

    /** 渲染 DataX job.json：Hive 源用 hdfsreader（读 Hive 表 HDFS 落地文件），MySQL 源用 mysqlreader；writer 均为 mysqlwriter */
    private String renderJobJson(PipelineTask task, Map<String, Object> source, Map<String, Object> target) {
        String st = String.valueOf(source.get("ds_type")).toUpperCase();
        JSONObject reader = "HIVE".equalsIgnoreCase(st)
                ? buildHiveReader(task)
                : buildMysqlReader(task, source);

        JSONObject writer = new JSONObject();
        writer.set("name", "mysqlwriter");
        JSONObject writerParam = new JSONObject();
        writerParam.set("username", target.get("username"));
        writerParam.set("password", EngineUtils.decryptPwd(String.valueOf(target.get("password")), aesKey));
        writerParam.set("writeMode", "replace");
        writerParam.set("column", new String[]{"*"});
        writerParam.set("connection", new Object[]{new JSONObject()
                .set("jdbcUrl", EngineUtils.jdbcUrl(target))
                .set("table", new String[]{task.getTargetTable()})});
        writer.set("parameter", writerParam);

        JSONObject content = new JSONObject();
        content.set("reader", reader);
        content.set("writer", writer);

        JSONObject job = new JSONObject();
        job.set("content", new Object[]{content});
        job.set("setting", new JSONObject()
                .set("speed", new JSONObject().set("channel", 1))
                .set("errorLimit", new JSONObject().set("record", 0)));
        return JSONUtil.toJsonPrettyStr(job);
    }

    /** Hive → DataX hdfsreader：读取 Hive 表在 HDFS 上的落地文件（文本分隔符可配置） */
    private JSONObject buildHiveReader(PipelineTask task) {
        String hdfsPath = cn.hutool.json.JSONUtil.parseObj(task.getConfig() == null ? "{}" : task.getConfig())
                .getStr("hdfsPath", "hdfs://localhost:9000/user/hive/warehouse/" + task.getSourceTable() + "/*");
        // 表名可能为 库.表，落到 /user/hive/warehouse/库.db/表
        String table = task.getSourceTable();
        if (StrUtil.isNotBlank(table) && table.contains(".")) {
            String db = table.substring(0, table.lastIndexOf('.'));
            String t = table.substring(table.lastIndexOf('.') + 1);
            hdfsPath = "hdfs://localhost:9000/user/hive/warehouse/" + db + ".db/" + t + "/*";
        }
        JSONObject reader = new JSONObject();
        reader.set("name", "hdfsreader");
        JSONObject param = new JSONObject();
        param.set("path", hdfsPath);
        param.set("defaultFS", "hdfs://localhost:9000");
        param.set("fileType", "text");
        param.set("fieldDelimiter", "\t");
        param.set("encoding", "UTF-8");
        param.set("column", new Object[]{new JSONObject().set("type", "string")});
        reader.set("parameter", param);
        return reader;
    }

    /** MySQL → DataX mysqlreader：全表 / 自定义 SQL */
    private JSONObject buildMysqlReader(PipelineTask task, Map<String, Object> source) {
        String sourceSql = StrUtil.isNotBlank(task.getSourceQuery())
                ? task.getSourceQuery().trim()
                : "select * from `" + task.getSourceTable() + "`";
        JSONObject reader = new JSONObject();
        reader.set("name", "mysqlreader");
        JSONObject readerParam = new JSONObject();
        readerParam.set("username", source.get("username"));
        readerParam.set("password", EngineUtils.decryptPwd(String.valueOf(source.get("password")), aesKey));
        readerParam.set("column", new String[]{"*"});
        readerParam.set("connection", new Object[]{new JSONObject()
                .set("jdbcUrl", new String[]{EngineUtils.jdbcUrl(source)})
                .set("querySql", new String[]{sourceSql})});
        reader.set("parameter", readerParam);
        return reader;
    }
}