package com.datakhaos.permission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 组织架构表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_organization")
public class SysOrganization extends BaseEntity {

    private String parentId;

    private String orgName;

    private String orgCode;

    /** DEPT / COMPANY / GROUP */
    private String orgType;

    private Integer sortOrder;

    private Integer status;
}
