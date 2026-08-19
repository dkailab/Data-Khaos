package com.datakhaos.workflow.executor;

import com.datakhaos.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Shell 脚本节点执行器：将节点脚本写入临时 .sh 文件后以 bash 执行。
 */
@Slf4j
@Component
public class ShellNodeExecutor extends AbstractScriptExecutor {

    @Override
    public String getType() {
        return NodeType.SHELL;
    }

    @Override
    public ExecResult execute(NodeContext context) {
        String script = context.getContent();
        if (script == null || script.isBlank()) {
            throw new BusinessException("Shell 脚本不能为空");
        }
        Path workDir;
        try {
            workDir = Files.createTempDirectory("wf-shell-");
        } catch (IOException e) {
            throw new BusinessException("创建临时目录失败: " + e.getMessage());
        }
        Path scriptFile = workDir.resolve("script.sh");
        try {
            Files.write(scriptFile, script.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new BusinessException("写入脚本失败: " + e.getMessage());
        }
        ProcessResult pr = runProcess(List.of("bash", scriptFile.toAbsolutePath().toString()),
                context.getTimeoutSeconds());
        return result(pr.getExitCode(), pr.getOutput());
    }
}