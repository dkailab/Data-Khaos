package com.datakhaos.permission.api.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 项目组 DTO（用户可加入多个项目组，携带当前组的角色与能力位）
 */
@Data
public class ProjectGroupDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String orgId;

    private String projectName;

    private String projectCode;

    private String leaderId;

    /** 是否主项目组（当前项目组上下文） */
    private Boolean primary;

    /** 当前组内角色编码（如 PG_LEADER / PG_DEV / PG_USER） */
    private String roleCode;

    /** 当前组内角色能力位集合 */
    private java.util.List<String> capabilityFlags = new java.util.ArrayList<>();
}