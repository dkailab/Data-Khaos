package com.datakhaos.mart.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.model.ResultCode;
import com.datakhaos.datasource.api.connector.DatasourceApiClient;
import com.datakhaos.datasource.api.model.QueryResult;
import com.datakhaos.mart.api.model.DimensionDto;
import com.datakhaos.mart.api.model.MetricDto;
import com.datakhaos.mart.api.model.ModelDto;
import com.datakhaos.mart.entity.MartDimLevel;
import com.datakhaos.mart.entity.MartDimension;
import com.datakhaos.mart.entity.MartMetric;
import com.datakhaos.mart.entity.MartModel;
import com.datakhaos.mart.entity.MartModelRel;
import com.datakhaos.mart.mapper.MartDimLevelMapper;
import com.datakhaos.mart.mapper.MartDimensionMapper;
import com.datakhaos.mart.mapper.MartMetricMapper;
import com.datakhaos.mart.mapper.MartModelMapper;
import com.datakhaos.mart.mapper.MartModelRelMapper;
import com.datakhaos.common.model.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据集市服务：模型建模、指标/维度管理、关联关系与数据预览。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MartService {

    private final MartModelMapper modelMapper;
    private final MartMetricMapper metricMapper;
    private final MartDimensionMapper dimensionMapper;
    private final MartDimLevelMapper dimLevelMapper;
    private final MartModelRelMapper modelRelMapper;
    private final DatasourceApiClient datasourceApiClient;

    // ==================== 模型 ====================

    public PageResult<MartModel> modelPage(long current, long size, String keyword, Integer status) {
        Page<MartModel> page = modelMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<MartModel>()
                        .like(StrUtil.isNotBlank(keyword), MartModel::getModelName, keyword)
                        .eq(status != null, MartModel::getStatus, status)
                        .orderByDesc(MartModel::getCreateTime));
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    public MartModel getModel(String id) {
        MartModel model = modelMapper.selectById(id);
        if (model == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "模型不存在: " + id);
        }
        return model;
    }

    @Transactional(rollbackFor = Exception.class)
    public void createModel(MartModel model) {
        validateModel(model);
        if (modelMapper.selectCount(new LambdaQueryWrapper<MartModel>()
                .eq(MartModel::getModelCode, model.getModelCode())) > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "模型编码已存在: " + model.getModelCode());
        }
        model.setStatus(model.getStatus() == null ? 0 : model.getStatus());
        model.setVersion(model.getVersion() == null ? 1 : model.getVersion());
        model.setModelType(model.getModelType() == null ? "STAR" : model.getModelType());
        modelMapper.insert(model);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateModel(MartModel model) {
        if (StrUtil.isBlank(model.getId())) {
            throw new BusinessException("模型ID不能为空");
        }
        getModel(model.getId());
        modelMapper.updateById(model);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(String id) {
        getModel(id);
        metricMapper.delete(new LambdaQueryWrapper<MartMetric>().eq(MartMetric::getModelId, id));
        dimensionMapper.delete(new LambdaQueryWrapper<MartDimension>().eq(MartDimension::getModelId, id));
        modelRelMapper.delete(new LambdaQueryWrapper<MartModelRel>().eq(MartModelRel::getModelId, id));
        modelMapper.deleteById(id);
    }

    /** 发布：草稿 -> 已发布，版本号 +1 */
    @Transactional(rollbackFor = Exception.class)
    public void publish(String id) {
        MartModel model = getModel(id);
        model.setStatus(1);
        model.setVersion((model.getVersion() == null ? 1 : model.getVersion()) + 1);
        modelMapper.updateById(model);
    }

    /** 下线 */
    @Transactional(rollbackFor = Exception.class)
    public void offline(String id) {
        MartModel model = getModel(id);
        model.setStatus(2);
        modelMapper.updateById(model);
    }

    // ==================== 指标 ====================

    public PageResult<MartMetric> metricPage(long current, long size, String modelId, String keyword) {
        Page<MartMetric> page = metricMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<MartMetric>()
                        .eq(StrUtil.isNotBlank(modelId), MartMetric::getModelId, modelId)
                        .like(StrUtil.isNotBlank(keyword), MartMetric::getMetricName, keyword)
                        .orderByDesc(MartMetric::getCreateTime));
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    public List<MetricDto> metricDtos(String modelId) {
        return metricMapper.selectList(new LambdaQueryWrapper<MartMetric>()
                        .eq(MartMetric::getModelId, modelId)
                        .eq(MartMetric::getStatus, 1))
                .stream().map(this::toMetricDto).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void createMetric(MartMetric metric) {
        if (StrUtil.isBlank(metric.getMetricName()) || StrUtil.isBlank(metric.getMetricCode())) {
            throw new BusinessException("指标名称与编码不能为空");
        }
        if (metricMapper.selectCount(new LambdaQueryWrapper<MartMetric>()
                .eq(MartMetric::getMetricCode, metric.getMetricCode())) > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "指标编码已存在: " + metric.getMetricCode());
        }
        metric.setMetricType(metric.getMetricType() == null ? "ATOMIC" : metric.getMetricType());
        metric.setDataType(metric.getDataType() == null ? "BIGINT" : metric.getDataType());
        metric.setStatus(metric.getStatus() == null ? 1 : metric.getStatus());
        metricMapper.insert(metric);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateMetric(MartMetric metric) {
        if (StrUtil.isBlank(metric.getId())) {
            throw new BusinessException("指标ID不能为空");
        }
        metricMapper.updateById(metric);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteMetric(String id) {
        metricMapper.deleteById(id);
    }

    // ==================== 维度 ====================

    public PageResult<MartDimension> dimensionPage(long current, long size, String modelId) {
        Page<MartDimension> page = dimensionMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<MartDimension>()
                        .eq(StrUtil.isNotBlank(modelId), MartDimension::getModelId, modelId)
                        .orderByAsc(MartDimension::getCreateTime));
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    public List<DimensionDto> dimensionDtos(String modelId) {
        return dimensionMapper.selectList(new LambdaQueryWrapper<MartDimension>()
                        .eq(MartDimension::getModelId, modelId)
                        .eq(MartDimension::getStatus, 1))
                .stream().map(this::toDimensionDto).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void createDimension(MartDimension dimension) {
        if (StrUtil.isBlank(dimension.getDimName()) || StrUtil.isBlank(dimension.getDimCode())) {
            throw new BusinessException("维度名称与编码不能为空");
        }
        if (dimensionMapper.selectCount(new LambdaQueryWrapper<MartDimension>()
                .eq(MartDimension::getDimCode, dimension.getDimCode())) > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "维度编码已存在: " + dimension.getDimCode());
        }
        dimension.setDimType(dimension.getDimType() == null ? "COMMON" : dimension.getDimType());
        dimension.setStatus(dimension.getStatus() == null ? 1 : dimension.getStatus());
        dimensionMapper.insert(dimension);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDimension(MartDimension dimension) {
        if (StrUtil.isBlank(dimension.getId())) {
            throw new BusinessException("维度ID不能为空");
        }
        dimensionMapper.updateById(dimension);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDimension(String id) {
        dimLevelMapper.delete(new LambdaQueryWrapper<MartDimLevel>().eq(MartDimLevel::getDimId, id));
        dimensionMapper.deleteById(id);
    }

    /** 维度层级 */
    public List<MartDimLevel> levels(String dimId) {
        return dimLevelMapper.selectList(new LambdaQueryWrapper<MartDimLevel>()
                .eq(MartDimLevel::getDimId, dimId)
                .orderByAsc(MartDimLevel::getLevelOrder));
    }

    /** 保存维度层级（全量替换） */
    @Transactional(rollbackFor = Exception.class)
    public void saveLevels(String dimId, List<MartDimLevel> levels) {
        dimLevelMapper.delete(new LambdaQueryWrapper<MartDimLevel>().eq(MartDimLevel::getDimId, dimId));
        if (levels != null) {
            int order = 1;
            for (MartDimLevel level : levels) {
                level.setId(null);
                level.setDimId(dimId);
                level.setLevelOrder(level.getLevelOrder() == null ? order++ : level.getLevelOrder());
                dimLevelMapper.insert(level);
            }
        }
    }

    // ==================== 关联关系 ====================

    public List<MartModelRel> rels(String modelId) {
        return modelRelMapper.selectList(new LambdaQueryWrapper<MartModelRel>()
                .eq(MartModelRel::getModelId, modelId)
                .orderByAsc(MartModelRel::getCreateTime));
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveRel(String modelId, MartModelRel rel) {
        getModel(modelId);
        rel.setModelId(modelId);
        rel.setJoinType(rel.getJoinType() == null ? "INNER" : rel.getJoinType());
        if (StrUtil.isBlank(rel.getId())) {
            modelRelMapper.insert(rel);
        } else {
            modelRelMapper.updateById(rel);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRel(String id) {
        modelRelMapper.deleteById(id);
    }

    // ==================== 模型详情 & 预览 ====================

    /** 模型详情（含指标/维度/关联） */
    public Map<String, Object> modelDetail(String id) {
        MartModel model = getModel(id);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("model", toModelDto(model));
        detail.put("metrics", metricDtos(id));
        detail.put("dimensions", dimensionDtos(id));
        detail.put("rels", rels(id));
        return detail;
    }

    /** 预览模型数据：SELECT 事实表前 100 行 */
    public QueryResult preview(String id) {
        MartModel model = getModel(id);
        if (StrUtil.isBlank(model.getDatasourceId())) {
            throw new BusinessException("模型未绑定数据源，无法预览");
        }
        String factTable = modelRelMapper.selectList(new LambdaQueryWrapper<MartModelRel>()
                        .eq(MartModelRel::getModelId, id))
                .stream().map(MartModelRel::getFactTable).findFirst().orElse(null);
        if (StrUtil.isBlank(factTable)) {
            throw new BusinessException("模型未配置事实表，无法预览");
        }
        R<QueryResult> r = datasourceApiClient.executeRaw(model.getDatasourceId(), "SELECT * FROM " + factTable + " LIMIT 100");
        if (r == null || r.getCode() != 0) {
            throw new BusinessException(r == null ? "预览失败" : r.getMsg());
        }
        return r.getData();
    }

    // ==================== 私有方法 ====================

    private void validateModel(MartModel model) {
        if (StrUtil.isBlank(model.getModelName()) || StrUtil.isBlank(model.getModelCode())) {
            throw new BusinessException("模型名称与编码不能为空");
        }
    }

    private ModelDto toModelDto(MartModel model) {
        ModelDto dto = new ModelDto();
        dto.setId(model.getId());
        dto.setModelName(model.getModelName());
        dto.setModelCode(model.getModelCode());
        dto.setModelType(model.getModelType());
        dto.setDatasourceId(model.getDatasourceId());
        dto.setDescription(model.getDescription());
        dto.setStatus(model.getStatus());
        dto.setVersion(model.getVersion());
        return dto;
    }

    private MetricDto toMetricDto(MartMetric metric) {
        MetricDto dto = new MetricDto();
        dto.setId(metric.getId());
        dto.setMetricName(metric.getMetricName());
        dto.setMetricCode(metric.getMetricCode());
        dto.setMetricType(metric.getMetricType());
        dto.setExpression(metric.getExpression());
        dto.setDataType(metric.getDataType());
        dto.setUnit(metric.getUnit());
        dto.setCategoryId(metric.getCategoryId());
        dto.setModelId(metric.getModelId());
        dto.setDescription(metric.getDescription());
        return dto;
    }

    private DimensionDto toDimensionDto(MartDimension dimension) {
        DimensionDto dto = new DimensionDto();
        dto.setId(dimension.getId());
        dto.setDimName(dimension.getDimName());
        dto.setDimCode(dimension.getDimCode());
        dto.setDimType(dimension.getDimType());
        dto.setModelId(dimension.getModelId());
        dto.setSourceTable(dimension.getSourceTable());
        dto.setSourceColumn(dimension.getSourceColumn());
        dto.setDescription(dimension.getDescription());
        return dto;
    }
}
