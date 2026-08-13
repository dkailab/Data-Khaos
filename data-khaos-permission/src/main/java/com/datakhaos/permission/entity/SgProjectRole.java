package com.datakhaos.permission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目组角色表（capability_flags 为能力位 JSON 数组；project_group_id 为空=全局模板）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sg_project_role")
public class SgProjectRole extends BaseEntity {

    private String orgId;

    /** 项目组ID（空=全局模板） */
    private String projectGroupId;

    private String roleName;

    private String roleCode;

    /** 能力位标识集合(JSON数组，如 ["model:develop","report:develop"]) */
    private String capabilityFlags;

    /** 状态 0:停用 1:启用 */
    private Integer status;

    private Integer sortOrder;
}