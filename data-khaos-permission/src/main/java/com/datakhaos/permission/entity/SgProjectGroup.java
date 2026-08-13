package com.datakhaos.permission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目组表（组织/业务线下的业务协作单元）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sg_project_group")
public class SgProjectGroup extends BaseEntity {

    /** 所属组织/业务线ID */
    private String orgId;

    private String projectName;

    private String projectCode;

    /** 组长用户ID */
    private String leaderId;

    /** 状态 0:停用 1:启用 */
    private Integer status;

    private Integer sortOrder;
}