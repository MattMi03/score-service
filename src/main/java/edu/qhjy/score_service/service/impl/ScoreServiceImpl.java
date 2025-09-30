package edu.qhjy.score_service.service.impl;

import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.domain.dto.ExamScoreQueryDTO;
import edu.qhjy.score_service.domain.dto.InitializeExamStudentsDTO;
import edu.qhjy.score_service.domain.dto.ScoreUpdateDTO;
import edu.qhjy.score_service.domain.entity.KscjEntity;
import edu.qhjy.score_service.domain.entity.KsjhEntity;
import edu.qhjy.score_service.domain.entity.KskmxxEntity;
import edu.qhjy.score_service.domain.vo.*;
import edu.qhjy.score_service.mapper.primary.*;
import edu.qhjy.score_service.service.ScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.aop.framework.AopContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成绩管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreServiceImpl implements ScoreService {

    private final KskmxxMapper kskmxxMapper;
    private final KscjMapper kscjMapper;
    private final YjxhMapper yjxhMapper;
    private final KqxxMapper kqxxMapper;
    private final XxjbxxMapper xxjbxxMapper;

    // 二级数据源相关Mapper
    private final KsjhMapper ksjhMapper;

    // Redis模板
    private final RedisTemplate<String, Object> redisTemplate;

    private ScoreStatisticsVO calculateStatistics(String kmmc) {
        List<KscjEntity> scores = kscjMapper.selectByKmmc(kmmc);

        if (scores.isEmpty()) {
            return new ScoreStatisticsVO();
        }

        ScoreStatisticsVO statistics = new ScoreStatisticsVO();

        List<BigDecimal> scoreList = scores.stream()
                .map(KscjEntity::getFslkscj)
                .filter(Objects::nonNull)
                .map(BigDecimal::new)
                .toList();

        if (!scoreList.isEmpty()) {
            // 计算基本统计信息
            BigDecimal sum = scoreList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            statistics.setAverageScore(sum.divide(BigDecimal.valueOf(scoreList.size()), 2, RoundingMode.HALF_UP));
            statistics.setMaxScore(scoreList.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            statistics.setMinScore(scoreList.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO));

            // 计算合格率
            long passCount = scores.stream()
                    .filter(s -> "合格".equals(s.getCjhgm()))
                    .count();
            statistics.setPassCount((int) passCount);
            statistics.setFailCount(scores.size() - (int) passCount);
            statistics.setPassRate(BigDecimal.valueOf(passCount)
                    .divide(BigDecimal.valueOf(scores.size()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));

            // 计算分数段分布
            Map<String, Integer> distribution = new LinkedHashMap<>();
            distribution.put("90-100", 0);
            distribution.put("80-89", 0);
            distribution.put("70-79", 0);
            distribution.put("60-69", 0);
            distribution.put("0-59", 0);

            for (BigDecimal score : scoreList) {
                // 成绩数据在导入时已经四舍五入为整数，直接转换
                int s = score.intValue();
                if (s >= 90) {
                    distribution.put("90-100", distribution.get("90-100") + 1);
                } else if (s >= 80) {
                    distribution.put("80-89", distribution.get("80-89") + 1);
                } else if (s >= 70) {
                    distribution.put("70-79", distribution.get("70-79") + 1);
                } else if (s >= 60) {
                    distribution.put("60-69", distribution.get("60-69") + 1);
                } else {
                    distribution.put("0-59", distribution.get("0-59") + 1);
                }
            }
            statistics.setScoreDistribution(distribution);
        }

        return statistics;
    }

    @Override
    public List<KskmxxEntity> listTemplates() {
        log.info("查询所有模板（科目）列表");
        return kskmxxMapper.selectAll();
    }

    @Override
    public KskmxxEntity getTemplate(String ksjhdm, String kmdm) {
        log.info("查询模板（科目）详情，考试计划代码: {}, 科目代码: {}", ksjhdm, kmdm);
        KskmxxEntity template = kskmxxMapper.selectByKsjhdmAndKmdm(ksjhdm, kmdm);
        if (template == null) {
            throw new RuntimeException("模板（科目）不存在，考试计划代码: " + ksjhdm + ", 科目代码: " + kmdm);
        }
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(String ksjhdm, String kmdm) {
        log.info("删除模板（科目），考试计划代码: {}, 科目代码: {}", ksjhdm, kmdm);

        // 检查模板是否存在
        KskmxxEntity template = kskmxxMapper.selectByKsjhdmAndKmdm(ksjhdm, kmdm);
        if (template == null) {
            throw new RuntimeException("模板（科目）不存在，考试计划代码: " + ksjhdm + ", 科目代码: " + kmdm);
        }

        // 检查是否有关联的成绩数据
        List<KscjEntity> relatedScores = kscjMapper.selectByKmmc(template.getKmmc());
        if (!relatedScores.isEmpty()) {
            throw new RuntimeException("该模板（科目）下存在成绩数据，无法删除。请先删除相关成绩数据。");
        }

        // 注释：已移除分项配置相关逻辑

        // 删除科目信息
        kskmxxMapper.deleteByKsjhdmAndKmdm(ksjhdm, kmdm);

        log.info("模板（科目）删除成功，考试计划代码: {}, 科目代码: {}", ksjhdm, kmdm);
    }

    // ==================== 统计分析方法实现 ====================

    @Override
    @Cacheable(value = "statistics", key = "'area_stats_' + #subjectName + '_' + #examPlanCode + '_' + #areaType + '_' + (#parentArea ?: 'all')")
    public AreaScoreStatisticsVO getAreaScoreStatistics(String subjectName, String examPlanCode,
                                                        String areaType, String parentArea) {
        log.info("获取区域成绩统计分布：科目={}, 考试计划={}, 区域类型={}, 上级区域={}",
                subjectName, examPlanCode, areaType, parentArea);

        try {
            // 查询区域统计数据
            List<Map<String, Object>> statisticsData = kscjMapper.selectAreaScoreStatistics(
                    examPlanCode, subjectName, areaType, parentArea);

            AreaScoreStatisticsVO result = new AreaScoreStatisticsVO();
            result.setSubjectName(subjectName);
            result.setExamPlanCode(examPlanCode);
            result.setAreaType(areaType);
            result.setAreaName(StringUtils.hasText(parentArea) ? parentArea : "全部");

            List<AreaScoreStatisticsVO.AreaStatisticsData> dataList = new ArrayList<>();

            for (Map<String, Object> row : statisticsData) {
                AreaScoreStatisticsVO.AreaStatisticsData data = new AreaScoreStatisticsVO.AreaStatisticsData();
                data.setAreaName((String) row.get("area_name"));
                data.setAverageScore(convertToBigDecimal(row.get("avg_score")));
                data.setMaxScore(convertToBigDecimal(row.get("max_score")));
                data.setMinScore(convertToBigDecimal(row.get("min_score")));

                // 安全的类型转换，处理Long到Integer的转换
                Object totalCountObj = row.get("total_count");
                data.setTotalCount(totalCountObj != null ? ((Number) totalCountObj).intValue() : 0);

                Object passCountObj = row.get("pass_count");
                data.setPassCount(passCountObj != null ? ((Number) passCountObj).intValue() : 0);

                data.setPassRate(convertToBigDecimal(row.get("pass_rate")));

                dataList.add(data);
            }

            result.setStatisticsData(dataList);

            log.info("成功获取区域成绩统计分布，共 {} 个区域", dataList.size());
            return result;

        } catch (Exception e) {
            log.error("获取区域成绩统计分布失败", e);
            throw new RuntimeException("获取区域成绩统计分布失败: " + e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "statistics", key = "'grade_dist_' + #subjectName + '_' + #examPlanCode + '_' + (#areaFilter ?: 'all')")
    public SubjectGradeDistributionVO getSubjectGradeDistribution(String subjectName, String examPlanCode,
                                                                  String areaFilter) {
        log.info("获取科目成绩等级分布：科目={}, 考试计划={}, 区域筛选={}",
                subjectName, examPlanCode, areaFilter);

        try {
            // 根据areaFilter确定区域级别和区域代码
            String areaLevel = null;
            String areaCode = null;

            if (StringUtils.hasText(areaFilter)) {
                // 根据区域名称判断区域级别
                // 这里需要根据实际的区域命名规则来判断
                if (areaFilter.contains("市")) {
                    areaLevel = "city";
                    areaCode = areaFilter;
                } else if (areaFilter.contains("区") || areaFilter.contains("县")) {
                    areaLevel = "county";
                    areaCode = areaFilter;
                } else {
                    // 默认按学校处理
                    areaLevel = "school";
                    areaCode = areaFilter;
                }
                log.info("区域过滤参数：areaFilter={}, 解析后 areaLevel={}, areaCode={}",
                        areaFilter, areaLevel, areaCode);
            } else {
                log.info("未设置区域过滤条件，将查询全部数据");
            }

            // 查询等级分布数据
            log.info("调用Mapper查询：examPlanCode={}, subjectName={}, areaLevel={}, areaCode={}",
                    examPlanCode, subjectName, areaLevel, areaCode);
            List<Map<String, Object>> gradeData = kscjMapper.selectSubjectGradeDistribution(
                    examPlanCode, subjectName, areaLevel, areaCode);

            log.info("Mapper查询结果：共返回 {} 条记录", gradeData.size());
            if (!gradeData.isEmpty()) {
                log.info("查询结果示例：{}", gradeData.get(0));
            }

            SubjectGradeDistributionVO result = new SubjectGradeDistributionVO();
            result.setSubjectName(subjectName);
            result.setExamPlanCode(examPlanCode);
            result.setAreaFilter(StringUtils.hasText(areaFilter) ? areaFilter : "全部");

            // 处理等级分布数据
            List<SubjectGradeDistributionVO.GradeDistributionData> gradeDistribution = new ArrayList<>();
            List<SubjectGradeDistributionVO.ScoreRangeDistributionData> scoreRangeDistribution = new ArrayList<>();

            // 计算总人数 - 只统计等级分布的总和，避免重复计算
            int totalCount = 0;

            log.info("开始处理查询结果，共 {} 条记录", gradeData.size());

            for (Map<String, Object> row : gradeData) {
                String gradeCode = (String) row.get("grade_code");
                String scoreRange = (String) row.get("score_range");
                Object countObj = row.get("count");
                Integer count = countObj != null ? ((Number) countObj).intValue() : 0;

                log.info("处理数据行：gradeCode={}, scoreRange={}, count={}", gradeCode, scoreRange, count);

                // 等级分布数据
                if (StringUtils.hasText(gradeCode)) {
                    // 累计总人数（只统计等级分布，避免重复计算）
                    totalCount += count;
                    log.info("累计等级分布：gradeCode={}, count={}, 当前totalCount={}", gradeCode, count, totalCount);

                    SubjectGradeDistributionVO.GradeDistributionData gradeItem = new SubjectGradeDistributionVO.GradeDistributionData();
                    gradeItem.setGradeCode(gradeCode);
                    gradeItem.setCount(count);
                    // 百分比稍后统一计算
                    gradeDistribution.add(gradeItem);
                }

                // 分数段分布数据
                if (StringUtils.hasText(scoreRange)) {
                    log.info("处理分数段分布：scoreRange={}, count={}", scoreRange, count);
                    SubjectGradeDistributionVO.ScoreRangeDistributionData rangeItem = new SubjectGradeDistributionVO.ScoreRangeDistributionData();
                    rangeItem.setScoreRange(scoreRange);
                    rangeItem.setCount(count);
                    // 百分比稍后统一计算
                    scoreRangeDistribution.add(rangeItem);
                }
            }

            log.info("数据处理完成，最终totalCount={}, 等级分布条数={}, 分数段分布条数={}",
                    totalCount, gradeDistribution.size(), scoreRangeDistribution.size());

            // 设置总人数
            result.setTotalCount(totalCount);

            // 重新计算百分比（基于正确的totalCount）
            for (SubjectGradeDistributionVO.GradeDistributionData item : gradeDistribution) {
                BigDecimal percentage = totalCount > 0
                        ? BigDecimal.valueOf(item.getCount() * 100.0 / totalCount).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                item.setPercentage(percentage);
            }

            for (SubjectGradeDistributionVO.ScoreRangeDistributionData item : scoreRangeDistribution) {
                BigDecimal percentage = totalCount > 0
                        ? BigDecimal.valueOf(item.getCount() * 100.0 / totalCount).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                item.setPercentage(percentage);
            }

            result.setGradeDistribution(gradeDistribution);
            result.setScoreRangeDistribution(scoreRangeDistribution);

            log.info("成功获取科目成绩等级分布，总人数={}, 等级分布={}, 分数段分布={}",
                    totalCount, gradeDistribution.size(), scoreRangeDistribution.size());
            return result;

        } catch (Exception e) {
            log.error("获取科目成绩等级分布失败", e);
            throw new RuntimeException("获取科目成绩等级分布失败: " + e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "trend_analysis", key = "'trend_' + #subjectName + '_' + (#areaFilter ?: 'all') + '_' + (#startYear ?: 'all') + '_' + (#endYear ?: 'all')")
    public ScoreTrendAnalysisVO getHistoricalTrends(String subjectName, String areaFilter,
                                                    Integer startYear, Integer endYear) {
        log.info("获取历史成绩趋势分析：科目={}, 区域筛选={}, 开始年份={}, 结束年份={}",
                subjectName, areaFilter, startYear, endYear);

        try {
            // 查询历史趋势数据
            List<Map<String, Object>> trendData = kscjMapper.selectHistoricalTrends(
                    subjectName, "all", areaFilter,
                    startYear != null ? startYear.toString() : null,
                    endYear != null ? endYear.toString() : null);

            ScoreTrendAnalysisVO result = new ScoreTrendAnalysisVO();
            result.setKmmc(subjectName);
            result.setAreaLevel("all");
            result.setAreaCode(StringUtils.hasText(areaFilter) ? areaFilter : "全部");

            List<ScoreTrendAnalysisVO.TrendDataPoint> trendPoints = new ArrayList<>();

            for (Map<String, Object> row : trendData) {
                ScoreTrendAnalysisVO.TrendDataPoint point = new ScoreTrendAnalysisVO.TrendDataPoint();
                point.setKsjhdm((String) row.get("exam_plan_code"));
                point.setYear(row.get("year").toString());
                point.setPeriod((String) row.get("period"));
                point.setAvgScore(convertToBigDecimal(row.get("avg_score")));
                point.setMaxScore(convertToBigDecimal(row.get("max_score")));
                point.setMinScore(convertToBigDecimal(row.get("min_score")));

                // 安全的类型转换，处理Long到Integer的转换
                Object totalCountObj = row.get("total_count");
                point.setTotalCount(totalCountObj != null ? ((Number) totalCountObj).intValue() : 0);

                Object passCountObj = row.get("pass_count");
                point.setPassCount(passCountObj != null ? ((Number) passCountObj).intValue() : 0);

                point.setPassRate(convertToBigDecimal(row.get("pass_rate")));

                trendPoints.add(point);
            }

            result.setTrendData(trendPoints);

            // 计算趋势摘要
            if (!trendPoints.isEmpty()) {
                ScoreTrendAnalysisVO.TrendSummary summary = calculateTrendSummary(trendPoints);
                result.setSummary(summary);
            }

            log.info("成功获取历史成绩趋势分析，共 {} 个数据点", trendPoints.size());
            return result;

        } catch (Exception e) {
            log.error("获取历史成绩趋势分析失败", e);
            throw new RuntimeException("获取历史成绩趋势分析失败: " + e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "score_lists", key = "'exam_plans_' + (#subjectName ?: 'all')")
    public List<String> getAvailableExamPlans(String subjectName) {
        log.info("获取可用的考试计划列表：科目={}", subjectName);

        try {
            List<String> examPlans = kscjMapper.selectAvailableExamPlans(subjectName);

            log.info("成功获取可用的考试计划列表，共 {} 个", examPlans.size());
            return examPlans;

        } catch (Exception e) {
            log.error("获取可用的考试计划列表失败", e);
            throw new RuntimeException("获取可用的考试计划列表失败: " + e.getMessage());
        }
    }

    @Override
    public List<AreaHierarchyVO> getAreaHierarchyEnhanced(String parentArea, String areaType) {
        log.info("[Service层] 获取增强区域层级数据：上级区域={}, 区域类型={}", parentArea, areaType);

        if ("class".equalsIgnoreCase(areaType)) {
            if (parentArea == null || parentArea.length() < 13) { // 9位xxdm + 4位年级
                throw new IllegalArgumentException("班级区域类型必须指定完整的学校代码+年级，例如'6323210312024'");
            }
        }

        try {
            // 验证areaType是否为有效的区域级别
            if (!isValidAreaLevel(areaType)) {
                log.warn("无效的区域类型: {}, 使用默认值 'city'", areaType);
                areaType = "city";
            }

            // 兼容code参数：如果parentArea是code，转换为name
            String actualParentArea = convertCodeToNameIfNeeded(parentArea, getParentAreaType(areaType));
            log.info("原始parentArea: {}, 转换后actualParentArea: {}", parentArea, actualParentArea);

            // 使用转换后的name作为缓存key，确保code和name参数使用相同的缓存
            // 通过代理对象调用，确保@Cacheable注解生效
            ScoreServiceImpl proxy = (ScoreServiceImpl) AopContext.currentProxy();
            return proxy.getAreaHierarchyCached(actualParentArea, areaType);
        } catch (Exception e) {
            log.error("获取增强区域层级数据失败", e);
            return new ArrayList<>();
        }
    }

    @Cacheable(
            value = "score_lists",
            key = "'area_hierarchy_enhanced_' + (#actualParentArea ?: 'all') + '_' + #areaType",
            unless = "true"
    )
    public List<AreaHierarchyVO> getAreaHierarchyCached(String actualParentArea, String areaType) {
        log.info("[缓存方法] 执行数据库查询：actualParentArea={}, areaType={}", actualParentArea, areaType);

        List<Map<String, Object>> areaData = kscjMapper.selectAreaHierarchy(areaType, actualParentArea);
        List<AreaHierarchyVO> areas = new ArrayList<>();

        for (Map<String, Object> row : areaData) {
            String areaCode = (String) row.get("area_code");
            String areaName = (String) row.get("area_name");
            String parentCode = (String) row.get("parent_code");
            String parentName = (String) row.get("parent_name");
            String level = (String) row.get("area_level");

            // 从SQL查询结果中直接获取子级数量，避免额外查询
            Object childrenCountObj = row.get("children_count");
            Integer childrenCount = childrenCountObj != null ? ((Number) childrenCountObj).intValue() : 0;

            if (areaName != null) {
                AreaHierarchyVO areaVO = AreaHierarchyVO.builder()
                        .code(areaCode != null ? areaCode : areaName)
                        .name(areaName)
                        .level(level != null ? level : areaType)
                        .parentCode(parentCode)
                        .parentName(parentName)
                        .hasChildren(determineHasChildren(areaType))
                        .childrenCount(childrenCount)
                        .build();
                areas.add(areaVO);
            }
        }

        log.info("成功获取增强区域层级数据，共 {} 个", areas.size());
        return areas;
    }

    /**
     * 判断当前区域级别是否有下级区域
     */
    private Boolean determineHasChildren(String areaType) {
        return switch (areaType) {
            case "city" -> true; // 地市有考区
            case "county" -> true; // 考区有学校
            case "school" -> true; // 学校有级别
            case "grade" -> true; // 级别有班级
            case "class" -> false; // 班级没有下级
            default -> false;
        };
    }

    /**
     * 获取下级区域数量
     */
    @Cacheable(value = "childrenCount", key = "#areaName + '_' + #areaType")
    public Integer getChildrenCount(String areaName, String areaType) {
        try {
            String childAreaType = getChildAreaType(areaType);
            if (childAreaType == null) {
                return 0;
            }

            List<Map<String, Object>> childData = kscjMapper.selectAreaHierarchy(childAreaType, areaName);
            return childData.size();
        } catch (Exception e) {
            log.warn("获取下级区域数量失败：{}", e.getMessage());
            return 0;
        }
    }

    /**
     * 获取下级区域类型
     */
    private String getChildAreaType(String areaType) {
        return switch (areaType) {
            case "city" -> "county";
            case "county" -> "school";
            case "school" -> "grade";
            case "grade" -> "class";
            default -> null;
        };
    }

    /**
     * 获取父级区域类型
     */
    private String getParentAreaType(String areaType) {
        return switch (areaType) {
            case "county" -> "city";
            case "school" -> "county";
            case "grade" -> "school";
            case "class" -> "grade";
            default -> null;
        };
    }

    @Override
    @Cacheable(value = "score_lists", key = "'available_subjects'")
    public List<String> getAvailableSubjects() {
        log.info("获取可用的科目列表");

        try {
            List<String> subjects = kscjMapper.selectAvailableSubjects();

            log.info("成功获取可用的科目列表，共 {} 个", subjects.size());
            return subjects;

        } catch (Exception e) {
            log.error("获取可用的科目列表失败", e);
            throw new RuntimeException("获取可用的科目列表失败: " + e.getMessage());
        }
    }

    /**
     * 验证区域级别是否有效
     */
    private boolean isValidAreaLevel(String areaLevel) {
        return ("city".equals(areaLevel) || "county".equals(areaLevel) ||
                "school".equals(areaLevel) || "grade".equals(areaLevel) || "class".equals(areaLevel));
    }

    /**
     * 根据区域类型获取对应的code值
     *
     * @param areaType 区域类型
     * @param areaName 区域名称
     * @return 对应的code值
     */
    @Cacheable(value = "areaCode", key = "#areaType + '_' + #areaName")
    public String getAreaCodeByType(String areaType, String areaName) {
        try {
            String result = null;
            switch (areaType) {
                case "city":
                    // 从kqxx表中查询SSXZQHDM
                    result = kqxxMapper.selectSsxzqhdmByName(areaName);
                    log.info("查询city代码: areaName={}, result={}", areaName, result);
                    break;
                case "county":
                    // 从kqxx表中查询KQDM
                    result = kqxxMapper.selectKqdmByName(areaName);
                    log.info("查询county代码: areaName={}, result={}", areaName, result);
                    break;
                case "school":
                    // 从xxjbxx表中查询XXDM
                    result = xxjbxxMapper.selectXxdmByName(areaName);
                    log.info("查询school代码: areaName={}, result={}", areaName, result);
                    break;
                case "grade":
                    // 年级直接使用原始值作为code
                    result = areaName;
                    break;
                case "class":
                    // 班级保持现有逻辑不变，直接返回name作为code
                    result = areaName;
                    break;
                default:
                    log.warn("未知的区域类型: {}", areaType);
                    break;
            }
            return result;
        } catch (Exception e) {
            log.error("获取区域代码失败，areaType: {}, areaName: {}, 错误: {}", areaType, areaName, e.getMessage());
            // 查询失败时返回null，不再使用name作为fallback
            return null;
        }
    }

    /**
     * 兼容code参数：如果传入的是code，转换为name；如果是name，直接返回
     */
    private String convertCodeToNameIfNeeded(String parentArea, String parentAreaType) {
        if (parentArea == null || parentAreaType == null) {
            return parentArea;
        }

        try {
            if ("grade".equalsIgnoreCase(parentAreaType)) {
                // 假设前9位是学校代码，后4位是年级
                String schoolCode = parentArea.substring(0, 9);
                String grade = parentArea.substring(9);

                String schoolName = xxjbxxMapper.selectXxmcByCode(schoolCode);
                if (schoolName != null) {
                    // 拼接成 class 查询用的 parentName：学校名称 + 年级
                    return schoolName + grade;
                } else {
                    return parentArea;
                }
            }

            // 原有逻辑
            String nameFromCode = getAreaNameByCode(parentAreaType, parentArea);
            if (nameFromCode != null) {
                log.info("Code转换成功：{} -> {}", parentArea, nameFromCode);
                return nameFromCode;
            } else {
                return parentArea;
            }
        } catch (Exception e) {
            log.warn("Code转换失败，使用原值：{}, 错误: {}", parentArea, e.getMessage());
            return parentArea;
        }
    }
    /**
     * 根据区域类型和代码获取对应的名称
     */
    private String getAreaNameByCode(String areaType, String areaCode) {
        if (areaCode == null || areaType == null) {
            return null;
        }

        try {
            String result = null;
            switch (areaType.toLowerCase()) {
                case "city":
                    result = kqxxMapper.selectSsxzqhmcByCode(areaCode);
                    break;
                case "county":
                    result = kqxxMapper.selectKqmcByCode(areaCode);
                    break;
                case "school":
                    result = xxjbxxMapper.selectXxmcByCode(areaCode);
                    break;
                case "grade":
                    // 对于grade，name就是code本身
                    result = areaCode;
                    break;
                default:
                    log.warn("未知的区域类型: {}", areaType);
                    break;
            }
            return result;
        } catch (Exception e) {
            log.error("根据代码获取区域名称失败，areaType: {}, areaCode: {}, 错误: {}", areaType, areaCode, e.getMessage());
            return null;
        }
    }

    // /**
    // * 获取等级名称
    // */
    // private String getGradeName(String gradeCode) {
    // return switch (gradeCode) {
    // case "A" -> "优秀";
    // case "B" -> "良好";
    // case "C" -> "中等";
    // case "D" -> "及格";
    // case "E" -> "不及格";
    // default -> gradeCode;
    // };
    // }

    /**
     * 计算趋势摘要
     */
    private ScoreTrendAnalysisVO.TrendSummary calculateTrendSummary(
            List<ScoreTrendAnalysisVO.TrendDataPoint> trendPoints) {
        ScoreTrendAnalysisVO.TrendSummary summary = new ScoreTrendAnalysisVO.TrendSummary();

        summary.setDataPointCount(trendPoints.size());

        if (trendPoints.size() >= 2) {
            ScoreTrendAnalysisVO.TrendDataPoint first = trendPoints.get(0);
            ScoreTrendAnalysisVO.TrendDataPoint last = trendPoints.get(trendPoints.size() - 1);

            // 计算平均分变化
            BigDecimal avgScoreChange = last.getAvgScore().subtract(first.getAvgScore());
            summary.setAvgScoreChange(avgScoreChange);
            summary.setAvgScoreTrend(avgScoreChange.compareTo(BigDecimal.ZERO) > 0 ? "上升"
                    : avgScoreChange.compareTo(BigDecimal.ZERO) < 0 ? "下降" : "稳定");

            // 计算及格率变化
            BigDecimal passRateChange = last.getPassRate().subtract(first.getPassRate());
            summary.setPassRateChange(passRateChange);
            summary.setPassRateTrend(passRateChange.compareTo(BigDecimal.ZERO) > 0 ? "上升"
                    : passRateChange.compareTo(BigDecimal.ZERO) < 0 ? "下降" : "稳定");

            // 找出最好和最差的时期
            ScoreTrendAnalysisVO.TrendDataPoint bestPeriod = trendPoints.stream()
                    .max(Comparator.comparing(ScoreTrendAnalysisVO.TrendDataPoint::getAvgScore))
                    .orElse(first);
            ScoreTrendAnalysisVO.TrendDataPoint worstPeriod = trendPoints.stream()
                    .min(Comparator.comparing(ScoreTrendAnalysisVO.TrendDataPoint::getAvgScore))
                    .orElse(first);

            summary.setBestPeriod(bestPeriod.getYear() + bestPeriod.getPeriod());
            summary.setWorstPeriod(worstPeriod.getYear() + worstPeriod.getPeriod());
        }

        return summary;
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

    @Override
    public List<ExamPlanStatisticsVO> getExamPlanStatistics(String ksjhdm) {
        log.info("获取考试计划统计信息，考试计划代码: {}", ksjhdm);

        try {
            // 调用YjxhMapper查询考试计划科目统计信息
            List<Map<String, Object>> statisticsData = yjxhMapper.selectExamPlanSubjectStatistics(ksjhdm);

            List<ExamPlanStatisticsVO> result = new ArrayList<>();

            for (Map<String, Object> row : statisticsData) {
                String examPlanCode = (String) row.get("ksjhdm");
                String ksjhmc = (String) row.get("ksjhmc");

                ExamPlanStatisticsVO vo = ExamPlanStatisticsVO.builder()
                        .ksjhdm(examPlanCode)
                        .ksjhmc(ksjhmc)
                        .kmmc((String) row.get("kmmc"))
                        .studentCount(((Number) row.get("student_count")).intValue())
                        .build();
                result.add(vo);
            }

            log.info("成功获取考试计划统计信息，共 {} 条记录", result.size());
            return result;

        } catch (Exception e) {
            log.error("获取考试计划统计信息失败", e);
            throw new RuntimeException("获取考试计划统计信息失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> getSubjectsByKsjhdmAndKmlx(String ksjhdm, Integer kmlx) {
        log.info("根据考试计划代码和科目类型查询科目列表，考试计划代码：{}，科目类型：{}", ksjhdm, kmlx);
        try {
            List<String> subjects;
            if (kmlx == null) {
                // 当kmlx为null时，查询所有科目类型（kmlx=0和kmlx=1）
                subjects = kskmxxMapper.selectKmmcByKsjhdmAllTypes(ksjhdm);
                log.info("查询所有科目类型，返回{}个科目", subjects.size());
            } else {
                subjects = kskmxxMapper.selectKmmcByKsjhdmAndKmlx(ksjhdm, kmlx);
                log.info("查询科目列表成功，返回{}个科目", subjects.size());
            }
            return subjects;
        } catch (Exception e) {
            log.error("查询科目列表失败", e);
            throw new RuntimeException("查询科目列表失败：" + e.getMessage());
        }
    }

    @Override
    public PageResult<ExamScoreVO> getExamScoresWithPagination(ExamScoreQueryDTO query) {
        // 查询总数
        Long total = kscjMapper.countExamScores(query);

        // 查询数据列表
        List<ExamScoreVO> records = kscjMapper.selectExamScoresWithPagination(query);

        // 构建分页结果
        return PageResult.of(records, query.getPageNum(), query.getPageSize(), total);
    }

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public InitializeResultVO initializeExamStudents(InitializeExamStudentsDTO request) {
        log.info("开始初始化考试科目考生，参数：{}", request);

        try {
            // 1. 查询KSJHMC（考试计划名称）
            String ksjhmc = getKsjhmc(request.getKsjhdm());

            // 2. 查询符合条件的报名学生信息
            List<Map<String, Object>> students = kscjMapper.selectStudentsForInitialize(
                    request.getKsjhdm(),
                    request.getKmmc(),
                    request.getSzsmc(),
                    request.getKqmc(),
                    request.getXxmc());

            if (students.isEmpty()) {
                return InitializeResultVO.builder()
                        .success(true)
                        .totalStudents(0)
                        .successCount(0)
                        .failCount(0)
                        .schoolCount(0)
                        .message("未找到符合条件的学生")
                        .details("根据指定条件未查询到需要初始化的学生信息")
                        .build();
            }

            // 3. 统计学校数量
            Set<String> schools = students.stream()
                    .map(student -> (String) student.get("xxmc"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // 4. 批量检查已存在的成绩记录（优化N+1查询问题）
            List<String> allKshList = students.stream()
                    .map(student -> (String) student.get("ksh"))
                    .collect(Collectors.toList());

            Set<String> existingKshSet = new HashSet<>();
            if (!allKshList.isEmpty()) {
                List<String> existingKshList = kscjMapper.selectExistingKsh(
                        request.getKsjhdm(),
                        request.getKmmc(),
                        allKshList);
                existingKshSet = new HashSet<>(existingKshList);
                log.info("批量查询已存在成绩记录，总学生数：{}，已存在记录数：{}", allKshList.size(), existingKshSet.size());
            }

            // 5. 批量初始化成绩记录
            List<KscjEntity> scoreRecords = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;

            for (Map<String, Object> student : students) {
                try {
                    String ksh = (String) student.get("ksh");
                    // 检查是否已存在成绩记录（使用批量查询结果）
                    if (existingKshSet.contains(ksh)) {
                        log.debug("学生{}的{}科目成绩记录已存在，跳过初始化", ksh, request.getKmmc());
                        continue;
                    }

                    // 创建成绩记录
                    KscjEntity scoreRecord = new KscjEntity();
                    scoreRecord.setKsjhdm(request.getKsjhdm());
                    scoreRecord.setKsjhmc(ksjhmc); // 设置考试计划名称
                    scoreRecord.setKmmc(request.getKmmc());
                    scoreRecord.setKsh(ksh);
                    scoreRecord.setKklxmc("正考"); // 设置开考类型名称
                    scoreRecord.setKmlx(1); // 考查性科目
                    // 设置阅卷序号
                    scoreRecord.setYjxh((String) student.get("yjxh"));
                    // TODO:有关cjr, gxr字段的设置需要从登陆信息中获取
                    scoreRecord.setCjrxm("系统初始化");
                    scoreRecord.setCjsj(LocalDateTime.now());

                    scoreRecords.add(scoreRecord);
                    successCount++;

                } catch (Exception e) {
                    String ksh = (String) student.get("ksh");
                    log.error("初始化学生{}成绩记录失败: {}", ksh, e.getMessage());
                    failCount++;
                }
            }

            // 6. 分批执行批量插入（按文档要求实现分批执行）
            if (!scoreRecords.isEmpty()) {
                int batchSize = 1000; // 每批1000条记录
                int totalInserted = 0;

                for (int i = 0; i < scoreRecords.size(); i += batchSize) {
                    int endIndex = Math.min(i + batchSize, scoreRecords.size());
                    List<KscjEntity> batch = scoreRecords.subList(i, endIndex);
                    int insertCount = kscjMapper.batchInsert(batch);
                    totalInserted += insertCount;
                    log.debug("批量插入第{}批，插入{}条记录", (i / batchSize + 1), insertCount);
                }

                log.info("批量插入成绩记录完成，总共插入{}条记录", totalInserted);
            }

            // 7. 构建返回结果
            InitializeResultVO result = InitializeResultVO.builder()
                    .success(true)
                    .totalStudents(students.size())
                    .successCount(successCount)
                    .failCount(failCount)
                    .schoolCount(schools.size())
                    .message("初始化完成")
                    .details(String.format("成功初始化%d名学生的%s科目考试记录，涉及%d所学校",
                            successCount, request.getKmmc(), schools.size()))
                    .build();

            log.info("初始化考试科目考生完成，结果：{}", result);
            return result;

        } catch (Exception e) {
            log.error("初始化考试科目考生失败", e);
            // 抛出异常以触发事务回滚
            throw new RuntimeException("初始化考试科目考生失败：" + e.getMessage(), e);
        }
    }

    /**
     * 根据考试计划代码查询考试计划名称
     *
     * @param ksjhdm 考试计划代码
     * @return 考试计划名称
     */
    private String getKsjhmc(String ksjhdm) {
        try {
            KsjhEntity ksjh = ksjhMapper.selectByKsjhdm(ksjhdm);
            if (ksjh != null && StringUtils.hasText(ksjh.getKsjhmc())) {
                log.debug("查询到考试计划名称: {} -> {}", ksjhdm, ksjh.getKsjhmc());
                return ksjh.getKsjhmc();
            } else {
                log.warn("未找到考试计划代码 {} 对应的考试计划名称或名称为空", ksjhdm);
                return "考查性科目考试"; // 默认值
            }
        } catch (Exception e) {
            log.error("获取考试计划名称失败: ksjhdm={}", ksjhdm, e);
            return "考查性科目考试"; // 默认值
        }
    }

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public byte[] generateExcelTemplate(String ksjhdm, String kmmc, String szsmc, String kqmc, String xxmc) {
        log.info("开始生成Excel导入模板，考试计划代码: {}, 科目名称: {}, 地市: {}, 考区: {}, 学校: {}",
                ksjhdm, kmmc, szsmc, kqmc, xxmc);

        // 参数验证
        if (ksjhdm == null || ksjhdm.trim().isEmpty()) {
            throw new RuntimeException("生成Excel模板失败：考试计划代码(ksjhdm)参数为必传，不能为空");
        }
        if (kmmc == null || kmmc.trim().isEmpty()) {
            throw new RuntimeException("生成Excel模板失败：科目名称(kmmc)参数为必传，不能为空");
        }
        if (szsmc == null || szsmc.trim().isEmpty()) {
            throw new RuntimeException("生成Excel模板失败：地市名称(szsmc)参数为必传，不能为空");
        }
        if (kqmc == null || kqmc.trim().isEmpty()) {
            throw new RuntimeException("生成Excel模板失败：考区名称(kqmc)参数为必传，不能为空");
        }
        if (xxmc == null || xxmc.trim().isEmpty()) {
            throw new RuntimeException("生成Excel模板失败：学校名称(xxmc)参数为必传，不能为空");
        }

        // 1. 预检查和自动初始化逻辑（双重验证：Redis缓存 + 数据库记录数）
        boolean needInitialize = false;
        int existingRecordCount = 0; // 声明变量并初始化
        Boolean isInitialized;

        // 使用完整的缓存键
        String initCacheKey = String.format("score:init_flag:%s:%s:%s:%s:%s",
                ksjhdm, kmmc, szsmc,
                kqmc != null ? kqmc : "all",
                xxmc != null ? xxmc : "all");

        // 1.1 检查Redis缓存标志位 - 优先级最高
        isInitialized = (Boolean) redisTemplate.opsForValue().get(initCacheKey);

        if (isInitialized == null || !isInitialized) {
            // 1.2 缓存键不存在或未初始化时，检查数据库中是否存在对应地区的成绩记录
            existingRecordCount = kscjMapper.countExistingRecordsWithArea(ksjhdm, kmmc, szsmc, kqmc, xxmc);
            needInitialize = (existingRecordCount == 0);
        }
        // 如果缓存键存在且已初始化，直接跳过初始化

        if (needInitialize) {
            log.info("检测到需要初始化，开始自动初始化kscj表数据，条件: ksjhdm={}, kmmc={}, szsmc={}, kqmc={}, xxmc={}, 缓存标志: {}, 记录数: {}",
                    ksjhdm, kmmc, szsmc, kqmc, xxmc, isInitialized, existingRecordCount);

            // 检查szsmc参数
            if (szsmc == null || szsmc.trim().isEmpty()) {
                throw new RuntimeException("生成Excel模板失败：地市名称(szsmc)参数为必传，不能为空");
            }

            try {
                // 执行两层校验筛选逻辑：xkbmxx → xkbmkm → ksxx联查
                InitializeExamStudentsDTO initRequest = new InitializeExamStudentsDTO();
                initRequest.setKsjhdm(ksjhdm);
                initRequest.setKmmc(kmmc);
                initRequest.setSzsmc(szsmc.trim());
                initRequest.setKqmc(kqmc);
                initRequest.setXxmc(xxmc);

                InitializeResultVO initResult = initializeExamStudents(initRequest);
                log.info("自动初始化完成，结果: {}", initResult);

                // 验证初始化是否成功
                int newRecordCount = kscjMapper.countExistingRecordsWithArea(ksjhdm, kmmc, szsmc, kqmc, xxmc);
                if (newRecordCount == 0) {
                    throw new RuntimeException("初始化失败，未找到符合条件的学生数据");
                }

                // 设置Redis缓存标志位
                redisTemplate.opsForValue().set(initCacheKey, true, Duration.ofHours(24));
                log.info("设置Redis缓存标志位: {}", initCacheKey);

                log.info("初始化验证通过，新增记录数: {}", newRecordCount);

            } catch (Exception e) {
                log.error("自动初始化失败: {}", e.getMessage(), e);
                throw new RuntimeException("生成Excel模板失败，初始化数据时出错: " + e.getMessage(), e);
            }
        } else {
            // 缓存已初始化，获取实际记录数用于日志记录
            existingRecordCount = kscjMapper.countExistingRecordsWithArea(ksjhdm, kmmc, szsmc, kqmc, xxmc);
            log.info("检测到已初始化，跳过自动初始化步骤，缓存标志: {}, 现有记录数: {}", isInitialized, existingRecordCount);
        }

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(1000)) { // 内存中保持1000行
            Sheet sheet = workbook.createSheet("成绩导入模板");

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);

            // 创建数据样式（锁定的基本信息列）
            CellStyle lockedDataStyle = workbook.createCellStyle();
            lockedDataStyle.setBorderTop(BorderStyle.THIN);
            lockedDataStyle.setBorderBottom(BorderStyle.THIN);
            lockedDataStyle.setBorderLeft(BorderStyle.THIN);
            lockedDataStyle.setBorderRight(BorderStyle.THIN);
            lockedDataStyle.setAlignment(HorizontalAlignment.CENTER);
            lockedDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            lockedDataStyle.setLocked(true); // 锁定基本信息列

            // 创建可编辑样式（成绩和合格评定列）
            CellStyle editableDataStyle = workbook.createCellStyle();
            editableDataStyle.setBorderTop(BorderStyle.THIN);
            editableDataStyle.setBorderBottom(BorderStyle.THIN);
            editableDataStyle.setBorderLeft(BorderStyle.THIN);
            editableDataStyle.setBorderRight(BorderStyle.THIN);
            editableDataStyle.setAlignment(HorizontalAlignment.CENTER);
            editableDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            editableDataStyle.setLocked(false); // 允许编辑成绩列

            // 创建表头（按文档要求）
            Row headerRow = sheet.createRow(0);
            String[] headers = {"考籍号", "姓名", "身份证号", "学校", "班级", "科目", "成绩", "合格评定"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);

                // 根据列内容设置不同的列宽
                if (i == 2) { // 身份证号列
                    sheet.setColumnWidth(i, 6500); // 身份证号列宽度增加
                } else if (i == 3) { // 学校列
                    sheet.setColumnWidth(i, 6000); // 学校列宽度增加
                } else if (i == 5) { // 科目列
                    sheet.setColumnWidth(i, 5000); // 科目列宽度增加
                } else {
                    sheet.setColumnWidth(i, 4000); // 其他列保持原宽度
                }
            }

            // 锁定表头
            sheet.createFreezePane(0, 1);

            // 启用工作表保护，实现"锁定基本列，允许编辑成绩列"的要求
            // 密码为空，用户可以取消保护，但默认情况下基本信息列被锁定
            sheet.protectSheet("");

            // 分页查询学生数据并写入Excel
            int pageSize = 1000;
            int pageNum = 1;
            int currentRow = 1;

            while (true) {
                // 构建查询条件
                ExamScoreQueryDTO queryDTO = new ExamScoreQueryDTO();
                queryDTO.setKsjhdm(ksjhdm);
                queryDTO.setKmmc(kmmc);
                queryDTO.setSzsmc(szsmc);
                queryDTO.setKqmc(kqmc);
                queryDTO.setXxmc(xxmc);
                queryDTO.setPageNum(pageNum);
                queryDTO.setPageSize(pageSize);

                PageResult<ExamScoreVO> pageResult = getExamScoresWithPagination(queryDTO);
                List<ExamScoreVO> students = pageResult.getRecords();

                if (students.isEmpty()) {
                    break;
                }

                // 写入学生数据（按文档要求的字段顺序）
                for (ExamScoreVO student : students) {
                    Row dataRow = sheet.createRow(currentRow++);

                    // 考籍号（锁定）
                    Cell kshCell = dataRow.createCell(0);
                    kshCell.setCellValue(student.getKsh() != null ? student.getKsh() : "");
                    kshCell.setCellStyle(lockedDataStyle);

                    // 姓名（锁定）
                    Cell xmCell = dataRow.createCell(1);
                    xmCell.setCellValue(student.getXm() != null ? student.getXm() : "");
                    xmCell.setCellStyle(lockedDataStyle);

                    // 身份证号（锁定）
                    Cell sfzjhCell = dataRow.createCell(2);
                    sfzjhCell.setCellValue(student.getSfzjh() != null ? student.getSfzjh() : "");
                    sfzjhCell.setCellStyle(lockedDataStyle);

                    // 学校（锁定）
                    Cell xxmcCell = dataRow.createCell(3);
                    xxmcCell.setCellValue(student.getXxmc() != null ? student.getXxmc() : "");
                    xxmcCell.setCellStyle(lockedDataStyle);

                    // 班级（锁定）
                    Cell bjmcCell = dataRow.createCell(4);
                    bjmcCell.setCellValue(student.getBjmc() != null ? student.getBjmc() : "");
                    bjmcCell.setCellStyle(lockedDataStyle);

                    // 科目（锁定，预填科目名称）
                    Cell kmmcCell = dataRow.createCell(5);
                    kmmcCell.setCellValue(kmmc != null ? kmmc : "");
                    kmmcCell.setCellStyle(lockedDataStyle);

                    // 成绩（可编辑，供用户填写）
                    Cell scoreCell = dataRow.createCell(6);
                    scoreCell.setCellValue("");
                    scoreCell.setCellStyle(editableDataStyle);

                    // 合格评定（可编辑，供用户填写）
                    Cell gradeEvalCell = dataRow.createCell(7);
                    gradeEvalCell.setCellValue("");
                    gradeEvalCell.setCellStyle(editableDataStyle);
                }

                // 如果当前页数据少于pageSize，说明已经是最后一页
                if (students.size() < pageSize) {
                    break;
                }

                pageNum++;
            }

            // 将工作簿写入字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            byte[] excelBytes = outputStream.toByteArray();

            log.info("Excel导入模板生成成功，共{}行数据，文件大小: {}KB", currentRow - 1, excelBytes.length / 1024);
            return excelBytes;

        } catch (Exception e) {
            log.error("生成Excel导入模板失败，考试计划代码: {}, 科目名称: {}, 地市: {}, 考区: {}, 学校: {}",
                    ksjhdm, kmmc, szsmc, kqmc, xxmc, e);
            throw new RuntimeException("生成Excel导入模板失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importExcelScores(String ksjhdm, String kmmc, MultipartFile file) {
        log.info("开始导入Excel成绩文件，考试计划代码: {}, 科目名称: {}, 文件名: {}", ksjhdm, kmmc, file.getOriginalFilename());

        // 1. 验证kscj表是否已初始化
        // 检查数据库中是否存在该考试计划和科目的成绩记录
        int existingRecordCount = kscjMapper.countExistingRecords(ksjhdm, kmmc);
        if (existingRecordCount == 0) {
            log.warn("检测到kscj表未初始化，考试计划代码: {}, 科目名称: {}, 记录数: {}",
                    ksjhdm, kmmc, existingRecordCount);
            return ImportResultVO.failure("请先生成Excel导入模板以完成数据初始化，然后再进行成绩导入");
        }
        log.info("kscj表已初始化验证通过，现有记录数: {}", existingRecordCount);

        LocalDateTime startTime = LocalDateTime.now();
        List<String> errorMessages = new ArrayList<>();
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;

        try (InputStream inputStream = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return ImportResultVO.failure("Excel文件中没有找到工作表");
            }

            // 验证表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return ImportResultVO.failure("Excel文件中没有找到表头行");
            }

            // 验证必需的列
            Map<String, Integer> columnIndexMap = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String columnName = cell.getStringCellValue().trim();
                    columnIndexMap.put(columnName, i);
                }
            }

            // 检查必需的列是否存在
            String[] requiredColumns = {"考籍号", "姓名", "科目", "成绩", "合格评定"};
            for (String column : requiredColumns) {
                if (!columnIndexMap.containsKey(column)) {
                    return ImportResultVO.failure("Excel文件缺少必需的列: " + column);
                }
            }

            // 获取列索引
            int kshIndex = columnIndexMap.get("考籍号");
            int xmIndex = columnIndexMap.get("姓名");
            int kmmcIndex = columnIndexMap.get("科目");
            int scoreIndex = columnIndexMap.get("成绩");
            int hgpdIndex = columnIndexMap.get("合格评定");

            // 分批处理数据
            List<ScoreUpdateDTO> batchData = new ArrayList<>();
            int batchSize = 1000;

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null)
                    continue;

                totalCount++;

                try {
                    // 读取数据
                    String ksh = getCellStringValue(row.getCell(kshIndex));
                    String xm = getCellStringValue(row.getCell(xmIndex));
                    String kmmcValue = getCellStringValue(row.getCell(kmmcIndex));
                    String score = getCellStringValue(row.getCell(scoreIndex));
                    String hgpd = getCellStringValue(row.getCell(hgpdIndex));

                    // 验证数据
                    if (ksh == null || ksh.trim().isEmpty()) {
                        errorMessages.add(String.format("第%d行：考籍号不能为空", rowIndex + 1));
                        failCount++;
                        continue;
                    }

                    if (!kmmc.equals(kmmcValue)) {
                        errorMessages.add(String.format("第%d行：科目名称不匹配，期望: %s，实际: %s", rowIndex + 1, kmmc, kmmcValue));
                        failCount++;
                        continue;
                    }

                    // 创建更新DTO
                    ScoreUpdateDTO updateDTO = new ScoreUpdateDTO();
                    updateDTO.setKsh(ksh.trim());
                    updateDTO.setKsjhdm(ksjhdm);
                    updateDTO.setKmmc(kmmc);

                    // 转换成绩为整数
                    Integer scoreValue = null;
                    if (score != null && !score.trim().isEmpty()) {
                        try {
                            scoreValue = Integer.parseInt(score.trim());
                        } catch (NumberFormatException e) {
                            errorMessages.add(String.format("第%d行：成绩格式错误，必须为整数", rowIndex + 1));
                            failCount++;
                            continue;
                        }
                    }

                    updateDTO.setFslkscj(scoreValue);
                    updateDTO.setCjhgm(hgpd != null ? hgpd.trim() : null);

                    // 当成绩和合格评定都为空时，设置考考类型名称为'缺考'
                    if (scoreValue == null && (hgpd == null || hgpd.trim().isEmpty())) {
                        updateDTO.setKklxmc("缺考");
                    } else if (scoreValue != null || (hgpd != null && !hgpd.trim().isEmpty())) {
                        // 当成绩或合格评定不为空时，设置考考类型名称为'正考'
                        updateDTO.setKklxmc("正考");
                    }

                    // TODO:gxr有关字段需要从登陆信息中获取
                    updateDTO.setGxrxm("系统导入");
                    updateDTO.setGxrgzrym("SYSTEM");
                    updateDTO.setGxsj(LocalDateTime.now());

                    batchData.add(updateDTO);

                    // 达到批次大小时执行批量更新
                    if (batchData.size() >= batchSize) {
                        int batchSuccessCount = processBatchUpdate(batchData, errorMessages);
                        successCount += batchSuccessCount;
                        failCount += (batchData.size() - batchSuccessCount);
                        batchData.clear();
                    }

                } catch (Exception e) {
                    errorMessages.add(String.format("第%d行：处理失败 - %s", rowIndex + 1, e.getMessage()));
                    failCount++;
                }
            }

            // 处理剩余的数据
            if (!batchData.isEmpty()) {
                int batchSuccessCount = processBatchUpdate(batchData, errorMessages);
                successCount += batchSuccessCount;
                failCount += (batchData.size() - batchSuccessCount);
            }

            LocalDateTime endTime = LocalDateTime.now();
            long duration = java.time.Duration.between(startTime, endTime).toMillis();

            log.info("Excel成绩导入完成，总数: {}, 成功: {}, 失败: {}, 耗时: {}ms", totalCount, successCount, failCount, duration);

            return ImportResultVO.builder()
                    .success(failCount == 0)
                    .message(failCount == 0 ? "导入成功" : String.format("导入完成，但有%d条记录失败", failCount))
                    .totalCount(totalCount)
                    .successCount(successCount)
                    .failCount(failCount)
                    .errorMessages(errorMessages)
                    .fileName(file.getOriginalFilename())
                    .startTime(startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .endTime(endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .duration(duration)
                    .build();

        } catch (Exception e) {
            log.error("导入Excel成绩文件失败，考试计划代码: {}, 科目名称: {}", ksjhdm, kmmc, e);
            return ImportResultVO.failure("导入失败: " + e.getMessage());
        }
    }

    /**
     * 获取单元格字符串值
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 处理数字，避免科学计数法
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    /**
     * 处理批量更新
     */
    private int processBatchUpdate(List<ScoreUpdateDTO> batchData, List<String> errorMessages) {
        int successCount = 0;

        try {
            // 验证KSH是否存在于kscj表中
            List<String> kshList = batchData.stream()
                    .map(ScoreUpdateDTO::getKsh)
                    .collect(Collectors.toList());

            List<String> existingKshList = kscjMapper.selectExistingKsh(batchData.get(0).getKsjhdm(),
                    batchData.get(0).getKmmc(), kshList);
            Set<String> existingKshSet = new HashSet<>(existingKshList);

            // 过滤出存在的记录进行更新
            List<ScoreUpdateDTO> validUpdates = new ArrayList<>();
            for (ScoreUpdateDTO dto : batchData) {
                if (existingKshSet.contains(dto.getKsh())) {
                    validUpdates.add(dto);
                } else {
                    errorMessages.add(String.format("考籍号 %s 在系统中不存在或未初始化", dto.getKsh()));
                }
            }

            // 执行批量更新
            if (!validUpdates.isEmpty()) {
                successCount = kscjMapper.batchUpdateScores(validUpdates);
                log.debug("批量更新成绩完成，更新记录数: {}", successCount);
            }

        } catch (Exception e) {
            log.error("批量更新成绩失败: {}", e.getMessage(), e);
            errorMessages.add("批量更新失败: " + e.getMessage());
        }

        return successCount;
    }

}