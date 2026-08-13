package com.datakhaos.visual.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 保存即席查询（收藏）请求
 */
@Data
public class AdhocSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 收藏ID（更新时必填） */
    private String id;

    /** 查询名称 */
    private String name;

    /** 数据源ID */
    private String datasourceId;

    /** 查询SQL（支持 ${param} 占位符） */
    private String sql;

    /** 默认参数：key 为占位符名，value 为默认值 */
    private Map<String, Object> params;

    /** 分组/文件夹（可选） */
    private String folder;
}
