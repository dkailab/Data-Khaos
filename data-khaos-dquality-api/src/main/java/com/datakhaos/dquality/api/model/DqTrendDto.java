package com.datakhaos.dquality.api.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 质量评分趋势 DTO
 */
@Data
public class DqTrendDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDateTime snapshotTime;

    /** 评分 */
    private BigDecimal score;

    /** 通过率 */
    private BigDecimal passRate;
}