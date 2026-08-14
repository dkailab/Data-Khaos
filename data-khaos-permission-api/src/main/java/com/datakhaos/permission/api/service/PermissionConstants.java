package com.datakhaos.permission.api.service;

/**
 * 权限标识常量
 */
public interface PermissionConstants {

    /** 超级管理员角色 */
    String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    String PERM_USER_LIST = "system:user:list";
    String PERM_USER_EDIT = "system:user:edit";
    String PERM_ROLE_LIST = "system:role:list";
    String PERM_ROLE_EDIT = "system:role:edit";
    String PERM_MENU_LIST = "system:menu:list";
    String PERM_MENU_EDIT = "system:menu:edit";
    String PERM_ORG_LIST = "system:org:list";
    String PERM_ORG_EDIT = "system:org:edit";

    String PERM_DS_LIST = "data:datasource:list";
    String PERM_DS_EDIT = "data:datasource:edit";
    String PERM_META_LIST = "data:metadata:list";
    String PERM_MART_LIST = "data:mart:list";
    String PERM_MART_EDIT = "data:mart:edit";

    /** 语义模型(Mart)能力位（permission-handbook §4.2） */
    String CAP_MODEL_BROWSE = "model:browse";
    String CAP_MODEL_DEVELOP = "model:develop";
    String CAP_MODEL_PUBLISH = "model:publish";

    /** 数据质量能力位（数据治理能力域） */
    String CAP_QUALITY_MANAGE = "quality:manage";
    String CAP_QUALITY_BROWSE = "quality:browse";
    String CAP_QUALITY_RUN = "quality:run";

    /** 门户模块展示配置能力位（管理员级，全局模块可插拔配置） */
    String CAP_MODULE_CONFIG = "module:config";

    /** 数据管道能力位（数据接入能力域） */
    String CAP_PIPELINE_MANAGE = "pipeline:manage";
    String CAP_PIPELINE_BROWSE = "pipeline:browse";
    String CAP_PIPELINE_RUN = "pipeline:run";

    String PERM_SQL_EXECUTE = "query:sql:execute";
    String PERM_DASHBOARD_VIEW = "query:dashboard:view";
    String PERM_ANALYSIS_VIEW = "query:analysis:view";

    String PERM_SCHEDULE_LIST = "ops:schedule:list";
    String PERM_SCHEDULE_EDIT = "ops:schedule:edit";
    String PERM_NOTIFY_LIST = "ops:notification:list";
    String PERM_NOTIFY_EDIT = "ops:notification:edit";
    String PERM_APPROVAL_LIST = "ops:approval:list";
    String PERM_APPROVAL_EDIT = "ops:approval:edit";
}
