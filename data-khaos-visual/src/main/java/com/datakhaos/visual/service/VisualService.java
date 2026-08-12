package com.datakhaos.visual.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.model.PageResult;
import com.datakhaos.common.model.R;
import com.datakhaos.common.model.ResultCode;
import com.datakhaos.common.security.MetadataHolder;
import com.datakhaos.datasource.api.connector.DatasourceApiClient;
import com.datakhaos.datasource.api.model.QueryResult;
import com.datakhaos.visual.entity.VisualDashboard;
import com.datakhaos.visual.entity.VisualDashboardItem;
import com.datakhaos.visual.mapper.VisualDashboardItemMapper;
import com.datakhaos.visual.mapper.VisualDashboardMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 可视化服务：仪表板/组件管理与数据查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisualService {

    private final VisualDashboardMapper dashboardMapper;
    private final VisualDashboardItemMapper itemMapper;
    private final DatasourceApiClient datasourceApiClient;

    // ==================== 仪表板 ====================

    public PageResult<VisualDashboard> dashboardPage(long current, long size, String keyword) {
        Page<VisualDashboard> page = dashboardMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<VisualDashboard>()
                        .like(StrUtil.isNotBlank(keyword), VisualDashboard::getName, keyword)
                        .orderByDesc(VisualDashboard::getCreateTime));
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    public VisualDashboard getDashboard(String id) {
        VisualDashboard dashboard = dashboardMapper.selectById(id);
        if (dashboard == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "仪表板不存在: " + id);
        }
        return dashboard;
    }

    @Transactional(rollbackFor = Exception.class)
    public void createDashboard(VisualDashboard dashboard) {
        if (StrUtil.isBlank(dashboard.getName())) {
            throw new BusinessException("仪表板名称不能为空");
        }
        dashboard.setStatus(dashboard.getStatus() == null ? 1 : dashboard.getStatus());
        dashboard.setRefreshInterval(dashboard.getRefreshInterval() == null ? 60 : dashboard.getRefreshInterval());
        if (StrUtil.isBlank(dashboard.getCreateBy())) {
            dashboard.setCreateBy(MetadataHolder.getUserId());
        }
        dashboardMapper.insert(dashboard);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDashboard(VisualDashboard dashboard) {
        if (StrUtil.isBlank(dashboard.getId())) {
            throw new BusinessException("仪表板ID不能为空");
        }
        getDashboard(dashboard.getId());
        dashboardMapper.updateById(dashboard);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDashboard(String id) {
        getDashboard(id);
        itemMapper.delete(new LambdaQueryWrapper<VisualDashboardItem>()
                .eq(VisualDashboardItem::getDashboardId, id));
        dashboardMapper.deleteById(id);
    }

    // ==================== 组件 ====================

    public List<VisualDashboardItem> items(String dashboardId) {
        getDashboard(dashboardId);
        return itemMapper.selectList(new LambdaQueryWrapper<VisualDashboardItem>()
                .eq(VisualDashboardItem::getDashboardId, dashboardId)
                .orderByAsc(VisualDashboardItem::getPosY)
                .orderByAsc(VisualDashboardItem::getPosX));
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveItem(VisualDashboardItem item) {
        if (StrUtil.isBlank(item.getDashboardId())) {
            throw new BusinessException("仪表板ID不能为空");
        }
        getDashboard(item.getDashboardId());
        if (StrUtil.isBlank(item.getChartType())) {
            item.setChartType("TABLE");
        }
        if (StrUtil.isBlank(item.getId())) {
            itemMapper.insert(item);
        } else {
            itemMapper.updateById(item);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(String id) {
        itemMapper.deleteById(id);
    }

    // ==================== 数据执行 ====================

    /** 执行组件查询 */
    public QueryResult executeItem(String itemId) {
        VisualDashboardItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "组件不存在: " + itemId);
        }
        if (StrUtil.isBlank(item.getDatasourceId()) || StrUtil.isBlank(item.getQuerySql())) {
            throw new BusinessException("组件未配置数据源或查询SQL");
        }
        return executeOnDataSource(item.getDatasourceId(), item.getQuerySql());
    }

    /** 即席分析查询（分析板） */
    public QueryResult executeAdhoc(String datasourceId, String sql) {
        if (StrUtil.isBlank(datasourceId)) {
            throw new BusinessException("数据源ID不能为空");
        }
        return executeOnDataSource(datasourceId, sql);
    }

    private QueryResult executeOnDataSource(String datasourceId, String sql) {
        R<QueryResult> result = datasourceApiClient.executeRaw(datasourceId, sql);
        if (result == null || result.getCode() != 0) {
            throw new BusinessException(result == null ? "查询失败" : result.getMsg());
        }
        return result.getData();
    }
}
