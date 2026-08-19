package com.datakhaos.visual.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datakhaos.visual.dto.AdhocQueryRequest;
import com.datakhaos.visual.dto.DatasetDto;
import com.datakhaos.visual.entity.VisualDataset;
import com.datakhaos.visual.mapper.VisualDatasetMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 数据集服务
 */
@Service
@RequiredArgsConstructor
public class DatasetService {

    private final VisualDatasetMapper datasetMapper;
    private final VisualService visualService;
    private final ObjectMapper objectMapper;

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";

    /**
     * 分页查询数据集列表
     */
    public Page<VisualDataset> page(Page<VisualDataset> page, String keyword, String datasetType, String status, String orgId) {
        LambdaQueryWrapper<VisualDataset> wrapper = new LambdaQueryWrapper<VisualDataset>()
                .like(StringUtils.hasText(keyword), VisualDataset::getName, keyword)
                .eq(StringUtils.hasText(datasetType), VisualDataset::getDatasetType, datasetType)
                .eq(StringUtils.hasText(status), VisualDataset::getStatus, status)
                .eq(StringUtils.hasText(orgId), VisualDataset::getOrgId, orgId)
                .orderByDesc(VisualDataset::getUpdateTime);
        return datasetMapper.selectPage(page, wrapper);
    }

    public VisualDataset getById(String id) {
        return datasetMapper.selectById(id);
    }

    /**
     * 创建数据集(草稿)
     */
    @Transactional
    public String create(DatasetDto dto) {
        VisualDataset entity = new VisualDataset();
        copyToEntity(dto, entity);
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setStatus(STATUS_DRAFT);
        entity.setVersion(1);
        entity.setDeleted(0);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        datasetMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 更新数据集
     */
    @Transactional
    public void update(String id, DatasetDto dto) {
        VisualDataset entity = datasetMapper.selectById(id);
        if (entity == null) throw new RuntimeException("数据集不存在: " + id);
        copyToEntity(dto, entity);
        entity.setUpdateTime(LocalDateTime.now());
        datasetMapper.updateById(entity);
    }

    public void delete(String id) {
        datasetMapper.deleteById(id);
    }

    /**
     * 发布数据集
     */
    @Transactional
    public Integer publish(String id, String remark) {
        VisualDataset entity = datasetMapper.selectById(id);
        if (entity == null) throw new RuntimeException("数据集不存在: " + id);
        entity.setStatus(STATUS_PUBLISHED);
        entity.setVersion(entity.getVersion() == null ? 1 : entity.getVersion() + 1);
        entity.setUpdateTime(LocalDateTime.now());
        datasetMapper.updateById(entity);
        return entity.getVersion();
    }

    /**
     * 下线数据集
     */
    @Transactional
    public void unpublish(String id) {
        VisualDataset entity = datasetMapper.selectById(id);
        if (entity == null) throw new RuntimeException("数据集不存在: " + id);
        entity.setStatus("OFFLINE");
        entity.setUpdateTime(LocalDateTime.now());
        datasetMapper.updateById(entity);
    }

    /**
     * 测试SQL查询并返回字段信息
     */
    public DatasetDto.DatasetPreviewResult preview(String datasourceId, String querySql) {
        DatasetDto.DatasetPreviewResult result = new DatasetDto.DatasetPreviewResult();

        try {
            AdhocQueryRequest request = new AdhocQueryRequest();
            request.setDatasourceId(datasourceId);
            request.setSql(querySql);
            var adhocResult = visualService.executeAdhoc(request);

            if (adhocResult != null && adhocResult.getResult() != null) {
                var queryResult = adhocResult.getResult();
                if (queryResult.getColumns() != null) {
                    List<String> columns = new ArrayList<>();
                    queryResult.getColumns().forEach(c -> columns.add(c.getColumnName()));
                    result.setColumns(columns);
                }
                result.setRows(queryResult.getRows() != null ? queryResult.getRows() : new ArrayList<>());
            }
        } catch (Exception e) {
            result.setColumns(new ArrayList<>());
            result.setRows(new ArrayList<>());
        }

        return result;
    }

    /**
     * 根据模型自动提取字段定义
     */
    public List<DatasetDto.DatasetFieldDto> extractFieldsFromModel(String modelId) {
        // TODO: 根据模型关联的指标和维度自动生成字段列表
        return new ArrayList<>();
    }

    private void copyToEntity(DatasetDto dto, VisualDataset entity) {
        entity.setName(dto.getName());
        entity.setCode(dto.getCode());
        entity.setDescription(dto.getDescription());
        entity.setDatasetType(dto.getDatasetType());
        entity.setDatasourceId(dto.getDatasourceId());
        entity.setQuerySql(dto.getQuerySql());
        entity.setModelId(dto.getModelId());
        entity.setRefreshInterval(dto.getRefreshInterval());
        entity.setVisibility(dto.getVisibility());

        try {
            if (dto.getFields() != null) {
                entity.setFieldsJson(objectMapper.writeValueAsString(dto.getFields()));
            }
            if (dto.getVariables() != null) {
                entity.setVariablesJson(objectMapper.writeValueAsString(dto.getVariables()));
            }
        } catch (Exception e) {
            throw new RuntimeException("序列化字段定义失败", e);
        }
    }

    /**
     * 解析字段JSON
     */
    public List<DatasetDto.DatasetFieldDto> parseFields(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<DatasetDto.DatasetFieldDto>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
