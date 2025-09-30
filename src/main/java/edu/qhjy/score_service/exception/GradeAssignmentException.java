package edu.qhjy.score_service.exception;

import lombok.Getter;

/**
 * 等级赋分异常
 */
@Getter
public class GradeAssignmentException extends RuntimeException {

    // 常见错误代码常量
    public static final String INSUFFICIENT_PARTICIPANTS = "INSUFFICIENT_PARTICIPANTS";
    public static final String INVALID_GRADE_RATIOS = "INVALID_GRADE_RATIOS";
    public static final String LOCK_ACQUISITION_FAILED = "LOCK_ACQUISITION_FAILED";
    public static final String CALCULATION_FAILED = "CALCULATION_FAILED";
    public static final String DATA_ACCESS_ERROR = "DATA_ACCESS_ERROR";
    public static final String TASK_PROCESSING_ERROR = "TASK_PROCESSING_ERROR";
    public static final String CONFIGURATION_ERROR = "CONFIGURATION_ERROR";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    private final String errorCode;
    private final Object[] args;

    public GradeAssignmentException(String message) {
        super(message);
        this.errorCode = "GRADE_ASSIGNMENT_ERROR";
        this.args = null;
    }

    public GradeAssignmentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GRADE_ASSIGNMENT_ERROR";
        this.args = null;
    }

    public GradeAssignmentException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = null;
    }

    public GradeAssignmentException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    public GradeAssignmentException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = null;
    }

    // 便捷的静态工厂方法
    public static GradeAssignmentException insufficientParticipants(int actual, int required) {
        return new GradeAssignmentException(
                INSUFFICIENT_PARTICIPANTS,
                String.format("参与人数不足，实际: %d，要求: %d", actual, required),
                actual, required
        );
    }

    public static GradeAssignmentException invalidGradeRatios(String details) {
        return new GradeAssignmentException(
                INVALID_GRADE_RATIOS,
                "等级比例配置无效: " + details,
                details
        );
    }

    public static GradeAssignmentException lockAcquisitionFailed(String lockKey) {
        return new GradeAssignmentException(
                LOCK_ACQUISITION_FAILED,
                "获取分布式锁失败: " + lockKey,
                lockKey
        );
    }

    public static GradeAssignmentException calculationFailed(String reason) {
        return new GradeAssignmentException(
                CALCULATION_FAILED,
                "等级计算失败: " + reason,
                reason
        );
    }

    public static GradeAssignmentException dataAccessError(String operation, Throwable cause) {
        return new GradeAssignmentException(
                DATA_ACCESS_ERROR,
                "数据访问错误: " + operation,
                cause
        );
    }

    public static GradeAssignmentException taskProcessingError(String taskId, String reason) {
        return new GradeAssignmentException(
                TASK_PROCESSING_ERROR,
                String.format("任务处理失败: taskId=%s, reason=%s", taskId, reason),
                taskId, reason
        );
    }

    public static GradeAssignmentException configurationError(String configName, String reason) {
        return new GradeAssignmentException(
                CONFIGURATION_ERROR,
                String.format("配置错误: %s - %s", configName, reason),
                configName, reason
        );
    }

    public static GradeAssignmentException validationError(String field, String reason) {
        return new GradeAssignmentException(
                VALIDATION_ERROR,
                String.format("参数验证失败: %s - %s", field, reason),
                field, reason
        );
    }
}