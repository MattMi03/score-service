package edu.qhjy.score_service.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 等级同步结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeSyncResultVO {

    /**
     * 同步是否成功
     */
    private Boolean success;
    /**
     * 同步的学生数量
     */
    private Integer syncedStudentCount;
    /**
     * 处理时间（毫秒）
     */
    private Long processingTime;
    /**
     * 结果消息
     */
    private String message;
    /**
     * 错误信息（如果有）
     */
    private String errorMessage;
    /**
     * 考试计划代码
     */
    private String ksjhdm;
    /**
     * 科目名称
     */
    private String kmmc;
    /**
     * 市州名称
     */
    private String szsmc;
    /**
     * 等级分布统计
     */
    private GradeSyncStatistics gradeStatistics;

    /**
     * 判断同步是否成功
     */
    public boolean isSuccess() {
        return success != null && success;
    }

    /**
     * 等级同步统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradeSyncStatistics {

        /**
         * A等级同步数量
         */
        private Integer gradeACount;

        /**
         * B等级同步数量
         */
        private Integer gradeBCount;

        /**
         * C等级同步数量
         */
        private Integer gradeCCount;

        /**
         * D等级同步数量
         */
        private Integer gradeDCount;

        /**
         * E等级同步数量
         */
        private Integer gradeECount;

        /**
         * 总同步数量
         */
        private Integer totalCount;
    }
}