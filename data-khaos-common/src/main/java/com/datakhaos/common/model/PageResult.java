package com.datakhaos.common.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 通用分页返回
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private long total;

    /** 总页数 */
    private long pages;

    /** 当前页码 */
    private long current;

    /** 每页大小 */
    private long size;

    /** 数据列表 */
    private List<T> records;

    public static <T> PageResult<T> of(long current, long size, long total, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setCurrent(current);
        result.setSize(size);
        result.setTotal(total);
        result.setRecords(records);
        result.setPages(size <= 0 ? 0 : (total + size - 1) / size);
        return result;
    }
}
