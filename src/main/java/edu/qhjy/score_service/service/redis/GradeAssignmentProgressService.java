package edu.qhjy.score_service.service.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 等级划分进度跟踪服务
 * 实时监控处理进度
 */
@Slf4j
@Service
public class GradeAssignmentProgressService {

    private static final String PROGRESS_PREFIX = "grade_progress:";
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 更新等级赋分进度
     *
     * @param taskId    任务ID
     * @param szsmc     当前处理的市州名称
     * @param processed 已处理的学生数量
     * @param total     总学生数量
     */
    public void updateProgress(String taskId, String szsmc, int processed, int total) {
        String progressKey = PROGRESS_PREFIX + taskId;

        try {
            Map<String, Object> progressData = new HashMap<>();
            progressData.put("taskId", taskId);
            progressData.put("szsmc", szsmc);
            progressData.put("processed", processed);
            progressData.put("total", total);
            progressData.put("percentage", total > 0 ? (double) processed / total * 100 : 0);
            progressData.put("status", processed >= total ? "COMPLETED" : "PROCESSING");
            progressData.put("updateTime", System.currentTimeMillis());

            redisTemplate.opsForHash().putAll(progressKey, progressData);
            redisTemplate.expire(progressKey, Duration.ofHours(2));

            log.debug("等级赋分进度已更新: taskId={}, city={}, progress={}/{}",
                    taskId, szsmc, processed, total);

        } catch (Exception e) {
            log.error("更新等级赋分进度失败: taskId={}", taskId, e);
        }
    }

    /**
     * 获取处理进度
     *
     * @param taskId 任务ID
     * @return 进度信息
     */
    public Map<String, Object> getProgress(String taskId) {
        String progressKey = PROGRESS_PREFIX + taskId;
        try {
            Map<Object, Object> rawData = redisTemplate.opsForHash().entries(progressKey);
            Map<String, Object> progressData = new HashMap<>();
            rawData.forEach((k, v) -> progressData.put(k.toString(), v));
            return progressData;
        } catch (Exception e) {
            log.error("获取进度失败: taskId={}", taskId, e);
            return new HashMap<>();
        }
    }

    /**
     * 设置任务开始状态
     *
     * @param taskId      任务ID
     * @param description 任务描述
     */
    public void startTask(String taskId, String description) {
        updateProgress(taskId, description, 0, 100);
    }

    /**
     * 更新进度（百分比）
     *
     * @param taskId      任务ID
     * @param percentage  进度百分比
     * @param description 当前步骤描述
     */
    public void updateProgress(String taskId, int percentage, String description) {
        updateProgress(taskId, description, percentage, 100);
    }

    /**
     * 设置任务完成状态
     *
     * @param taskId      任务ID
     * @param description 完成描述
     */
    public void completeTask(String taskId, String description) {
        updateProgress(taskId, description, 100, 100);
    }

    /**
     * 设置任务开始状态
     *
     * @param taskId      任务ID
     * @param totalCities 总市州数
     */
    public void startTask(String taskId, int totalCities) {
        updateProgress(taskId, "开始处理", 0, totalCities);
    }

    /**
     * 设置任务完成状态
     *
     * @param taskId      任务ID
     * @param totalCities 总市州数
     */
    public void completeTask(String taskId, int totalCities) {
        updateProgress(taskId, "处理完成", totalCities, totalCities);
    }

    /**
     * 删除进度信息
     *
     * @param taskId 任务ID
     */
    public void removeProgress(String taskId) {
        String progressKey = PROGRESS_PREFIX + taskId;
        try {
            redisTemplate.delete(progressKey);
            log.debug("删除进度信息: taskId={}", taskId);
        } catch (Exception e) {
            log.error("删除进度信息失败: taskId={}", taskId, e);
        }
    }
}