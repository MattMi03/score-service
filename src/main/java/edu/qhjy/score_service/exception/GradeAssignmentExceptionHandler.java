package edu.qhjy.score_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 等级赋分异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GradeAssignmentExceptionHandler {

    /**
     * 处理等级赋分异常
     */
    @ExceptionHandler(GradeAssignmentException.class)
    public ResponseEntity<Map<String, Object>> handleGradeAssignmentException(GradeAssignmentException e) {
        log.error("等级赋分异常: errorCode={}, message={}", e.getErrorCode(), e.getMessage(), e);

        HttpStatus status = getHttpStatus(e.getErrorCode());

        return ResponseEntity.status(status).body(Map.of(
                "success", false,
                "errorCode", e.getErrorCode(),
                "message", e.getMessage(),
                "timestamp", System.currentTimeMillis()));
    }

    /**
     * 处理一般运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "errorCode", "INTERNAL_ERROR",
                "message", "系统内部错误: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()));
    }

    /**
     * 处理一般异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("系统异常", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "errorCode", "SYSTEM_ERROR",
                "message", "系统异常: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()));
    }

    /**
     * 根据错误代码获取HTTP状态码
     */
    private HttpStatus getHttpStatus(String errorCode) {
        return switch (errorCode) {
            case GradeAssignmentException.VALIDATION_ERROR, GradeAssignmentException.INVALID_GRADE_RATIOS,
                 GradeAssignmentException.INSUFFICIENT_PARTICIPANTS -> HttpStatus.BAD_REQUEST;
            case GradeAssignmentException.LOCK_ACQUISITION_FAILED -> HttpStatus.CONFLICT;
            case GradeAssignmentException.CONFIGURATION_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}