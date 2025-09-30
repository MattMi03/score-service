package edu.qhjy.score_service.service.task;

import edu.qhjy.score_service.domain.dto.GradeAssignmentRequestDTO;
import edu.qhjy.score_service.domain.vo.GradeAssignmentResultVO;
import edu.qhjy.score_service.service.GradeAssignmentService;
import edu.qhjy.score_service.service.redis.GradeAssignmentQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 等级赋分任务处理器
 * 异步处理队列中的等级赋分任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GradeAssignmentTaskProcessor implements ApplicationRunner {

    // 配置参数
    private static final int POLL_INTERVAL_SECONDS = 5; // 轮询间隔
    private static final int CLEANUP_INTERVAL_HOURS = 6; // 清理间隔
    private final GradeAssignmentService gradeAssignmentService;
    private final GradeAssignmentQueueService queueService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("启动等级赋分任务处理器");
        startTaskProcessor();
        startCleanupScheduler();
    }

    /**
     * 启动任务处理器
     */
    private void startTaskProcessor() {
        scheduler.scheduleWithFixedDelay(this::processNextTask,
                0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("等级赋分任务处理器已启动，轮询间隔: {}秒", POLL_INTERVAL_SECONDS);
    }

    /**
     * 启动清理调度器
     */
    private void startCleanupScheduler() {
        scheduler.scheduleWithFixedDelay(queueService::cleanupExpiredTasks,
                CLEANUP_INTERVAL_HOURS, CLEANUP_INTERVAL_HOURS, TimeUnit.HOURS);
        log.info("任务清理调度器已启动，清理间隔: {}小时", CLEANUP_INTERVAL_HOURS);
    }

    /**
     * 处理下一个任务
     */
    private void processNextTask() {
        if (!isRunning.compareAndSet(false, true)) {
            log.debug("任务处理器正在运行中，跳过本次轮询");
            return;
        }

        try {
            Map<String, Object> taskInfo = queueService.getNextTask();

            if (taskInfo == null) {
                log.debug("队列中没有待处理任务");
                return;
            }

            String taskId = (String) taskInfo.get("taskId");
            log.info("开始处理等级赋分任务: taskId={}", taskId);

            try {
                // 解析请求参数
                GradeAssignmentRequestDTO request = parseRequest(taskInfo);

                // 执行等级赋分
                GradeAssignmentResultVO result = gradeAssignmentService.assignGrades(request);

                // 保存结果并更新状态
                queueService.saveTaskResult(taskId, result);
                queueService.updateTaskStatus(taskId,
                        GradeAssignmentQueueService.TaskStatus.COMPLETED,
                        "任务执行成功");

                log.info("等级赋分任务执行成功: taskId={}, 处理学生数={}, 处理市州数={}",
                        taskId, result.getProcessedStudentCount(), result.getProcessedCityCount());

            } catch (Exception e) {
                // 任务执行失败
                String errorMessage = "任务执行失败: " + e.getMessage();
                queueService.updateTaskStatus(taskId,
                        GradeAssignmentQueueService.TaskStatus.FAILED,
                        errorMessage);

                log.error("等级赋分任务执行失败: taskId={}", taskId, e);
            }

        } catch (Exception e) {
            log.error("处理队列任务时发生异常", e);
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * 解析任务请求参数
     */
    private GradeAssignmentRequestDTO parseRequest(Map<String, Object> taskInfo) {
        try {
            Object requestObj = taskInfo.get("request");

            if (requestObj instanceof GradeAssignmentRequestDTO) {
                return (GradeAssignmentRequestDTO) requestObj;
            }

            if (requestObj instanceof Map) {
                Map<String, Object> requestMap = (Map<String, Object>) requestObj;

                GradeAssignmentRequestDTO request = new GradeAssignmentRequestDTO();
                request.setKsjhdm((String) requestMap.get("ksjhdm"));
                request.setKmmc((String) requestMap.get("kmmc"));
                request.setSzsmc((String) requestMap.get("szsmc"));
                request.setOperatorName((String) requestMap.get("operatorName"));
                request.setOperatorCode((String) requestMap.get("operatorCode"));

                log.info("开始处理等级赋分任务: city={}, subject={}",
                        request.getSzsmc(), request.getKmmc());

                return request;
            }

            throw new IllegalArgumentException("无法解析任务请求参数");

        } catch (Exception e) {
            log.error("解析任务请求参数失败", e);
            throw new RuntimeException("解析任务请求参数失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取处理器状态
     */
    public Map<String, Object> getProcessorStatus() {
        return Map.of(
                "isRunning", isRunning.get(),
                "pollInterval", POLL_INTERVAL_SECONDS,
                "cleanupInterval", CLEANUP_INTERVAL_HOURS,
                "queueStatistics", queueService.getQueueStatistics());
    }

    /**
     * 停止任务处理器
     */
    public void shutdown() {
        log.info("正在停止等级赋分任务处理器");
        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                log.warn("任务处理器强制停止");
            } else {
                log.info("任务处理器已正常停止");
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            log.error("停止任务处理器时被中断", e);
        }
    }
}