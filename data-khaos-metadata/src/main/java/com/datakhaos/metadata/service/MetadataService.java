package com.datakhaos.metadata.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datakhaos.common.exception.BusinessException;
import com.datakhaos.common.model.PageResult;
import com.datakhaos.datasource.api.connector.DatasourceApiClient;
import com.datakhaos.datasource.api.model.ColumnInfo;
import com.datakhaos.metadata.entity.MetaColumn;
import com.datakhaos.metadata.entity.MetaDatabase;
import com.datakhaos.metadata.entity.MetaTable;
import com.datakhaos.metadata.entity.MetaTableLineage;
import com.datakhaos.metadata.mapper.MetaColumnMapper;
import com.datakhaos.metadata.mapper.MetaDatabaseMapper;
import com.datakhaos.metadata.mapper.MetaTableLineageMapper;
import com.datakhaos.metadata.mapper.MetaTableMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 元数据服务：通过 DatasourceApiClient 拉取库/表/字段并落库（幂等 upsert），
 * 提供结构树、检索与血缘能力。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataService {

    private final MetaDatabaseMapper databaseMapper;
    private final MetaTableMapper tableMapper;
    private final MetaColumnMapper columnMapper;
    private final MetaTableLineageMapper lineageMapper;
    private final DatasourceApiClient datasourceApiClient;

    // ---------- 采集同步 ----------

    /** 全量同步：数据源下所有库 -> 表 -> 字段 */
    @Transactional(rollbackFor = Exception.class)
    public void sync(String datasourceId) {
        List<String> databases = datasourceApiClient.databases(datasourceId);
        if (databases.isEmpty()) {
            log.warn("数据源 {} 未返回任何数据库", datasourceId);
            return;
        }
        for (String database : databases) {
            syncDatabase(datasourceId, database);
        }
        log.info("数据源 {} 元数据同步完成，共 {} 个库", datasourceId, databases.size());
    }

    /** 同步单个库下的表与字段 */
    @Transactional(rollbackFor = Exception.class)
    public void syncDatabase(String datasourceId, String database) {
        MetaDatabase db = upsertDatabase(datasourceId, database);
        List<String> tables = datasourceApiClient.tables(datasourceId, database);
        for (String tableName : tables) {
            MetaTable table = upsertTable(db.getId(), tableName, null);
            List<ColumnInfo> columns = datasourceApiClient.columnInfos(datasourceId, database, tableName);
            for (ColumnInfo column : columns) {
                upsertColumn(table.getId(), column);
            }
        }
    }

    /** 同步单表字段 */
    public void syncTable(String datasourceId, String database, String tableName) {
        MetaDatabase db = upsertDatabase(datasourceId, database);
        MetaTable table = upsertTable(db.getId(), tableName, null);
        List<ColumnInfo> columns = datasourceApiClient.columnInfos(datasourceId, database, tableName);
        for (ColumnInfo column : columns) {
            upsertColumn(table.getId(), column);
        }
    }

    // ---------- 查询 ----------

    /** 结构树：库 -> 表 -> 字段 */
    public List<Map<String, Object>> structure(String datasourceId) {
        List<MetaDatabase> databases = databaseMapper.selectList(new LambdaQueryWrapper<MetaDatabase>()
                .eq(MetaDatabase::getDatasourceId, datasourceId)
                .orderByAsc(MetaDatabase::getDatabaseName));
        List<Map<String, Object>> result = new ArrayList<>();
        for (MetaDatabase db : databases) {
            List<MetaTable> tables = tableMapper.selectList(new LambdaQueryWrapper<MetaTable>()
                    .eq(MetaTable::getDatabaseId, db.getId())
                    .orderByAsc(MetaTable::getTableName));
            List<Map<String, Object>> tableNodes = new ArrayList<>();
            for (MetaTable table : tables) {
                List<MetaColumn> columns = columnMapper.selectList(new LambdaQueryWrapper<MetaColumn>()
                        .eq(MetaColumn::getTableId, table.getId())
                        .orderByAsc(MetaColumn::getSortOrder));
                Map<String, Object> tableNode = new LinkedHashMap<>();
                tableNode.put("table", table);
                tableNode.put("columns", columns);
                tableNodes.add(tableNode);
            }
            Map<String, Object> dbNode = new LinkedHashMap<>();
            dbNode.put("database", db);
            dbNode.put("tables", tableNodes);
            result.add(dbNode);
        }
        return result;
    }

    /** 数据库列表（已采集） */
    public List<MetaDatabase> databases(String datasourceId) {
        return databaseMapper.selectList(new LambdaQueryWrapper<MetaDatabase>()
                .eq(MetaDatabase::getDatasourceId, datasourceId)
                .orderByAsc(MetaDatabase::getDatabaseName));
    }

    /** 分页查询表 */
    public PageResult<MetaTable> tablePage(long current, long size, String datasourceId, String keyword) {
        Page<MetaTable> page = tableMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<MetaTable>()
                        .like(StrUtil.isNotBlank(keyword), MetaTable::getTableName, keyword)
                        .orderByAsc(MetaTable::getTableName));
        if (StrUtil.isNotBlank(datasourceId)) {
            // 按数据源过滤：先找其库，再过滤表
            List<String> dbIds = databaseMapper.selectList(new LambdaQueryWrapper<MetaDatabase>()
                            .eq(MetaDatabase::getDatasourceId, datasourceId))
                    .stream().map(MetaDatabase::getId).toList();
            page = dbIds.isEmpty() ? new Page<>(current, size) : tableMapper.selectPage(new Page<>(current, size),
                    new LambdaQueryWrapper<MetaTable>()
                            .in(MetaTable::getDatabaseId, dbIds)
                            .like(StrUtil.isNotBlank(keyword), MetaTable::getTableName, keyword)
                            .orderByAsc(MetaTable::getTableName));
        }
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    /** 分页查询字段 */
    public PageResult<MetaColumn> columnPage(long current, long size, String tableId) {
        Page<MetaColumn> page = columnMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<MetaColumn>()
                        .eq(StrUtil.isNotBlank(tableId), MetaColumn::getTableId, tableId)
                        .orderByAsc(MetaColumn::getSortOrder));
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    /** 检索：表名 / 表注释 / 字段名 */
    public List<Map<String, Object>> search(String keyword) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (StrUtil.isBlank(keyword)) {
            return result;
        }
        List<MetaTable> tables = tableMapper.selectList(new LambdaQueryWrapper<MetaTable>()
                .like(MetaTable::getTableName, keyword)
                .or().like(MetaTable::getDescription, keyword));
        for (MetaTable table : tables) {
            MetaDatabase db = table.getDatabaseId() == null ? null : databaseMapper.selectById(table.getDatabaseId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "TABLE");
            item.put("table", table);
            item.put("databaseName", db == null ? null : db.getDatabaseName());
            result.add(item);
        }
        List<MetaColumn> columns = columnMapper.selectList(new LambdaQueryWrapper<MetaColumn>()
                .like(MetaColumn::getColumnName, keyword));
        for (MetaColumn column : columns) {
            MetaTable table = column.getTableId() == null ? null : tableMapper.selectById(column.getTableId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "COLUMN");
            item.put("column", column);
            item.put("tableName", table == null ? null : table.getTableName());
            result.add(item);
        }
        return result;
    }

    // ---------- 血缘 ----------

    /** 查询某表的血缘（上下游） */
    public List<MetaTableLineage> lineage(String tableId) {
        return lineageMapper.selectList(new LambdaQueryWrapper<MetaTableLineage>()
                .eq(MetaTableLineage::getSourceTableId, tableId)
                .or().eq(MetaTableLineage::getTargetTableId, tableId));
    }

    /** 记录血缘关系 */
    public void saveLineage(MetaTableLineage lineage) {
        if (StrUtil.isBlank(lineage.getSourceTableId()) || StrUtil.isBlank(lineage.getTargetTableId())) {
            throw new BusinessException("源表与目标表不能为空");
        }
        if (lineage.getRelationType() == null) {
            lineage.setRelationType("ETL");
        }
        lineageMapper.insert(lineage);
    }

    /** 按数据源/库/表名解析表ID */
    public String tableIdOf(String datasourceId, String database, String tableName) {
        MetaDatabase db = databaseMapper.selectOne(new LambdaQueryWrapper<MetaDatabase>()
                .eq(MetaDatabase::getDatasourceId, datasourceId)
                .eq(MetaDatabase::getDatabaseName, database));
        if (db == null) {
            return null;
        }
        MetaTable table = tableMapper.selectOne(new LambdaQueryWrapper<MetaTable>()
                .eq(MetaTable::getDatabaseId, db.getId())
                .eq(MetaTable::getTableName, tableName));
        return table == null ? null : table.getId();
    }

    // ---------- upsert ----------

    private MetaDatabase upsertDatabase(String datasourceId, String databaseName) {
        MetaDatabase exist = databaseMapper.selectOne(new LambdaQueryWrapper<MetaDatabase>()
                .eq(MetaDatabase::getDatasourceId, datasourceId)
                .eq(MetaDatabase::getDatabaseName, databaseName));
        MetaDatabase db = exist == null ? new MetaDatabase() : exist;
        db.setDatasourceId(datasourceId);
        db.setDatabaseName(databaseName);
        db.setSyncTime(LocalDateTime.now());
        if (exist == null) {
            databaseMapper.insert(db);
        } else {
            databaseMapper.updateById(db);
        }
        return db;
    }

    private MetaTable upsertTable(String databaseId, String tableName, String tableType) {
        MetaTable exist = tableMapper.selectOne(new LambdaQueryWrapper<MetaTable>()
                .eq(MetaTable::getDatabaseId, databaseId)
                .eq(MetaTable::getTableName, tableName));
        MetaTable table = exist == null ? new MetaTable() : exist;
        table.setDatabaseId(databaseId);
        table.setTableName(tableName);
        if (StrUtil.isNotBlank(tableType)) {
            table.setTableType(tableType);
        }
        table.setSyncTime(LocalDateTime.now());
        if (exist == null) {
            tableMapper.insert(table);
        } else {
            tableMapper.updateById(table);
        }
        return table;
    }

    private MetaColumn upsertColumn(String tableId, ColumnInfo info) {
        MetaColumn exist = columnMapper.selectOne(new LambdaQueryWrapper<MetaColumn>()
                .eq(MetaColumn::getTableId, tableId)
                .eq(MetaColumn::getColumnName, info.getColumnName()));
        MetaColumn column = exist == null ? new MetaColumn() : exist;
        column.setTableId(tableId);
        column.setColumnName(info.getColumnName());
        column.setColumnType(info.getColumnType());
        column.setColumnLength(info.getColumnLength());
        column.setColumnScale(info.getColumnScale());
        column.setIsNullable(Boolean.TRUE.equals(info.getNullable()) ? 1 : 0);
        column.setIsPrimaryKey(Boolean.TRUE.equals(info.getPrimaryKey()) ? 1 : 0);
        column.setDefaultValue(info.getDefaultValue());
        column.setDescription(info.getDescription());
        column.setSortOrder(info.getSortOrder());
        column.setSensitiveLevel(info.getSensitiveLevel());
        if (exist == null) {
            columnMapper.insert(column);
        } else {
            columnMapper.updateById(column);
        }
        return column;
    }
}
