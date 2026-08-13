package com.datakhaos.permission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目组资源表（组下绑定的开发任务/报表/表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sg_project_group_resource")
public class SgProjectGroupResource extends BaseEntity {

    private String projectGroupId;

    /** 资源类型 TASK/REPORT/TABLE */
    private String resourceType;

    private String resourceId;
}