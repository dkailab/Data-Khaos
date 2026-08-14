package com.datakhaos.pipeline.engine.impl;

import cn.hutool.core.util.StrUtil;
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
 * SeaTunnel（Waterdrop）引擎适配器（Apache 开源，支持流/批）。
 *
 * <p>生成 SeaTunnel conf 文件并调用 <code>bin/start-seatunnel-spark-*.sh</code> 执行
 * （当前简化模式：批处理 mode 仅用 local[*] 直接执行 MySQL → MySQL 同步）。
 * 引擎未安装（未配置 SEATUNNEL_HOME）时明确提示；安装后无缝启用，属「可插拔引擎」扩展实现。
 */
@Slf4j
@Component
public class SeaTunnelEngine implements PipelineEngine {

    private final JdbcTemplate jdbcTemplate;
    private final String aesKey;

    public SeaTunnelEngine(JdbcTemplate jdbcTemplate,
                           @Value("${data-khaos.aes-key:dk-aes-key-16byte}") String aesKey) {
        this.jdbcTemplate = jdbcTemplate;
        this.aesKey = aesKey;
    }

    @Override
    public String type() {
        return "SEATUNNEL";
    }

    @Override
    public String name() {
        return "SeaTunnel（Waterdrop）";
    }

    @Override
    public boolean available() {
        String home = System.getenv("SEATUNNEL_HOME");
        return StrUtil.isNotBlank(home) && new File(home, "bin/start-seatunnel.sh").exists();
    }

    @Override
    public String buildRunConfig(PipelineTask task) {
        return cn.hutool.json.JSONUtil.toJsonStr(Map.of(
                "job_type", "MYSQL_TO_MYSQL",
                "source_table", task.getSourceTable(),
                "target_table", task.getTargetTable()));
    }

    @Override
    public int execute(PipelineTask task, PipelineInstance instance) throws Exception {
        String home = System.getenv("SEATUNNEL_HOME");
        if (StrUtil.isBlank(home)) {
            throw new BusinessException("SeaTunnel 引擎未安装：请设置环境变量 SEATUNNEL_HOME 并安装 SeaTunnel");
        }
        File script = findStartScript(home);
        if (script == null || !script.exists()) {
            throw new BusinessException("SeaTunnel 启动脚本不存在: " + (script != null ? script.getAbsolutePath() : home + "/bin/"));
        }
        Map<String, Object> source = EngineUtils.getDs(jdbcTemplate, task.getSourceDsId());
        Map<String, Object> target = EngineUtils.getDs(jdbcTemplate, task.getTargetDsId());
        EngineUtils.checkMysql(source, target);

        // 1. 生成 conf/ job.config 配置文件
        String confContent = renderConf(task, source, target);
        Path workDir = Files.createTempDirectory("seatunnel-job-");
        Path confFile = workDir.resolve("job.conf");
        Files.write(confFile, confContent.getBytes(StandardCharsets.UTF_8));
        log.info("SeaTunnel job conf 已生成: {} \n{}", confFile, confContent);

        // 2. 调用 ./start-seatunnel.sh --config job.conf
        List<String> cmd = new ArrayList<>();
        cmd.add(script.getAbsolutePath());
        cmd.add("--config");
        cmd.add(confFile.toFile().getAbsolutePath());
        cmd.add("--name");
        cmd.add("dk-pipeline-" + task.getId());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        long timeoutSeconds = EngineUtils.configTimeout(task.getConfig());
        Process process = pb.start();
        boolean finished = process.waitFor(timeoutSeconds > 0 ? timeoutSeconds : 7200, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new BusinessException("SeaTunnel 执行超时（> " + (timeoutSeconds > 0 ? timeoutSeconds : 7200) + "s）");
        }
        int exit = process.exitValue();
        log.info("SeaTunnel 执行完成，exit={}", exit);
        if (exit != 0) {
            throw new BusinessException("SeaTunnel 执行失败，退出码=" + exit + "，请查看引擎日志");
        }
        return 0;
    }

    /** 找启动脚本（尝试多个常见命名） */
    private File findStartScript(String home) {
        List<String> candidates = List.of(
                "bin/start-seatunnel.sh",
                "bin/start-seatunnel-spark-connector-v2.sh",
                "bin/start-seatunnel-flink-connector-v2.sh"
        );
        for (String c : candidates) {
            File f = new File(home, c);
            if (f.exists()) {
                return f;
            }
        }
        return null;
    }

    /**
     * 渲染 MySQL → MySQL 的 SeaTunnel HOCON 配置文件（v2 格式）
     * env → source → sink
     */
    private String renderConf(PipelineTask task, Map<String, Object> source, Map<String, Object> target) {
        String sourceUrl = EngineUtils.jdbcUrl(source);
        String sourceUser = String.valueOf(source.get("username"));
        String sourcePass = EngineUtils.decryptPwd(String.valueOf(source.get("password")), aesKey);
        String targetUrl = EngineUtils.jdbcUrl(target);
        String targetUser = String.valueOf(target.get("username"));
        String targetPass = EngineUtils.decryptPwd(String.valueOf(target.get("password")), aesKey);
        String query = StrUtil.isNotBlank(task.getSourceQuery())
                ? task.getSourceQuery().trim()
                : "SELECT * FROM " + task.getSourceTable();

        // SeaTunnel 2.x HOCON 格式
        StringBuilder sb = new StringBuilder();
        sb.append("env {\n");
        sb.append("  execution.parallelism = 1\n");
        sb.append("  job.mode = BATCH\n}\n\n");

        sb.append("source {\n");
        sb.append("  Jdbc {\n");
        sb.append("    url = \"").append(sourceUrl).append("\"\n");
        sb.append("    user = \"").append(sourceUser).append("\"\n");
        sb.append("    password = \"").append(sourcePass).append("\"\n");
        sb.append("    query = \"").append(query.replace("\"", "\\\"")).append("\"\n");
        sb.append("  }\n}\n\n");

        sb.append("sink {\n");
        sb.append("  Jdbc {\n");
        sb.append("    url = \"").append(targetUrl).append("\"\n");
        sb.append("    user = \"").append(targetUser).append("\"\n");
        sb.append("    password = \"").append(targetPass).append("\"\n");
        sb.append("    table = \"").append(task.getTargetTable()).append("\"\n");
        sb.append("    primary_keys = [\"id\"]\n");
        sb.append("  }\n}\n");
        return sb.toString();
    }
}