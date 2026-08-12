package com.datakhaos.common.constant;

/**
 * 全局常量
 */
public interface CommonConstants {

    /** JWT 请求头 */
    String AUTH_HEADER = "Authorization";

    /** 网关透传的用户头 */
    String HEADER_USER_ID = "X-User-Id";
    String HEADER_USERNAME = "X-Username";
    String HEADER_REAL_NAME = "X-Real-Name";
    String HEADER_ROLES = "X-Roles";
    String HEADER_PERMISSIONS = "X-Permissions";

    /** 超级管理员角色 */
    String SUPER_ADMIN = "SUPER_ADMIN";

    /** 超级管理员默认用户ID */
    String DEFAULT_ADMIN_ID = "1";

    /** 删除标记 */
    Integer DELETED = 1;
    Integer NOT_DELETED = 0;

    /** 通用状态 */
    Integer STATUS_ENABLE = 1;
    Integer STATUS_DISABLE = 0;

    /** 密码加密（BCrypt）标识前缀 */
    String BCRYPT_PREFIX = "{bcrypt}";
}
