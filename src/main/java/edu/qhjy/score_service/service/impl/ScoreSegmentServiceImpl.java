package edu.qhjy.score_service.service.impl;

import edu.qhjy.score_service.config.GradeAssignmentProperties;
import edu.qhjy.score_service.domain.dto.*;
import edu.qhjy.score_service.domain.entity.KsjhEntity;
import edu.qhjy.score_service.domain.entity.WcxxEntity;
import edu.qhjy.score_service.domain.vo.*;
import edu.qhjy.score_service.mapper.primary.KscjMapper;
import edu.qhjy.score_service.mapper.primary.KsjhMapper;
import edu.qhjy.score_service.mapper.primary.WcxxMapper;
import edu.qhjy.score_service.service.ScoreSegmentService;
import edu.qhjy.score_service.service.algorithm.GradeCalculationAlgorithm;
import edu.qhjy.score_service.service.redis.ScoreSegmentCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 一分一段表服务实现类
 */
@Slf4j
@Service
public class ScoreSegmentServiceImpl implements ScoreSegmentService {

    @Autowired
    private KscjMapper kscjMapper;

    @Autowired
    private WcxxMapper wcxxMapper;

    @Autowired
    private KsjhMapper ksjhMapper;

    @Autowired
    private ScoreSegmentCacheService cacheService;

    @Autowired
    private GradeCalculationAlgorithm gradeCalculationAlgorithm;

    @Autowired
    private GradeAssignmentProperties gradeAssignmentProperties;

    // @Override
    // public ScoreSegmentOverviewVO getScoreSegmentOverview(ScoreSegmentQueryDTO
    // queryDTO) {
    // log.info("获取一分一段表总览: ksjhdm={}, kmmc={}", queryDTO.getKsjhdm(),
    // queryDTO.getKmmc());

    // // 尝试从缓存获取
    // if (Boolean.TRUE.equals(queryDTO.getUseCache())) {
    // ScoreSegmentOverviewVO cached = cacheService.getCachedOverviewData(
    // queryDTO.getKsjhdm(), queryDTO.getKmmc(), null);
    // if (cached != null) {
    // return cached;
    // }
    // }

    // try {
    // // 获取基础统计数据
    // Map<String, Object> basicStats = kscjMapper.getBasicStatistics(
    // queryDTO.getKsjhdm(), queryDTO.getKmmc());

    // if (basicStats == null || basicStats.isEmpty()) {
    // throw new RuntimeException("未找到相关成绩数据");
    // }

    // // 获取等级分布数据
    // List<Map<String, Object>> gradeStats = wcxxMapper.getGradeDistribution(
    // queryDTO.getKsjhdm(), queryDTO.getKmmc(), null);

    // // 获取市州分布数据
    // List<Map<String, Object>> cityStats = kscjMapper.getCityStatistics(
    // queryDTO.getKsjhdm(), queryDTO.getKmmc());

    // // 获取一分一段数据（前100条用于总览）
    // List<ScoreSegmentDTO> segmentData = calculateScoreSegments(
    // queryDTO.getKsjhdm(), queryDTO.getKmmc(), null, 100);

    // // 构建总览VO
    // ScoreSegmentOverviewVO overview = buildOverviewVO(
    // queryDTO, basicStats, gradeStats, cityStats, segmentData);

    // // 缓存结果
    // if (Boolean.TRUE.equals(queryDTO.getUseCache())) {
    // cacheService.cacheOverviewData(queryDTO.getKsjhdm(), queryDTO.getKmmc(),
    // overview);
    // }

    // return overview;

    // } catch (Exception e) {
    // log.error("获取一分一段表总览失败", e);
    // throw new RuntimeException("获取一分一段表总览失败: " + e.getMessage());
    // }
    // }

    @Override
    public GradeAdjustmentResultVO previewGradeAdjustment(GradeAdjustmentRequestDTO requestDTO) {
        // 兼容新的数据结构，获取第一个市州的数据进行预览
        AdjustedSzsmcDTO firstCityData = requestDTO.getFirstCityData();
        if (firstCityData == null) {
            throw new IllegalArgumentException("市州调整数据不能为空");
        }

        log.info("预览等级调整: ksjhdm={}, kmmc={}, szsmc={}",
                requestDTO.getKsjhdm(), requestDTO.getKmmc(), firstCityData.getSzsmc());

        long startTime = System.currentTimeMillis();

        try {
            // 获取原始等级分布
            List<GradeAdjustmentResultVO.GradeDistributionData> originalDistribution = getCurrentGradeDistribution(
                    requestDTO.getKsjhdm(), requestDTO.getKmmc(), firstCityData.getSzsmc());

            // 获取原始分界线
            Map<String, BigDecimal> originalThresholds = extractThresholds(originalDistribution);

            // 合并原始分界线和调整分界线（支持部分等级调整）
            Map<String, BigDecimal> completeAdjustedThresholds = mergeThresholds(originalThresholds,
                    firstCityData.getAdjustedThresholds());
            firstCityData.setAdjustedThresholds(completeAdjustedThresholds);

            // 验证调整幅度
            boolean isValid = firstCityData.validateAdjustmentLevel();
            String warningLevel = isValid ? "NORMAL" : "ERROR";
            String warningMessage = generateWarningMessage(warningLevel, originalThresholds,
                    completeAdjustedThresholds);

            // 计算调整后的等级分布
            List<GradeAdjustmentResultVO.GradeDistributionData> adjustedDistribution = calculateAdjustedDistribution(
                    requestDTO, originalDistribution);

            // 计算变化统计
            GradeAdjustmentResultVO.GradeChangeStatistics changeStats = calculateChangeStatistics(originalDistribution,
                    adjustedDistribution);

            long processingTime = System.currentTimeMillis() - startTime;

            return GradeAdjustmentResultVO.builder()
                    .ksjhdm(requestDTO.getKsjhdm())
                    .kmmc(requestDTO.getKmmc())
                    .szsmc(firstCityData.getSzsmc())
                    .success(true)
                    .warningLevel(warningLevel)
                    .warningMessage(warningMessage)
                    .originalDistribution(originalDistribution)
                    .adjustedDistribution(adjustedDistribution)
                    .changeStatistics(changeStats)
                    .affectedStudentCount(changeStats.getUpgradedStudents() + changeStats.getDowngradedStudents())
                    .processingTime(processingTime)
                    // 预览模式结果
                    .operationTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build();

        } catch (Exception e) {
            log.error("预览等级调整失败", e);
            return GradeAdjustmentResultVO.builder()
                    .success(false)
                    .warningLevel("DANGER")
                    .warningMessage("预览失败: " + e.getMessage())
                    // 预览模式结果
                    .build();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GradeAdjustmentConfirmResultVO confirmGradeAdjustment(GradeAdjustmentRequestDTO requestDTO) {
        // 获取第一个市州的数据用于单市州调整
        AdjustedSzsmcDTO firstCityData = requestDTO.getFirstCityData();
        log.info("确认等级调整: ksjhdm={}, kmmc={}, szsmc={}",
                requestDTO.getKsjhdm(), requestDTO.getKmmc(), firstCityData.getSzsmc());

        long startTime = System.currentTimeMillis();

        try {
            // 先进行预览计算
            GradeAdjustmentResultVO gradeAdjustmentResult = previewGradeAdjustment(requestDTO);
            if (!gradeAdjustmentResult.getSuccess()) {
                // 如果等级调整预览失败，返回失败结果
                GradeAdjustmentConfirmResultVO confirmResult = new GradeAdjustmentConfirmResultVO();
                confirmResult.setSuccess(false);
                confirmResult.setMessage(gradeAdjustmentResult.getWarningMessage());
                confirmResult.setGradeAdjustmentResult(gradeAdjustmentResult);
                return confirmResult;
            }

            // 获取一分一段数据变化预览
            ScoreSegmentChangePreviewVO scoreSegmentChangePreview = previewScoreSegmentChanges(requestDTO);

            // 更新WCXX表中的等级分界线
            updateGradeThresholds(requestDTO);

            // 更新WCXX表中一分一段数据的DJM字段
            updateWcxxScoreSegmentGrades(requestDTO);

            // 重新计算调整后的等级分布数据，确保包含最新的累计人数信息
            // 获取原始等级分布
            List<GradeAdjustmentResultVO.GradeDistributionData> originalDistribution = getCurrentGradeDistribution(
                    requestDTO.getKsjhdm(), requestDTO.getKmmc(), firstCityData.getSzsmc());

            // 重新计算调整后的分布（使用与预览相同的逻辑）
            List<GradeAdjustmentResultVO.GradeDistributionData> updatedAdjustedDistribution = calculateAdjustedDistribution(
                    requestDTO, originalDistribution);
            gradeAdjustmentResult.setAdjustedDistribution(updatedAdjustedDistribution);

            // 使用调整后的分布数据更新DJZDF=0的等级统计数据
            recalculateGradeStatistics(requestDTO, updatedAdjustedDistribution);

            // 清除相关缓存
            cacheService.clearCache(requestDTO.getKsjhdm(), requestDTO.getKmmc());

            // 将更新后的数据写入Redis缓存
            // 缓存更新后的等级分布数据
            cacheService.cacheGradeDistribution(requestDTO.getKsjhdm(), requestDTO.getKmmc(),
                    firstCityData.getSzsmc(), updatedAdjustedDistribution);

            // 缓存更新后的等级阈值数据
            Map<String, BigDecimal> updatedThresholds = extractThresholds(updatedAdjustedDistribution);
            cacheService.cacheGradeThresholds(requestDTO.getKsjhdm(), requestDTO.getKmmc(),
                    firstCityData.getSzsmc(), updatedThresholds);

            // 缓存更新后的一分一段数据
            List<ScoreSegmentDTO> updatedScoreSegmentData = getScoreSegmentDataFromDB(
                    requestDTO.getKsjhdm(), requestDTO.getKmmc(), firstCityData.getSzsmc());
            if (!updatedScoreSegmentData.isEmpty()) {
                cacheService.cacheScoreSegmentData(requestDTO.getKsjhdm(), requestDTO.getKmmc(),
                        firstCityData.getSzsmc(), updatedScoreSegmentData);
            }

            long processingTime = System.currentTimeMillis() - startTime;

            // 构建确认结果
            GradeAdjustmentConfirmResultVO confirmResult = new GradeAdjustmentConfirmResultVO();
            confirmResult.setSuccess(true);
            confirmResult.setMessage("等级调整确认成功");
            confirmResult.setProcessingTime(processingTime);
            confirmResult.setAffectedStudentCount(0); // WCXX表更新，不涉及学生数统计

            // 设置等级调整结果
            gradeAdjustmentResult.setProcessingTime(processingTime);
            confirmResult.setGradeAdjustmentResult(gradeAdjustmentResult);

            // 设置一分一段数据变化预览
            confirmResult.setScoreSegmentChangePreview(scoreSegmentChangePreview);

            log.info("等级调整完成: WCXX表DJM字段已更新, 处理时间={}ms", processingTime);
            return confirmResult;

        } catch (Exception e) {
            log.error("确认等级调整失败", e);
            throw new RuntimeException("等级调整失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchGradeAdjustmentResultVO batchConfirmGradeAdjustment(GradeAdjustmentRequestDTO requestDTO) {
        log.info("批量确认等级调整: ksjhdm={}, kmmc={}, 市州数量={}",
                requestDTO.getKsjhdm(), requestDTO.getKmmc(),
                requestDTO.getAdjustedszsmcs() != null ? requestDTO.getAdjustedszsmcs().size() : 0);

        long startTime = System.currentTimeMillis();
        List<BatchGradeAdjustmentResultVO.CityAdjustmentResult> cityResults = new ArrayList<>();
        int successCount = 0;
        int totalAffectedStudents = 0;

        try {
            // 验证请求数据
            if (requestDTO.getAdjustedszsmcs() == null || requestDTO.getAdjustedszsmcs().isEmpty()) {
                throw new IllegalArgumentException("市州调整数据不能为空");
            }

            // 逐个处理每个市州的调整
            for (AdjustedSzsmcDTO cityData : requestDTO.getAdjustedszsmcs()) {
                BatchGradeAdjustmentResultVO.CityAdjustmentResult cityResult = processSingleCityAdjustment(requestDTO,
                        cityData);
                cityResults.add(cityResult);

                if (cityResult.getSuccess()) {
                    successCount++;
                    totalAffectedStudents += cityResult.getAffectedStudentCount() != null
                            ? cityResult.getAffectedStudentCount()
                            : 0;
                }
            }

            long processingTime = System.currentTimeMillis() - startTime;
            boolean overallSuccess = successCount == requestDTO.getAdjustedszsmcs().size();

            return BatchGradeAdjustmentResultVO.builder()
                    .success(overallSuccess)
                    .message(overallSuccess ? "批量调整完成"
                            : String.format("部分调整失败，成功: %d/%d", successCount, requestDTO.getAdjustedszsmcs().size()))
                    .ksjhdm(requestDTO.getKsjhdm())
                    .kmmc(requestDTO.getKmmc())
                    .totalProcessingTime(processingTime)
                    .successCount(successCount)
                    .failureCount(requestDTO.getAdjustedszsmcs().size() - successCount)
                    .totalAffectedStudentCount(totalAffectedStudents)
                    .cityResults(cityResults)
                    .build();

        } catch (Exception e) {
            log.error("批量确认等级调整失败", e);
            long processingTime = System.currentTimeMillis() - startTime;

            return BatchGradeAdjustmentResultVO.builder()
                    .success(false)
                    .message("批量调整失败: " + e.getMessage())
                    .ksjhdm(requestDTO.getKsjhdm())
                    .kmmc(requestDTO.getKmmc())
                    .totalProcessingTime(processingTime)
                    .successCount(successCount)
                    .failureCount(requestDTO.getAdjustedszsmcs().size() - successCount)
                    .totalAffectedStudentCount(totalAffectedStudents)
                    .cityResults(cityResults)
                    .build();
        }
    }

    /**
     * 处理单个市州的等级调整
     */
    private BatchGradeAdjustmentResultVO.CityAdjustmentResult processSingleCityAdjustment(
            GradeAdjustmentRequestDTO originalRequest, AdjustedSzsmcDTO cityData) {
        long cityStartTime = System.currentTimeMillis();

        try {
            // 创建单个市州的调整请求
            GradeAdjustmentRequestDTO singleCityRequest = GradeAdjustmentRequestDTO.builder()
                    .ksjhdm(originalRequest.getKsjhdm())
                    .kmmc(originalRequest.getKmmc())
                    .adjustedszsmcs(Collections.singletonList(cityData))
                    .operatorName(originalRequest.getOperatorName())
                    .operatorCode(originalRequest.getOperatorCode())
                    .adjustmentReason(originalRequest.getAdjustmentReason())
                    .build();

            // 执行单个市州的调整（使用现有的确认方法）
            GradeAdjustmentConfirmResultVO confirmResult = confirmGradeAdjustment(singleCityRequest);

            long cityProcessingTime = System.currentTimeMillis() - cityStartTime;

            return BatchGradeAdjustmentResultVO.CityAdjustmentResult.builder()
                    .szsmc(cityData.getSzsmc())
                    .success(confirmResult.getSuccess())
                    .message(confirmResult.getMessage())
                    .processingTime(cityProcessingTime)
                    .affectedStudentCount(confirmResult.getAffectedStudentCount())
                    .gradeAdjustmentResult(confirmResult.getGradeAdjustmentResult())
                    .scoreSegmentChangePreview(confirmResult.getScoreSegmentChangePreview())
                    .build();

        } catch (Exception e) {
            log.error("处理市州 {} 的等级调整失败", cityData.getSzsmc(), e);
            long cityProcessingTime = System.currentTimeMillis() - cityStartTime;

            return BatchGradeAdjustmentResultVO.CityAdjustmentResult.builder()
                    .szsmc(cityData.getSzsmc())
                    .success(false)
                    .message("调整失败: " + e.getMessage())
                    .processingTime(cityProcessingTime)
                    .affectedStudentCount(0)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    public List<ExamPlanSubjectStatisticsVO> getHistoricalExamPlans() {
        try {
            return kscjMapper.getHistoricalExamPlansWithScores();
        } catch (Exception e) {
            log.error("获取历史考试计划失败", e);
            throw new RuntimeException("获取历史考试计划失败: " + e.getMessage());
        }
    }

    // @Override
    // public Boolean preCalculateScoreSegment(String ksjhdm, String kmmc) {
    // log.info("预计算一分一段表: ksjhdm={}, kmmc={}", ksjhdm, kmmc);

    // // 尝试获取锁
    // if (!cacheService.tryLockPreCalculation(ksjhdm, kmmc)) {
    // log.warn("预计算已在进行中: ksjhdm={}, kmmc={}", ksjhdm, kmmc);
    // return false;
    // }

    // try {
    // // 设置计算状态
    // cacheService.setCalculationStatus(ksjhdm, kmmc, "CALCULATING");

    // // 预计算总览数据
    // ScoreSegmentQueryDTO queryDTO = ScoreSegmentQueryDTO.builder()
    // .ksjhdm(ksjhdm)
    // .kmmc(kmmc)
    // .useCache(false)
    // .build();

    // getScoreSegmentOverview(queryDTO);

    // // 设置完成状态
    // cacheService.setCalculationStatus(ksjhdm, kmmc, "COMPLETED");

    // log.info("预计算完成: ksjhdm={}, kmmc={}", ksjhdm, kmmc);
    // return true;

    // } catch (Exception e) {
    // log.error("预计算失败: ksjhdm={}, kmmc={}", ksjhdm, kmmc, e);
    // cacheService.setCalculationStatus(ksjhdm, kmmc, "FAILED");
    // return false;
    // } finally {
    // // 释放锁
    // cacheService.releaseLockPreCalculation(ksjhdm, kmmc);
    // }
    // }

    @Override
    public void clearScoreSegmentCache(String ksjhdm, String kmmc) {
        log.info("清除一分一段表缓存: ksjhdm={}, kmmc={}", ksjhdm, kmmc);
        cacheService.clearCache(ksjhdm, kmmc);
    }

    @Override
    public String getCalculationStatus(String ksjhdm, String kmmc) {
        return cacheService.getCalculationStatus(ksjhdm, kmmc);
    }

    @Override
    public byte[] exportScoreSegmentToExcel(ScoreSegmentQueryDTO queryDTO) {
        // TODO: 实现Excel导出功能
        throw new UnsupportedOperationException("Excel导出功能待实现");
    }

    @Override
    public String batchPreCalculate(List<String> examPlans) {
        // TODO: 实现批量预计算功能
        throw new UnsupportedOperationException("批量预计算功能待实现");
    }

    @Override
    public GradeAssignmentProgressVO getBatchCalculationProgress(String taskId) {
        // TODO: 实现批量计算进度查询
        throw new UnsupportedOperationException("批量计算进度查询待实现");
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 计算一分一段数据
     */
    private List<ScoreSegmentDTO> calculateScoreSegments(String ksjhdm, String kmmc,
                                                         List<String> cities, Integer limit) {
        List<ScoreSegmentDTO> segments = new ArrayList<>();

        try {
            log.info("=== 开始计算一分一段数据 ===");
            log.info("查询参数: ksjhdm={}, kmmc={}, cities={}", ksjhdm, kmmc, cities);

            // 始终获取全省分数范围作为基准
            Map<String, Object> scoreRange = kscjMapper.getScoreRange(ksjhdm, kmmc, null);
            if (scoreRange == null || scoreRange.isEmpty()) {
                log.warn("未找到全省分数范围数据: ksjhdm={}, kmmc={}", ksjhdm, kmmc);
                return segments;
            }

            BigDecimal minScore = convertToBigDecimal(scoreRange.get("min_score"));
            BigDecimal maxScore = convertToBigDecimal(scoreRange.get("max_score"));

            if (minScore == null || maxScore == null) {
                log.warn("分数范围数据无效: minScore={}, maxScore={}", minScore, maxScore);
                return segments;
            }

            log.info("分数范围: {} - {}", minScore, maxScore);

            if (cities == null || cities.isEmpty()) {
                // 处理全省数据：为每个市州单独计算一分一段
                List<String> allCities = kscjMapper.selectCitiesForGradeAssignment(ksjhdm, kmmc);
                log.info("处理全省数据，共{}个市州，使用全省分数范围: {} - {}", allCities.size(), minScore, maxScore);

                for (String cityName : allCities) {
                    List<ScoreSegmentDTO> citySegments = calculateCityScoreSegments(
                            ksjhdm, kmmc, cityName, minScore.intValue(), maxScore.intValue());
                    segments.addAll(citySegments);

                    // 检查限制
                    if (limit != null && segments.size() >= limit) {
                        log.info("达到限制数量 {}, 停止生成", limit);
                        break;
                    }
                }
            } else {
                // 处理单个市州数据，使用全省分数范围
                String cityName = cities.get(0);
                log.info("处理单个市州数据: {}，使用全省分数范围: {} - {}", cityName, minScore, maxScore);
                List<ScoreSegmentDTO> citySegments = calculateCityScoreSegments(
                        ksjhdm, kmmc, cityName, minScore.intValue(), maxScore.intValue());
                segments.addAll(citySegments);
            }

            log.info("生成一分一段数据完成，共 {} 条记录", segments.size());
            return segments;

        } catch (Exception e) {
            log.error("计算一分一段数据时发生错误", e);
            throw new RuntimeException("计算一分一段数据失败", e);
        }
    }

    /**
     * 为单个市州计算一分一段数据
     */
    private List<ScoreSegmentDTO> calculateCityScoreSegments(String ksjhdm, String kmmc,
                                                             String cityName, int minScore, int maxScore) {
        List<ScoreSegmentDTO> segments = new ArrayList<>();

        // 获取该市州的学生成绩数据
        List<StudentScoreRankDTO> cityStudents = kscjMapper.getStudentScoreRanks(
                ksjhdm, kmmc, Collections.singletonList(cityName));

        if (cityStudents == null || cityStudents.isEmpty()) {
            log.warn("市州 {} 未找到学生成绩数据，生成全部count=0的记录", cityName);
            // 即使没有学生数据，也要生成空的分数段记录
            for (int score = maxScore; score >= minScore; score--) {
                ScoreSegmentDTO segment = ScoreSegmentDTO.builder()
                        .score(BigDecimal.valueOf(score))
                        .count(0)
                        .cumulativeCount(0)
                        .cumulativePercentage(BigDecimal.ZERO)
                        .grade(null)
                        .rank(0)
                        .ksjhdm(ksjhdm)
                        .kmmc(kmmc)
                        .szsmc(cityName)
                        .build();
                segments.add(segment);
            }
            return segments;
        }

        log.debug("市州 {} 查询到学生成绩数据: {} 条", cityName, cityStudents.size());

        // 使用动态等级计算算法重新计算等级
        try {
            // 计算等级阈值并分配等级（一次性完成，避免重复计算）
            GradeThresholdsDTO thresholds = GradeCalculationAlgorithm.calculateGradeThresholds(
                    cityStudents,
                    gradeAssignmentProperties.getGradeConfig().getGradeRatios(),
                    cityName,
                    kmmc,
                    gradeAssignmentProperties.isUseEGradePriorityAlgorithm(),
                    gradeAssignmentProperties.getEGradeMinPercentage());

            // 为学生分配等级
            if (thresholds != null) {
                gradeCalculationAlgorithm.assignGradesToStudents(cityStudents, thresholds);
                log.debug("市州 {} 完成动态等级计算", cityName);
            } else {
                log.warn("市州 {} 等级阈值计算失败，使用原有等级数据", cityName);
            }
        } catch (Exception e) {
            log.error("市州 {} 动态等级计算失败: {}", cityName, e.getMessage(), e);
            // 如果动态等级计算失败，继续使用原有等级数据
        }

        // 按分数分组（成绩数据已经是整数）
        Map<Integer, List<StudentScoreRankDTO>> scoreGroups = new HashMap<>();
        for (StudentScoreRankDTO student : cityStudents) {
            // 成绩数据在导入时已经四舍五入为整数，直接转换
            int intScore = student.getFslkscj().intValue();
            scoreGroups.computeIfAbsent(intScore, k -> new ArrayList<>()).add(student);
        }

        int cumulativeCount = 0;
        int totalStudents = cityStudents.size();

        // 为全省分数范围内的每个整数分数生成记录
        for (int score = maxScore; score >= minScore; score--) {
            List<StudentScoreRankDTO> studentsAtScore = scoreGroups.get(score);
            int countAtScore = studentsAtScore != null ? studentsAtScore.size() : 0;
            cumulativeCount += countAtScore;

            // 计算该市州的累计百分比（动态精度方案）
            BigDecimal cumulativePercentage = totalStudents > 0
                    ? calculateNaturalPrecisionPercentage(cumulativeCount, totalStudents)
                    : BigDecimal.ZERO;

            String grade = (studentsAtScore != null && !studentsAtScore.isEmpty())
                    ? studentsAtScore.get(0).getGrade()
                    : null;

            ScoreSegmentDTO segment = ScoreSegmentDTO.builder()
                    .score(BigDecimal.valueOf(score))
                    .count(countAtScore)
                    .cumulativeCount(cumulativeCount)
                    .cumulativePercentage(cumulativePercentage)
                    .grade(grade)
                    .rank(cumulativeCount)
                    .ksjhdm(ksjhdm)
                    .kmmc(kmmc)
                    .szsmc(cityName)
                    .build();

            segments.add(segment);
        }

        log.debug("市州 {} 生成一分一段数据: {} 条", cityName, segments.size());
        return segments;
    }

    /**
     * 构建总览VO
     */
    private ScoreSegmentOverviewVO buildOverviewVO(ScoreSegmentQueryDTO queryDTO,
                                                   Map<String, Object> basicStats,
                                                   List<Map<String, Object>> gradeStats,
                                                   List<Map<String, Object>> cityStats,
                                                   List<ScoreSegmentDTO> segmentData) {

        // 构建等级分布
        Map<String, ScoreSegmentOverviewVO.GradeStatistics> gradeDistribution = new HashMap<>();
        for (Map<String, Object> grade : gradeStats) {
            String gradeCode = (String) grade.get("djm");
            ScoreSegmentOverviewVO.GradeStatistics gradeStat = ScoreSegmentOverviewVO.GradeStatistics.builder()
                    .gradeCode(gradeCode)
                    .gradeName(getGradeName(gradeCode))
                    .count(grade.get("rs") != null ? ((Number) grade.get("rs")).intValue() : 0)
                    .percentage(convertToBigDecimal(grade.get("bl")).setScale(1, RoundingMode.HALF_UP))
                    .threshold(convertToBigDecimal(grade.get("fsx")))
                    .adjustable(true)
                    .build();
            gradeDistribution.put(gradeCode, gradeStat);
        }

        // 构建市州分布
        List<ScoreSegmentOverviewVO.CityStatistics> cityDistribution = cityStats.stream()
                .map(city -> ScoreSegmentOverviewVO.CityStatistics.builder()
                        .szsmc((String) city.get("szsmc"))
                        .count(city.get("count") != null ? ((Number) city.get("count")).intValue() : 0)
                        .percentage(calculatePercentage(
                                city.get("count") != null ? ((Number) city.get("count")).intValue() : 0,
                                basicStats.get("totalCount") != null
                                        ? ((Number) basicStats.get("totalCount")).intValue()
                                        : 0))
                        .avgScore(convertToBigDecimal(city.get("avgScore")))
                        .gradeDistribution(new HashMap<>()) // TODO: 实现市州等级分布
                        .build())
                .collect(Collectors.toList());

        // 转换分数段数据
        List<ScoreSegmentOverviewVO.ScoreSegmentData> segmentDataVO = segmentData.stream()
                .map(segment -> ScoreSegmentOverviewVO.ScoreSegmentData.builder()
                        .score(segment.getScore())
                        .count(segment.getCount())
                        .cumulativeCount(segment.getCumulativeCount())
                        .cumulativePercentage(segment.getCumulativePercentage())
                        .grade(segment.getGrade())
                        // 数据库中没有存储rank字段，因此不设置rank
                        // .rank(segment.getRank())
                        .build())
                .collect(Collectors.toList());

        return ScoreSegmentOverviewVO.builder()
                .ksjhdm(queryDTO.getKsjhdm())
                .ksjhmc((String) basicStats.get("ksjhmc"))
                .kmmc(queryDTO.getKmmc())
                .totalCount(
                        basicStats.get("totalCount") != null ? ((Number) basicStats.get("totalCount")).intValue() : 0)
                .maxScore(convertToBigDecimal(basicStats.get("maxScore")))
                .minScore(convertToBigDecimal(basicStats.get("minScore")))
                .avgScore(convertToBigDecimal(basicStats.get("avgScore")))
                .gradeDistribution(gradeDistribution)
                .cityDistribution(cityDistribution)
                .segmentData(segmentDataVO)
                .updateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .adjustable(true)
                .build();
    }

    // 其他辅助方法的实现...
    private ScoreSegmentOverviewVO generateCityOverview(ScoreSegmentQueryDTO queryDTO, String city) {
        try {
            // 获取该市州的基础统计数据（使用市州统计方法）
            List<Map<String, Object>> cityStatsList = kscjMapper.getCityStatistics(
                    queryDTO.getKsjhdm(), queryDTO.getKmmc());

            // 从市州统计列表中找到当前市州的数据
            Map<String, Object> basicStats = cityStatsList.stream()
                    .filter(stats -> city.equals(stats.get("szsmc")))
                    .findFirst()
                    .orElse(null);

            if (basicStats == null) {
                return null;
            }

            // 获取该市州的等级分布（使用支持市州参数的方法）
            List<Map<String, Object>> gradeStats = kscjMapper.selectGradeDistributionStats(
                    queryDTO.getKsjhdm(), queryDTO.getKmmc(), city);

            // 获取该市州的一分一段数据
            List<ScoreSegmentDTO> segmentData = calculateScoreSegments(
                    queryDTO.getKsjhdm(), queryDTO.getKmmc(), List.of(city), null);

            // 应用过滤条件
            segmentData = applyFilters(segmentData, queryDTO);

            // 应用排序
            segmentData = applySorting(segmentData, queryDTO.getSortBy());

            // 应用分页（对于总览，限制为前50条）
            segmentData = applyPagination(segmentData, 1, 50);

            // 构建市州总览VO
            return buildCityOverviewVO(queryDTO, city, basicStats, gradeStats, segmentData);

        } catch (Exception e) {
            log.error("生成市州总览数据失败: city={}", city, e);
            return null;
        }
    }

    private List<ScoreSegmentDTO> applyFilters(List<ScoreSegmentDTO> data, ScoreSegmentQueryDTO queryDTO) {
        log.info("应用过滤条件前数据量: {}, 过滤条件: minScore={}, maxScore={}, gradeFilter={}",
                data.size(), queryDTO.getMinScore(), queryDTO.getMaxScore(), queryDTO.getGradeFilter());

        List<ScoreSegmentDTO> filteredData = data.stream()
                .filter(segment -> {
                    // 分数范围过滤
                    if (queryDTO.getMinScore() != null
                            && segment.getScore().compareTo(BigDecimal.valueOf(queryDTO.getMinScore())) < 0) {
                        return false;
                    }
                    if (queryDTO.getMaxScore() != null
                            && segment.getScore().compareTo(BigDecimal.valueOf(queryDTO.getMaxScore())) > 0) {
                        return false;
                    }

                    // 等级过滤
                    if (queryDTO.getGradeFilter() != null && !queryDTO.getGradeFilter().isEmpty()) {
                        // 如果分数段没有等级（即没有学生），则保留该记录
                        // 只对有等级的记录进行过滤
                        return segment.getGrade() == null || queryDTO.getGradeFilter().contains(segment.getGrade());
                    }

                    return true;
                })
                .collect(Collectors.toList());

        log.info("应用过滤条件后数据量: {}", filteredData.size());
        return filteredData;
    }

    private List<ScoreSegmentDTO> applySorting(List<ScoreSegmentDTO> data, String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            return data; // 默认已按分数降序排列
        }

        return switch (sortBy.toLowerCase()) {
            case "score_asc" -> data.stream()
                    .sorted(Comparator.comparing(ScoreSegmentDTO::getScore))
                    .collect(Collectors.toList());
            case "score_desc" -> data.stream()
                    .sorted(Comparator.comparing(ScoreSegmentDTO::getScore).reversed())
                    .collect(Collectors.toList());
            case "count_asc" -> data.stream()
                    .sorted(Comparator.comparing(ScoreSegmentDTO::getCount))
                    .collect(Collectors.toList());
            case "count_desc" -> data.stream()
                    .sorted(Comparator.comparing(ScoreSegmentDTO::getCount).reversed())
                    .collect(Collectors.toList());
            case "rank_asc" -> data.stream()
                    .sorted(Comparator.comparing(ScoreSegmentDTO::getRank))
                    .collect(Collectors.toList());
            case "rank_desc" -> data.stream()
                    .sorted(Comparator.comparing(ScoreSegmentDTO::getRank).reversed())
                    .collect(Collectors.toList());
            default -> data;
        };
    }

    private List<ScoreSegmentDTO> applyPagination(List<ScoreSegmentDTO> data, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageSize == null || pageNum <= 0 || pageSize <= 0) {
            return data;
        }

        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, data.size());

        if (startIndex >= data.size()) {
            return new ArrayList<>();
        }

        return data.subList(startIndex, endIndex);
    }

    private List<GradeAdjustmentResultVO.GradeDistributionData> getCurrentGradeDistribution(
            String ksjhdm, String kmmc, String szsmc) {
        try {
            // 先尝试从Redis缓存获取
            List<GradeAdjustmentResultVO.GradeDistributionData> cachedData = cacheService
                    .getCachedGradeDistribution(ksjhdm, kmmc, szsmc);
            if (cachedData != null && !cachedData.isEmpty()) {
                log.debug("从缓存获取等级分布数据: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc);
                return cachedData;
            }

            // 缓存中没有数据，从数据库查询
            List<Map<String, Object>> gradeStats = wcxxMapper.getGradeDistribution(ksjhdm, kmmc, szsmc);
            List<GradeAdjustmentResultVO.GradeDistributionData> result = gradeStats.stream()
                    .map(stat -> {
                        GradeAdjustmentResultVO.GradeDistributionData data = new GradeAdjustmentResultVO.GradeDistributionData();
                        data.setGrade((String) stat.get("djm"));
                        data.setThreshold(convertToBigDecimal(stat.get("fsx")));
                        data.setCount(((Number) stat.get("rs")).intValue());
                        data.setPercentage(convertToBigDecimal(stat.get("bl")));
                        // 设置累计人数
                        Object ljrsObj = stat.get("ljrs");
                        if (ljrsObj != null) {
                            data.setCumulativeCount(((Number) ljrsObj).intValue());
                        }
                        return data;
                    })
                    .collect(Collectors.toList());

            // 将查询结果缓存到Redis
            if (!result.isEmpty()) {
                cacheService.cacheGradeDistribution(ksjhdm, kmmc, szsmc, result);
                log.debug("缓存等级分布数据: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc);
            }

            return result;
        } catch (Exception e) {
            log.error("获取当前等级分布失败: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);
            return new ArrayList<>();
        }
    }

    private Map<String, BigDecimal> extractThresholds(
            List<GradeAdjustmentResultVO.GradeDistributionData> distribution) {
        // 使用merge函数处理重复键，取第一个值
        return distribution.stream()
                .collect(Collectors.toMap(
                        GradeAdjustmentResultVO.GradeDistributionData::getGrade,
                        GradeAdjustmentResultVO.GradeDistributionData::getThreshold,
                        (existing, replacement) -> {
                            log.warn("发现重复等级: {}, 现有值: {}, 替换值: {}, 保留现有值",
                                    existing, replacement);
                            return existing;
                        }));
    }

    /**
     * 合并原始分界线和调整分界线，支持部分等级调整
     * 如果adjustedThresholds中没有某个等级的分界线，则使用原始分界线
     *
     * @param originalThresholds 原始分界线
     * @param adjustedThresholds 调整分界线（可能只包含部分等级）
     * @return 完整的调整分界线
     */
    private Map<String, BigDecimal> mergeThresholds(
            Map<String, BigDecimal> originalThresholds,
            Map<String, BigDecimal> adjustedThresholds) {

        Map<String, BigDecimal> mergedThresholds = new HashMap<>(originalThresholds);

        // 如果adjustedThresholds不为空，则用调整值覆盖对应等级的分界线
        if (adjustedThresholds != null && !adjustedThresholds.isEmpty()) {
            adjustedThresholds.forEach((grade, threshold) -> {
                if (threshold != null) {
                    mergedThresholds.put(grade, threshold);
                }
            });
        }

        log.info("合并分界线 - 原始: {}, 调整: {}, 合并后: {}",
                originalThresholds, adjustedThresholds, mergedThresholds);

        return mergedThresholds;
    }

    /**
     * 获取缺失的等级列表
     *
     * @param thresholds 等级分界线Map
     * @return 缺失的等级列表字符串
     */
    private String getMissingGrades(Map<String, BigDecimal> thresholds) {
        List<String> missingGrades = new ArrayList<>();
        String[] allGrades = {"A", "B", "C", "D"};

        for (String grade : allGrades) {
            if (thresholds.get(grade) == null) {
                missingGrades.add(grade);
            }
        }

        return String.join(", ", missingGrades);
    }

    private String generateWarningMessage(String warningLevel,
                                          Map<String, BigDecimal> original,
                                          Map<String, BigDecimal> adjusted) {
        if ("NONE".equals(warningLevel)) {
            return "等级调整幅度正常，可以安全执行。";
        } else if ("WARNING".equals(warningLevel)) {
            return "等级调整幅度较大，请仔细检查调整参数。建议先进行预览确认。";
        } else if ("DANGER".equals(warningLevel)) {
            return "等级调整幅度过大，可能对学生成绩产生重大影响。强烈建议重新评估调整参数。";
        }
        return "";
    }

    private List<GradeAdjustmentResultVO.GradeDistributionData> calculateAdjustedDistribution(
            GradeAdjustmentRequestDTO requestDTO,
            List<GradeAdjustmentResultVO.GradeDistributionData> original) {
        // 获取第一个市州的数据
        AdjustedSzsmcDTO firstCityData = requestDTO.getFirstCityData();
        Map<String, BigDecimal> adjustedThresholds = firstCityData.getAdjustedThresholds();

        try {
            // 获取学生成绩数据
            List<StudentScoreRankDTO> students = kscjMapper.getStudentScoreRanks(
                    requestDTO.getKsjhdm(), requestDTO.getKmmc(),
                    Collections.singletonList(firstCityData.getSzsmc()));

            if (students == null || students.isEmpty()) {
                log.warn("未找到学生成绩数据，使用原始分布");
                return original.stream()
                        .map(data -> {
                            GradeAdjustmentResultVO.GradeDistributionData adjusted = new GradeAdjustmentResultVO.GradeDistributionData();
                            adjusted.setGrade(data.getGrade());
                            BigDecimal newThreshold = adjustedThresholds.get(data.getGrade());
                            adjusted.setThreshold(newThreshold != null ? newThreshold : data.getThreshold());
                            adjusted.setCount(data.getCount());
                            adjusted.setPercentage(data.getPercentage());
                            return adjusted;
                        })
                        .collect(Collectors.toList());
            }

            // 构建完整的分界线映射（包含原始和调整后的）
            Map<String, BigDecimal> allThresholds = new HashMap<>();
            for (GradeAdjustmentResultVO.GradeDistributionData data : original) {
                allThresholds.put(data.getGrade(), data.getThreshold());
            }
            allThresholds.putAll(adjustedThresholds);

            // 根据新分界线重新统计各等级人数
            Map<String, Integer> newCounts = new HashMap<>();
            for (String grade : Arrays.asList("A", "B", "C", "D", "E")) {
                newCounts.put(grade, 0);
            }

            int totalStudents = students.size();
            for (StudentScoreRankDTO student : students) {
                String newGrade = determineGradeByThresholds(student.getFslkscj(), allThresholds);
                newCounts.put(newGrade, newCounts.get(newGrade) + 1);
            }

            // 构建调整后的分布数据并计算累计百分比
            List<GradeAdjustmentResultVO.GradeDistributionData> adjustedList = new ArrayList<>();
            int cumulativeCount = 0;

            // 按等级顺序A、B、C、D、E处理
            for (String grade : Arrays.asList("A", "B", "C", "D", "E")) {
                GradeAdjustmentResultVO.GradeDistributionData originalData = original.stream()
                        .filter(data -> grade.equals(data.getGrade()))
                        .findFirst()
                        .orElse(null);

                if (originalData != null) {
                    GradeAdjustmentResultVO.GradeDistributionData adjusted = new GradeAdjustmentResultVO.GradeDistributionData();
                    adjusted.setGrade(grade);

                    // 使用调整后的分界线
                    BigDecimal newThreshold = adjustedThresholds.get(grade);
                    BigDecimal finalThreshold = newThreshold != null ? newThreshold : originalData.getThreshold();
                    adjusted.setThreshold(finalThreshold);

                    // 使用重新计算的人数
                    Integer newCount = newCounts.get(grade);
                    adjusted.setCount(newCount != null ? newCount : 0);

                    // 累计人数
                    cumulativeCount += adjusted.getCount();
                    adjusted.setCumulativeCount(cumulativeCount);

                    // 计算累计百分比（保留一位小数）
                    BigDecimal cumulativePercentage = totalStudents > 0
                            ? calculateNaturalPrecisionPercentage(cumulativeCount, totalStudents)
                            : BigDecimal.ZERO;
                    adjusted.setPercentage(cumulativePercentage);

                    // 计算分界线变化
                    BigDecimal thresholdChange = finalThreshold.subtract(originalData.getThreshold());
                    adjusted.setThresholdChange(thresholdChange);

                    // 计算人数变化
                    Integer countChange = adjusted.getCount() - originalData.getCount();
                    adjusted.setCountChange(countChange);

                    adjustedList.add(adjusted);
                }
            }

            return adjustedList;

        } catch (Exception e) {
            log.error("重新计算等级分布失败，使用原始数据: {}", e.getMessage());
            return original.stream()
                    .map(data -> {
                        GradeAdjustmentResultVO.GradeDistributionData adjusted = new GradeAdjustmentResultVO.GradeDistributionData();
                        adjusted.setGrade(data.getGrade());
                        BigDecimal newThreshold = adjustedThresholds.get(data.getGrade());
                        adjusted.setThreshold(newThreshold != null ? newThreshold : data.getThreshold());
                        adjusted.setCount(data.getCount());
                        adjusted.setPercentage(data.getPercentage());
                        return adjusted;
                    })
                    .collect(Collectors.toList());
        }
    }

    private GradeAdjustmentResultVO.GradeChangeStatistics calculateChangeStatistics(
            List<GradeAdjustmentResultVO.GradeDistributionData> original,
            List<GradeAdjustmentResultVO.GradeDistributionData> adjusted) {
        GradeAdjustmentResultVO.GradeChangeStatistics statistics = new GradeAdjustmentResultVO.GradeChangeStatistics();

        Map<String, Integer> originalCounts = original.stream()
                .collect(Collectors.toMap(
                        GradeAdjustmentResultVO.GradeDistributionData::getGrade,
                        GradeAdjustmentResultVO.GradeDistributionData::getCount));

        Map<String, Integer> adjustedCounts = adjusted.stream()
                .collect(Collectors.toMap(
                        GradeAdjustmentResultVO.GradeDistributionData::getGrade,
                        GradeAdjustmentResultVO.GradeDistributionData::getCount));

        Map<String, BigDecimal> originalThresholds = original.stream()
                .collect(Collectors.toMap(
                        GradeAdjustmentResultVO.GradeDistributionData::getGrade,
                        GradeAdjustmentResultVO.GradeDistributionData::getThreshold));

        Map<String, BigDecimal> adjustedThresholds = adjusted.stream()
                .collect(Collectors.toMap(
                        GradeAdjustmentResultVO.GradeDistributionData::getGrade,
                        GradeAdjustmentResultVO.GradeDistributionData::getThreshold));

        // 计算各等级变化
        Map<String, Integer> gradeChanges = new HashMap<>();
        List<BigDecimal> thresholdChanges = new ArrayList<>();
        Map<String, String> changeDetails = new HashMap<>();

        for (String grade : originalCounts.keySet()) {
            int countChange = adjustedCounts.getOrDefault(grade, 0) - originalCounts.get(grade);
            gradeChanges.put(grade, countChange);

            // 计算分界线变化
            BigDecimal originalThreshold = originalThresholds.get(grade);
            BigDecimal adjustedThreshold = adjustedThresholds.get(grade);
            if (originalThreshold != null && adjustedThreshold != null) {
                BigDecimal thresholdChange = adjustedThreshold.subtract(originalThreshold);
                thresholdChanges.add(thresholdChange.abs());

                // 构建变化详情
                String changeDetail = String.format("%s等级: 分界线从%.1f调整到%.1f(变化%.1f), 人数变化%d人",
                        grade, originalThreshold, adjustedThreshold, thresholdChange, countChange);
                changeDetails.put(grade, changeDetail);
            }
        }

        statistics.setGradeChanges(gradeChanges);
        statistics.setTotalAffected(gradeChanges.values().stream().mapToInt(Math::abs).sum());
        statistics.setUpgradeCount(gradeChanges.values().stream().mapToInt(v -> Math.max(0, v)).sum());
        statistics.setDowngradeCount(gradeChanges.values().stream().mapToInt(v -> Math.max(0, -v)).sum());

        // 计算分界线调整统计
        if (!thresholdChanges.isEmpty()) {
            statistics.setMaxThresholdAdjustment(
                    thresholdChanges.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            BigDecimal avgAdjustment = thresholdChanges.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(thresholdChanges.size()), 2, RoundingMode.HALF_UP);
            statistics.setAvgThresholdAdjustment(avgAdjustment);
        } else {
            statistics.setMaxThresholdAdjustment(BigDecimal.ZERO);
            statistics.setAvgThresholdAdjustment(BigDecimal.ZERO);
        }

        // 设置变化详情
        changeDetails.put("summary", String.format("共影响%d名学生，其中%d人等级提升，%d人等级降低",
                statistics.getTotalAffected(), statistics.getUpgradeCount(), statistics.getDowngradeCount()));
        changeDetails.put("impact",
                statistics.getTotalAffected() > 1000 ? "高影响" : statistics.getTotalAffected() > 500 ? "中等影响" : "低影响");
        statistics.setChangeDetails(changeDetails);

        return statistics;
    }

    private void updateGradeThresholds(GradeAdjustmentRequestDTO requestDTO) {
        try {
            // 获取第一个市州的数据
            AdjustedSzsmcDTO firstCityData = requestDTO.getFirstCityData();
            wcxxMapper.updateGradeThresholds(
                    requestDTO.getKsjhdm(),
                    requestDTO.getKmmc(),
                    firstCityData.getSzsmc(),
                    firstCityData.getAdjustedThresholds(),
                    requestDTO.getOperatorName(),
                    requestDTO.getOperatorCode());
        } catch (Exception e) {
            log.error("更新等级分界线失败: {}", requestDTO, e);
            throw new RuntimeException("更新等级分界线失败", e);
        }
    }

    private int updateStudentGrades(GradeAdjustmentRequestDTO requestDTO) {
        try {
            // 获取第一个市州的数据
            AdjustedSzsmcDTO firstCityData = requestDTO.getFirstCityData();

            // 根据新的分界线重新计算学生等级
            List<String> cities = firstCityData.getSzsmc() != null ? List.of(firstCityData.getSzsmc()) : null;
            List<StudentScoreRankDTO> students = kscjMapper.getStudentScoreRanks(
                    requestDTO.getKsjhdm(),
                    requestDTO.getKmmc(),
                    cities);

            // 批量更新学生等级
            Map<String, BigDecimal> thresholds = firstCityData.getAdjustedThresholds();

            // 确保所有等级的分界线都存在，如果不存在则抛出异常
            BigDecimal gradeAThreshold = thresholds.get("A");
            BigDecimal gradeBThreshold = thresholds.get("B");
            BigDecimal gradeCThreshold = thresholds.get("C");
            BigDecimal gradeDThreshold = thresholds.get("D");

            if (gradeAThreshold == null || gradeBThreshold == null ||
                    gradeCThreshold == null || gradeDThreshold == null) {
                throw new RuntimeException("等级分界线不完整，无法更新学生等级。缺失的等级: " +
                        getMissingGrades(thresholds));
            }

            return kscjMapper.batchUpdateGrades(
                    requestDTO.getKsjhdm(),
                    requestDTO.getKmmc(),
                    firstCityData.getSzsmc(),
                    gradeAThreshold,
                    gradeBThreshold,
                    gradeCThreshold,
                    gradeDThreshold,
                    requestDTO.getOperatorName(),
                    requestDTO.getOperatorCode());
        } catch (Exception e) {
            log.error("更新学生等级失败: {}", requestDTO, e);
            throw new RuntimeException("更新学生等级失败", e);
        }
    }

    /**
     * 更新WCXX表中一分一段数据的DJM字段
     */
    private void updateWcxxScoreSegmentGrades(GradeAdjustmentRequestDTO requestDTO) {
        try {
            // 获取第一个市州的数据
            AdjustedSzsmcDTO firstCityData = requestDTO.getFirstCityData();
            log.info("开始更新WCXX表一分一段数据DJM字段: ksjhdm={}, kmmc={}, szsmc={}",
                    requestDTO.getKsjhdm(), requestDTO.getKmmc(), firstCityData.getSzsmc());

            // 获取新的等级分界线
            Map<String, BigDecimal> thresholds = firstCityData.getAdjustedThresholds();

            // 确保所有等级的分界线都存在
            BigDecimal gradeAThreshold = thresholds.get("A");
            BigDecimal gradeBThreshold = thresholds.get("B");
            BigDecimal gradeCThreshold = thresholds.get("C");
            BigDecimal gradeDThreshold = thresholds.get("D");

            if (gradeAThreshold == null || gradeBThreshold == null ||
                    gradeCThreshold == null || gradeDThreshold == null) {
                throw new RuntimeException("等级分界线不完整，无法更新WCXX表DJM字段。缺失的等级: " +
                        getMissingGrades(thresholds));
            }

            // 批量更新WCXX表中一分一段数据的DJM字段
            int updatedCount = wcxxMapper.updateScoreSegmentGrades(
                    requestDTO.getKsjhdm(),
                    requestDTO.getKmmc(),
                    firstCityData.getSzsmc(),
                    gradeAThreshold,
                    gradeBThreshold,
                    gradeCThreshold,
                    gradeDThreshold);

            log.info("WCXX表一分一段数据DJM字段更新完成: 更新记录数={}", updatedCount);

        } catch (Exception e) {
            log.error("更新WCXX表一分一段数据DJM字段失败", e);
            throw new RuntimeException("更新WCXX表DJM字段失败: " + e.getMessage(), e);
        }
    }

    /**
     * 重新计算并更新DJZDF=0的等级统计数据
     * 确保等级调整后的数据一致性
     */
    private void recalculateGradeStatistics(GradeAdjustmentRequestDTO requestDTO,
                                            List<GradeAdjustmentResultVO.GradeDistributionData> adjustedDistribution) {
        try {
            // 获取第一个市州的数据
            AdjustedSzsmcDTO firstCityData = requestDTO.getFirstCityData();

            log.info("开始重新计算等级统计数据: ksjhdm={}, kmmc={}, szsmc={}",
                    requestDTO.getKsjhdm(), requestDTO.getKmmc(), firstCityData.getSzsmc());

            if (adjustedDistribution == null || adjustedDistribution.isEmpty()) {
                log.warn("调整后的等级分布数据为空，跳过更新");
                return;
            }

            // 使用调整后的等级分布数据更新WCXX表中DJZDF=0的记录
            for (GradeAdjustmentResultVO.GradeDistributionData data : adjustedDistribution) {
                // 更新WCXX表中对应等级的统计数据
                // threshold写入FSLKSCJ，count写入BFDRS，percentage写入LJBFB，cumulativeCount写入LJRS
                wcxxMapper.updateGradeStatistics(
                        requestDTO.getKsjhdm(),
                        requestDTO.getKmmc(),
                        firstCityData.getSzsmc(),
                        data.getGrade(),
                        data.getThreshold(), // fslkscj字段 - 分界线
                        0, // bfb字段暂不使用
                        data.getCount(), // bfdrs字段
                        data.getPercentage(), // ljbfb字段 - 累计百分比
                        data.getCumulativeCount(), // ljrs字段 - 累计人数
                        requestDTO.getOperatorName(),
                        requestDTO.getOperatorCode());
            }

            log.info("等级统计数据重新计算完成: 更新等级数={}", adjustedDistribution.size());

        } catch (Exception e) {
            log.error("重新计算等级统计数据失败: {}", requestDTO, e);
            throw new RuntimeException("重新计算等级统计数据失败", e);
        }
    }

    private String getGradeName(String gradeCode) {
        return switch (gradeCode) {
            case "A" -> "优秀";
            case "B" -> "良好";
            case "C" -> "合格";
            case "D" -> "不合格";
            case "E" -> "缺考";
            case "UNGRADED" -> "未分等级";
            default -> gradeCode;
        };
    }

    /**
     * 计算自然精度的百分比（动态精度方案）
     * 在一位小数限制下，避免不必要的四舍五入
     *
     * @param count 当前计数
     * @param total 总数
     * @return 自然精度的百分比，最多保留一位小数
     */
    /**
     * 根据分界线确定学生等级
     * 使用四舍五入逻辑，与一分一段统计保持一致
     */
    private String determineGradeByThresholds(BigDecimal score, Map<String, BigDecimal> thresholds) {
        if (score == null) {
            return "E";
        }

        // 将分数四舍五入为整数，与一分一段统计逻辑保持一致
        BigDecimal roundedScore = score.setScale(0, RoundingMode.HALF_UP);

        // 按等级顺序检查分界线（分界线也四舍五入为整数）
        if (thresholds.get("A") != null) {
            BigDecimal roundedThresholdA = thresholds.get("A").setScale(0, RoundingMode.HALF_UP);
            if (roundedScore.compareTo(roundedThresholdA) >= 0) {
                return "A";
            }
        }
        if (thresholds.get("B") != null) {
            BigDecimal roundedThresholdB = thresholds.get("B").setScale(0, RoundingMode.HALF_UP);
            if (roundedScore.compareTo(roundedThresholdB) >= 0) {
                return "B";
            }
        }
        if (thresholds.get("C") != null) {
            BigDecimal roundedThresholdC = thresholds.get("C").setScale(0, RoundingMode.HALF_UP);
            if (roundedScore.compareTo(roundedThresholdC) >= 0) {
                return "C";
            }
        }
        if (thresholds.get("D") != null) {
            BigDecimal roundedThresholdD = thresholds.get("D").setScale(0, RoundingMode.HALF_UP);
            if (roundedScore.compareTo(roundedThresholdD) >= 0) {
                return "D";
            }
        }
        return "E";
    }

    private BigDecimal calculateNaturalPrecisionPercentage(int count, int total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }

        // 计算精确的百分比
        BigDecimal exactPercentage = BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 10, RoundingMode.HALF_UP);

        // 检查是否为整数
        if (exactPercentage.stripTrailingZeros().scale() <= 0) {
            return exactPercentage.setScale(0, RoundingMode.UNNECESSARY);
        }

        // 检查一位小数是否足够精确表示
        BigDecimal oneDecimal = exactPercentage.setScale(1, RoundingMode.HALF_UP);
        BigDecimal difference = exactPercentage.subtract(oneDecimal).abs();

        // 如果差异很小（小于0.01），使用一位小数
        if (difference.compareTo(BigDecimal.valueOf(0.01)) < 0) {
            return oneDecimal;
        }

        // 否则使用截断到一位小数（避免四舍五入失真）
        return exactPercentage.setScale(1, RoundingMode.DOWN);
    }

    private BigDecimal calculatePercentage(Integer count, Integer total) {
        if (total == null || total == 0) {
            return BigDecimal.ZERO;
        }
        return calculateNaturalPrecisionPercentage(count, total);
    }

    /**
     * 安全地将Object转换为BigDecimal
     */
    private BigDecimal convertToBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                log.warn("无法将字符串转换为BigDecimal: {}", value);
                return BigDecimal.ZERO;
            }
        }
        log.warn("无法将类型{}转换为BigDecimal: {}", value.getClass().getSimpleName(), value);
        return BigDecimal.ZERO;
    }

    private ScoreSegmentOverviewVO buildCityOverviewVO(ScoreSegmentQueryDTO queryDTO,
                                                       String city,
                                                       Map<String, Object> basicStats,
                                                       List<Map<String, Object>> gradeStats,
                                                       List<ScoreSegmentDTO> segmentData) {
        // 构建等级分布
        Map<String, ScoreSegmentOverviewVO.GradeStatistics> gradeDistribution = new HashMap<>();
        for (Map<String, Object> grade : gradeStats) {
            String gradeCode = (String) grade.get("grade"); // selectGradeDistributionStats返回的字段名
            if (gradeCode != null) {
                ScoreSegmentOverviewVO.GradeStatistics gradeStat = ScoreSegmentOverviewVO.GradeStatistics.builder()
                        .gradeCode(gradeCode)
                        .gradeName(getGradeName(gradeCode))
                        .count(grade.get("count") != null ? ((Number) grade.get("count")).intValue() : 0)
                        .percentage(convertToBigDecimal(grade.get("percentage")).setScale(1, RoundingMode.HALF_UP))
                        .threshold(convertToBigDecimal(grade.get("avgScore"))) // 使用平均分作为阈值
                        .adjustable(true)
                        .build();
                gradeDistribution.put(gradeCode, gradeStat);
            }
        }

        // 转换分数段数据
        List<ScoreSegmentOverviewVO.ScoreSegmentData> segmentDataVO = segmentData.stream()
                .map(segment -> ScoreSegmentOverviewVO.ScoreSegmentData.builder()
                        .score(segment.getScore())
                        .count(segment.getCount())
                        .cumulativeCount(segment.getCumulativeCount())
                        .cumulativePercentage(segment.getCumulativePercentage())
                        .grade(segment.getGrade())
                        // 数据库中没有存储rank字段，因此不设置rank
                        // .rank(segment.getRank())
                        .build())
                .collect(Collectors.toList());

        // 获取考试计划名称（需要单独查询或从其他地方获取）
        String ksjhmc = queryDTO.getKsjhdm(); // 临时使用代码作为名称

        return ScoreSegmentOverviewVO.builder()
                .ksjhdm(queryDTO.getKsjhdm())
                .ksjhmc(ksjhmc)
                .kmmc(queryDTO.getKmmc())
                .szsmc(city)
                .totalCount(basicStats.get("count") != null ? ((Number) basicStats.get("count")).intValue() : 0) // getCityStatistics返回的字段名
                .maxScore(convertToBigDecimal(basicStats.get("maxScore")))
                .minScore(convertToBigDecimal(basicStats.get("minScore")))
                .avgScore(convertToBigDecimal(basicStats.get("avgScore")))
                .gradeDistribution(gradeDistribution)
                .cityDistribution(new ArrayList<>()) // 单个市州不需要市州分布
                .segmentData(segmentDataVO)
                .updateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .adjustable(true)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveScoreSegmentData(String ksjhdm, String kmmc, String szsmc, String operatorName,
                                        String operatorCode) {
        try {
            log.info("开始保存一分一段数据: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc);

            // 1. 删除旧数据
            wcxxMapper.deleteScoreSegmentData(ksjhdm, kmmc, szsmc);

            // 2. 计算一分一段数据
            if (szsmc != null) {
                // 处理单个市州数据
                List<String> cities = List.of(szsmc);
                List<ScoreSegmentDTO> scoreSegments = calculateScoreSegments(ksjhdm, kmmc, cities, null);

                if (scoreSegments.isEmpty()) {
                    log.warn("没有计算出一分一段数据: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc);
                    return false;
                }

                // 转换为WcxxEntity并批量插入
                List<WcxxEntity> entities = convertToWcxxEntities(scoreSegments, ksjhdm, kmmc, szsmc, operatorName,
                        operatorCode);
                wcxxMapper.batchInsertScoreSegments(entities);

                log.info("成功保存单个市州一分一段数据: ksjhdm={}, kmmc={}, szsmc={}, 数据条数={}", ksjhdm, kmmc, szsmc, entities.size());
            } else {
                // 处理全省数据：按市州顺序逐个生成所有市州的一分一段数据
                List<String> allCities = kscjMapper.selectCitiesForGradeAssignment(ksjhdm, kmmc);
                if (allCities.isEmpty()) {
                    log.warn("没有找到任何市州数据: ksjhdm={}, kmmc={}", ksjhdm, kmmc);
                    return false;
                }

                log.info("开始处理全省数据，共{}个市州", allCities.size());
                int totalSavedCount = 0;

                // 按市州顺序逐个处理
                for (String cityName : allCities) {
                    try {
                        log.info("正在处理市州: {}", cityName);

                        // 为单个市州计算一分一段数据
                        List<String> singleCityList = Collections.singletonList(cityName);
                        List<ScoreSegmentDTO> cityScoreSegments = calculateScoreSegments(ksjhdm, kmmc, singleCityList,
                                null);

                        if (!cityScoreSegments.isEmpty()) {
                            // 转换为WcxxEntity并批量插入
                            List<WcxxEntity> cityEntities = convertToWcxxEntities(cityScoreSegments, ksjhdm, kmmc,
                                    cityName, operatorName,
                                    operatorCode);
                            wcxxMapper.batchInsertScoreSegments(cityEntities);
                            totalSavedCount += cityEntities.size();
                            log.info("成功保存市州 {} 的一分一段数据，数据条数={}", cityName, cityEntities.size());
                        } else {
                            log.warn("市州 {} 没有计算出一分一段数据", cityName);
                        }
                    } catch (Exception e) {
                        log.error("处理市州 {} 的一分一段数据时发生错误: {}", cityName, e.getMessage(), e);
                        // 继续处理其他市州，不中断整个流程
                    }
                }

                log.info("成功保存全省一分一段数据: ksjhdm={}, kmmc={}, 总数据条数={}", ksjhdm, kmmc, totalSavedCount);

                if (totalSavedCount == 0) {
                    log.warn("全省所有市州都没有计算出一分一段数据: ksjhdm={}, kmmc={}", ksjhdm, kmmc);
                    return false;
                }
            }

            // 3. 清理相关缓存，确保Redis与MySQL数据一致性
            clearRelatedCache(ksjhdm, kmmc, szsmc);

            return true;
        } catch (Exception e) {
            log.error("保存一分一段数据失败: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);
            throw new RuntimeException("保存一分一段数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ScoreSegmentDTO> getScoreSegmentDataFromDB(String ksjhdm, String kmmc, String szsmc) {
        try {
            // 先尝试从Redis缓存获取
            List<ScoreSegmentDTO> cachedData = cacheService.getCachedScoreSegmentData(ksjhdm, kmmc, szsmc);
            if (cachedData != null) {
                log.info("从缓存获取一分一段数据: ksjhdm={}, kmmc={}, szsmc={}, 数据条数={}", ksjhdm, kmmc, szsmc, cachedData.size());
                return cachedData;
            }

            log.info("从数据库查询一分一段数据: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc);

            List<WcxxEntity> entities = wcxxMapper.selectScoreSegmentData(ksjhdm, kmmc, szsmc);
            List<ScoreSegmentDTO> result = convertToScoreSegmentDTOs(entities);

            // 将查询结果缓存到Redis
            if (!result.isEmpty()) {
                cacheService.cacheScoreSegmentData(ksjhdm, kmmc, szsmc, result);
            }

            log.info("从数据库查询到一分一段数据: ksjhdm={}, kmmc={}, szsmc={}, 数据条数={}", ksjhdm, kmmc, szsmc, result.size());
            return result;
        } catch (Exception e) {
            log.error("从数据库查询一分一段数据失败: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);
            throw new RuntimeException("查询一分一段数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteScoreSegmentData(String ksjhdm, String kmmc, String szsmc) {
        try {
            log.info("开始删除一分一段数据: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc);

            // 参数校验 - 支持灵活的删除模式
            if (ksjhdm == null || ksjhdm.trim().isEmpty()) {
                throw new IllegalArgumentException("考试计划代码不能为空");
            }

            // 至少需要传入kmmc或szsmc中的一个
            if ((kmmc == null || kmmc.trim().isEmpty()) && (szsmc == null || szsmc.trim().isEmpty())) {
                throw new IllegalArgumentException("科目名称和市州名称至少需要传入一个");
            }

            // 执行删除操作
            int deletedCount = wcxxMapper.deleteScoreSegmentData(ksjhdm, kmmc, szsmc);

            if (deletedCount > 0) {
                log.info("成功删除一分一段数据: ksjhdm={}, kmmc={}, szsmc={}, 删除记录数={}",
                        ksjhdm, kmmc, szsmc, deletedCount);

                // 清理相关缓存
                clearRelatedCache(ksjhdm, kmmc, szsmc);

                return true;
            } else {
                log.warn("未找到匹配的一分一段数据: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc);
                return false;
            }
        } catch (Exception e) {
            log.error("删除一分一段数据失败: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);
            throw new RuntimeException("删除一分一段数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将ScoreSegmentDTO转换为WcxxEntity
     */
    private List<WcxxEntity> convertToWcxxEntities(List<ScoreSegmentDTO> scoreSegments, String ksjhdm, String kmmc,
                                                   String szsmc, String operatorName, String operatorCode) {
        String ksjhmc = getKsjhmc(ksjhdm);
        LocalDateTime now = LocalDateTime.now();

        return scoreSegments.stream().map(segment -> {
            WcxxEntity entity = new WcxxEntity();
            entity.setKsjhdm(ksjhdm);
            // 只有当ksjhmc不为null且不为空时才设置，避免设置错误的值
            if (ksjhmc != null && !ksjhmc.trim().isEmpty()) {
                entity.setKsjhmc(ksjhmc);
            } else {
                entity.setKsjhmc(null); // 明确设置为null，而不是错误的ksjhdm值
            }
            entity.setKmmc(kmmc);
            entity.setSzsmc(segment.getSzsmc() != null ? segment.getSzsmc() : szsmc);

            // 处理等级值：如果为UNGRADED或无效值，根据分数重新计算等级
            String grade = segment.getGrade();
            if (grade == null || grade.trim().isEmpty() || "UNGRADED".equals(grade) || grade.length() > 1) {
                // 使用动态等级划分算法根据分数计算等级
                String currentSzsmc = segment.getSzsmc() != null ? segment.getSzsmc() : szsmc;
                grade = calculateGradeByScore(segment.getScore(), ksjhdm, kmmc, currentSzsmc);
                log.debug("DJM字段值无效({}), 使用动态算法根据分数{}重新计算等级: {}", segment.getGrade(), segment.getScore(), grade);
            }
            entity.setDjm(grade); // 等级值

            entity.setDjzdf(BigDecimal.valueOf(1)); // 详细数据标识为1

            // 根据szsmc计算szsxh
            String currentSzsmc = segment.getSzsmc() != null ? segment.getSzsmc() : szsmc;
            Integer szsxh = calculateSzsxh(currentSzsmc);
            entity.setSzsxh(szsxh);
            entity.setFslkscj(
                    segment.getScore() != null ? segment.getScore().intValue() : null);
            entity.setBfb(
                    segment.getCumulativePercentage() != null ? segment.getCumulativePercentage().intValue() : null);
            entity.setBfdrs(segment.getCount());
            entity.setLjbfb(segment.getCumulativePercentage());
            entity.setLjrs(segment.getCumulativeCount());
            // 处理操作人信息，如果为null则设置默认值
            // TODO：需要从登陆信息中获取
            entity.setCjrxm(operatorName != null && !operatorName.trim().isEmpty() ? operatorName : "系统生成");
            entity.setCjrgzrym(operatorCode != null && !operatorCode.trim().isEmpty() ? operatorCode : "000");
            entity.setCjsj(now);
            entity.setGxrxm(operatorName != null && !operatorName.trim().isEmpty() ? operatorName : "系统生成");
            entity.setGxrgzrym(operatorCode != null && !operatorCode.trim().isEmpty() ? operatorCode : "000");
            entity.setGxsj(now);
            return entity;
        }).collect(Collectors.toList());
    }

    /**
     * 根据分数计算等级
     * 使用动态等级划分算法，与现有的等级划分方法保持一致
     */
    private String calculateGradeByScore(BigDecimal score, String ksjhdm, String kmmc, String szsmc) {
        if (score == null) {
            return "E"; // 默认最低等级
        }

        try {
            // 尝试从WCXX表获取该市州的等级分界线
            List<Map<String, Object>> gradeThresholdsList = wcxxMapper.getGradeThresholds(ksjhdm, kmmc, szsmc);

            if (gradeThresholdsList != null && !gradeThresholdsList.isEmpty()) {
                // 将List转换为Map
                Map<String, BigDecimal> gradeThresholds = new HashMap<>();
                for (Map<String, Object> item : gradeThresholdsList) {
                    String key = (String) item.get("key");
                    Object value = item.get("value");
                    if (key != null && value != null) {
                        gradeThresholds.put(key, convertToBigDecimal(value));
                    }
                }

                // 使用动态分界线确定等级
                if (gradeThresholds.containsKey("A") && gradeThresholds.containsKey("B") &&
                        gradeThresholds.containsKey("C") && gradeThresholds.containsKey("D")) {
                    return determineGradeByThresholds(score, gradeThresholds);
                }
            }

            log.debug("未找到动态等级分界线，使用默认等级划分: ksjhdm={}, kmmc={}, szsmc={}, score={}",
                    ksjhdm, kmmc, szsmc, score);

        } catch (Exception e) {
            log.warn("获取动态等级分界线失败，使用默认等级划分: ksjhdm={}, kmmc={}, szsmc={}, score={}, error={}",
                    ksjhdm, kmmc, szsmc, score, e.getMessage());
        }

        // 如果无法获取动态分界线，使用默认的固定区间（作为兜底方案）
        if (score.compareTo(BigDecimal.valueOf(90)) >= 0) {
            return "A";
        } else if (score.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "B";
        } else if (score.compareTo(BigDecimal.valueOf(70)) >= 0) {
            return "C";
        } else if (score.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return "D";
        } else {
            return "E";
        }
    }

    /**
     * 根据市州名称计算市州序号
     */
    private Integer calculateSzsxh(String szsmc) {
        if (szsmc == null) {
            return 0;
        }

        return switch (szsmc) {
            case "玉树州" -> 1;
            case "果洛州" -> 2;
            case "黄南州" -> 3;
            case "海南州" -> 4;
            case "海西州" -> 5;
            case "海北州" -> 6;
            case "石油局" -> 7;
            case "西宁市" -> 8;
            case "海东市" -> 9;
            case "省直" -> 10;
            default -> 0;
        };
    }

    /**
     * 将WcxxEntity转换为ScoreSegmentDTO
     */
    private List<ScoreSegmentDTO> convertToScoreSegmentDTOs(List<WcxxEntity> entities) {
        return entities.stream().map(entity -> {
            ScoreSegmentDTO dto = new ScoreSegmentDTO();
            dto.setScore(entity.getFslkscj() != null ? BigDecimal.valueOf(entity.getFslkscj()) : null);
            dto.setCount(entity.getBfdrs());
            dto.setCumulativeCount(entity.getLjrs());
            dto.setCumulativePercentage(entity.getLjbfb());
            dto.setGrade(entity.getDjm());
            // 数据库中没有存储rank字段，因此不设置rank
            // dto.setRank(entity.getSzsxh());
            dto.setSzsmc(entity.getSzsmc());
            dto.setSzsxh(calculateSzsxh(entity.getSzsmc()));
            dto.setKsjhdm(entity.getKsjhdm());
            dto.setKmmc(entity.getKmmc());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 获取考试计划名称
     */
    private String getKsjhmc(String ksjhdm) {
        try {
            // 通过ksjhMapper查询考试计划信息
            KsjhEntity ksjh = ksjhMapper.selectByKsjhdm(ksjhdm);
            if (ksjh != null && ksjh.getKsjhmc() != null && !ksjh.getKsjhmc().trim().isEmpty()) {
                log.debug("查询到考试计划名称: {} -> {}", ksjhdm, ksjh.getKsjhmc());
                return ksjh.getKsjhmc();
            } else {
                log.warn("未找到考试计划代码 {} 对应的考试计划名称或名称为空", ksjhdm);
                return null; // 返回null而不是ksjhdm，避免错误设置
            }
        } catch (Exception e) {
            log.error("获取考试计划名称失败: ksjhdm={}", ksjhdm, e);
            return null; // 返回null而不是ksjhdm，避免错误设置
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GradeSyncResultVO syncStudentGradesFromWcxx(String ksjhdm, String kmmc, String szsmc) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("开始同步学生等级: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc);

            // 参数校验
            if (ksjhdm == null || ksjhdm.trim().isEmpty()) {
                throw new IllegalArgumentException("考试计划代码不能为空");
            }
            if (kmmc == null || kmmc.trim().isEmpty()) {
                throw new IllegalArgumentException("科目名称不能为空");
            }

            // 如果szsmc为空，则批量处理所有市州
            if (szsmc == null || szsmc.trim().isEmpty()) {
                return syncStudentGradesForAllCities(ksjhdm, kmmc, startTime);
            } else {
                // 单市州处理
                return syncStudentGradesForSingleCity(ksjhdm, kmmc, szsmc, startTime);
            }

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("学生等级同步失败: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);

            return GradeSyncResultVO.builder()
                    .success(false)
                    .syncedStudentCount(0)
                    .processingTime(processingTime)
                    .message("学生等级同步失败")
                    .errorMessage(e.getMessage())
                    .ksjhdm(ksjhdm)
                    .kmmc(kmmc)
                    .szsmc(szsmc)
                    .build();
        }
    }

    /**
     * 批量同步所有市州的学生等级
     */
    private GradeSyncResultVO syncStudentGradesForAllCities(String ksjhdm, String kmmc, long startTime) {
        // 获取所有市州列表
        List<String> cities = kscjMapper.selectCitiesForGradeAssignment(ksjhdm, kmmc);

        if (cities == null || cities.isEmpty()) {
            return GradeSyncResultVO.builder()
                    .success(false)
                    .syncedStudentCount(0)
                    .processingTime(System.currentTimeMillis() - startTime)
                    .message(String.format("未找到考试计划[%s]科目[%s]的市州数据", ksjhdm, kmmc))
                    .ksjhdm(ksjhdm)
                    .kmmc(kmmc)
                    .szsmc(null)
                    .build();
        }

        int totalSyncedStudents = 0;
        int successfulCities = 0;
        int failedCities = 0;
        StringBuilder messageBuilder = new StringBuilder();

        // 逐个处理每个市州
        for (String city : cities) {
            try {
                GradeSyncResultVO cityResult = syncStudentGradesForSingleCity(ksjhdm, kmmc, city, startTime);

                if (cityResult.isSuccess()) {
                    successfulCities++;
                    totalSyncedStudents += cityResult.getSyncedStudentCount();
                    log.info("市州[{}]同步成功，更新学生数: {}", city, cityResult.getSyncedStudentCount());
                } else {
                    failedCities++;
                    log.warn("市州[{}]同步失败: {}", city, cityResult.getMessage());
                }

            } catch (Exception e) {
                failedCities++;
                log.error("处理市州[{}]时发生错误", city, e);
            }
        }

        long processingTime = System.currentTimeMillis() - startTime;

        // 构建批量处理结果消息
        messageBuilder.append(String.format("批量同步完成，共处理%d个市州，成功%d个，失败%d个，总计更新%d名学生",
                cities.size(), successfulCities, failedCities, totalSyncedStudents));

        return GradeSyncResultVO.builder()
                .success(failedCities == 0)
                .syncedStudentCount(totalSyncedStudents)
                .processingTime(processingTime)
                .message(messageBuilder.toString())
                .ksjhdm(ksjhdm)
                .kmmc(kmmc)
                .szsmc(null) // 批量模式下szsmc为null
                .build();
    }

    /**
     * 单市州学生等级同步
     */
    private GradeSyncResultVO syncStudentGradesForSingleCity(String ksjhdm, String kmmc, String szsmc, long startTime) {
        // 从WCXX表获取等级分界线
        List<Map<String, Object>> gradeThresholdsList = wcxxMapper.getGradeThresholds(ksjhdm, kmmc, szsmc);
        if (gradeThresholdsList == null || gradeThresholdsList.isEmpty()) {
            return GradeSyncResultVO.builder()
                    .success(false)
                    .syncedStudentCount(0)
                    .processingTime(System.currentTimeMillis() - startTime)
                    .message(String.format("未找到考试计划[%s]科目[%s]市州[%s]的等级分界线数据", ksjhdm, kmmc, szsmc))
                    .ksjhdm(ksjhdm)
                    .kmmc(kmmc)
                    .szsmc(szsmc)
                    .build();
        }

        // 将List转换为Map
        Map<String, BigDecimal> gradeThresholds = new HashMap<>();
        for (Map<String, Object> item : gradeThresholdsList) {
            String key = (String) item.get("key");
            Object value = item.get("value");
            if (key != null && value != null) {
                gradeThresholds.put(key, convertToBigDecimal(value));
            }
        }

        // 确保所有等级的分界线都存在
        BigDecimal gradeAThreshold = gradeThresholds.get("A");
        BigDecimal gradeBThreshold = gradeThresholds.get("B");
        BigDecimal gradeCThreshold = gradeThresholds.get("C");
        BigDecimal gradeDThreshold = gradeThresholds.get("D");

        if (gradeAThreshold == null || gradeBThreshold == null ||
                gradeCThreshold == null || gradeDThreshold == null) {
            return GradeSyncResultVO.builder()
                    .success(false)
                    .syncedStudentCount(0)
                    .processingTime(System.currentTimeMillis() - startTime)
                    .message("等级分界线不完整，缺失的等级: " + getMissingGrades(gradeThresholds))
                    .ksjhdm(ksjhdm)
                    .kmmc(kmmc)
                    .szsmc(szsmc)
                    .build();
        }

        // 批量更新学生等级
        int syncedCount = kscjMapper.batchUpdateGrades(
                ksjhdm,
                kmmc,
                szsmc,
                gradeAThreshold,
                gradeBThreshold,
                gradeCThreshold,
                gradeDThreshold,
                "SYSTEM",
                "SYSTEM");

        // 获取同步后的等级分布统计
        List<Map<String, Object>> gradeStats = kscjMapper.selectGradeDistributionStats(ksjhdm, kmmc, szsmc);
        GradeSyncResultVO.GradeSyncStatistics statistics = buildGradeSyncStatistics(gradeStats);

        long processingTime = System.currentTimeMillis() - startTime;

        log.info("学生等级同步完成: 市州={}, 同步学生数={}, 处理时间={}ms", szsmc, syncedCount, processingTime);

        return GradeSyncResultVO.builder()
                .success(true)
                .syncedStudentCount(syncedCount)
                .processingTime(processingTime)
                .message(String.format("成功同步市州[%s]的学生等级，共更新%d名学生", szsmc, syncedCount))
                .ksjhdm(ksjhdm)
                .kmmc(kmmc)
                .szsmc(szsmc)
                .gradeStatistics(statistics)
                .build();
    }

    @Override
    public ScoreSegmentChangePreviewVO previewScoreSegmentChanges(GradeAdjustmentRequestDTO requestDTO) {
        try {
            // 获取第一个市州的数据用于预览
            AdjustedSzsmcDTO firstCityData = requestDTO.getFirstCityData();
            log.info("开始预览一分一段数据变化: ksjhdm={}, kmmc={}, szsmc={}",
                    requestDTO.getKsjhdm(), requestDTO.getKmmc(), firstCityData.getSzsmc());

            ScoreSegmentChangePreviewVO result = new ScoreSegmentChangePreviewVO();

            // 1. 获取原始一分一段数据
            List<ScoreSegmentDTO> originalSegments = getScoreSegmentDataFromDB(
                    requestDTO.getKsjhdm(), requestDTO.getKmmc(), firstCityData.getSzsmc());

            if (originalSegments == null || originalSegments.isEmpty()) {
                log.warn("未找到原始一分一段数据，无法预览变化");
                return result;
            }

            // 2. 获取原始等级分界线
            Map<String, BigDecimal> originalThresholds = getOriginalGradeThresholds(
                    requestDTO.getKsjhdm(), requestDTO.getKmmc(), firstCityData.getSzsmc());

            // 如果原始分界线为空，从当前等级分布中获取
            if (originalThresholds.isEmpty()) {
                List<GradeAdjustmentResultVO.GradeDistributionData> currentDistribution = getCurrentGradeDistribution(
                        requestDTO.getKsjhdm(), requestDTO.getKmmc(), firstCityData.getSzsmc());
                originalThresholds = extractThresholds(currentDistribution);
                log.info("从当前等级分布中获取原始分界线: {}", originalThresholds);
            }

            // 3. 计算调整后的一分一段数据
            List<ScoreSegmentChangePreviewVO.ScoreSegmentData> adjustedSegments = calculateAdjustedScoreSegments(
                    originalSegments, originalThresholds, firstCityData.getAdjustedThresholds());

            // 4. 转换原始数据格式
            List<ScoreSegmentChangePreviewVO.ScoreSegmentData> originalData = convertToScoreSegmentData(
                    originalSegments, originalThresholds);

            // 5. 计算变化统计
            ScoreSegmentChangePreviewVO.ChangeStatistics changeStats = calculateScoreSegmentChangeStatistics(
                    originalData, adjustedSegments, originalThresholds, firstCityData.getAdjustedThresholds());

            result.setOriginalData(originalData);
            result.setAdjustedData(adjustedSegments);
            result.setChangeStatistics(changeStats);

            log.info("一分一段数据变化预览完成: 原始数据{}条，调整后数据{}条",
                    originalData.size(), adjustedSegments.size());

            return result;

        } catch (Exception e) {
            log.error("预览一分一段数据变化失败", e);
            throw new RuntimeException("预览一分一段数据变化失败: " + e.getMessage());
        }
    }

    /**
     * 获取原始等级分界线
     */
    private Map<String, BigDecimal> getOriginalGradeThresholds(String ksjhdm, String kmmc, String szsmc) {
        try {
            // 先尝试从Redis缓存获取
            Map<String, BigDecimal> cachedThresholds = cacheService.getCachedGradeThresholds(ksjhdm, kmmc, szsmc);
            if (cachedThresholds != null && !cachedThresholds.isEmpty()) {
                log.debug("从缓存获取等级分界线数据: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc);
                return cachedThresholds;
            }

            // 缓存中没有数据，从数据库查询
            List<Map<String, Object>> gradeThresholdsList = wcxxMapper.getGradeThresholds(ksjhdm, kmmc, szsmc);
            Map<String, BigDecimal> gradeThresholds = new HashMap<>();
            for (Map<String, Object> threshold : gradeThresholdsList) {
                String key = (String) threshold.get("key");
                Object value = threshold.get("value");
                if (key != null && value != null) {
                    gradeThresholds.put(key, new BigDecimal(value.toString()));
                }
            }

            // 将查询结果缓存到Redis
            if (!gradeThresholds.isEmpty()) {
                cacheService.cacheGradeThresholds(ksjhdm, kmmc, szsmc, gradeThresholds);
                log.debug("缓存等级分界线数据: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc);
            }

            return gradeThresholds;
        } catch (Exception e) {
            log.error("获取原始等级分界线失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 计算调整后的一分一段数据
     */
    private List<ScoreSegmentChangePreviewVO.ScoreSegmentData> calculateAdjustedScoreSegments(
            List<ScoreSegmentDTO> originalSegments,
            Map<String, BigDecimal> originalThresholds,
            Map<String, BigDecimal> adjustedThresholds) {

        List<ScoreSegmentChangePreviewVO.ScoreSegmentData> result = new ArrayList<>();

        // 按分数从高到低排序
        List<ScoreSegmentDTO> sortedSegments = originalSegments.stream()
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .toList();

        int totalCount = sortedSegments.stream().mapToInt(ScoreSegmentDTO::getCount).sum();
        int cumulativeCount = 0;

        for (ScoreSegmentDTO segment : sortedSegments) {
            ScoreSegmentChangePreviewVO.ScoreSegmentData adjustedData = new ScoreSegmentChangePreviewVO.ScoreSegmentData();
            adjustedData.setScore(segment.getScore());
            adjustedData.setCount(segment.getCount());

            // 重新计算累计人数
            cumulativeCount += segment.getCount();
            adjustedData.setCumulativeCount(cumulativeCount);

            // 重新计算累计百分比
            BigDecimal cumulativePercentage = calculatePercentage(cumulativeCount, totalCount);
            adjustedData.setCumulativePercentage(cumulativePercentage);

            // 根据新的分界线重新确定等级
            String newGrade = determineGradeByThresholds(segment.getScore(), adjustedThresholds);
            adjustedData.setGrade(newGrade);

            result.add(adjustedData);
        }

        // 按分数从高到低排序（A等级到E等级）
        result.sort((a, b) -> b.getScore().compareTo(a.getScore()));

        return result;
    }

    /**
     * 转换原始数据格式
     */
    private List<ScoreSegmentChangePreviewVO.ScoreSegmentData> convertToScoreSegmentData(
            List<ScoreSegmentDTO> segments, Map<String, BigDecimal> thresholds) {

        List<ScoreSegmentChangePreviewVO.ScoreSegmentData> result = new ArrayList<>();

        for (ScoreSegmentDTO segment : segments) {
            ScoreSegmentChangePreviewVO.ScoreSegmentData data = new ScoreSegmentChangePreviewVO.ScoreSegmentData();
            data.setScore(segment.getScore());
            data.setCount(segment.getCount());
            data.setCumulativeCount(segment.getCumulativeCount());
            data.setCumulativePercentage(segment.getCumulativePercentage());

            // 根据原始分界线确定等级
            String grade = determineGradeByThresholds(segment.getScore(), thresholds);
            data.setGrade(grade);

            result.add(data);
        }

        // 按分数从高到低排序（A等级到E等级）
        result.sort((a, b) -> b.getScore().compareTo(a.getScore()));

        return result;
    }

    /**
     * 计算一分一段数据变化统计
     */
    private ScoreSegmentChangePreviewVO.ChangeStatistics calculateScoreSegmentChangeStatistics(
            List<ScoreSegmentChangePreviewVO.ScoreSegmentData> originalData,
            List<ScoreSegmentChangePreviewVO.ScoreSegmentData> adjustedData,
            Map<String, BigDecimal> originalThresholds,
            Map<String, BigDecimal> adjustedThresholds) {

        ScoreSegmentChangePreviewVO.ChangeStatistics stats = new ScoreSegmentChangePreviewVO.ChangeStatistics();

        // 创建分数到数据的映射，便于查找对应的原始数据
        Map<BigDecimal, ScoreSegmentChangePreviewVO.ScoreSegmentData> originalMap = originalData.stream()
                .collect(Collectors.toMap(
                        ScoreSegmentChangePreviewVO.ScoreSegmentData::getScore,
                        data -> data,
                        (existing, replacement) -> existing));

        // 计算变化数据
        int totalAffectedScores = 0;
        int gradeChangedScores = 0;

        // 遍历调整后的数据，计算变化
        for (ScoreSegmentChangePreviewVO.ScoreSegmentData adjusted : adjustedData) {
            ScoreSegmentChangePreviewVO.ScoreSegmentData original = originalMap.get(adjusted.getScore());

            if (original != null) {
                // 统计
                if (!original.getGrade().equals(adjusted.getGrade())) {
                    totalAffectedScores++;
                    gradeChangedScores++;
                }
            }
        }

        stats.setTotalAffectedScores(totalAffectedScores);
        stats.setGradeChangedScores(gradeChangedScores);

        // 计算等级分界线变化
        List<ScoreSegmentChangePreviewVO.GradeThresholdChange> thresholdChanges = new ArrayList<>();
        for (String grade : Arrays.asList("A", "B", "C", "D")) {
            BigDecimal originalThreshold = originalThresholds.get(grade);
            BigDecimal adjustedThreshold = adjustedThresholds.get(grade);

            if (originalThreshold != null && adjustedThreshold != null) {
                ScoreSegmentChangePreviewVO.GradeThresholdChange change = new ScoreSegmentChangePreviewVO.GradeThresholdChange();
                change.setGrade(grade);
                change.setOriginalThreshold(originalThreshold);
                change.setAdjustedThreshold(adjustedThreshold);
                change.setThresholdChange(adjustedThreshold.subtract(originalThreshold));
                thresholdChanges.add(change);
            }
        }
        stats.setGradeThresholdChanges(thresholdChanges);

        return stats;
    }

    /**
     * 构建等级同步统计信息
     */
    private GradeSyncResultVO.GradeSyncStatistics buildGradeSyncStatistics(List<Map<String, Object>> gradeStats) {
        if (gradeStats == null || gradeStats.isEmpty()) {
            return GradeSyncResultVO.GradeSyncStatistics.builder()
                    .gradeACount(0)
                    .gradeBCount(0)
                    .gradeCCount(0)
                    .gradeDCount(0)
                    .gradeECount(0)
                    .totalCount(0)
                    .build();
        }

        int gradeACount = 0, gradeBCount = 0, gradeCCount = 0, gradeDCount = 0, gradeECount = 0;

        for (Map<String, Object> stat : gradeStats) {
            String grade = (String) stat.get("grade");
            int count = ((Number) stat.get("count")).intValue();

            switch (grade) {
                case "A":
                    gradeACount = count;
                    break;
                case "B":
                    gradeBCount = count;
                    break;
                case "C":
                    gradeCCount = count;
                    break;
                case "D":
                    gradeDCount = count;
                    break;
                case "E":
                    gradeECount = count;
                    break;
            }
        }

        int totalCount = gradeACount + gradeBCount + gradeCCount + gradeDCount + gradeECount;

        return GradeSyncResultVO.GradeSyncStatistics.builder()
                .gradeACount(gradeACount)
                .gradeBCount(gradeBCount)
                .gradeCCount(gradeCCount)
                .gradeDCount(gradeDCount)
                .gradeECount(gradeECount)
                .totalCount(totalCount)
                .build();
    }

    /**
     * 清理相关缓存，确保Redis与MySQL数据一致性
     * 当一分一段数据保存到数据库后，需要清理相关的Redis缓存
     */
    private void clearRelatedCache(String ksjhdm, String kmmc, String szsmc) {
        try {
            log.info("开始清理相关缓存: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc);

            // 1. 清理一分一段相关缓存
            cacheService.clearCache(ksjhdm, kmmc);
            if (szsmc != null) {
                cacheService.clearCityCache(ksjhdm, kmmc, szsmc);
            }

            // 2. 清理计算状态缓存
            cacheService.clearCalculationStatus(ksjhdm, kmmc);

            // 3. 清理预计算锁
            cacheService.clearPrecomputeLock(ksjhdm, kmmc);

            log.info("成功清理相关缓存: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc);
        } catch (Exception e) {
            log.warn("清理缓存失败，但不影响数据保存: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);
        }
    }

}