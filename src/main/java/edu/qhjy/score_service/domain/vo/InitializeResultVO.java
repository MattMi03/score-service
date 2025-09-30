package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 初始化结果VO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "初始化结果VO")
public class InitializeResultVO {

    @Schema(description = "是否成功", example = "true")
    private Boolean success;

    @Schema(description = "初始化的学生总数", example = "1500")
    private Integer totalStudents;

    @Schema(description = "成功初始化的学生数", example = "1450")
    private Integer successCount;

    @Schema(description = "失败的学生数", example = "50")
    private Integer failCount;

    @Schema(description = "涉及的学校数量", example = "5")
    private Integer schoolCount;

    @Schema(description = "处理消息", example = "初始化完成")
    private String message;

    @Schema(description = "详细信息", example = "成功初始化1450名学生的语文科目考试记录")
    private String details;
}