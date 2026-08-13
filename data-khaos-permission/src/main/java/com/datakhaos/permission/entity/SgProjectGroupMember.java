package com.datakhaos.permission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目组成员表（人→项目组，含组内角色与主组标记）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sg_project_group_member")
public class SgProjectGroupMember extends BaseEntity {

    private String projectGroupId;

    private String userId;

    /** 组内角色ID（引用 sg_project_role.id） */
    private String projectRoleId;

    /** 是否主项目组 0:否 1:是（用于当前项目组上下文） */
    private Integer isPrimary;
}