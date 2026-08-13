package com.datakhaos.permission.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.model.PageResult;
import com.datakhaos.permission.api.model.ProjectGroupDto;
import com.datakhaos.permission.entity.SgProjectGroup;
import com.datakhaos.permission.entity.SgProjectGroupMember;
import com.datakhaos.permission.entity.SgProjectGroupResource;
import com.datakhaos.permission.entity.SgProjectRole;
import com.datakhaos.permission.mapper.SgProjectGroupMapper;
import com.datakhaos.permission.mapper.SgProjectGroupMemberMapper;
import com.datakhaos.permission.mapper.SgProjectGroupResourceMapper;
import com.datakhaos.permission.mapper.SgProjectRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 项目组权限服务：组织(业务线) → 项目组 → 人 三级模型。
 * 人加入项目组即获得组内角色能力位（操作权限）与组下资源（数据权限）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectGroupService {

    private final SgProjectGroupMapper groupMapper;
    private final SgProjectGroupMemberMapper memberMapper;
    private final SgProjectRoleMapper roleMapper;
    private final SgProjectGroupResourceMapper resourceMapper;

    // ---------- 项目组 CRUD ----------

    public PageResult<SgProjectGroup> page(long current, long size, String orgId, String keyword) {
        LambdaQueryWrapper<SgProjectGroup> wrapper = new LambdaQueryWrapper<SgProjectGroup>()
                .eq(StrUtil.isNotBlank(orgId), SgProjectGroup::getOrgId, orgId)
                .and(StrUtil.isNotBlank(keyword), w -> w
                        .like(SgProjectGroup::getProjectName, keyword)
                        .or().like(SgProjectGroup::getProjectCode, keyword))
                .orderByAsc(SgProjectGroup::getSortOrder);
        var result = groupMapper.selectPage(
                com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(current, size), wrapper);
        return PageResult.of(current, size, result.getTotal(), result.getRecords());
    }

    public List<SgProjectGroup> list(String orgId) {
        return groupMapper.selectList(new LambdaQueryWrapper<SgProjectGroup>()
                .eq(StrUtil.isNotBlank(orgId), SgProjectGroup::getOrgId, orgId)
                .orderByAsc(SgProjectGroup::getSortOrder));
    }

    public void save(SgProjectGroup group) {
        if (group.getStatus() == null) {
            group.setStatus(1);
        }
        groupMapper.insert(group);
    }

    public void update(SgProjectGroup group) {
        groupMapper.updateById(group);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        Long members = memberMapper.selectCount(new LambdaQueryWrapper<SgProjectGroupMember>()
                .eq(SgProjectGroupMember::getProjectGroupId, id));
        if (members != null && members > 0) {
            throw new BusinessException("项目组下仍有成员，无法删除");
        }
        groupMapper.deleteById(id);
        roleMapper.delete(new LambdaQueryWrapper<SgProjectRole>().eq(SgProjectRole::getProjectGroupId, id));
        resourceMapper.delete(new LambdaQueryWrapper<SgProjectGroupResource>()
                .eq(SgProjectGroupResource::getProjectGroupId, id));
    }

    // ---------- 成员管理 ----------

    public List<Map<String, Object>> listMembers(String projectGroupId) {
        List<SgProjectGroupMember> members = memberMapper.selectList(new LambdaQueryWrapper<SgProjectGroupMember>()
                .eq(SgProjectGroupMember::getProjectGroupId, projectGroupId)
                .orderByDesc(SgProjectGroupMember::getIsPrimary));
        if (members.isEmpty()) {
            return List.of();
        }
        List<String> roleIds = members.stream().map(SgProjectGroupMember::getProjectRoleId)
                .filter(StrUtil::isNotBlank).distinct().toList();
        Map<String, SgProjectRole> roleMap = roleIds.isEmpty() ? Map.of()
                : roleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(SgProjectRole::getId, r -> r));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SgProjectGroupMember m : members) {
            SgProjectRole role = roleMap.get(m.getProjectRoleId());
            result.add(Map.of(
                    "memberId", m.getId(),
                    "userId", m.getUserId(),
                    "projectRoleId", m.getProjectRoleId() == null ? "" : m.getProjectRoleId(),
                    "roleCode", role == null ? "" : role.getRoleCode(),
                    "roleName", role == null ? "" : role.getRoleName(),
                    "capabilityFlags", role == null ? List.of() : parseFlags(role.getCapabilityFlags()),
                    "primary", m.getIsPrimary() != null && m.getIsPrimary() == 1));
        }
        return result;
    }

    /**
     * 设置项目组成员（全量替换）。默认把首个成员列为主项目组。
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignMembers(String projectGroupId, List<MemberAssign> assigns) {
        memberMapper.delete(new LambdaQueryWrapper<SgProjectGroupMember>()
                .eq(SgProjectGroupMember::getProjectGroupId, projectGroupId));
        if (assigns == null || assigns.isEmpty()) {
            return;
        }
        boolean hasPrimary = assigns.stream().anyMatch(a -> Boolean.TRUE.equals(a.primary()));
        for (int i = 0; i < assigns.size(); i++) {
            MemberAssign a = assigns.get(i);
            SgProjectGroupMember m = new SgProjectGroupMember();
            m.setProjectGroupId(projectGroupId);
            m.setUserId(a.userId());
            m.setProjectRoleId(a.projectRoleId());
            boolean primary = !hasPrimary && i == 0;
            m.setIsPrimary(primary || Boolean.TRUE.equals(a.primary()) ? 1 : 0);
            memberMapper.insert(m);
        }
    }

    // ---------- 角色 CRUD（能力位） ----------

    public PageResult<SgProjectRole> rolePage(long current, long size, String projectGroupId) {
        LambdaQueryWrapper<SgProjectRole> wrapper = new LambdaQueryWrapper<SgProjectRole>()
                .eq(StrUtil.isNotBlank(projectGroupId), SgProjectRole::getProjectGroupId, projectGroupId)
                .orderByAsc(SgProjectRole::getSortOrder);
        var result = roleMapper.selectPage(
                com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(current, size), wrapper);
        return PageResult.of(current, size, result.getTotal(), result.getRecords());
    }

    public void saveRole(SgProjectRole role) {
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        roleMapper.insert(role);
    }

    public void updateRole(SgProjectRole role) {
        roleMapper.updateById(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(String id) {
        Long used = memberMapper.selectCount(new LambdaQueryWrapper<SgProjectGroupMember>()
                .eq(SgProjectGroupMember::getProjectRoleId, id));
        if (used != null && used > 0) {
            throw new BusinessException("角色正被成员使用，无法删除");
        }
        roleMapper.deleteById(id);
    }

    // ---------- 资源绑定 ----------

    public List<SgProjectGroupResource> listResources(String projectGroupId) {
        return resourceMapper.selectList(new LambdaQueryWrapper<SgProjectGroupResource>()
                .eq(StrUtil.isNotBlank(projectGroupId), SgProjectGroupResource::getProjectGroupId, projectGroupId)
                .orderByAsc(SgProjectGroupResource::getCreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindResources(String projectGroupId, List<SgProjectGroupResource> resources) {
        if (resources == null) {
            return;
        }
        for (SgProjectGroupResource r : resources) {
            r.setProjectGroupId(projectGroupId);
            resourceMapper.insert(r);
        }
    }

    public void deleteResource(String id) {
        resourceMapper.deleteById(id);
    }

    // ---------- 权限下发：用户项目组视图 + 能力位合并 ----------

    /**
     * 查询用户加入的项目组（含组内角色与能力位），并标记主项目组。
     */
    public List<ProjectGroupDto> getUserProjectGroups(String userId) {
        List<SgProjectGroupMember> members = memberMapper.selectList(new LambdaQueryWrapper<SgProjectGroupMember>()
                .eq(SgProjectGroupMember::getUserId, userId));
        if (members.isEmpty()) {
            return List.of();
        }
        List<String> groupIds = members.stream().map(SgProjectGroupMember::getProjectGroupId).distinct().toList();
        Map<String, SgProjectGroup> groupMap = groupMapper.selectBatchIds(groupIds).stream()
                .filter(g -> g.getStatus() == null || g.getStatus() == 1)
                .collect(Collectors.toMap(SgProjectGroup::getId, g -> g));
        List<String> roleIds = members.stream().map(SgProjectGroupMember::getProjectRoleId)
                .filter(StrUtil::isNotBlank).distinct().toList();
        Map<String, SgProjectRole> roleMap = roleIds.isEmpty() ? Map.of()
                : roleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(SgProjectRole::getId, r -> r));

        List<ProjectGroupDto> result = new ArrayList<>();
        for (SgProjectGroupMember m : members) {
            SgProjectGroup group = groupMap.get(m.getProjectGroupId());
            if (group == null) {
                continue;
            }
            SgProjectRole role = roleMap.get(m.getProjectRoleId());
            ProjectGroupDto dto = new ProjectGroupDto();
            dto.setId(group.getId());
            dto.setOrgId(group.getOrgId());
            dto.setProjectName(group.getProjectName());
            dto.setProjectCode(group.getProjectCode());
            dto.setLeaderId(group.getLeaderId());
            dto.setPrimary(m.getIsPrimary() != null && m.getIsPrimary() == 1);
            if (role != null) {
                dto.setRoleCode(role.getRoleCode());
                dto.setCapabilityFlags(parseFlags(role.getCapabilityFlags()));
            }
            result.add(dto);
        }
        return result;
    }

    /**
     * 取用户当前项目组（主项目组；无主组时取第一个）。
     */
    public ProjectGroupDto getCurrentProjectGroup(String userId) {
        List<ProjectGroupDto> groups = getUserProjectGroups(userId);
        return groups.stream().filter(ProjectGroupDto::getPrimary).findFirst()
                .orElse(groups.isEmpty() ? null : groups.get(0));
    }

    /**
     * 合并用户在指定项目组内的能力位（当前组角色的能力位集合）。
     */
    public List<String> getCapabilityFlags(String userId, String projectGroupId) {
        if (StrUtil.isBlank(projectGroupId)) {
            ProjectGroupDto current = getCurrentProjectGroup(userId);
            if (current == null) {
                return List.of();
            }
            projectGroupId = current.getId();
        }
        SgProjectGroupMember m = memberMapper.selectOne(new LambdaQueryWrapper<SgProjectGroupMember>()
                .eq(SgProjectGroupMember::getUserId, userId)
                .eq(SgProjectGroupMember::getProjectGroupId, projectGroupId));
        if (m == null || StrUtil.isBlank(m.getProjectRoleId())) {
            return List.of();
        }
        SgProjectRole role = roleMapper.selectById(m.getProjectRoleId());
        return role == null ? List.of() : parseFlags(role.getCapabilityFlags());
    }

    /** 解析能力位 JSON 数组字符串为去重、保序列表 */
    private List<String> parseFlags(String json) {
        Set<String> set = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(json)) {
            try {
                for (Object o : JSONUtil.parseArray(json)) {
                    set.add(String.valueOf(o));
                }
            } catch (Exception e) {
                log.warn("解析能力位失败: {}", json);
            }
        }
        return new ArrayList<>(set);
    }

    /** 成员分配入参 */
    public record MemberAssign(String userId, String projectRoleId, Boolean primary) {
    }
}