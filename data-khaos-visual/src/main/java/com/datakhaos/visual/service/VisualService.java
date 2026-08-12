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
import com.datakhaos.visual.entity.VisualBoard;
import com.datakhaos.visual.entity.VisualDashboard;
import com.datakhaos.visual.entity.VisualDashboardItem;
import com.datakhaos.visual.entity.VisualDashboardVersion;
import com.datakhaos.visual.mapper.VisualBoardMapper;
import com.datakhaos.visual.mapper.VisualDashboardItemMapper;
import com.datakhaos.visual.mapper.VisualDashboardMapper;
import com.datakhaos.visual.mapper.VisualDashboardVersionMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * 可视化服务：仪表板/组件管理与数据查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisualService {

    private final VisualDashboardMapper dashboardMapper;
    private final VisualDashboardItemMapper itemMapper;
    private final VisualDashboardVersionMapper versionMapper;
    private final VisualBoardMapper boardMapper;
    private final DatasourceApiClient datasourceApiClient;
    private final ObjectMapper objectMapper;

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
        boardMapper.delete(new LambdaQueryWrapper<VisualBoard>()
                .eq(VisualBoard::getDashboardId, id));
        versionMapper.delete(new LambdaQueryWrapper<VisualDashboardVersion>()
                .eq(VisualDashboardVersion::getDashboardId, id));
        dashboardMapper.deleteById(id);
    }

    // ==================== 版本控制 ====================

    /** 上线：生成当前草稿的快照版本，并置为已上线 */
    @Transactional(rollbackFor = Exception.class)
    public Integer publish(String dashboardId, String remark) {
        VisualDashboard dashboard = getDashboard(dashboardId);
        List<VisualDashboardItem> items = itemMapper.selectList(new LambdaQueryWrapper<VisualDashboardItem>()
                .eq(VisualDashboardItem::getDashboardId, dashboardId));
        List<VisualBoard> boards = boardMapper.selectList(new LambdaQueryWrapper<VisualBoard>()
                .eq(VisualBoard::getDashboardId, dashboardId)
                .orderByAsc(VisualBoard::getSortOrder));
        int newVersion = (dashboard.getVersion() == null ? 0 : dashboard.getVersion()) + 1;

        VisualDashboardVersion version = new VisualDashboardVersion();
        version.setDashboardId(dashboardId);
        version.setVersion(newVersion);
        version.setName(dashboard.getName());
        version.setDescription(dashboard.getDescription());
        version.setLayout(dashboard.getLayout());
        version.setRefreshInterval(dashboard.getRefreshInterval());
        version.setItemsJson(writeJson(items));
        version.setBoardsJson(writeJson(boards));
        version.setRemark(remark);
        version.setCreateBy(MetadataHolder.getUserId());
        versionMapper.insert(version);

        VisualDashboard update = new VisualDashboard();
        update.setId(dashboardId);
        update.setVersion(newVersion);
        update.setStatus(2);
        dashboardMapper.updateById(update);
        return newVersion;
    }

    /** 下线：回到草稿状态 */
    @Transactional(rollbackFor = Exception.class)
    public void unpublish(String dashboardId) {
        getDashboard(dashboardId);
        VisualDashboard update = new VisualDashboard();
        update.setId(dashboardId);
        update.setStatus(1);
        dashboardMapper.updateById(update);
    }

    /** 版本列表 */
    public List<VisualDashboardVersion> versionList(String dashboardId) {
        getDashboard(dashboardId);
        return versionMapper.selectList(new LambdaQueryWrapper<VisualDashboardVersion>()
                .eq(VisualDashboardVersion::getDashboardId, dashboardId)
                .orderByDesc(VisualDashboardVersion::getVersion));
    }

    /** 版本快照详情 */
    public VisualDashboardVersion versionDetail(String versionId) {
        VisualDashboardVersion version = versionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "版本不存在: " + versionId);
        }
        return version;
    }

    /** 回滚到指定版本：用快照覆盖仪表板信息与组件 */
    @Transactional(rollbackFor = Exception.class)
    public void rollback(String dashboardId, String versionId) {
        VisualDashboard dashboard = getDashboard(dashboardId);
        VisualDashboardVersion version = versionDetail(versionId);
        if (!StrUtil.equals(version.getDashboardId(), dashboardId)) {
            throw new BusinessException("版本不属于该仪表板");
        }

        VisualDashboard update = new VisualDashboard();
        update.setId(dashboardId);
        update.setName(version.getName());
        update.setDescription(version.getDescription());
        update.setLayout(version.getLayout());
        update.setRefreshInterval(version.getRefreshInterval());
        update.setStatus(1);
        dashboardMapper.updateById(update);

        List<VisualDashboardItem> items = readItems(version.getItemsJson());
        itemMapper.delete(new LambdaQueryWrapper<VisualDashboardItem>()
                .eq(VisualDashboardItem::getDashboardId, dashboardId));
        for (VisualDashboardItem item : items) {
            item.setId(null);
            item.setCreateTime(null);
            itemMapper.insert(item);
        }

        List<VisualBoard> boards = readBoards(version.getBoardsJson());
        boardMapper.delete(new LambdaQueryWrapper<VisualBoard>()
                .eq(VisualBoard::getDashboardId, dashboardId));
        for (VisualBoard board : boards) {
            board.setId(null);
            board.setCreateTime(null);
            boardMapper.insert(board);
        }
    }

    private List<VisualBoard> readBoards(String boardsJson) {
        if (StrUtil.isBlank(boardsJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(boardsJson, new TypeReference<List<VisualBoard>>() {
            });
        } catch (Exception e) {
            throw new BusinessException("解析分析板快照失败: " + e.getMessage());
        }
    }

    private String writeJson(List<?> items) {
        try {
            return objectMapper.writeValueAsString(items == null ? Collections.emptyList() : items);
        } catch (Exception e) {
            throw new BusinessException("生成版本快照失败: " + e.getMessage());
        }
    }

    private List<VisualDashboardItem> readItems(String itemsJson) {
        if (StrUtil.isBlank(itemsJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(itemsJson, new TypeReference<List<VisualDashboardItem>>() {
            });
        } catch (Exception e) {
            throw new BusinessException("解析版本快照失败: " + e.getMessage());
        }
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

    // ==================== 分析板 ====================

    /** 分析板列表 */
    public List<VisualBoard> boards(String dashboardId) {
        getDashboard(dashboardId);
        return boardMapper.selectList(new LambdaQueryWrapper<VisualBoard>()
                .eq(VisualBoard::getDashboardId, dashboardId)
                .orderByAsc(VisualBoard::getSortOrder));
    }

    @Transactional(rollbackFor = Exception.class)
    public void createBoard(VisualBoard board) {
        if (StrUtil.isBlank(board.getDashboardId())) {
            throw new BusinessException("仪表板ID不能为空");
        }
        getDashboard(board.getDashboardId());
        if (StrUtil.isBlank(board.getBoardName())) {
            throw new BusinessException("分析板标题不能为空");
        }
        board.setStatus(board.getStatus() == null ? 1 : board.getStatus());
        board.setCollapse(board.getCollapse() == null ? 0 : board.getCollapse());
        board.setLocked(board.getLocked() == null ? 0 : board.getLocked());
        board.setRefreshInterval(board.getRefreshInterval() == null ? 60 : board.getRefreshInterval());
        board.setSortOrder(board.getSortOrder() == null ? 0 : board.getSortOrder());
        boardMapper.insert(board);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateBoard(VisualBoard board) {
        if (StrUtil.isBlank(board.getId())) {
            throw new BusinessException("分析板ID不能为空");
        }
        boardMapper.updateById(board);
    }

    /** 删除分析板（级联删除其组件） */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBoard(String id) {
        itemMapper.delete(new LambdaQueryWrapper<VisualDashboardItem>()
                .eq(VisualDashboardItem::getBoardId, id));
        boardMapper.deleteById(id);
    }

    /** 复制分析板（含其组件），返回新分析板ID */
    @Transactional(rollbackFor = Exception.class)
    public String duplicateBoard(String id) {
        VisualBoard source = boardMapper.selectById(id);
        if (source == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分析板不存在: " + id);
        }
        VisualBoard copy = new VisualBoard();
        copy.setDashboardId(source.getDashboardId());
        copy.setBoardName(source.getBoardName() + "（副本）");
        copy.setSubtitle(source.getSubtitle());
        copy.setIcon(source.getIcon());
        copy.setBoardType(source.getBoardType());
        copy.setLayout(source.getLayout());
        copy.setRefreshInterval(source.getRefreshInterval());
        copy.setCollapse(0);
        copy.setLocked(source.getLocked());
        copy.setStatus(1);
        copy.setSortOrder(source.getSortOrder());
        boardMapper.insert(copy);

        List<VisualDashboardItem> items = itemMapper.selectList(new LambdaQueryWrapper<VisualDashboardItem>()
                .eq(VisualDashboardItem::getBoardId, id));
        for (VisualDashboardItem item : items) {
            item.setId(null);
            item.setCreateTime(null);
            item.setBoardId(copy.getId());
            itemMapper.insert(item);
        }
        return copy.getId();
    }

    // ==================== 数据执行 ====================

    /** 执行组件查询（可选传入分析板独立筛选 JSON，优先级高于全局筛选） */
    public QueryResult executeItem(String itemId, String filtersJson) {
        VisualDashboardItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "组件不存在: " + itemId);
        }
        if (StrUtil.isBlank(item.getDatasourceId()) || StrUtil.isBlank(item.getQuerySql())) {
            throw new BusinessException("组件未配置数据源或查询SQL");
        }
        String sql = item.getQuerySql();
        if (StrUtil.isNotBlank(filtersJson)) {
            sql = applyFilters(sql, filtersJson);
        }
        return executeOnDataSource(item.getDatasourceId(), sql);
    }

    /**
     * 将分析板独立筛选配置包装进 SQL 的 WHERE 条件。
     * filtersJson 结构：{"timeRange":"30d","dateColumn":"order_date","conditions":[{"field":"category","op":"eq","value":"手机"}]}
     * 实现方式：SELECT * FROM (<原SQL>) t WHERE <条件>，字段名按白名单校验，值统一转义，防注入。
     */
    private String applyFilters(String sql, String filtersJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> root = mapper.readValue(filtersJson, new TypeReference<Map<String, Object>>() {});
            List<String> where = new ArrayList<>();

            // 时间范围筛选（需配置日期列）
            String timeRange = root.get("timeRange") == null ? null : String.valueOf(root.get("timeRange"));
            String dateColumn = root.get("dateColumn") == null ? null : String.valueOf(root.get("dateColumn"));
            if (StrUtil.isNotBlank(timeRange) && StrUtil.isNotBlank(dateColumn) && !"all".equals(timeRange)) {
                String start = timeRangeStart(timeRange);
                if (start != null) {
                    where.add(safeColumn(dateColumn) + " >= '" + start + "'");
                }
            }

            // 条件筛选（eq/ne/gt/gte/lt/lte/contains/in）
            Object conds = root.get("conditions");
            if (conds instanceof List) {
                for (Object c : (List<?>) conds) {
                    if (!(c instanceof Map)) continue;
                    Map<?, ?> cond = (Map<?, ?>) c;
                    String field = cond.get("field") == null ? "" : String.valueOf(cond.get("field"));
                    String op = cond.get("op") == null ? "eq" : String.valueOf(cond.get("op"));
                    Object rawVal = cond.get("value");
                    if (StrUtil.isBlank(field) || rawVal == null) continue;
                    String column = safeColumn(field);
                    String value = String.valueOf(rawVal);
                    switch (op) {
                        case "ne":
                            where.add(column + " <> '" + escape(value) + "'");
                            break;
                        case "gt":
                            where.add(column + " > '" + escape(value) + "'");
                            break;
                        case "gte":
                            where.add(column + " >= '" + escape(value) + "'");
                            break;
                        case "lt":
                            where.add(column + " < '" + escape(value) + "'");
                            break;
                        case "lte":
                            where.add(column + " <= '" + escape(value) + "'");
                            break;
                        case "contains":
                            where.add(column + " LIKE '%" + escape(value) + "%'");
                            break;
                        case "in":
                            String[] items = value.split(",");
                            List<String> esc = new ArrayList<>();
                            for (String it : items) esc.add("'" + escape(it.trim()) + "'");
                            where.add(column + " IN (" + String.join(",", esc) + ")");
                            break;
                        default: // eq
                            where.add(column + " = '" + escape(value) + "'");
                    }
                }
            }

            if (where.isEmpty()) return sql;
            return "SELECT * FROM (" + sql + ") t WHERE " + String.join(" AND ", where);
        } catch (Exception e) {
            log.warn("应用分析板筛选失败，忽略筛选: {}", e.getMessage());
            return sql;
        }
    }

    /** 字段名白名单校验：仅允许字母数字下划线，防止注入 */
    private String safeColumn(String col) {
        return col.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "");
    }

    /** 单引号转义 */
    private String escape(String s) {
        return s == null ? "" : s.replace("'", "''");
    }

    /** 时间范围 → 起始日期字符串 yyyy-MM-dd */
    private String timeRangeStart(String timeRange) {
        LocalDate today = LocalDate.now();
        switch (timeRange) {
            case "today":
                return today.toString();
            case "yesterday":
                return today.minusDays(1).toString();
            case "7d":
                return today.minusDays(6).toString();
            case "30d":
                return today.minusDays(29).toString();
            case "month":
                return today.withDayOfMonth(1).toString();
            case "lastMonth":
                return today.minusMonths(1).withDayOfMonth(1).toString();
            case "year":
                return today.withDayOfMonth(1).withMonth(1).toString();
            default:
                return null;
        }
    }

    /** 即席分析查询（分析板） */
    public QueryResult executeAdhoc(String datasourceId, String sql) {
        if (StrUtil.isBlank(datasourceId)) {
            throw new BusinessException("数据源ID不能为空");
        }
        return executeOnDataSource(datasourceId, sql);
    }

    /**
     * 组件下钻查询。
     * 优先使用组件配置的 drillSql（明细/次级聚合口径），否则回退到组件原查询SQL；
     * 将点击的维度列=值作为 WHERE 条件注入，再叠加分析板独立筛选。
     */
    public QueryResult drillItem(String itemId, String column, String value, String filtersJson) {
        VisualDashboardItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "组件不存在: " + itemId);
        }
        if (StrUtil.isBlank(item.getDatasourceId()) || (StrUtil.isBlank(item.getQuerySql()) && StrUtil.isBlank(item.getDrillSql()))) {
            throw new BusinessException("组件未配置数据源或查询SQL");
        }
        String sql = StrUtil.isNotBlank(item.getDrillSql()) ? item.getDrillSql() : item.getQuerySql();
        sql = StrUtil.isBlank(sql) ? item.getQuerySql() : sql;
        List<String> where = new ArrayList<>();
        if (StrUtil.isNotBlank(column) && value != null) {
            where.add(safeColumn(column) + " = '" + escape(value) + "'");
        }
        if (StrUtil.isNotBlank(filtersJson)) {
            String wrapped = applyFilters(sql, filtersJson);
            // applyFilters 已注入条件，直接复用
            sql = wrapped;
        }
        if (!where.isEmpty()) {
            sql = "SELECT * FROM (" + sql + ") t WHERE " + String.join(" AND ", where);
        }
        return executeOnDataSource(item.getDatasourceId(), sql);
    }

    private QueryResult executeOnDataSource(String datasourceId, String sql) {
        R<QueryResult> result = datasourceApiClient.executeRaw(datasourceId, sql);
        if (result == null || result.getCode() != 0) {
            throw new BusinessException(result == null ? "查询失败" : result.getMsg());
        }
        return result.getData();
    }
}
