package com.datakhaos.workflow.executor;

import com.datakhaos.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Shell / Python 脚本执行支撑：通过 {@link ProcessBuilder} 启动本地子进程，
 * 合并 stdout/stderr 采集输出，支持超时强杀。
 */
@Slf4j
public abstract class AbstractScriptExecutor implements NodeExecutor {

    public static final long DEFAULT_TIMEOUT_SECONDS = 3600;

    protected ProcessResult runProcess(List<String> cmd, Long timeoutSeconds) {
        long timeout = timeoutSeconds == null || timeoutSeconds <= 0 ? DEFAULT_TIMEOUT_SECONDS : timeoutSeconds;
        StringBuilder output = new StringBuilder();
        Process process;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            process = pb.start();
        } catch (Exception e) {
            throw new BusinessException("启动进程失败: " + e.getMessage());
        }

        // 读取子进程输出（daemon，避免阻塞）
        Thread forwarder = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() < 100_000) {
                        output.append(line).append('\n');
                    }
                }
            } catch (Exception ignore) {
                // 子进程结束后通道关闭
            }
        }, "wf-script-forwarder");
        forwarder.setDaemon(true);
        forwarder.start();

        try {
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException("脚本执行超时（> " + timeout + "s）");
            }
            int exit = process.exitValue();
            log.info("脚本执行完成, exit={}, cmd={}", exit, cmd);
            return new ProcessResult(exit, output.toString());
        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new BusinessException("脚本执行被中断");
        }
    }

    protected ExecResult result(int exit, String output) {
        boolean success = exit == 0;
        return ExecResult.builder()
                .success(success)
                .rows(null)
                .log(success ? "退出码: 0\n" + output : "退出码: " + exit + "\n" + output)
                .build();
    }

    protected static final class ProcessResult {
        private final int exitCode;
        private final String output;

        private ProcessResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getOutput() {
            return output;
        }
    }
}