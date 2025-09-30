package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务状态VO
 */
@Data
@Schema(description = "任务状态")
public class TaskStatusVO {

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "进度百分比")
    private double progress;

    @Schema(description = "任务结果")
    private Object result;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "完成时间")
    private LocalDateTime endTime;

    @Schema(description = "任务参数")
    private Object parameters;
}