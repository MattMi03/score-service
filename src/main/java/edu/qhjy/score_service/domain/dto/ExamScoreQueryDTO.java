package edu.qhjy.score_service.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 考查科目成绩查询DTO
 */
@Data
public class ExamScoreQueryDTO {

    /**
     * 考试计划代码（必填）
     */
    @NotBlank(message = "考试计划代码不能为空")
    private String ksjhdm;

    /**
     * 科目名称（必填）
     */
    @NotBlank(message = "科目名称不能为空")
    private String kmmc;

    /**
     * 所在市名称（可选，用于级联筛选）
     */
    private String szsmc;

    /**
     * 考区名称（可选，用于级联筛选）
     */
    private String kqmc;

    /**
     * 学校名称（可选，用于级联筛选）
     */
    private String xxmc;

    /**
     * 考籍号（可选，用于级联筛选）
     */
    private String ksh;

    /**
     * 是否只查询有成绩的记录
     * true: 只返回FSLKSCJ和CJHGM字段均不为空的记录（有成绩数据）
     * false: 只返回FSLKSCJ或CJHGM字段有一个为空的记录（无成绩数据）
     * null: 返回所有记录（默认行为）
     */
    private Boolean withScores;

    /**
     * 页码（从1开始）
     */
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小必须大于0")
    @Max(value = 1000, message = "每页大小不能超过1000")
    private Integer pageSize = 20;

    /**
     * 计算偏移量
     */
    public Integer getOffset() {
        return (pageNum - 1) * pageSize;
    }

    /**
     * 获取限制数量
     */
    public Integer getLimit() {
        return pageSize;
    }
}