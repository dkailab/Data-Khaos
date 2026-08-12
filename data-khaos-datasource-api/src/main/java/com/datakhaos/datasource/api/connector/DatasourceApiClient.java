package com.datakhaos.datasource.api.connector;

import com.datakhaos.common.model.R;
import com.datakhaos.datasource.api.model.ColumnInfo;
import com.datakhaos.datasource.api.model.DsConfig;
import com.datakhaos.datasource.api.model.QueryResult;
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
 * 数据源服务 REST 客户端（通过注册中心负载均衡调用 data-khaos-datasource）。
 * 供 metadata / mart / query 等下游服务复用「执行查询 / 拉取元数据」能力。
 */
@Slf4j
public class DatasourceApiClient {

    public static final String SERVICE_URL = "http://data-khaos-datasource/api/ds";

    private final RestTemplate restTemplate;

    public DatasourceApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** 测试连接 */
    public boolean test(DsConfig config) {
        try {
            ResponseEntity<R<Boolean>> resp = restTemplate.exchange(
                    SERVICE_URL + "/test", HttpMethod.POST, new HttpEntity<>(config),
                    new ParameterizedTypeReference<R<Boolean>>() {
                    });
            R<Boolean> r = resp.getBody();
            return r != null && r.getCode() == 0 && Boolean.TRUE.equals(r.getData());
        } catch (Exception e) {
            log.warn("测试数据源连接失败: {}", e.getMessage());
            return false;
        }
    }

    /** 获取数据库列表 */
    public List<String> databases(String dsId) {
        try {
            ResponseEntity<R<List<String>>> resp = restTemplate.exchange(
                    SERVICE_URL + "/{id}/databases", HttpMethod.GET, null,
                    new ParameterizedTypeReference<R<List<String>>>() {
                    }, dsId);
            R<List<String>> r = resp.getBody();
            if (r != null && r.getCode() == 0) {
                return r.getData() == null ? new ArrayList<>() : r.getData();
            }
        } catch (Exception e) {
            log.warn("获取数据库列表失败: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /** 获取表列表 */
    public List<String> tables(String dsId, String database) {
        try {
            ResponseEntity<R<List<String>>> resp = restTemplate.exchange(
                    SERVICE_URL + "/{id}/tables/{db}", HttpMethod.GET, null,
                    new ParameterizedTypeReference<R<List<String>>>() {
                    }, dsId, database);
            R<List<String>> r = resp.getBody();
            if (r != null && r.getCode() == 0) {
                return r.getData() == null ? new ArrayList<>() : r.getData();
            }
        } catch (Exception e) {
            log.warn("获取表列表失败: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /** 获取字段列表 */
    public List<String> columns(String dsId, String database, String table) {
        try {
            ResponseEntity<R<List<String>>> resp = restTemplate.exchange(
                    SERVICE_URL + "/{id}/columns/{db}/{table}", HttpMethod.GET, null,
                    new ParameterizedTypeReference<R<List<String>>>() {
                    }, dsId, database, table);
            R<List<String>> r = resp.getBody();
            if (r != null && r.getCode() == 0) {
                return r.getData() == null ? new ArrayList<>() : r.getData();
            }
        } catch (Exception e) {
            log.warn("获取字段列表失败: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /** 获取字段详情（元数据同步用） */
    public List<ColumnInfo> columnInfos(String dsId, String database, String table) {
        try {
            ResponseEntity<R<List<ColumnInfo>>> resp = restTemplate.exchange(
                    SERVICE_URL + "/{id}/column-info/{db}/{table}", HttpMethod.GET, null,
                    new ParameterizedTypeReference<R<List<ColumnInfo>>>() {
                    }, dsId, database, table);
            R<List<ColumnInfo>> r = resp.getBody();
            if (r != null && r.getCode() == 0) {
                return r.getData() == null ? new ArrayList<>() : r.getData();
            }
        } catch (Exception e) {
            log.warn("获取字段详情失败: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * 在指定数据源上执行 SQL，返回查询结果
     */
    public QueryResult execute(String dsId, String sql) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("sql", sql);
            ResponseEntity<R<QueryResult>> resp = restTemplate.exchange(
                    SERVICE_URL + "/{id}/execute", HttpMethod.POST, new HttpEntity<>(body),
                    new ParameterizedTypeReference<R<QueryResult>>() {
                    }, dsId);
            R<QueryResult> r = resp.getBody();
            if (r != null && r.getCode() == 0) {
                return r.getData();
            }
        } catch (Exception e) {
            log.warn("调用数据源服务执行 SQL 失败: {}", e.getMessage());
        }
        return new QueryResult();
    }

    /**
     * 在指定数据源上执行 SQL，返回原始 {@link R} 包装（调用方可感知审核 / 执行错误）。
     */
    public R<QueryResult> executeRaw(String dsId, String sql) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("sql", sql);
            ResponseEntity<R<QueryResult>> resp = restTemplate.exchange(
                    SERVICE_URL + "/{id}/execute", HttpMethod.POST, new HttpEntity<>(body),
                    new ParameterizedTypeReference<R<QueryResult>>() {
                    }, dsId);
            return resp.getBody();
        } catch (Exception e) {
            log.warn("调用数据源服务执行 SQL 失败: {}", e.getMessage());
            return R.fail("调用数据源服务执行 SQL 失败: " + e.getMessage());
        }
    }
}
