package edu.qhjy.score_service.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 一分一段表查询请求DTO
 * 支持历史数据查询和多维度筛选
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "一分一段表查询请求")
public class ScoreSegmentQueryDTO {

    @NotBlank(message = "考试计划代码不能为空")
    @Schema(description = "考试计划代码")
    private String ksjhdm;

    @NotBlank(message = "科目名称不能为空")
    @Schema(description = "科目名称")
    private String kmmc;

    @Schema(description = "市州名称列表（为空表示全省）")
    private List<String> szsmcList;

    @Schema(description = "分页页码（从1开始）")
    @Builder.Default
    private Integer pageNum = 1;

    @Schema(description = "分页大小")
    @Builder.Default
    private Integer pageSize = 100;

    @Schema(description = "是否包含等级信息")
    @Builder.Default
    private Boolean includeGrade = true;

    @Schema(description = "是否包含排名信息")
    @Builder.Default
    private Boolean includeRank = true;

    @Schema(description = "是否使用缓存")
    @Builder.Default
    private Boolean useCache = true;

    @Schema(description = "分数范围过滤 - 最低分")
    private Integer minScore;

    @Schema(description = "分数范围过滤 - 最高分")
    private Integer maxScore;

    @Schema(description = "等级过滤（A/B/C/D/E）")
    private List<String> gradeFilter;

    @Schema(description = "排序方式：score_desc(分数降序), score_asc(分数升序), rank_asc(排名升序)")
    @Builder.Default
    private String sortBy = "score_desc";

    /**
     * 生成缓存键
     */
    public String generateCacheKey() {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("score_segment:")
                .append(ksjhdm).append(":")
                .append(kmmc);

        if (szsmcList != null && !szsmcList.isEmpty()) {
            keyBuilder.append(":cities:").append(String.join(",", szsmcList));
        }

        if (minScore != null || maxScore != null) {
            keyBuilder.append(":range:").append(minScore).append("-").append(maxScore);
        }

        if (gradeFilter != null && !gradeFilter.isEmpty()) {
            keyBuilder.append(":grades:").append(String.join(",", gradeFilter));
        }

        keyBuilder.append(":sort:").append(sortBy);

        return keyBuilder.toString();
    }

    /**
     * 验证查询参数
     */
    public boolean isValid() {
        if (pageNum == null || pageNum < 1) {
            return false;
        }
        if (pageSize == null || pageSize < 1) {
            return false;
        }
        return minScore == null || maxScore == null || minScore <= maxScore;
    }
}