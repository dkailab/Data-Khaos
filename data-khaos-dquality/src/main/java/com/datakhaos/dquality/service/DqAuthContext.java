package com.datakhaos.dquality.service;

import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.model.ResultCode;
import com.datakhaos.common.security.MetadataHolder;
import com.datakhaos.permission.api.model.UserPermissionDto;
import com.datakhaos.permission.api.service.PermissionApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据质量权限上下文：从当前用户解析项目组、能力位与超管标记。
 */
@Component
@RequiredArgsConstructor
public class DqAuthContext {

    private final PermissionApiClient permissionApiClient;

    /** 当前请求的权限上下文 */
    public record AuthContext(String userId, String projectGroupId, Set<String> capabilities, boolean superAdmin) {
        public boolean hasCap(String cap) {
            return superAdmin || (capabilities != null && capabilities.contains(cap));
        }
    }

    public AuthContext current() {
        String userId = MetadataHolder.getUserId();
        boolean sa = MetadataHolder.isSuperAdmin();
        if (userId == null) {
            return new AuthContext(null, null, Set.of(), sa);
        }
        if (sa) {
            return new AuthContext(userId, null, null, true);
        }
        UserPermissionDto perm = permissionApiClient.getUserPermission(userId);
        List<String> caps = perm.getCapabilityFlags() == null ? List.of() : perm.getCapabilityFlags();
        return new AuthContext(userId, perm.getProjectGroupId(), new HashSet<>(caps), false);
    }

    public void requireCap(AuthContext ctx, String cap) {
        if (!ctx.hasCap(cap)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无「" + cap + "」操作权限");
        }
    }

    /** 校验资源归属当前项目组（或超管），否则拒绝 */
    public void checkGroup(AuthContext ctx, String resourceGroupId, String resourceName) {
        if (ctx.superAdmin()) {
            return;
        }
        if (ctx.projectGroupId() == null || !ctx.projectGroupId().equals(resourceGroupId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该项目组的" + resourceName);
        }
    }
}