package edu.qhjy.score_service.service.algorithm;

import edu.qhjy.score_service.domain.dto.GradeThresholdsDTO;
import edu.qhjy.score_service.domain.dto.StudentScoreRankDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 等级计算核心算法
 * 实现等级临界值计算和成绩等级分配
 */
@Slf4j
@Component
public class GradeCalculationAlgorithm {

    /**
     * 计算等级临界值
     *
     * @param scores      学生成绩列表
     * @param gradeRatios 等级比例配置
     * @param szsmc       市州名称
     * @param kmmc        科目名称
     * @return 等级临界值
     */
    public static GradeThresholdsDTO calculateGradeThresholds(List<StudentScoreRankDTO> scores,
                                                              Map<String, Double> gradeRatios,
                                                              String szsmc, String kmmc) {
        return calculateGradeThresholds(scores, gradeRatios, szsmc, kmmc, false, 0.02);
    }

    /**
     * 计算等级临界值（支持新旧算法）
     *
     * @param scores                     学生成绩列表
     * @param gradeRatios                等级比例配置
     * @param szsmc                      市州名称
     * @param kmmc                       科目名称
     * @param useEGradePriorityAlgorithm 是否使用E等级优先算法
     * @param eGradeMinPercentage        E等级最小百分比
     * @return 等级临界值
     */
    public static GradeThresholdsDTO calculateGradeThresholds(List<StudentScoreRankDTO> scores,
                                                              Map<String, Double> gradeRatios,
                                                              String szsmc, String kmmc,
                                                              boolean useEGradePriorityAlgorithm,
                                                              double eGradeMinPercentage) {

        if (scores == null || scores.isEmpty()) {
            log.warn("成绩数据为空，无法计算等级临界值: city={}, subject={}", szsmc, kmmc);
            return null;
        }

        int totalCount = scores.size();
        log.info("开始计算等级临界值: city={}, subject={}, totalCount={}, useNewAlgorithm={}",
                szsmc, kmmc, totalCount, useEGradePriorityAlgorithm);

        GradeThresholdsDTO thresholds = new GradeThresholdsDTO();
        thresholds.setSzsmc(szsmc);
        thresholds.setKmmc(kmmc);
        thresholds.setTotalCount(totalCount);

        int aCount, bCount, cCount, dCount, eCount;
        DEBoundaryResult deBoundary = null; // 缓存DE分界线结果，避免重复计算

        if (useEGradePriorityAlgorithm) {
            // 新算法：E等级优先
            log.info("使用E等级优先算法，市州: {}, 科目: {}, 总人数: {}, E等级最小百分比: {}%",
                    szsmc, kmmc, totalCount, eGradeMinPercentage * 100);

            // 1. 先确定DE分界线和E等级人数（只计算一次）
            deBoundary = findDEBoundary(scores, eGradeMinPercentage);
            eCount = deBoundary.getECount();
            int abcdTotalCount = totalCount - eCount;

            // 2. 在剩余学生中按固定比例划分ABCD（保持原有比例）
            aCount = (int) Math.round(totalCount * gradeRatios.get("A"));
            bCount = (int) Math.round(totalCount * gradeRatios.get("B"));
            cCount = (int) Math.round(totalCount * gradeRatios.get("C"));
            dCount = (int) Math.round(totalCount * gradeRatios.get("D"));

            // 3. 调整ABCD人数以确保总数正确
            int abcdCalculatedTotal = aCount + bCount + cCount + dCount;
            if (abcdCalculatedTotal != abcdTotalCount) {
                // 按比例调整，优先调整D等级
                int diff = abcdTotalCount - abcdCalculatedTotal;
                dCount += diff;
                if (dCount < 0) {
                    cCount += dCount;
                    dCount = 0;
                }
            }

            log.info("新算法计算结果: A={}, B={}, C={}, D={}, E={}, ABCD总计={}",
                    aCount, bCount, cCount, dCount, eCount, aCount + bCount + cCount + dCount);

        } else {
            // 原算法：E等级作为缓冲区
            log.info("使用原算法（E等级作为缓冲区），市州: {}, 科目: {}, 总人数: {}", szsmc, kmmc, totalCount);

            aCount = (int) Math.round(totalCount * gradeRatios.get("A"));
            bCount = (int) Math.round(totalCount * gradeRatios.get("B"));
            cCount = (int) Math.round(totalCount * gradeRatios.get("C"));
            dCount = (int) Math.round(totalCount * gradeRatios.get("D"));
            eCount = totalCount - aCount - bCount - cCount - dCount; // 剩余的分配给E

            // 确保E等级至少有最小人数
            if (eCount < 0) {
                eCount = Math.max(1, (int) Math.round(totalCount * gradeRatios.get("E")));
                // 重新调整其他等级人数
                int remaining = totalCount - eCount;
                aCount = (int) Math.round(remaining * gradeRatios.get("A") / (1 - gradeRatios.get("E")));
                bCount = (int) Math.round(remaining * gradeRatios.get("B") / (1 - gradeRatios.get("E")));
                cCount = (int) Math.round(remaining * gradeRatios.get("C") / (1 - gradeRatios.get("E")));
                dCount = remaining - aCount - bCount - cCount;
            }

            // 确保累计人数不超过总人数（防止边界情况）
            int cumulativeCount = aCount + bCount + cCount + dCount;
            if (cumulativeCount > totalCount) {
                // 如果累计人数超过总人数，调整D等级人数
                dCount = Math.max(0, totalCount - aCount - bCount - cCount);
                eCount = totalCount - aCount - bCount - cCount - dCount;
                log.warn("调整等级人数以避免超出总数: totalCount={}, 调整后 dCount={}, eCount={}",
                        totalCount, dCount, eCount);
            }
        }

        // 设置人数
        thresholds.setGradeACount(aCount);
        thresholds.setGradeBCount(bCount);
        thresholds.setGradeCCount(cCount);
        thresholds.setGradeDCount(dCount);
        thresholds.setGradeECount(eCount);

        // 计算临界值
        log.info("计算等级临界值: city={}, subject={}, 人数分布 A={}, B={}, C={}, D={}, E={}",
                szsmc, kmmc, aCount, bCount, cCount, dCount, eCount);

        if (useEGradePriorityAlgorithm) {
            // 新算法：使用缓存的DE分界线结果，避免重复计算
            thresholds.setGradeAThreshold(findOptimalThreshold(scores, aCount, "A"));
            thresholds.setGradeBThreshold(findOptimalThreshold(scores, aCount + bCount, "B"));
            thresholds.setGradeCThreshold(findOptimalThreshold(scores, aCount + bCount + cCount, "C"));
            thresholds.setGradeDThreshold(deBoundary.getBoundaryScore());
            thresholds.setGradeEThreshold(scores.get(scores.size() - 1).getFslkscj());

            log.info("新算法阈值: A阈值={}, B阈值={}, C阈值={}, D阈值(DE分界线)={}, E阈值={}",
                    thresholds.getGradeAThreshold(), thresholds.getGradeBThreshold(),
                    thresholds.getGradeCThreshold(), thresholds.getGradeDThreshold(),
                    thresholds.getGradeEThreshold());
        } else {
            // 原算法：按累计人数计算所有阈值
            thresholds.setGradeAThreshold(findOptimalThreshold(scores, aCount, "A"));
            thresholds.setGradeBThreshold(findOptimalThreshold(scores, aCount + bCount, "B"));
            thresholds.setGradeCThreshold(findOptimalThreshold(scores, aCount + bCount + cCount, "C"));
            thresholds.setGradeDThreshold(findOptimalThreshold(scores, aCount + bCount + cCount + dCount, "D"));
            // 设置E等级阈值为最低分，而不是0
            thresholds.setGradeEThreshold(scores.get(scores.size() - 1).getFslkscj());
        }

        log.info("等级临界值计算完成: city={}, subject={}, A阈值={}, B阈值={}, C阈值={}, D阈值={}, E阈值={}, E人数={}",
                szsmc, kmmc, thresholds.getGradeAThreshold(), thresholds.getGradeBThreshold(),
                thresholds.getGradeCThreshold(), thresholds.getGradeDThreshold(), thresholds.getGradeEThreshold(),
                thresholds.getGradeECount());

        return thresholds;
    }

    /**
     * 寻找DE分界线（新算法）
     * 找到最接近98%但小于98%的累计百分比对应的分数线
     *
     * @param scores              按分数降序排列的学生成绩列表
     * @param eGradeMinPercentage E等级最小百分比（如0.02表示2%）
     * @return DE分界线分数和对应的E等级起始索引
     */
    private static DEBoundaryResult findDEBoundary(List<StudentScoreRankDTO> scores,
                                                   double eGradeMinPercentage) {
        int totalCount = scores.size();
        double targetPercentile = 1.0 - eGradeMinPercentage; // 98% = 1.0 - 0.02

        log.debug("查找DE分界线: totalCount={}, targetPercentile={}", totalCount, targetPercentile);

        // 计算目标索引（从0开始，降序排列）
        int targetIndex = (int) Math.floor(totalCount * targetPercentile);

        // 确保索引在有效范围内
        if (targetIndex >= totalCount) {
            targetIndex = totalCount - 1;
        }
        if (targetIndex < 0) {
            targetIndex = 0;
        }

        BigDecimal targetScore = scores.get(targetIndex).getFslkscj();

        // 查找同分范围
        int sameScoreStart = targetIndex;
        int sameScoreEnd = targetIndex;

        // 向前查找同分（更高排名）
        while (sameScoreStart > 0 &&
                scores.get(sameScoreStart - 1).getFslkscj().compareTo(targetScore) == 0) {
            sameScoreStart--;
        }

        // 向后查找同分（更低排名）
        while (sameScoreEnd < totalCount - 1 &&
                scores.get(sameScoreEnd + 1).getFslkscj().compareTo(targetScore) == 0) {
            sameScoreEnd++;
        }

        // 计算两种选择的累计百分比
        double option1Percentage = (double) sameScoreStart / totalCount; // 同分都不进入E等级
        double option2Percentage = (double) (sameScoreEnd + 1) / totalCount; // 同分都进入E等级

        log.debug("同分处理: targetScore={}, sameScoreStart={}, sameScoreEnd={}, option1%={}, option2%={}",
                targetScore, sameScoreStart, sameScoreEnd,
                Math.round(option1Percentage * 1000) / 10.0,
                Math.round(option2Percentage * 1000) / 10.0);

        // 选择最接近98%但小于98%的选项
        int eStartIndex;
        BigDecimal deBoundaryScore;

        if (option1Percentage < targetPercentile && option2Percentage >= targetPercentile) {
            // option1 < 98% < option2，选择option1
            eStartIndex = sameScoreStart;
            deBoundaryScore = sameScoreStart > 0 ? scores.get(sameScoreStart - 1).getFslkscj() : targetScore;
        } else if (option1Percentage >= targetPercentile) {
            // 两个选项都 >= 98%，选择更小的那个
            eStartIndex = sameScoreStart;
            deBoundaryScore = sameScoreStart > 0 ? scores.get(sameScoreStart - 1).getFslkscj() : targetScore;
        } else {
            // 两个选项都 < 98%，选择更大的那个（更接近98%）
            eStartIndex = sameScoreEnd + 1;
            deBoundaryScore = targetScore;
        }

        // 确保E等级至少有1个学生
        if (eStartIndex >= totalCount) {
            eStartIndex = totalCount - 1;
            deBoundaryScore = scores.get(eStartIndex - 1).getFslkscj();
        }

        double actualPercentage = (double) eStartIndex / totalCount;
        int eCount = totalCount - eStartIndex;

        log.info("DE分界线确定: 分界分数={}, E等级起始索引={}, E等级人数={}, 实际累计百分比={}%",
                deBoundaryScore, eStartIndex, eCount, Math.round(actualPercentage * 1000) / 10.0);

        return new DEBoundaryResult(deBoundaryScore, eStartIndex, eCount);
    }

    /**
     * 寻找最优等级临界值
     * 处理同分情况，选择最接近目标比例的临界值
     *
     * @param scores      按分数降序排列的学生成绩列表
     * @param targetCount 目标人数（累计）
     * @param grade       等级
     * @return 最优临界值
     */
    private static BigDecimal findOptimalThreshold(List<StudentScoreRankDTO> scores,
                                                   int targetCount, String grade) {
        log.debug("计算{}等级阈值: targetCount={}, totalSize={}", grade, targetCount, scores.size());

        if (targetCount >= scores.size()) {
            // 如果目标人数超过总人数，返回最低分数而不是0
            // 这样可以确保所有学生都能被正确分级
            BigDecimal minScore = scores.get(scores.size() - 1).getFslkscj();
            log.warn("{}等级目标人数({})超过总人数({}), 返回最低分数: {}",
                    grade, targetCount, scores.size(), minScore);
            return minScore;
        }

        // 目标位置的分数
        BigDecimal targetScore = scores.get(targetCount - 1).getFslkscj();

        // 查找同分的范围
        int sameScoreStart = targetCount - 1;
        int sameScoreEnd = targetCount - 1;

        // 向前查找同分
        while (sameScoreStart > 0 &&
                scores.get(sameScoreStart - 1).getFslkscj().compareTo(targetScore) == 0) {
            sameScoreStart--;
        }

        // 向后查找同分
        while (sameScoreEnd < scores.size() - 1 &&
                scores.get(sameScoreEnd + 1).getFslkscj().compareTo(targetScore) == 0) {
            sameScoreEnd++;
        }

        // 如果没有同分情况，直接返回
        if (sameScoreStart == sameScoreEnd) {
            return targetScore;
        }

        // 有同分情况，选择最接近目标比例的边界
        int option1Count = sameScoreStart; // 同分都不包含
        int option2Count = sameScoreEnd + 1; // 同分都包含

        int diff1 = Math.abs(option1Count - targetCount);
        int diff2 = Math.abs(option2Count - targetCount);

        if (diff1 <= diff2) {
            // 选择较高的分数作为临界值（同分都不达到该等级）
            return sameScoreStart > 0 ? scores.get(sameScoreStart - 1).getFslkscj() : targetScore;
        } else {
            // 选择当前分数作为临界值（同分都达到该等级）
            return targetScore;
        }
    }

    /**
     * 根据分数和临界值确定等级
     *
     * @param score      学生分数
     * @param thresholds 等级临界值
     * @return 等级
     */
    private static String determineGrade(BigDecimal score, GradeThresholdsDTO thresholds) {
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
     * 为学生分配等级
     *
     * @param scoreRanks 学生成绩列表
     * @param thresholds 等级临界值
     */
    public void assignGradesToStudents(List<StudentScoreRankDTO> scoreRanks,
                                       GradeThresholdsDTO thresholds) {
        if (scoreRanks == null || scoreRanks.isEmpty() || thresholds == null) {
            log.warn("数据为空，无法分配等级");
            return;
        }

        for (StudentScoreRankDTO student : scoreRanks) {
            String grade = determineGrade(student.getFslkscj(), thresholds);
            student.setGrade(grade);
            // A、B、C等级为合格，D、E等级为不合格
            student.setQualified("A".equals(grade) || "B".equals(grade) || "C".equals(grade) ? "合格" : "不合格");
        }

        log.info("等级分配完成: 总人数={}, A={}, B={}, C={}, D={}, E={}",
                scoreRanks.size(), thresholds.getGradeACount(), thresholds.getGradeBCount(),
                thresholds.getGradeCCount(), thresholds.getGradeDCount(), thresholds.getGradeECount());
    }

    /**
     * 验证等级分配结果
     *
     * @param scoreRanks 分配等级后的学生成绩列表
     * @param thresholds 等级临界值
     * @return 验证是否通过
     */
    public boolean validateGradeAssignment(List<StudentScoreRankDTO> scoreRanks,
                                           GradeThresholdsDTO thresholds) {
        if (scoreRanks == null || scoreRanks.isEmpty() || thresholds == null) {
            return false;
        }

        // 统计实际分配的等级数量
        long actualA = scoreRanks.stream().filter(s -> "A".equals(s.getGrade())).count();
        long actualB = scoreRanks.stream().filter(s -> "B".equals(s.getGrade())).count();
        long actualC = scoreRanks.stream().filter(s -> "C".equals(s.getGrade())).count();
        long actualD = scoreRanks.stream().filter(s -> "D".equals(s.getGrade())).count();
        long actualE = scoreRanks.stream().filter(s -> "E".equals(s.getGrade())).count();

        // 允许的误差范围（由于同分处理可能导致的偏差）
        int tolerance = Math.max(1, scoreRanks.size() / 100); // 1%的容错

        boolean valid = Math.abs(actualA - thresholds.getGradeACount()) <= tolerance &&
                Math.abs(actualB - thresholds.getGradeBCount()) <= tolerance &&
                Math.abs(actualC - thresholds.getGradeCCount()) <= tolerance &&
                Math.abs(actualD - thresholds.getGradeDCount()) <= tolerance &&
                Math.abs(actualE - thresholds.getGradeECount()) <= tolerance;

        if (!valid) {
            log.warn("等级分配验证失败: 预期A={}, 实际A={}, 预期B={}, 实际B={}, 预期C={}, 实际C={}, 预期D={}, 实际D={}, 预期E={}, 实际E={}",
                    thresholds.getGradeACount(), actualA, thresholds.getGradeBCount(), actualB,
                    thresholds.getGradeCCount(), actualC, thresholds.getGradeDCount(), actualD,
                    thresholds.getGradeECount(), actualE);
        }

        return valid;
    }

    /**
     * DE分界线查找结果
     */
    private static class DEBoundaryResult {
        private final BigDecimal boundaryScore;
        private final int eStartIndex;
        private final int eCount;

        public DEBoundaryResult(BigDecimal boundaryScore, int eStartIndex, int eCount) {
            this.boundaryScore = boundaryScore;
            this.eStartIndex = eStartIndex;
            this.eCount = eCount;
        }

        public BigDecimal getBoundaryScore() {
            return boundaryScore;
        }

        public int getEStartIndex() {
            return eStartIndex;
        }

        public int getECount() {
            return eCount;
        }
    }
}