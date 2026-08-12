package com.datakhaos.visual.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 仪表板上线请求
 */
@Data
public class PublishRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 发布说明 */
    private String remark;
}