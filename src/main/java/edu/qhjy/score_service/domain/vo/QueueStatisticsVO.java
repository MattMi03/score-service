package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 队列统计VO
 */
@Data
@Schema(description = "队列统计")
public class QueueStatisticsVO {

    @Schema(description = "等待中的任务数")
    private int pendingTasks;

    @Schema(description = "运行中的任务数")
    private int runningTasks;

    @Schema(description = "已完成的任务数")
    private int completedTasks;

    @Schema(description = "失败的任务数")
    private int failedTasks;

    @Schema(description = "总任务数")
    private int totalTasks;

    @Schema(description = "队列长度")
    private int queueLength;

    @Schema(description = "处理器状态")
    private String processorStatus;
}