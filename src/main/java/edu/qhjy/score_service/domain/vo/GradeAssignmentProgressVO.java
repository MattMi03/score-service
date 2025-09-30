package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 等级赋分进度VO
 */
@Data
@Schema(description = "等级赋分进度")
public class GradeAssignmentProgressVO {

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "进度百分比")
    private double progressPercentage;

    @Schema(description = "当前步骤")
    private String currentStep;

    @Schema(description = "总步骤数")
    private int totalSteps;

    @Schema(description = "当前步骤数")
    private int currentStepNumber;

    @Schema(description = "已处理数量")
    private int processedCount;

    @Schema(description = "总数量")
    private int totalCount;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "预计完成时间")
    private LocalDateTime estimatedEndTime;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "详细信息")
    private String details;
}