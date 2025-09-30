package edu.qhjy.score_service.util;

import edu.qhjy.score_service.exception.GradeAssignmentException;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 等级赋分工具类
 */
@Slf4j
public class GradeAssignmentUtils {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 生成任务ID
     */
    public static String generateTaskId(String ksjhdm, String kmmc, String szsmc) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String hash = String.valueOf(Objects.hash(ksjhdm, kmmc, szsmc));
        return String.format("task_%s_%s_%s_%s", ksjhdm, kmmc, szsmc, timestamp).replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /**
     * 生成分布式锁的键
     */
    public static String generateLockKey(String ksjhdm, String kmmc, String szsmc) {
        return String.format("lock:grade_assignment:%s:%s:%s", ksjhdm, kmmc, szsmc);
    }

    /**
     * 生成进度跟踪的键
     */
    public static String generateProgressKey(String ksjhdm, String kmmc, String szsmc) {
        return String.format("progress:grade_assignment:%s:%s:%s", ksjhdm, kmmc, szsmc);
    }

    /**
     * 生成缓存键名
     */
    public static String generateCacheKey(String prefix, String... parts) {
        return prefix + ":" + String.join(":", parts);
    }

    /**
     * 验证等级比例配置
     */
    public static void validateGradeRatios(Map<String, Double> gradeRatios) {
        if (gradeRatios == null || gradeRatios.isEmpty()) {
            throw GradeAssignmentException.invalidGradeRatios("等级比例配置不能为空");
        }

        double totalRatio = gradeRatios.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        if (Math.abs(totalRatio - 1.0) > 0.001) {
            throw GradeAssignmentException.invalidGradeRatios(
                    String.format("等级比例总和应为1.0，实际为: %.3f", totalRatio));
        }

        for (Map.Entry<String, Double> entry : gradeRatios.entrySet()) {
            Double ratio = entry.getValue();
            if (ratio == null || ratio < 0 || ratio > 1) {
                throw GradeAssignmentException.invalidGradeRatios(
                        String.format("等级 %s 的比例无效: %s", entry.getKey(), ratio));
            }
        }
    }

    /**
     * 计算等级阈值
     */
    public static Map<String, BigDecimal> calculateGradeThresholds(
            List<BigDecimal> sortedScores,
            Map<String, Double> gradeRatios,
            List<String> grades) {

        if (sortedScores == null || sortedScores.isEmpty()) {
            throw GradeAssignmentException.calculationFailed("分数列表不能为空");
        }

        validateGradeRatios(gradeRatios);

        Map<String, BigDecimal> thresholds = new LinkedHashMap<>();
        int totalCount = sortedScores.size();
        int currentIndex = 0;

        for (String grade : grades) {
            Double ratio = gradeRatios.get(grade);
            if (ratio == null || ratio == 0.0) {
                // 如果比例为0，设置阈值为最高分+1（表示没有学生能达到这个等级）
                thresholds.put(grade, sortedScores.get(0).add(BigDecimal.ONE));
                continue;
            }

            int gradeCount = (int) Math.round(totalCount * ratio);
            if (gradeCount == 0) {
                // 如果计算出的人数为0，设置阈值为最高分+1
                thresholds.put(grade, sortedScores.get(0).add(BigDecimal.ONE));
                continue;
            }

            int thresholdIndex = Math.min(currentIndex + gradeCount - 1, totalCount - 1);
            BigDecimal threshold = sortedScores.get(thresholdIndex);
            thresholds.put(grade, threshold);

            currentIndex += gradeCount;
        }

        return thresholds;
    }

    /**
     * 根据分数和阈值确定等级
     */
    public static String determineGrade(BigDecimal score, Map<String, BigDecimal> thresholds, List<String> grades) {
        for (String grade : grades) {
            BigDecimal threshold = thresholds.get(grade);
            if (threshold != null && score.compareTo(threshold) >= 0) {
                return grade;
            }
        }
        // 如果没有匹配的等级，返回最低等级
        return grades.get(grades.size() - 1);
    }

    /**
     * 格式化百分比
     */
    public static String formatPercentage(double value) {
        return String.format("%.1f%%", value * 100);
    }

    /**
     * 格式化分数
     */
    public static BigDecimal formatScore(BigDecimal score, int precision) {
        if (score == null) {
            return BigDecimal.ZERO;
        }
        return score.setScale(precision, RoundingMode.HALF_UP);
    }

    /**
     * 格式化时间
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATETIME_FORMATTER);
    }

    /**
     * 计算处理进度百分比
     */
    public static int calculateProgressPercentage(int processed, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.min(100, (int) Math.round((double) processed / total * 100));
    }

    /**
     * 分批处理列表
     */
    public static <T> List<List<T>> partition(List<T> list, int batchSize) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException("批处理大小必须大于0");
        }

        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            batches.add(list.subList(i, end));
        }
        return batches;
    }

    /**
     * 安全地获取Map中的值
     */
    public static <K, V> V safeGet(Map<K, V> map, K key, V defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        V value = map.get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 检查字符串是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 检查字符串是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 生成统计摘要
     */
    public static Map<String, Object> generateStatisticsSummary(
            int totalStudents,
            int successCount,
            int failureCount,
            long processingTime) {

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalStudents", totalStudents);
        summary.put("successCount", successCount);
        summary.put("failureCount", failureCount);
        summary.put("successRate",
                totalStudents > 0 ? formatPercentage((double) successCount / totalStudents) : "0.0%");
        summary.put("processingTime", processingTime + "ms");
        summary.put("averageTimePerStudent",
                totalStudents > 0 ? String.format("%.2fms", (double) processingTime / totalStudents) : "0.00ms");

        return summary;
    }

    /**
     * 验证必需参数
     */
    public static void requireNonNull(Object obj, String paramName) {
        if (obj == null) {
            throw GradeAssignmentException.validationError(paramName, "参数不能为空");
        }
    }

    /**
     * 验证字符串参数
     */
    public static void requireNonEmpty(String str, String paramName) {
        if (isEmpty(str)) {
            throw GradeAssignmentException.validationError(paramName, "参数不能为空");
        }
    }

    /**
     * 验证数值参数
     */
    public static void requirePositive(Number number, String paramName) {
        if (number == null || number.doubleValue() <= 0) {
            throw GradeAssignmentException.validationError(paramName, "参数必须大于0");
        }
    }
}