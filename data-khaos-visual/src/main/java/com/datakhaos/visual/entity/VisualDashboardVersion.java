package com.datakhaos.visual.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 仪表板版本快照表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visual_dashboard_version")
public class VisualDashboardVersion extends BaseEntity {

    /** 仪表板ID */
    private String dashboardId;

    /** 版本号 */
    private Integer version;

    /** 仪表板名称快照 */
    private String name;

    /** 描述快照 */
    private String description;

    /** 布局快照（JSON） */
    private String layout;

    /** 刷新间隔快照（秒） */
    private Integer refreshInterval;

    /** 组件快照（JSON 数组） */
    private String itemsJson;

    /** 分析板快照（JSON 数组） */
    private String boardsJson;

    /** 发布说明 */
    private String remark;

    /** 发布人 */
    private String createBy;
}