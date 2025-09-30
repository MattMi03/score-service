package edu.qhjy.score_service.domain.util;

import edu.qhjy.score_service.domain.dto.GradeThresholdsDTO;
import edu.qhjy.score_service.domain.dto.StudentScoreRankDTO;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 等级计算工具类
 * 实现等级划分的核心算法
 */
@Slf4j
public class GradeCalculationUtil {

    /**
     * 计算等级临界值
     *
     * @param sortedStudents 按成绩降序排列的学生列表
     * @param szsmc          市州名称
     * @param kmmc           科目名称
     * @return 等级临界值信息
     */
    public static GradeThresholdsDTO calculateThresholds(List<StudentScoreRankDTO> sortedStudents,
                                                         String szsmc, String kmmc) {
        if (sortedStudents == null || sortedStudents.isEmpty()) {
            throw new IllegalArgumentException("学生成绩数据不能为空");
        }

        int totalCount = sortedStudents.size();
        GradeThresholdsDTO thresholds = new GradeThresholdsDTO();
        thresholds.setSzsmc(szsmc);
        thresholds.setKmmc(kmmc);
        thresholds.setTotalCount(totalCount);

        // 计算各等级目标人数 (使用硬编码比例)
        int targetA = (int) Math.round(totalCount * 0.15);  // A等级15%
        int targetB = (int) Math.round(totalCount * 0.35);  // B等级35%
        int targetC = (int) Math.round(totalCount * 0.35);  // C等级35%
        int targetD = (int) Math.round(totalCount * 0.13);  // D等级13%
        int targetE = totalCount - targetA - targetB - targetC - targetD; // 剩余的都是E等级

        log.debug("市州: {}, 科目: {}, 总人数: {}, 目标人数分布 A:{}, B:{}, C:{}, D:{}, E:{}",
                szsmc, kmmc, totalCount, targetA, targetB, targetC, targetD, targetE);

        // 计算实际的等级分界点
        int currentIndex = 0;

        // A等级
        int actualA = findOptimalThreshold(sortedStudents, currentIndex, targetA);
        thresholds.setGradeAThreshold(sortedStudents.get(actualA - 1).getFslkscj());
        thresholds.setGradeACount(actualA);
        currentIndex = actualA;

        // B等级
        int actualB = findOptimalThreshold(sortedStudents, currentIndex, targetB);
        thresholds.setGradeBThreshold(sortedStudents.get(currentIndex + actualB - 1).getFslkscj());
        thresholds.setGradeBCount(actualB);
        currentIndex += actualB;

        // C等级
        int actualC = findOptimalThreshold(sortedStudents, currentIndex, targetC);
        thresholds.setGradeCThreshold(sortedStudents.get(currentIndex + actualC - 1).getFslkscj());
        thresholds.setGradeCCount(actualC);
        currentIndex += actualC;

        // D等级
        int actualD = findOptimalThreshold(sortedStudents, currentIndex, targetD);
        thresholds.setGradeDThreshold(sortedStudents.get(currentIndex + actualD - 1).getFslkscj());
        thresholds.setGradeDCount(actualD);
        currentIndex += actualD;

        // E等级（剩余的所有学生）
        int actualE = totalCount - currentIndex;
        thresholds.setGradeECount(actualE);

        // 构建详细信息Map
        Map<String, Map<String, Object>> gradeDetails = new HashMap<>();
        gradeDetails.put("A", createGradeDetailMap(thresholds.getGradeAThreshold(), actualA));
        gradeDetails.put("B", createGradeDetailMap(thresholds.getGradeBThreshold(), actualB));
        gradeDetails.put("C", createGradeDetailMap(thresholds.getGradeCThreshold(), actualC));
        gradeDetails.put("D", createGradeDetailMap(thresholds.getGradeDThreshold(), actualD));
        gradeDetails.put("E", createGradeDetailMap(BigDecimal.ZERO, actualE));
        thresholds.setGradeDetails(gradeDetails);

        log.info("等级划分完成 - 市州: {}, 科目: {}, 实际分布 A:{}, B:{}, C:{}, D:{}, E:{}",
                szsmc, kmmc, actualA, actualB, actualC, actualD, actualE);

        return thresholds;
    }

    /**
     * 找到最优的等级分界点
     * 处理相同分数的边界情况
     */
    private static int findOptimalThreshold(List<StudentScoreRankDTO> sortedStudents,
                                            int startIndex, int targetCount) {
        if (targetCount <= 0) {
            return 0;
        }

        int totalRemaining = sortedStudents.size() - startIndex;
        if (targetCount >= totalRemaining) {
            return totalRemaining;
        }

        int targetIndex = startIndex + targetCount - 1;
        BigDecimal targetScore = sortedStudents.get(targetIndex).getFslkscj();

        // 向前查找相同分数
        int beforeIndex = targetIndex;
        while (beforeIndex > startIndex &&
                sortedStudents.get(beforeIndex - 1).getFslkscj().equals(targetScore)) {
            beforeIndex--;
        }

        // 向后查找相同分数
        int afterIndex = targetIndex;
        while (afterIndex < sortedStudents.size() - 1 &&
                sortedStudents.get(afterIndex + 1).getFslkscj().equals(targetScore)) {
            afterIndex++;
        }

        // 选择更接近目标人数的边界
        int beforeCount = beforeIndex - startIndex;
        int afterCount = afterIndex - startIndex + 1;

        int beforeDistance = Math.abs(beforeCount - targetCount);
        int afterDistance = Math.abs(afterCount - targetCount);

        int optimalCount = beforeDistance <= afterDistance ? beforeCount : afterCount;

        log.debug("临界值处理 - 目标人数: {}, 相同分数: {}, 前边界人数: {}, 后边界人数: {}, 选择: {}",
                targetCount, targetScore, beforeCount, afterCount, optimalCount);

        return optimalCount;
    }

    /**
     * 创建等级详细信息Map
     */
    private static Map<String, Object> createGradeDetailMap(BigDecimal threshold, int count) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("threshold", threshold);
        detail.put("count", count);
        return detail;
    }

    /**
     * 根据分数和临界值确定学生等级
     */
    public static String determineGrade(BigDecimal score, GradeThresholdsDTO thresholds) {
        if (score.compareTo(thresholds.getGradeAThreshold()) >= 0) {
            return "A";
        } else if (score.compareTo(thresholds.getGradeBThreshold()) >= 0) {
            return "B";
        } else if (score.compareTo(thresholds.getGradeCThreshold()) >= 0) {
            return "C";
        } else if (score.compareTo(thresholds.getGradeDThreshold()) >= 0) {
            return "D";
        } else {
            return "E";
        }
    }

    /**
     * 批量为学生分配等级
     */
    public static void assignGradesToStudents(List<StudentScoreRankDTO> students,
                                              GradeThresholdsDTO thresholds) {
        for (StudentScoreRankDTO student : students) {
            String grade = determineGrade(student.getFslkscj(), thresholds);
            student.setGrade(grade);
            // A、B、C等级为合格，D、E等级为不合格
            student.setQualified("A".equals(grade) || "B".equals(grade) || "C".equals(grade) ? "合格" : "不合格");
        }
    }
}