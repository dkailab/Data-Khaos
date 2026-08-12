package com.datakhaos.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datakhaos.permission.entity.SysOrgPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysOrgPermissionMapper extends BaseMapper<SysOrgPermission> {

    /**
     * 查询组织下的成员（关联 sys_user 取用户信息）
     */
    @Select("SELECT u.id AS userId, u.username AS username, u.real_name AS realName, u.email AS email, " +
            "u.status AS status, uo.is_primary AS isPrimary " +
            "FROM sys_user_org uo JOIN sys_user u ON uo.user_id = u.id " +
            "WHERE uo.org_id = #{orgId} ORDER BY uo.is_primary DESC, u.username")
    List<Map<String, Object>> selectOrgUsers(@Param("orgId") String orgId);
}