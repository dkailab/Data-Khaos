package com.datakhaos.pipeline.engine;

import com.datakhaos.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 引擎工厂：按引擎标识路由到具体实现。新增引擎在此注册即插拔。
 */
@Component
public class EngineFactory {

    private final Map<String, PipelineEngine> engines = new LinkedHashMap<>();

    public EngineFactory(List<PipelineEngine> engineList) {
        for (PipelineEngine engine : engineList) {
            engines.put(engine.type().toUpperCase(), engine);
        }
    }

    public PipelineEngine get(String type) {
        PipelineEngine engine = engines.get(type == null ? "" : type.toUpperCase());
        if (engine == null) {
            throw new BusinessException("不支持的管道引擎: " + type);
        }
        return engine;
    }

    /** 全部注册的引擎（含可用性），供前端「引擎列表」展示 */
    public List<Map<String, Object>> list() {
        return engines.values().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", e.type());
            m.put("name", e.name());
            m.put("available", e.available());
            return m;
        }).toList();
    }
}