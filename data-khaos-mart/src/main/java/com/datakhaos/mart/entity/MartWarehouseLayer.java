package com.datakhaos.mart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.datakhaos.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数仓分层定义（一线大厂标准：ODS / DWD / DWS / ADS）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mart_warehouse_layer")
public class MartWarehouseLayer extends BaseEntity {

    /** 分层编码 ODS/DWD/DWS/ADS */
    private String layerCode;

    /** 分层名称 */
    private String layerName;

    /** 分层说明 */
    private String layerDesc;

    /** 排序 */
    private Integer sortOrder;

    /** 0:停用 1:启用 */
    private Integer status;
}