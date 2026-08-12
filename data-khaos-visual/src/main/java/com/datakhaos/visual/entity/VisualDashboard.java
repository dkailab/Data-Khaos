package com.datakhaos.visual.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 仪表板表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visual_dashboard")
public class VisualDashboard extends BaseEntity {

    /** 仪表板名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 布局配置（JSON） */
    private String layout;

    /** 刷新间隔（秒） */
    private Integer refreshInterval;

    private Integer status;

    /** 创建人 */
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
