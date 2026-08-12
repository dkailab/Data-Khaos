package com.datakhaos.permission.api.service;

import com.datakhaos.common.model.R;
import com.datakhaos.permission.api.model.UserPermissionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限服务 REST 客户端（通过注册中心负载均衡调用 data-khaos-permission）。
 * 依赖一个 @LoadBalanced RestTemplate（bean 名 lbRestTemplate）。
 */
@Slf4j
public class PermissionApiClient {

    private final RestTemplate restTemplate;

    public PermissionApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 获取用户权限视图（角色 / 权限标识 / 菜单）
     */
    public UserPermissionDto getUserPermission(String userId) {
        try {
            ResponseEntity<R<UserPermissionDto>> resp = restTemplate.exchange(
                    "http://data-khaos-permission/api/permission/user/{userId}",
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<R<UserPermissionDto>>() {
                    }, userId);
            R<UserPermissionDto> body = resp.getBody();
            if (body != null && body.getCode() == 0) {
                return body.getData() == null ? new UserPermissionDto() : body.getData();
            }
        } catch (Exception e) {
            log.warn("调用权限服务查询用户权限失败: {}", e.getMessage());
        }
        return new UserPermissionDto();
    }

    /**
     * 获取用户在指定数据源/库表上拥有的表级权限集合
     */
    public List<Map<String, Object>> getUserTablePermissions(String userId) {
        try {
            ResponseEntity<R<List<Map<String, Object>>>> resp = restTemplate.exchange(
                    "http://data-khaos-permission/api/permission/table/user/{userId}",
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<R<List<Map<String, Object>>>>() {
                    }, userId);
            R<List<Map<String, Object>>> body = resp.getBody();
            if (body != null && body.getCode() == 0) {
                return body.getData() == null ? new ArrayList<>() : body.getData();
            }
        } catch (Exception e) {
            log.warn("调用权限服务查询用户表权限失败: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * 校验用户对某库表是否拥有指定操作权限
     */
    public boolean checkTablePermission(String userId, String datasourceId, String database, String table, String type) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("userId", userId);
            body.put("datasourceId", datasourceId);
            body.put("databaseName", database);
            body.put("tableName", table);
            body.put("permissionType", type);
            ResponseEntity<R<Boolean>> resp = restTemplate.exchange(
                    "http://data-khaos-permission/api/permission/table/check",
                    HttpMethod.POST, new HttpEntity<>(body),
                    new ParameterizedTypeReference<R<Boolean>>() {
                    });
            R<Boolean> r = resp.getBody();
            if (r != null && r.getCode() == 0) {
                return Boolean.TRUE.equals(r.getData());
            }
        } catch (Exception e) {
            log.warn("调用权限服务校验表权限失败: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 授予用户/角色表级权限（审批通过后自动授权）。
     *
     * @param permission 字段：datasourceId / databaseName / tableName / permissionType / userId / roleId / grantType
     */
    public boolean grantTablePermission(Map<String, Object> permission) {
        try {
            ResponseEntity<R<Void>> resp = restTemplate.exchange(
                    "http://data-khaos-permission/api/permission/table",
                    HttpMethod.POST, new HttpEntity<>(permission),
                    new ParameterizedTypeReference<R<Void>>() {
                    });
            R<Void> body = resp.getBody();
            return body != null && body.getCode() == 0;
        } catch (Exception e) {
            log.warn("调用权限服务自动授权失败: {}", e.getMessage());
            return false;
        }
    }
}
