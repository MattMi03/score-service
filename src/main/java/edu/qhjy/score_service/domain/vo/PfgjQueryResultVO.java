package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 评分轨迹查询结果VO
 *
 * @author dadalv
 * @since 2025-08-20
 */
@Data
@Schema(description = "评分轨迹查询结果")
public class PfgjQueryResultVO {

    @Schema(description = "序号 - 评分轨迹标识", example = "1")
    private Long pfgjbs;

    @Schema(description = "流水号", example = "LSH001")
    private String lsh;

    @Schema(description = "大题号", example = "1")
    private Integer itemid;

    @Schema(description = "密号", example = "MH001")
    private String mh;

    @Schema(description = "大题分数", example = "85.5")
    private String marksum;

    @Schema(description = "小题分值串", example = "10,15,20")
    private String submark;

    @Schema(description = "评次", example = "1")
    private String tasktype;

    @Schema(description = "评卷用户号", example = "1001")
    private Integer teacherid;
}