package edu.qhjy.score_service.service.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.qhjy.score_service.domain.dto.GradeAssignmentRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 等级赋分任务队列服务
 * 使用Redis实现异步任务队列，支持大数据量的等级赋分处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeAssignmentQueueService {

    private static final String QUEUE_KEY = "grade_assignment:queue";
    private static final String TASK_KEY_PREFIX = "grade_assignment:task:";
    private static final String RESULT_KEY_PREFIX = "grade_assignment:result:";
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 提交等级赋分任务到队列
     */
    public String submitTask(GradeAssignmentRequestDTO request) {
        try {
            String taskId = generateTaskId(request);

            // 创建任务信息
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("taskId", taskId);
            taskInfo.put("request", request);
            taskInfo.put("status", TaskStatus.PENDING.name());
            taskInfo.put("submitTime", LocalDateTime.now().toString());
            taskInfo.put("priority", calculatePriority(request));

            // 保存任务详情
            String taskKey = TASK_KEY_PREFIX + taskId;
            redisTemplate.opsForValue().set(taskKey, taskInfo, 24, TimeUnit.HOURS);

            // 添加到队列（使用优先级队列）
            double priority = (double) taskInfo.get("priority");
            redisTemplate.opsForZSet().add(QUEUE_KEY, taskId, priority);

            log.info("等级赋分任务已提交到队列: taskId={}, priority={}", taskId, priority);
            return taskId;

        } catch (Exception e) {
            log.error("提交等级赋分任务失败", e);
            throw new RuntimeException("提交任务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从队列中获取下一个待处理任务
     */
    public Map<String, Object> getNextTask() {
        try {
            // 从优先级队列中获取最高优先级的任务
            var taskIds = redisTemplate.opsForZSet().reverseRange(QUEUE_KEY, 0, 0);

            if (taskIds == null || taskIds.isEmpty()) {
                return null;
            }

            String taskId = taskIds.iterator().next().toString();

            // 从队列中移除任务
            redisTemplate.opsForZSet().remove(QUEUE_KEY, taskId);

            // 获取任务详情
            String taskKey = TASK_KEY_PREFIX + taskId;
            Map<String, Object> taskInfo = (Map<String, Object>) redisTemplate.opsForValue().get(taskKey);

            if (taskInfo != null) {
                // 更新任务状态为处理中
                taskInfo.put("status", TaskStatus.PROCESSING.name());
                taskInfo.put("startTime", LocalDateTime.now().toString());
                redisTemplate.opsForValue().set(taskKey, taskInfo, 24, TimeUnit.HOURS);

                log.info("从队列中获取任务: taskId={}", taskId);
            }

            return taskInfo;

        } catch (Exception e) {
            log.error("获取队列任务失败", e);
            return null;
        }
    }

    /**
     * 更新任务状态
     */
    public void updateTaskStatus(String taskId, TaskStatus status, String message) {
        try {
            String taskKey = TASK_KEY_PREFIX + taskId;
            Map<String, Object> taskInfo = (Map<String, Object>) redisTemplate.opsForValue().get(taskKey);

            if (taskInfo != null) {
                taskInfo.put("status", status.name());
                taskInfo.put("updateTime", LocalDateTime.now().toString());

                if (message != null) {
                    taskInfo.put("message", message);
                }

                if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) {
                    taskInfo.put("endTime", LocalDateTime.now().toString());
                }

                redisTemplate.opsForValue().set(taskKey, taskInfo, 24, TimeUnit.HOURS);
                log.info("任务状态已更新: taskId={}, status={}", taskId, status);
            }

        } catch (Exception e) {
            log.error("更新任务状态失败: taskId={}", taskId, e);
        }
    }

    /**
     * 保存任务执行结果
     */
    public void saveTaskResult(String taskId, Object result) {
        try {
            String resultKey = RESULT_KEY_PREFIX + taskId;
            redisTemplate.opsForValue().set(resultKey, result, 7, TimeUnit.DAYS);

            log.info("任务结果已保存: taskId={}", taskId);

        } catch (Exception e) {
            log.error("保存任务结果失败: taskId={}", taskId, e);
        }
    }

    /**
     * 获取任务状态
     */
    public Map<String, Object> getTaskStatus(String taskId) {
        try {
            String taskKey = TASK_KEY_PREFIX + taskId;
            Map<String, Object> taskInfo = (Map<String, Object>) redisTemplate.opsForValue().get(taskKey);

            if (taskInfo == null) {
                return Map.of("exists", false, "message", "任务不存在");
            }

            // 如果任务已完成，尝试获取结果
            String status = (String) taskInfo.get("status");
            if (TaskStatus.COMPLETED.name().equals(status)) {
                String resultKey = RESULT_KEY_PREFIX + taskId;
                Object result = redisTemplate.opsForValue().get(resultKey);
                if (result != null) {
                    taskInfo.put("result", result);
                }
            }

            taskInfo.put("exists", true);
            return taskInfo;

        } catch (Exception e) {
            log.error("获取任务状态失败: taskId={}", taskId, e);
            return Map.of("exists", false, "message", "获取任务状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取队列统计信息
     */
    public Map<String, Object> getQueueStatistics() {
        try {
            Long queueSize = redisTemplate.opsForZSet().count(QUEUE_KEY, Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY);

            Map<String, Object> stats = new HashMap<>();
            stats.put("queueSize", queueSize != null ? queueSize : 0);
            stats.put("updateTime", LocalDateTime.now().toString());

            return stats;

        } catch (Exception e) {
            log.error("获取队列统计信息失败", e);
            return Map.of("error", "获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 清理过期任务
     */
    public int cleanupExpiredTasks() {
        try {
            // 这里可以实现清理逻辑，比如清理超过一定时间的已完成任务
            log.debug("执行过期任务清理");

            // 目前返回0，后续可以实现具体的清理逻辑
            return 0;

        } catch (Exception e) {
            log.error("清理过期任务失败", e);
            return 0;
        }
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId(GradeAssignmentRequestDTO request) {
        return String.format("grade_%s_%s_%s_%d",
                request.getKsjhdm(),
                request.getKmmc().replaceAll("[^a-zA-Z0-9]", "_"),
                request.getSzsmc() != null ? request.getSzsmc().replaceAll("[^a-zA-Z0-9]", "_") : "all",
                System.currentTimeMillis());
    }

    /**
     * 计算任务优先级
     * 优先级越高，数值越大
     */
    private int calculatePriority(GradeAssignmentRequestDTO request) {
        int priority = 100; // 基础优先级

        // 如果是单个城市处理，优先级更高
        if (request.getSzsmc() != null) {
            priority += 50;
        }

        // 可以根据其他因素调整优先级
        // 比如：紧急程度、数据量大小等

        return priority;
    }

    // 任务状态
    public enum TaskStatus {
        PENDING, // 等待处理
        PROCESSING, // 处理中
        COMPLETED, // 已完成
        FAILED, // 失败
        CANCELLED // 已取消
    }
}