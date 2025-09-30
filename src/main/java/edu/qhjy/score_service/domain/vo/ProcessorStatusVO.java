package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 处理器状态VO
 */
@Data
@Schema(description = "处理器状态")
public class ProcessorStatusVO {

    @Schema(description = "处理器状态")
    private String status;

    @Schema(description = "是否运行中")
    private boolean running;

    @Schema(description = "启动时间")
    private LocalDateTime startTime;

    @Schema(description = "处理的任务数")
    private int processedTasks;

    @Schema(description = "当前处理的任务ID")
    private String currentTaskId;

    @Schema(description = "最后活动时间")
    private LocalDateTime lastActivityTime;

    @Schema(description = "错误信息")
    private String errorMessage;
}