package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成绩分项VO
 * 用于展示单个分项的信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "成绩分项信息")
public class ScoreItemVO {

    @Schema(description = "分项标识ID")
    private Long kscjfxbs;

    @Schema(description = "序号", example = "1")
    private Integer fxxh;

    @Schema(description = "分项名称", example = "过程性评价")
    private String fxmc;

    @Schema(description = "分项成绩分数", example = "40")
    private Integer fxcjfs;

    @Schema(description = "百分比显示", example = "40%")
    private String percentage;

    @Schema(description = "是否已有学生数据")
    private Boolean hasStudentData;
}