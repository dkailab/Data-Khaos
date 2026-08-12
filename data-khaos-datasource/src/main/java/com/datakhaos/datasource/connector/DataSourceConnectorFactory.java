package com.datakhaos.datasource.connector;

import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.datasource.api.connector.DataSourceConnector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据源连接器工厂：按 {@link DataSourceConnector#getType()} 注册所有连接器 Bean，
 * 提供「按数据源类型取连接器」能力。新增数据源只需实现 SPI 接口并注册为 Spring Bean。
 */
@Slf4j
@Component
public class DataSourceConnectorFactory {

    /** 类型(大写) → 连接器 */
    private final Map<String, DataSourceConnector> connectors;

    public DataSourceConnectorFactory(List<DataSourceConnector> connectorList) {
        Map<String, DataSourceConnector> map = new HashMap<>();
        if (connectorList != null) {
            for (DataSourceConnector connector : connectorList) {
                map.put(connector.getType().toUpperCase(), connector);
            }
        }
        this.connectors = Collections.unmodifiableMap(map);
        log.info("已注册数据源连接器: {}", map.keySet());
    }

    /**
     * 获取指定类型的数据源连接器
     *
     * @param type 数据源类型（如 MYSQL / DM8 / HIVE），不区分大小写
     */
    public DataSourceConnector getConnector(String type) {
        DataSourceConnector connector = connectors.get(type == null ? null : type.toUpperCase());
        if (connector == null) {
            throw new BusinessException("不支持的数据源类型: " + type + "，可用类型: " + connectors.keySet());
        }
        return connector;
    }
}
