package com.datakhaos.dquality.controller;

import com.datakhaos.common.model.R;
import com.datakhaos.dquality.entity.DqSnapshot;
import com.datakhaos.dquality.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 数据质量内部接口（供调度系统等微服务间调用，不经过网关鉴权）。
 * 调用方需以系统管理员身份透传用户头（X-User-Id / X-Roles: SUPER_ADMIN），
 * 由 {@code MetadataContextFilter} 注入上下文，此处不再重复校验权限。
 */
@Tag(name = "数据质量-内部接口")
@RestController
@RequestMapping("/api/dquality/internal")
@RequiredArgsConstructor
public class DqInternalController {

    private final TaskService taskService;

    @Operation(summary = "内部触发质量任务（供调度系统周期稽核）")
    @PostMapping("/task/{id}/run")
    public R<DqSnapshot> runTask(@PathVariable String id,
                                 @RequestParam(defaultValue = "SCHEDULE") String triggerType) {
        return R.ok(taskService.runInternal(id, triggerType));
    }
}
