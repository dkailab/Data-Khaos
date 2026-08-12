package com.datakhaos.visual.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 分析板表（仪表板内嵌套子业务模块）。
 * 每个分析板独立绑定业务主题、数据维度、筛选条件、分析组件，支持独立配置/刷新/联动。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visual_board")
public class VisualBoard extends BaseEntity {

    /** 仪表板ID */
    private String dashboardId;

    /** 分析板标题 */
    private String boardName;

    /** 副标题 */
    private String subtitle;

    /** 图标 */
    private String icon;

    /** 类型 ANALYSIS / CUSTOM */
    private String boardType;

    /** 板块样式与布局配置（JSON） */
    private String layout;

    /** 分析板独立筛选配置（JSON）[{field,op,value}] + timeRange + dateColumn，优先级高于全局筛选 */
    private String filters;

    /** 是否联动全局筛选 1:联动 0:独立（默认联动） */
    private Integer linkGlobal;

    /** 自动刷新周期（秒） */
    private Integer refreshInterval;

    /** 是否折叠 0:展开 1:折叠 */
    private Integer collapse;

    /** 布局锁定 */
    private Integer locked;

    /** 排序 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}