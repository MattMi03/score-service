package edu.qhjy.score_service.domain.vo;

import lombok.Data;

/**
 * 等级调整确认结果响应VO
 * 包含等级调整结果和一分一段数据变化信息
 */
@Data
public class GradeAdjustmentConfirmResultVO {

    /**
     * 操作是否成功
     */
    private Boolean success;

    /**
     * 消息
     */
    private String message;

    /**
     * 处理时间（毫秒）
     */
    private Long processingTime;

    /**
     * 受影响的学生数量
     */
    private Integer affectedStudentCount;

    /**
     * 等级调整结果
     */
    private GradeAdjustmentResultVO gradeAdjustmentResult;

    /**
     * 一分一段数据变化预览
     */
    private ScoreSegmentChangePreviewVO scoreSegmentChangePreview;
}