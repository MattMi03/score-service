package edu.qhjy.score_service.controller;

import edu.qhjy.score_service.domain.dto.GradeAssignmentRequestDTO;
import edu.qhjy.score_service.domain.vo.GradeAssignmentResultVO;
import edu.qhjy.score_service.service.redis.GradeAssignmentQueueService;
import edu.qhjy.score_service.service.task.GradeAssignmentTaskProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 等级赋分管理控制器
 * 提供队列管理和任务监控功能
 */
@Slf4j
@RestController
@RequestMapping("/api/grade-assignment/management")
@RequiredArgsConstructor
@Tag(name = "等级赋分队列管理", description = "等级赋分队列管理和任务监控API")
public class GradeAssignmentManagementController {

    private final GradeAssignmentQueueService queueService;
    private final GradeAssignmentTaskProcessor taskProcessor;

    /**
     * 提交异步等级赋分任务
     */
    @PostMapping("/submit-async")
    @Operation(summary = "提交异步等级赋分任务", description = "将等级赋分任务提交到队列中异步处理")
    public ResponseEntity<Map<String, Object>> submitAsyncTask(
            @Valid @RequestBody GradeAssignmentRequestDTO request) {

        try {
            log.info("提交异步等级赋分任务: ksjhdm={}, kmmc={}, szsmc={}",
                    request.getKsjhdm(), request.getKmmc(), request.getSzsmc());

            String taskId = queueService.submitTask(request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "任务提交成功",
                    "taskId", taskId,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("提交异步等级赋分任务失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "任务提交失败: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * 查询任务状态
     */
    @GetMapping("/task-status/{taskId}")
    @Operation(summary = "查询任务状态", description = "根据任务ID查询等级赋分任务的执行状态")
    public ResponseEntity<Map<String, Object>> getTaskStatus(
            @Parameter(description = "任务ID") @PathVariable String taskId) {

        try {
            Map<String, Object> status = queueService.getTaskStatus(taskId);

            if (status == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", status,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("查询任务状态失败: taskId={}", taskId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "查询任务状态失败: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * 获取任务结果
     */
    @GetMapping("/task-result/{taskId}")
    @Operation(summary = "获取任务结果", description = "根据任务ID获取等级赋分任务的执行结果")
    public ResponseEntity<Map<String, Object>> getTaskResult(
            @Parameter(description = "任务ID") @PathVariable String taskId) {

        try {
            Map<String, Object> status = queueService.getTaskStatus(taskId);

            if (status == null) {
                return ResponseEntity.notFound().build();
            }

            String taskStatus = (String) status.get("status");
            if (!"COMPLETED".equals(taskStatus)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "任务尚未完成，当前状态: " + taskStatus,
                        "timestamp", System.currentTimeMillis()
                ));
            }

            GradeAssignmentResultVO result = (GradeAssignmentResultVO) status.get("result");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("获取任务结果失败: taskId={}", taskId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "获取任务结果失败: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * 获取队列统计信息
     */
    @GetMapping("/queue-statistics")
    @Operation(summary = "获取队列统计信息", description = "获取等级赋分任务队列的统计信息")
    public ResponseEntity<Map<String, Object>> getQueueStatistics() {

        try {
            Map<String, Object> statistics = queueService.getQueueStatistics();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", statistics,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("获取队列统计信息失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "获取队列统计信息失败: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * 获取处理器状态
     */
    @GetMapping("/processor-status")
    @Operation(summary = "获取处理器状态", description = "获取等级赋分任务处理器的运行状态")
    public ResponseEntity<Map<String, Object>> getProcessorStatus() {

        try {
            Map<String, Object> status = taskProcessor.getProcessorStatus();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", status,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("获取处理器状态失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "获取处理器状态失败: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * 清理过期任务
     */
    @PostMapping("/cleanup-expired")
    @Operation(summary = "清理过期任务", description = "手动清理队列中的过期任务")
    public ResponseEntity<Map<String, Object>> cleanupExpiredTasks() {

        try {
            int cleanedCount = queueService.cleanupExpiredTasks();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "清理完成",
                    "cleanedCount", cleanedCount,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("清理过期任务失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "清理过期任务失败: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * 取消任务
     */
    @PostMapping("/cancel-task/{taskId}")
    @Operation(summary = "取消任务", description = "取消指定的等级赋分任务")
    public ResponseEntity<Map<String, Object>> cancelTask(
            @Parameter(description = "任务ID") @PathVariable String taskId) {

        try {
            Map<String, Object> status = queueService.getTaskStatus(taskId);

            if (status == null) {
                return ResponseEntity.notFound().build();
            }

            String taskStatus = (String) status.get("status");
            if ("COMPLETED".equals(taskStatus) || "FAILED".equals(taskStatus)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "任务已完成或失败，无法取消",
                        "timestamp", System.currentTimeMillis()
                ));
            }

            queueService.updateTaskStatus(taskId,
                    GradeAssignmentQueueService.TaskStatus.CANCELLED,
                    "任务已被用户取消");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "任务已取消",
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("取消任务失败: taskId={}", taskId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "取消任务失败: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }
}