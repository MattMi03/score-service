package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量等级调整结果VO
 * 用于返回多市州等级调整的结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量等级调整结果")
public class BatchGradeAdjustmentResultVO {

    @Schema(description = "整体操作是否成功", example = "true")
    private Boolean success;

    @Schema(description = "整体操作消息", example = "批量等级调整完成")
    private String message;

    @Schema(description = "考试计划代码", example = "2024001")
    private String ksjhdm;

    @Schema(description = "科目名称", example = "语文")
    private String kmmc;

    @Schema(description = "总处理时间（毫秒）", example = "1500")
    private Long totalProcessingTime;

    @Schema(description = "成功处理的市州数量", example = "3")
    private Integer successCount;

    @Schema(description = "失败处理的市州数量", example = "0")
    private Integer failureCount;

    @Schema(description = "总受影响的学生数量", example = "2500")
    private Integer totalAffectedStudentCount;

    @Schema(description = "各市州的调整结果列表")
    private List<CityAdjustmentResult> cityResults;

    /**
     * 判断是否所有市州都调整成功
     */
    public boolean isAllSuccess() {
        return failureCount != null && failureCount == 0;
    }

    /**
     * 获取调整成功率
     */
    public double getSuccessRate() {
        if (successCount == null || failureCount == null) {
            return 0.0;
        }
        int total = successCount + failureCount;
        if (total == 0) {
            return 0.0;
        }
        return (double) successCount / total * 100;
    }

    /**
     * 市州调整结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "市州调整结果")
    public static class CityAdjustmentResult {

        @Schema(description = "市州名称", example = "长沙市")
        private String szsmc;

        @Schema(description = "该市州调整是否成功", example = "true")
        private Boolean success;

        @Schema(description = "该市州调整消息", example = "等级调整确认成功")
        private String message;

        @Schema(description = "该市州处理时间（毫秒）", example = "500")
        private Long processingTime;

        @Schema(description = "该市州受影响的学生数量", example = "800")
        private Integer affectedStudentCount;

        @Schema(description = "该市州等级调整结果")
        private GradeAdjustmentResultVO gradeAdjustmentResult;

        @Schema(description = "该市州一分一段数据变化预览")
        private ScoreSegmentChangePreviewVO scoreSegmentChangePreview;

        @Schema(description = "错误信息（如果调整失败）")
        private String errorMessage;
    }
}