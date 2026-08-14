package com.datakhaos.permission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.security.MetadataHolder;
import com.datakhaos.permission.api.service.PermissionConstants;
import com.datakhaos.permission.entity.ModuleDisplayConfig;
import com.datakhaos.permission.mapper.ModuleDisplayConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 门户模块展示配置服务（可插拔模块）
 * 普通用户可读；修改需 module:config 能力位；系统必须模块(mandatory=1)不允许取消展示。
 */
@Service
@RequiredArgsConstructor
public class ModuleConfigService {

    private final ModuleDisplayConfigMapper moduleConfigMapper;
    private final PermissionService permissionService;

    /** 全部模块配置（含必须标识与可见性），按分类、排序返回 */
    public List<ModuleDisplayConfig> list() {
        return moduleConfigMapper.selectList(new LambdaQueryWrapper<ModuleDisplayConfig>()
                .orderByAsc(ModuleDisplayConfig::getCategory)
                .orderByAsc(ModuleDisplayConfig::getSortOrder));
    }

    /** 仅返回当前应展示的模块（visible=1） */
    public List<ModuleDisplayConfig> visibleList() {
        return moduleConfigMapper.selectList(new LambdaQueryWrapper<ModuleDisplayConfig>()
                .eq(ModuleDisplayConfig::getVisible, 1)
                .orderByAsc(ModuleDisplayConfig::getCategory)
                .orderByAsc(ModuleDisplayConfig::getSortOrder));
    }

    /**
     * 批量保存模块可见性。
     * 校验：调用方须具备 module:config 能力位；系统必须模块不可被隐藏。
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(List<ModuleDisplayConfig> updates) {
        checkCanConfig();
        if (updates == null || updates.isEmpty()) {
            return;
        }
        for (ModuleDisplayConfig u : updates) {
            if (u.getModuleKey() == null) {
                continue;
            }
            ModuleDisplayConfig exist = moduleConfigMapper.selectById(u.getModuleKey());
            if (exist == null) {
                continue;
            }
            // 必须模块不允许取消展示
            boolean mandatory = exist.getMandatory() != null && exist.getMandatory() == 1;
            boolean hide = u.getVisible() == null || u.getVisible() == 0;
            if (mandatory && hide) {
                throw new BusinessException("系统必须模块「" + exist.getModuleName() + "」不允许取消展示");
            }
            ModuleDisplayConfig patch = new ModuleDisplayConfig();
            patch.setModuleKey(u.getModuleKey());
            patch.setVisible(u.getVisible() == null || u.getVisible() == 0 ? 0 : 1);
            moduleConfigMapper.updateById(patch);
        }
    }

    /** 校验当前调用方是否具备模块配置能力（超级管理员天然具备） */
    private void checkCanConfig() {
        if (Boolean.TRUE.equals(MetadataHolder.isSuperAdmin())) {
            return;
        }
        String userId = MetadataHolder.getUserId();
        if (userId == null) {
            throw new BusinessException("未识别当前用户，无法执行模块配置");
        }
        List<String> flags = permissionService.getUserPermission(userId).getCapabilityFlags();
        if (flags == null || !flags.contains(PermissionConstants.CAP_MODULE_CONFIG)) {
            throw new BusinessException("无模块配置权限（module:config）");
        }
    }
}