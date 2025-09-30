package edu.qhjy.score_service.controller;

import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.vo.*;
import edu.qhjy.score_service.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 成绩统计分析控制器
 * 提供各种统计图表数据接口
 *
 * @author dadalv
 * @since 2025-08-01
 */
@Slf4j
@RestController
@RequestMapping("/api/score/statistics")
@RequiredArgsConstructor
@Validated
@Tag(name = "成绩统计分析", description = "提供柱状图、饼图、折线图等统计分析功能")
public class ScoreStatisticsController {

    private final ScoreService scoreService;

    /**
     * 获取区域成绩统计分布（柱状图数据）
     */
    @GetMapping("/area-distribution")
    @Operation(summary = "获取区域成绩统计分布", description = "用于柱状图展示不同区域的成绩分布情况")
    public Result<AreaScoreStatisticsVO> getAreaScoreStatistics(
            @Parameter(description = "考试计划代码", required = true) @RequestParam @NotBlank(message = "考试计划代码不能为空") String examPlanCode,

            @Parameter(description = "科目名称", required = true) @RequestParam @NotBlank(message = "科目名称不能为空") String subjectName,

            @Parameter(description = "区域级别(city/county/school/grade/class)", required = true) @RequestParam @NotBlank(message = "区域级别不能为空") String areaLevel,

            @Parameter(description = "区域筛选条件（可选）") @RequestParam(required = false) String areaFilter) {

        log.info("获取区域成绩统计分布：examPlanCode={}, subjectName={}, areaLevel={}, areaFilter={}",
                examPlanCode, subjectName, areaLevel, areaFilter);

        try {
            AreaScoreStatisticsVO result = scoreService.getAreaScoreStatistics(
                    subjectName, examPlanCode, areaLevel, areaFilter);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取区域成绩统计分布失败", e);
            return Result.error("获取区域成绩统计分布失败: " + e.getMessage());
        }
    }

    /**
     * 获取科目成绩等级分布（饼图数据）
     */
    @GetMapping("/grade-distribution")
    @Operation(summary = "获取科目成绩等级分布", description = "用于饼图展示成绩等级和分数段分布")
    public Result<SubjectGradeDistributionVO> getSubjectGradeDistribution(
            @Parameter(description = "科目名称", required = true) @RequestParam @NotBlank(message = "科目名称不能为空") String subjectName,

            @Parameter(description = "考试计划代码", required = true) @RequestParam @NotBlank(message = "考试计划代码不能为空") String examPlanCode,

            @Parameter(description = "区域筛选条件（可选）") @RequestParam(required = false) String areaFilter) {

        log.info("获取科目成绩等级分布：subjectName={}, examPlanCode={}, areaFilter={}",
                subjectName, examPlanCode, areaFilter);

        try {
            SubjectGradeDistributionVO result = scoreService.getSubjectGradeDistribution(
                    subjectName, examPlanCode, areaFilter);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取科目成绩等级分布失败", e);
            return Result.error("获取科目成绩等级分布失败: " + e.getMessage());
        }
    }

    /**
     * 获取历史成绩趋势分析（折线图数据）
     */
    @GetMapping("/historical-trends")
    @Operation(summary = "获取历史成绩趋势分析", description = "用于折线图展示历史成绩变化趋势")
    public Result<ScoreTrendAnalysisVO> getHistoricalTrends(
            @Parameter(description = "科目名称", required = true) @RequestParam @NotBlank(message = "科目名称不能为空") String subjectName,

            @Parameter(description = "区域筛选条件（可选）") @RequestParam(required = false) String areaFilter,

            @Parameter(description = "开始年份（可选）") @RequestParam(required = false) Integer startYear,

            @Parameter(description = "结束年份（可选）") @RequestParam(required = false) Integer endYear) {

        log.info("获取历史成绩趋势分析：subjectName={}, areaFilter={}, startYear={}, endYear={}",
                subjectName, areaFilter, startYear, endYear);

        try {
            ScoreTrendAnalysisVO result = scoreService.getHistoricalTrends(
                    subjectName, areaFilter, startYear, endYear);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取历史成绩趋势分析失败", e);
            return Result.error("获取历史成绩趋势分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取可用的考试计划列表
     */
    @GetMapping("/exam-plans")
    @Operation(summary = "获取可用的考试计划列表", description = "用于下拉选择框，支持趋势分析")
    public Result<List<String>> getAvailableExamPlans(
            @Parameter(description = "科目名称（可选，用于筛选）") @RequestParam(required = false) String subjectName) {

        log.info("获取可用的考试计划列表：subjectName={}", subjectName);

        try {
            List<String> result = scoreService.getAvailableExamPlans(subjectName);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取可用的考试计划列表失败", e);
            return Result.error("获取可用的考试计划列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取区域层级数据
     */
    @GetMapping("/area-hierarchy")
    @Operation(summary = "获取区域层级数据（增强版）", description = "用于React级联下拉选择框，支持五级级联查询（地市->考区->学校->级别->班级）")
    public Result<List<AreaHierarchyVO>> getAreaHierarchy(
            @Parameter(description = "区域类型(city/county/school/grade/class)", required = true) @RequestParam @NotBlank(message = "区域类型不能为空") String areaType,

            @Parameter(description = "上级区域代码（可选）") @RequestParam(required = false) String parentArea) {

        log.info("获取增强区域层级数据：areaType={}, parentArea={}", areaType, parentArea);

        try {
            List<AreaHierarchyVO> result = scoreService.getAreaHierarchyEnhanced(parentArea, areaType);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取增强区域层级数据失败", e);
            return Result.error("获取增强区域层级数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取统计分析概览
     */
    @GetMapping("/overview")
    @Operation(summary = "获取统计分析概览", description = "获取统计分析的基本信息和可用选项")
    public Result<StatisticsOverviewVO> getStatisticsOverview(
            @Parameter(description = "科目名称（可选）") @RequestParam(required = false) String subjectName) {

        log.info("获取统计分析概览：subjectName={}", subjectName);

        try {
            // 获取可用的考试计划
            List<String> examPlans = scoreService.getAvailableExamPlans(subjectName);

            // 获取可用的区域
            List<AreaHierarchyVO> cityHierarchy = scoreService.getAreaHierarchyEnhanced(null, "city");
            List<String> cities = cityHierarchy.stream()
                    .map(AreaHierarchyVO::getName)
                    .toList();

            // 获取所有可用的科目
            List<String> allSubjects = scoreService.getAvailableSubjects();

            // 构建统计信息
            StatisticsOverviewVO.StatisticsInfo statisticsInfo = StatisticsOverviewVO.StatisticsInfo.builder()
                    .totalExamPlans(examPlans.size())
                    .totalSubjects(allSubjects.size())
                    .totalAreas(cities.size())
                    .lastUpdateTime(java.time.LocalDateTime.now().toString())
                    .build();

            StatisticsOverviewVO overview = StatisticsOverviewVO.builder()
                    .availableExamPlans(examPlans)
                    .availableCities(cities)
                    .supportedAreaLevels(List.of("city", "county", "school", "class"))
                    .supportedChartTypes(List.of("bar", "pie", "line"))
                    .statisticsInfo(statisticsInfo)
                    .build();

            return Result.success(overview);
        } catch (Exception e) {
            log.error("获取统计分析概览失败", e);
            return Result.error("获取统计分析概览失败: " + e.getMessage());
        }
    }
}