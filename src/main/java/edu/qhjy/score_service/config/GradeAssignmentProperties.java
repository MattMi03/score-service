package edu.qhjy.score_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 等级赋分配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "grade.assignment")
public class GradeAssignmentProperties {

    /**
     * 等级配置
     */
    private GradeConfig gradeConfig = new GradeConfig();

    /**
     * Redis配置
     */
    private RedisConfig redisConfig = new RedisConfig();

    /**
     * 任务处理配置
     */
    private TaskConfig taskConfig = new TaskConfig();

    /**
     * 算法配置
     */
    private AlgorithmConfig algorithmConfig = new AlgorithmConfig();

    /**
     * 是否启用新的E等级优先算法
     *
     * @return true表示启用新算法，false表示使用原算法
     */
    public boolean isUseEGradePriorityAlgorithm() {
        return gradeConfig.isUseEGradePriorityAlgorithm();
    }

    /**
     * 获取E等级最小百分比阈值
     *
     * @return E等级最小百分比（如0.02表示2%）
     */
    public double getEGradeMinPercentage() {
        return gradeConfig.getEGradeMinPercentage();
    }

    /**
     * 获取等级对应的数字代码
     */
    public String getGradeCode(String grade) {
        Map<String, String> gradeCodes = Map.of(
                "A", "1",
                "B", "2",
                "C", "3",
                "D", "4",
                "E", "5");
        return gradeCodes.getOrDefault(grade, "5");
    }

    /**
     * 验证等级比例配置
     */
    public boolean validateGradeRatios() {
        double totalRatio = gradeConfig.getGradeRatios().values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        return Math.abs(totalRatio - 1.0) < 0.001; // 允许0.1%的误差
    }

    /**
     * 获取Redis键名
     */
    public String getRedisKey(String suffix) {
        return redisConfig.getKeyPrefix() + ":" + suffix;
    }

    @Data
    public static class GradeConfig {
        /**
         * 等级列表 (A, B, C, D, E)
         */
        private List<String> grades = List.of("A", "B", "C", "D", "E");

        /**
         * 等级比例配置
         */
        private Map<String, Double> gradeRatios = Map.of(
                "A", 0.15, // 15%
                "B", 0.35, // 35%
                "C", 0.35, // 35%
                "D", 0.13, // 13%
                "E", 0.02 // 2%
        );

        /**
         * 最小参与人数
         */
        private int minParticipants = 1000;

        /**
         * 是否启用同分处理
         */
        private boolean enableSameScoreHandling = true;

        /**
         * 是否启用新的E等级优先算法
         * true: 先确定E等级(≥2%)，再在剩余98%中按固定比例划分ABCD
         * false: 使用原有算法，E等级作为缓冲区
         */
        private boolean useEGradePriorityAlgorithm = true;

        /**
         * E等级最小百分比阈值（新算法使用）
         * 用于确定DE分界线，确保至少有该比例的学生为E等级
         */
        private double eGradeMinPercentage = 0.02; // 2%
    }

    @Data
    public static class RedisConfig {
        /**
         * 分布式锁超时时间（秒）
         */
        private int lockTimeout = 300;

        /**
         * 进度缓存过期时间（秒）
         */
        private int progressExpiration = 7200;

        /**
         * 计算结果缓存过期时间（秒）
         */
        private int calculationCacheExpiration = 3600;

        /**
         * 任务队列过期时间（秒）
         */
        private int taskQueueExpiration = 86400;

        /**
         * Redis键前缀
         */
        private String keyPrefix = "grade_assignment";
    }

    @Data
    public static class TaskConfig {
        /**
         * 任务轮询间隔（秒）
         */
        private int pollInterval = 5;

        /**
         * 清理间隔（小时）
         */
        private int cleanupInterval = 6;

        /**
         * 批处理大小
         */
        private int batchSize = 1000;

        /**
         * 最大重试次数
         */
        private int maxRetries = 3;

        /**
         * 任务超时时间（分钟）
         */
        private int taskTimeout = 30;
    }

    @Data
    public static class AlgorithmConfig {
        /**
         * 并行处理线程数
         */
        private int parallelThreads = 4;

        /**
         * 是否启用缓存
         */
        private boolean enableCache = true;

        /**
         * 计算精度（小数位数）
         */
        private int precision = 1;

        /**
         * 是否启用详细日志
         */
        private boolean enableDetailedLogging = false;
    }
}