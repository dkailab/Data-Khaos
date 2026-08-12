package com.datakhaos.visual.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 仪表板组件表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visual_dashboard_item")
public class VisualDashboardItem extends BaseEntity {

    /** 仪表板ID */
    private String dashboardId;

    /** 组件标题 */
    private String title;

    /** BAR / LINE / PIE / TABLE / NUMBER */
    private String chartType;

    /** 数据源ID */
    private String datasourceId;

    /** 查询SQL */
    private String querySql;

    /** 组件配置（JSON） */
    private String config;

    /** X坐标 */
    private Integer posX;

    /** Y坐标 */
    private Integer posY;

    /** 宽度 */
    private Integer width;

    /** 高度 */
    private Integer height;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
