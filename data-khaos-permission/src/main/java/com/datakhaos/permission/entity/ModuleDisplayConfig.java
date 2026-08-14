package com.datakhaos.permission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 门户模块展示配置（可插拔模块开关）
 * module_key 与前端模块注册表(registry.ts)一一对应。
 */
@Data
@TableName("module_display_config")
public class ModuleDisplayConfig {

    /** 模块唯一标识（主键，由系统/前端注册表定义） */
    @TableId(value = "module_key", type = IdType.INPUT)
    private String moduleKey;

    private String moduleName;

    /** 归属分类 ingress/dev/govern/asset/service/ops/system */
    private String category;

    private String categoryName;

    private String icon;

    /** 路由路径（空=待建设） */
    private String path;

    /** 1=系统必须，不可取消 0=可配置 */
    private Integer mandatory;

    /** 1=显示 0=隐藏 */
    private Integer visible;

    private Integer sortOrder;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}