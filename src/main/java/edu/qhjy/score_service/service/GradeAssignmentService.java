package edu.qhjy.score_service.service;

import edu.qhjy.score_service.config.GradeAssignmentProperties;
import edu.qhjy.score_service.domain.dto.GradeAssignmentRequestDTO;
import edu.qhjy.score_service.domain.dto.GradeThresholdsDTO;
import edu.qhjy.score_service.domain.dto.StudentScoreRankDTO;
import edu.qhjy.score_service.domain.entity.KsjhEntity;
import edu.qhjy.score_service.domain.entity.WcxxEntity;
import edu.qhjy.score_service.domain.vo.GradeAssignmentProgressVO;
import edu.qhjy.score_service.domain.vo.GradeAssignmentResultVO;
import edu.qhjy.score_service.mapper.primary.KscjMapper;
import edu.qhjy.score_service.mapper.primary.KsjhMapper;
import edu.qhjy.score_service.mapper.primary.WcxxMapper;
import edu.qhjy.score_service.service.algorithm.GradeCalculationAlgorithm;
import edu.qhjy.score_service.service.redis.GradeAssignmentLockService;
import edu.qhjy.score_service.service.redis.GradeAssignmentProgressService;
import edu.qhjy.score_service.service.redis.GradeCalculationCacheService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 等级赋分服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeAssignmentService {

    private final KscjMapper kscjMapper;
    private final WcxxMapper wcxxMapper;
    private final KsjhMapper ksjhMapper;
    private final GradeCalculationAlgorithm gradeAlgorithm;
    private final GradeAssignmentLockService lockService;
    private final GradeAssignmentProgressService progressService;
    private final GradeCalculationCacheService cacheService;
    private final GradeAssignmentProperties gradeAssignmentProperties;

    // 线程池用于并行处理
    private final Executor gradeAssignmentExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());

    /**
     * 计算自然精度的百分比（动态精度方案）
     * 在一位小数限制下，避免不必要的四舍五入
     *
     * @param count 当前计数
     * @param total 总数
     * @return 自然精度的百分比，最多保留一位小数
     */
    private BigDecimal calculateNaturalPrecisionPercentage(int count, int total) {
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

    /**
     * 执行等级赋分
     */
    @Transactional(rollbackFor = Exception.class)
    public GradeAssignmentResultVO assignGrades(GradeAssignmentRequestDTO request) {
        String lockKey = String.format("%s_%s", request.getKsjhdm(), request.getKmmc());
        String taskId = UUID.randomUUID().toString();

        try {
            // 1. 获取分布式锁
            if (!lockService.acquireLock(lockKey, taskId)) {
                throw new RuntimeException("等级赋分任务正在进行中，请稍后再试");
            }

            log.info("开始执行等级赋分: 考试计划={}, 科目={}, 处理范围={}",
                    request.getKsjhdm(), request.getKmmc(),
                    request.getSzsmc() != null ? "城市:" + request.getSzsmc() : "全部");

            // 2. 初始化进度跟踪
            progressService.startTask(taskId, "等级赋分任务");

            // 3. 检查是否已存在等级赋分记录，如果存在则先清除
            if (checkExistingGradeAssignment(request)) {
                String cityInfo = request.getSzsmc() != null && !request.getSzsmc().trim().isEmpty()
                        ? "市州[" + request.getSzsmc() + "]"
                        : "全省范围";
                log.info("检测到{}的考试计划[{}]科目[{}]已存在等级赋分记录，将先清除后重新赋分",
                        cityInfo, request.getKsjhdm(), request.getKmmc());

                // 先清除已存在的等级赋分记录
                Map<String, Object> clearResult = clearGradeAssignment(
                        request.getKsjhdm(), request.getKmmc(), request.getSzsmc());
                log.info("清除等级赋分记录完成: {}", clearResult.get("message"));
            }

            // 4. 获取城市列表
            List<String> cities = getCitiesForProcessing(request);
            progressService.updateProgress(taskId, 10, "获取处理城市列表完成");

            // 5. 第一阶段：并行计算各城市等级阈值
            CityThresholdsResult thresholdsResult = calculateCityThresholdsParallel(
                    request, cities, taskId);
            progressService.updateProgress(taskId, 50, "等级阈值计算完成");

            // 6. 第二阶段：串行批量更新所有学生等级
            GradeAssignmentResultVO result = batchUpdateStudentGrades(
                    request, thresholdsResult.getSuccessfulCities(),
                    thresholdsResult.getFailedCities().size(), taskId);
            progressService.updateProgress(taskId, 90, "学生等级更新完成");

            // 7. 保存等级阈值信息到WCXX表
            saveGradeThresholds(request, thresholdsResult.getSuccessfulCities());
            progressService.updateProgress(taskId, 95, "等级阈值信息保存完成");

            // 清除相关缓存
            cacheService.clearCache(request.getKsjhdm(), request.getKmmc());

            progressService.completeTask(taskId, "等级赋分任务完成");
            log.info("等级赋分完成: 处理学生数={}, 成功数={}",
                    result.getProcessedStudentCount(), result.getProcessedStudentCount());

            return result;

        } catch (Exception e) {
            progressService.updateProgress(taskId, -1, "任务执行失败: " + e.getMessage());
            log.error("等级赋分执行失败", e);
            throw new RuntimeException("等级赋分执行失败: " + e.getMessage(), e);
        } finally {
            lockService.releaseLock(lockKey, taskId);
        }
    }

    /**
     * 检查是否已存在等级赋分记录
     */
    private boolean checkExistingGradeAssignment(GradeAssignmentRequestDTO request) {
        try {
            // 参数验证
            if (request.getKsjhdm() == null || request.getKsjhdm().trim().isEmpty()) {
                throw new IllegalArgumentException("考试计划代码不能为空");
            }
            if (request.getKmmc() == null || request.getKmmc().trim().isEmpty()) {
                throw new IllegalArgumentException("科目名称不能为空");
            }

            boolean exists = wcxxMapper.existsGradeAssignment(request.getKsjhdm(), request.getKmmc(),
                    request.getSzsmc());

            if (exists) {
                String cityInfo = request.getSzsmc() != null && !request.getSzsmc().trim().isEmpty()
                        ? "市州[" + request.getSzsmc() + "]"
                        : "全省范围";
                log.warn("等级赋分记录已存在: 考试计划={}, 科目={}, 范围={}",
                        request.getKsjhdm(), request.getKmmc(), cityInfo);
            }

            return exists;
        } catch (Exception e) {
            log.error("检查等级赋分记录时发生错误: 考试计划={}, 科目={}, 市州={}",
                    request.getKsjhdm(), request.getKmmc(), request.getSzsmc(), e);
            throw new RuntimeException("检查等级赋分记录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取需要处理的城市列表
     */
    private List<String> getCitiesForProcessing(GradeAssignmentRequestDTO request) {
        if (request.getSzsmc() != null && !request.getSzsmc().trim().isEmpty()) {
            return List.of(request.getSzsmc());
        }

        return kscjMapper.selectCitiesForGradeAssignment(
                request.getKsjhdm(), request.getKmmc());
    }

    /**
     * 并行计算各城市等级阈值
     */
    private CityThresholdsResult calculateCityThresholdsParallel(
            GradeAssignmentRequestDTO request, List<String> cities, String taskId) {

        List<CompletableFuture<Map.Entry<String, GradeThresholdsDTO>>> futures = cities
                .stream().<CompletableFuture<Map.Entry<String, GradeThresholdsDTO>>>map(
                        city -> CompletableFuture.supplyAsync(() -> {
                            try {
                                // 检查缓存
                                GradeThresholdsDTO cached = cacheService.getCachedThresholds(
                                        request.getKsjhdm(), request.getKmmc(), city);
                                if (cached != null) {
                                    return new AbstractMap.SimpleEntry<>(city, cached);
                                }

                                // 查询该城市学生成绩排名
                                List<StudentScoreRankDTO> studentRanks = kscjMapper.selectStudentScoreRanks(
                                        request.getKsjhdm(), request.getKmmc(), city);

                                if (studentRanks.isEmpty()) {
                                    log.warn("城市 {} 没有找到学生成绩数据", city);
                                    return null;
                                }

                                // 计算等级阈值 - 使用配置文件中的比例
                                Map<String, Double> gradeRatios = gradeAssignmentProperties.getGradeConfig()
                                        .getGradeRatios();
                                GradeThresholdsDTO thresholds = GradeCalculationAlgorithm.calculateGradeThresholds(
                                        studentRanks, gradeRatios, city, request.getKmmc(),
                                        gradeAssignmentProperties.isUseEGradePriorityAlgorithm(),
                                        gradeAssignmentProperties.getEGradeMinPercentage());

                                // 缓存结果
                                cacheService.cacheThresholds(
                                        request.getKsjhdm(), request.getKmmc(), city, thresholds);

                                log.debug("城市 {} 等级阈值计算完成: {}", city, thresholds);
                                return new AbstractMap.SimpleEntry<>(city, thresholds);

                            } catch (Exception e) {
                                log.error("计算城市 {} 等级阈值失败", city, e);
                                throw new RuntimeException("计算城市 " + city + " 等级阈值失败", e);
                            }
                        }, gradeAssignmentExecutor))
                .toList();

        // 等待所有任务完成并收集结果
        Map<String, GradeThresholdsDTO> result = new HashMap<>();
        List<String> failedCities = new ArrayList<>();

        for (int i = 0; i < futures.size(); i++) {
            CompletableFuture<Map.Entry<String, GradeThresholdsDTO>> future = futures.get(i);
            String cityName = cities.get(i);

            try {
                Map.Entry<String, GradeThresholdsDTO> entry = future.get();
                if (entry != null) {
                    result.put(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                log.error("城市 {} 等级阈值计算失败", cityName, e);
                failedCities.add(cityName);
            }
        }

        if (!failedCities.isEmpty()) {
            log.warn("以下城市等级阈值计算失败: {}", failedCities);
        }

        return new CityThresholdsResult(result, failedCities);
    }

    /**
     * 批量更新学生等级（串行处理保证事务一致性）
     */
    private GradeAssignmentResultVO batchUpdateStudentGrades(
            GradeAssignmentRequestDTO request,
            Map<String, GradeThresholdsDTO> cityThresholds,
            int initialFailureCount,
            String taskId) {

        int totalStudents = 0;
        int successCount = 0;
        int failureCount = initialFailureCount; // 从初始失败数量开始计算
        List<String> errorMessages = new ArrayList<>();

        for (Map.Entry<String, GradeThresholdsDTO> entry : cityThresholds.entrySet()) {
            String city = entry.getKey();
            GradeThresholdsDTO thresholds = entry.getValue();

            try {
                // 查询该城市所有学生成绩
                List<StudentScoreRankDTO> studentRanks = kscjMapper.selectStudentScoreRanks(
                        request.getKsjhdm(), request.getKmmc(), city);

                if (studentRanks.isEmpty()) {
                    continue;
                }

                totalStudents += studentRanks.size();

                // 为学生分配等级
                gradeAlgorithm.assignGradesToStudents(studentRanks, thresholds);

                // 执行批量更新（一次性更新该城市所有学生）
                int updatedCount = kscjMapper.batchUpdateGrades(
                        request.getKsjhdm(),
                        request.getKmmc(),
                        city,
                        thresholds.getGradeAThreshold(),
                        thresholds.getGradeBThreshold(),
                        thresholds.getGradeCThreshold(),
                        thresholds.getGradeDThreshold(),
                        request.getOperatorName(),
                        request.getOperatorCode());
                successCount += updatedCount;

                log.info("城市 {} 学生等级更新完成: 查询学生数={}, 实际更新数={}",
                        city, studentRanks.size(), updatedCount);

            } catch (Exception e) {
                failureCount += 1; // 统计失败的城市数量，而不是学生数量
                String errorMsg = String.format("城市 %s 等级更新失败: %s", city, e.getMessage());
                errorMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }

        // 构建返回结果
        GradeAssignmentResultVO result = new GradeAssignmentResultVO();
        result.setStatus(failureCount == 0 ? "SUCCESS" : "PARTIAL_SUCCESS");
        result.setMessage(failureCount == 0 ? "等级赋分完成" : "等级赋分部分完成，有" + failureCount + "个城市处理失败");

        // 设置考试计划代码和科目名称
        result.setKsjhdm(request.getKsjhdm());
        result.setKmmc(request.getKmmc());

        // 设置处理的学生总数和城市数量
        result.setProcessedStudentCount(totalStudents);
        result.setProcessedCityCount(cityThresholds.size());
        // 设置处理时间（这里可以计算实际耗时）
        result.setProcessingTimeMs(System.currentTimeMillis());

        // 构建城市等级统计信息
        List<GradeAssignmentResultVO.CityGradeStatistics> cityStatsList = new ArrayList<>();
        for (Map.Entry<String, GradeThresholdsDTO> entry : cityThresholds.entrySet()) {
            String city = entry.getKey();
            GradeThresholdsDTO thresholds = entry.getValue();

            // 构建等级分布
            Map<String, Integer> gradeDistribution = new HashMap<>();
            gradeDistribution.put("A", thresholds.getGradeACount());
            gradeDistribution.put("B", thresholds.getGradeBCount());
            gradeDistribution.put("C", thresholds.getGradeCCount());
            gradeDistribution.put("D", thresholds.getGradeDCount());
            gradeDistribution.put("E", thresholds.getGradeECount());

            // 构建等级阈值
            Map<String, Double> gradeThresholdMap = new HashMap<>();
            gradeThresholdMap.put("A",
                    thresholds.getGradeAThreshold() != null ? thresholds.getGradeAThreshold().doubleValue() : 0.0);
            gradeThresholdMap.put("B",
                    thresholds.getGradeBThreshold() != null ? thresholds.getGradeBThreshold().doubleValue() : 0.0);
            gradeThresholdMap.put("C",
                    thresholds.getGradeCThreshold() != null ? thresholds.getGradeCThreshold().doubleValue() : 0.0);
            gradeThresholdMap.put("D",
                    thresholds.getGradeDThreshold() != null ? thresholds.getGradeDThreshold().doubleValue() : 0.0);
            gradeThresholdMap.put("E",
                    thresholds.getGradeEThreshold() != null ? thresholds.getGradeEThreshold().doubleValue() : 0.0);

            // 计算合格人数和合格率（A、B、C、D等级为合格）
            int qualifiedCount = thresholds.getGradeACount() + thresholds.getGradeBCount() +
                    thresholds.getGradeCCount() + thresholds.getGradeDCount();
            double qualifiedRate = thresholds.getTotalCount() > 0
                    ? (qualifiedCount * 100.0 / thresholds.getTotalCount())
                    : 0.0;

            GradeAssignmentResultVO.CityGradeStatistics cityStats = GradeAssignmentResultVO.CityGradeStatistics
                    .builder()
                    .szsmc(city)
                    .totalCount(thresholds.getTotalCount())
                    .gradeDistribution(gradeDistribution)
                    .gradeThresholds(gradeThresholdMap)
                    .qualifiedCount(qualifiedCount)
                    .qualifiedRate(Math.round(qualifiedRate * 100.0) / 100.0) // 保留两位小数
                    .build();

            cityStatsList.add(cityStats);
        }

        result.setCityGradeStatistics(cityStatsList);

        return result;
    }

    /**
     * 保存等级阈值信息到WCXX表
     */
    private void saveGradeThresholds(GradeAssignmentRequestDTO request,
                                     Map<String, GradeThresholdsDTO> cityThresholds) {

        // 查询考试计划名称
        String ksjhmc = null;
        try {
            KsjhEntity ksjhEntity = ksjhMapper.selectByKsjhdm(request.getKsjhdm());
            if (ksjhEntity != null && ksjhEntity.getKsjhmc() != null && !ksjhEntity.getKsjhmc().trim().isEmpty()) {
                ksjhmc = ksjhEntity.getKsjhmc();
                log.debug("查询到考试计划名称: {} -> {}", request.getKsjhdm(), ksjhmc);
            } else {
                log.warn("未找到考试计划代码 {} 对应的考试计划名称或名称为空", request.getKsjhdm());
            }
        } catch (Exception e) {
            log.error("查询考试计划名称失败: {}", request.getKsjhdm(), e);
        }

        List<WcxxEntity> wcxxEntities = new ArrayList<>();
        final String finalKsjhmc = ksjhmc; // 用于lambda表达式

        for (Map.Entry<String, GradeThresholdsDTO> entry : cityThresholds.entrySet()) {
            String city = entry.getKey();
            GradeThresholdsDTO thresholds = entry.getValue();

            // 统一数据源：使用与一分一段服务相同的学生数据获取方法
            List<StudentScoreRankDTO> cityStudents = kscjMapper.getStudentScoreRanks(
                    request.getKsjhdm(), request.getKmmc(), Collections.singletonList(city));

            if (cityStudents == null || cityStudents.isEmpty()) {
                log.warn("未找到城市 {} 的学生成绩数据，跳过保存", city);
                continue;
            }

            // 使用实际学生总数作为计算基准（与一分一段服务保持一致）
            int totalStudents = cityStudents.size();

            // 从学生数据中统计等级分布
            Map<String, Integer> gradeCountMap = new HashMap<>();
            for (String grade : Arrays.asList("A", "B", "C", "D", "E", "UNGRADED")) {
                gradeCountMap.put(grade, 0);
            }

            for (StudentScoreRankDTO student : cityStudents) {
                String grade = student.getGrade() != null ? student.getGrade() : "UNGRADED";
                gradeCountMap.put(grade, gradeCountMap.get(grade) + 1);
            }

            // 构建等级统计数据列表（模拟原有的actualGradeStats格式）
            List<Map<String, Object>> actualGradeStats = new ArrayList<>();
            for (Map.Entry<String, Integer> gradeEntry : gradeCountMap.entrySet()) {
                if (gradeEntry.getValue() > 0) { // 只包含有学生的等级
                    Map<String, Object> gradeStat = new HashMap<>();
                    gradeStat.put("grade", gradeEntry.getKey());
                    gradeStat.put("count", gradeEntry.getValue());
                    actualGradeStats.add(gradeStat);
                }
            }

            // 按等级排序并计算累计数据
            actualGradeStats.sort((a, b) -> {
                String gradeA = (String) a.get("grade");
                String gradeB = (String) b.get("grade");
                return gradeA.compareTo(gradeB);
            });

            int cumulativeCount = 0;

            for (Map<String, Object> gradeStat : actualGradeStats) {
                String grade = (String) gradeStat.get("grade");
                int count = ((Number) gradeStat.get("count")).intValue();

                // 跳过UNGRADED等级
                if ("UNGRADED".equals(grade)) {
                    continue;
                }

                cumulativeCount += count;

                // 计算累计百分比（动态精度方案，使用与一分一段服务相同的学生总数基准）
                BigDecimal cumulativePercentage = calculateNaturalPrecisionPercentage(
                        cumulativeCount, totalStudents);

                WcxxEntity wcxx = new WcxxEntity();
                wcxx.setKsjhdm(request.getKsjhdm());
                wcxx.setKsjhmc(finalKsjhmc);
                wcxx.setKmmc(request.getKmmc());
                wcxx.setSzsmc(city);

                // 从统计数据中获取szsxh
                Object szsxhObj = gradeStat.get("szsxh");
                Integer szsxh = null;
                if (szsxhObj instanceof Number) {
                    szsxh = ((Number) szsxhObj).intValue();
                } else {
                    // 如果统计数据中没有szsxh，则计算
                    szsxh = calculateSzsxh(city);
                }
                wcxx.setSzsxh(szsxh);

                wcxx.setDjm(grade);

                // 获取对应等级的阈值
                BigDecimal threshold = switch (grade) {
                    case "A" -> thresholds.getGradeAThreshold();
                    case "B" -> thresholds.getGradeBThreshold();
                    case "C" -> thresholds.getGradeCThreshold();
                    case "D" -> thresholds.getGradeDThreshold();
                    case "E" -> thresholds.getGradeEThreshold();
                    default -> null;
                };

                wcxx.setFslkscj(threshold != null ? threshold.setScale(0, RoundingMode.HALF_UP).intValue() : 0);
                wcxx.setBfb((int) Math.round(count * 100.0 / totalStudents));
                wcxx.setBfdrs(count);

                // 设置新增字段
                wcxx.setLjrs(cumulativeCount); // 累计人数
                wcxx.setLjbfb(cumulativePercentage); // 累计百分比
                wcxx.setDjzdf(BigDecimal.ZERO); // 等级赋分标识，设置为0表示已进行等级赋分

                wcxx.setCjrxm(request.getOperatorName());
                wcxx.setCjrgzrym(request.getOperatorCode());
                wcxx.setCjsj(LocalDateTime.now());
                wcxx.setGxrxm(request.getOperatorName());
                wcxx.setGxrgzrym(request.getOperatorCode());
                wcxx.setGxsj(LocalDateTime.now());

                wcxxEntities.add(wcxx);
            }
        }

        if (!wcxxEntities.isEmpty()) {
            wcxxMapper.batchInsert(wcxxEntities);
            log.info("等级阈值信息保存完成: 记录数={}", wcxxEntities.size());
        }
    }

    /**
     * 验证LJBFB计算的一致性
     * 比较等级赋分服务和一分一段服务的累计百分比计算结果
     *
     * @param ksjhdm 考试计划代码
     * @param kmmc   科目名称
     * @param szsmc  市州名称
     * @return 验证结果，包含差异信息
     */
    public Map<String, Object> validateLjbfbConsistency(String ksjhdm, String kmmc, String szsmc) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> inconsistencies = new ArrayList<>();

        try {
            // 获取学生成绩数据（与一分一段服务相同的数据源）
            List<StudentScoreRankDTO> cityStudents = kscjMapper.getStudentScoreRanks(
                    ksjhdm, kmmc, Collections.singletonList(szsmc));

            if (cityStudents == null || cityStudents.isEmpty()) {
                result.put("success", false);
                result.put("message", "未找到学生成绩数据");
                return result;
            }

            int totalStudents = cityStudents.size();

            // 统计等级分布
            Map<String, Integer> gradeCountMap = new HashMap<>();
            for (String grade : Arrays.asList("A", "B", "C", "D", "E")) {
                gradeCountMap.put(grade, 0);
            }

            for (StudentScoreRankDTO student : cityStudents) {
                String grade = student.getGrade();
                if (grade != null && gradeCountMap.containsKey(grade)) {
                    gradeCountMap.put(grade, gradeCountMap.get(grade) + 1);
                }
            }

            // 计算累计百分比（等级赋分服务的逻辑）
            int cumulativeCount = 0;
            Map<String, BigDecimal> gradeAssignmentLjbfb = new HashMap<>();

            for (String grade : Arrays.asList("A", "B", "C", "D", "E")) {
                int count = gradeCountMap.get(grade);
                if (count > 0) {
                    cumulativeCount += count;
                    BigDecimal ljbfb = calculateNaturalPrecisionPercentage(cumulativeCount, totalStudents);
                    gradeAssignmentLjbfb.put(grade, ljbfb);
                }
            }

            // 获取WCXX表中保存的LJBFB数据
            List<Map<String, Object>> wcxxData = wcxxMapper.getGradeDistribution(ksjhdm, kmmc, szsmc);
            Map<String, BigDecimal> savedLjbfb = new HashMap<>();

            for (Map<String, Object> data : wcxxData) {
                String grade = (String) data.get("djm");
                BigDecimal ljbfb = convertToBigDecimal(data.get("ljbfb"));
                if (grade != null && ljbfb != null) {
                    savedLjbfb.put(grade, ljbfb);
                }
            }

            // 比较两个结果
            boolean isConsistent = true;
            for (String grade : Arrays.asList("A", "B", "C", "D", "E")) {
                BigDecimal calculated = gradeAssignmentLjbfb.get(grade);
                BigDecimal saved = savedLjbfb.get(grade);

                if (calculated != null && saved != null) {
                    // 允许0.1%的误差
                    BigDecimal diff = calculated.subtract(saved).abs();
                    if (diff.compareTo(BigDecimal.valueOf(0.1)) > 0) {
                        isConsistent = false;
                        Map<String, Object> inconsistency = new HashMap<>();
                        inconsistency.put("grade", grade);
                        inconsistency.put("calculatedLjbfb", calculated);
                        inconsistency.put("savedLjbfb", saved);
                        inconsistency.put("difference", diff);
                        inconsistencies.add(inconsistency);
                    }
                } else if (calculated != null || saved != null) {
                    isConsistent = false;
                    Map<String, Object> inconsistency = new HashMap<>();
                    inconsistency.put("grade", grade);
                    inconsistency.put("calculatedLjbfb", calculated);
                    inconsistency.put("savedLjbfb", saved);
                    inconsistency.put("difference", "数据缺失");
                    inconsistencies.add(inconsistency);
                }
            }

            result.put("success", true);
            result.put("consistent", isConsistent);
            result.put("totalStudents", totalStudents);
            result.put("gradeDistribution", gradeCountMap);
            result.put("calculatedLjbfb", gradeAssignmentLjbfb);
            result.put("savedLjbfb", savedLjbfb);
            result.put("inconsistencies", inconsistencies);
            result.put("message", isConsistent ? "LJBFB计算一致" : "发现LJBFB计算不一致");

        } catch (Exception e) {
            log.error("验证LJBFB一致性失败", e);
            result.put("success", false);
            result.put("message", "验证失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 根据市州名称计算市州序号
     *
     * @param szsmc 市州名称
     * @return 市州序号
     */
    private Integer calculateSzsxh(String szsmc) {
        if (szsmc == null || szsmc.trim().isEmpty()) {
            return 0;
        }

        return switch (szsmc.trim()) {
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
            default -> {
                log.warn("未知的市州名称: {}, 使用默认序号0", szsmc);
                yield 0;
            }
        };
    }

    /**
     * 查询等级赋分结果统计
     */
    public Map<String, Object> getGradeAssignmentStatistics(String ksjhdm, String kmmc, String szsmc) {
        try {
            // 从WCXX表直接查询等级赋分统计数据
            List<Map<String, Object>> gradeStatsList = wcxxMapper.selectGradeAssignmentStats(ksjhdm, kmmc, szsmc);

            // 如果WCXX表中没有数据，返回空结果
            if (gradeStatsList == null || gradeStatsList.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("gradeDistribution", new ArrayList<>());
                result.put("queryTime", LocalDateTime.now());

                log.debug("WCXX表中未找到等级赋分统计数据: 考试计划={}, 科目={}, 城市={}",
                        ksjhdm, kmmc, szsmc);

                return result;
            }

            // SQL查询已经返回了正确的字段名，直接使用
            Map<String, Object> result = new HashMap<>();
            result.put("gradeDistribution", gradeStatsList);
            result.put("queryTime", LocalDateTime.now());

            log.debug("查询等级分布统计: 考试计划={}, 科目={}, 城市={}, 结果数量={}",
                    ksjhdm, kmmc, szsmc, gradeStatsList.size());

            return result;

        } catch (Exception e) {
            log.error("查询等级赋分统计失败", e);
            throw new RuntimeException("查询等级赋分统计失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取可用于等级赋分的城市列表
     */
    public List<String> getAvailableCities(String ksjhdm, String kmmc) {
        return kscjMapper.selectCitiesForGradeAssignment(ksjhdm, kmmc);
    }

    /**
     * 创建响应结果
     */
    private Map<String, Object> createResponse(boolean success, String message, int clearedStudentGrades,
                                               int deletedRecords) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("clearedStudentGrades", clearedStudentGrades);
        response.put("deletedRecords", deletedRecords);
        return response;
    }

    /**
     * 清除等级赋分相关缓存
     */
    private void clearGradeAssignmentCache(String ksjhdm, String kmmc, String szsmc) {
        try {
            cacheService.clearCache(ksjhdm, kmmc);
            log.debug("清除缓存完成: 考试计划={}, 科目={}", ksjhdm, kmmc);
        } catch (Exception e) {
            log.warn("清除缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 清除等级赋分
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> clearGradeAssignment(String ksjhdm, String kmmc, String szsmc) {
        try {
            log.info("开始清除等级赋分: 考试计划={}, 科目={}, 城市={}", ksjhdm, kmmc, szsmc);

            // 清除前检查
            boolean existsBefore = wcxxMapper.existsGradeAssignment(ksjhdm, kmmc, szsmc);
            if (!existsBefore) {
                log.info("没有找到需要清除的等级赋分记录");
                return createResponse(true, "没有找到需要清除的等级赋分记录", 0, 0);
            }

            // 删除等级阈值记录（WCXX表中djzdf=0的记录）
            int deletedRecords = wcxxMapper.deleteByCondition(ksjhdm, szsmc, kmmc);
            log.info("删除等级阈值记录: 影响记录数={}", deletedRecords);

            // 清除相关缓存
            clearGradeAssignmentCache(ksjhdm, kmmc, szsmc);

            log.info("等级赋分清除完成: 删除阈值记录数={}", deletedRecords);

            return createResponse(true, "等级赋分清除成功", 0, deletedRecords);

        } catch (Exception e) {
            log.error("清除等级赋分失败", e);
            throw new RuntimeException("清除等级赋分失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取等级赋分进度
     */
    public GradeAssignmentProgressVO getGradeAssignmentProgress(String taskId) {
        try {
            Map<String, Object> progress = progressService.getProgress(taskId);

            GradeAssignmentProgressVO progressVO = new GradeAssignmentProgressVO();
            progressVO.setTaskId(taskId);
            progressVO.setStatus((String) progress.getOrDefault("status", "UNKNOWN"));
            progressVO.setProgressPercentage(((Number) progress.getOrDefault("percentage", 0)).doubleValue());
            progressVO.setCurrentStep((String) progress.getOrDefault("szsmc", ""));
            progressVO.setProcessedCount(((Number) progress.getOrDefault("processed", 0)).intValue());
            progressVO.setTotalCount(((Number) progress.getOrDefault("total", 0)).intValue());

            return progressVO;
        } catch (Exception e) {
            log.error("获取等级赋分进度失败: taskId={}", taskId, e);
            throw new RuntimeException("获取等级赋分进度失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取等级赋分结果
     */
    public GradeAssignmentResultVO getGradeAssignmentResult(String ksjhdm, String kmmc, String szsmc) {
        try {
            // 查询等级分布统计
            Map<String, Object> statistics = getGradeAssignmentStatistics(ksjhdm, kmmc, szsmc);

            // 构建结果对象
            GradeAssignmentResultVO result = new GradeAssignmentResultVO();
            result.setKsjhdm(ksjhdm);
            result.setKmmc(kmmc);
            result.setStatus("SUCCESS");
            result.setMessage("等级赋分查询成功");

            // 设置统计信息
            Object gradeDistributionObj = statistics.get("gradeDistribution");
            if (gradeDistributionObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> gradeStatsList = (List<Map<String, Object>>) gradeDistributionObj;

                if (gradeStatsList.isEmpty()) {
                    result.setMessage("未找到等级赋分数据");
                    return result;
                }

                // 从List中提取统计信息
                Map<String, Integer> gradeDistribution = new HashMap<>();
                Map<String, Double> gradeThresholds = new HashMap<>();
                int totalStudents = 0;

                // 遍历每个等级的统计信息
                for (Map<String, Object> gradeStats : gradeStatsList) {
                    String grade = (String) gradeStats.get("grade");
                    Integer count = ((Number) gradeStats.get("count")).intValue();
                    Double minScore = ((Number) gradeStats.get("minScore")).doubleValue();

                    gradeDistribution.put(grade, count);
                    gradeThresholds.put(grade, minScore);
                    totalStudents += count;
                }

                result.setProcessedStudentCount(totalStudents);

                // 构建城市统计信息
                GradeAssignmentResultVO.CityGradeStatistics cityStats = GradeAssignmentResultVO.CityGradeStatistics
                        .builder()
                        .szsmc(szsmc)
                        .totalCount(totalStudents)
                        .gradeDistribution(gradeDistribution)
                        .gradeThresholds(gradeThresholds)
                        .qualifiedCount(totalStudents) // 假设所有学生都合格
                        .qualifiedRate(100.0)
                        .build();

                result.setCityGradeStatistics(Collections.singletonList(cityStats));
                result.setProcessedCityCount(1);
            }

            return result;

        } catch (Exception e) {
            log.error("获取等级赋分结果失败", e);
            throw new RuntimeException("获取等级赋分结果失败: " + e.getMessage(), e);
        }
    }

    /**
     * 城市阈值计算结果
     */
    @Getter
    private static class CityThresholdsResult {
        private final Map<String, GradeThresholdsDTO> successfulCities;
        private final List<String> failedCities;

        public CityThresholdsResult(Map<String, GradeThresholdsDTO> successfulCities, List<String> failedCities) {
            this.successfulCities = successfulCities;
            this.failedCities = failedCities;
        }

    }
}