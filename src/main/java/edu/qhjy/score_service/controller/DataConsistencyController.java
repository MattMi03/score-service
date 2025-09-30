package edu.qhjy.score_service.controller;

import edu.qhjy.score_service.mapper.primary.KscjMapper;
import edu.qhjy.score_service.mapper.primary.WcxxMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据一致性验证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/data-consistency")
@RequiredArgsConstructor
@Validated
@Tag(name = "数据一致性验证", description = "验证不同数据源的一致性")
public class DataConsistencyController {

    private final KscjMapper kscjMapper;
    private final WcxxMapper wcxxMapper;

    @Operation(summary = "对比等级赋分统计数据", description = "对比KSCJ表实时统计与WCXX表存储的等级赋分统计数据")
    @GetMapping("/compare-grade-stats")
    public ResponseEntity<Map<String, Object>> compareGradeStatistics(
            @Parameter(description = "考试计划代码") @RequestParam @NotBlank String ksjhdm,
            @Parameter(description = "科目名称") @RequestParam @NotBlank String kmmc,
            @Parameter(description = "市州名称（可选）") @RequestParam(required = false) String szsmc) {

        try {
            Map<String, Object> result = new HashMap<>();

            // 1. 从KSCJ表查询实时等级分布统计
            List<Map<String, Object>> kscjStats = kscjMapper.selectGradeDistributionStats(ksjhdm, kmmc, szsmc);

            // 2. 从WCXX表查询存储的等级赋分统计
            List<Map<String, Object>> wcxxStats = wcxxMapper.selectGradeAssignmentStats(ksjhdm, kmmc, szsmc);

            // 3. 数据对比分析
            Map<String, Object> comparison = compareStatistics(kscjStats, wcxxStats, szsmc);

            result.put("kscjData", kscjStats);
            result.put("wcxxData", wcxxStats);
            result.put("comparison", comparison);
            result.put("queryParams", Map.of(
                    "ksjhdm", ksjhdm,
                    "kmmc", kmmc,
                    "szsmc", szsmc != null ? szsmc : "全部市州"));
            result.put("queryTime", new Date());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("对比等级赋分统计数据失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", true);
            errorResult.put("message", "数据对比失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * 对比两个数据源的统计结果
     */
    private Map<String, Object> compareStatistics(List<Map<String, Object>> kscjStats,
                                                  List<Map<String, Object>> wcxxStats,
                                                  String szsmc) {
        Map<String, Object> comparison = new HashMap<>();

        // 基本信息
        comparison.put("kscjRecordCount", kscjStats.size());
        comparison.put("wcxxRecordCount", wcxxStats.size());

        // 数据存在性检查
        boolean kscjHasData = !kscjStats.isEmpty();
        boolean wcxxHasData = !wcxxStats.isEmpty();
        comparison.put("kscjHasData", kscjHasData);
        comparison.put("wcxxHasData", wcxxHasData);

        if (!kscjHasData && !wcxxHasData) {
            comparison.put("status", "BOTH_EMPTY");
            comparison.put("message", "两个数据源都没有数据");
            return comparison;
        }

        if (!kscjHasData) {
            comparison.put("status", "KSCJ_EMPTY");
            comparison.put("message", "KSCJ表没有数据，但WCXX表有数据");
            return comparison;
        }

        if (!wcxxHasData) {
            comparison.put("status", "WCXX_EMPTY");
            comparison.put("message", "WCXX表没有数据，但KSCJ表有数据（可能未执行等级赋分）");
            return comparison;
        }

        // 详细数据对比
        List<Map<String, Object>> differences = new ArrayList<>();

        if (szsmc == null || szsmc.trim().isEmpty()) {
            // 按市州分组对比
            Map<String, List<Map<String, Object>>> kscjByCity = kscjStats.stream()
                    .collect(Collectors.groupingBy(stat -> (String) stat.get("szsmc")));
            Map<String, List<Map<String, Object>>> wcxxByCity = wcxxStats.stream()
                    .collect(Collectors.groupingBy(stat -> (String) stat.get("szsmc")));

            Set<String> allCities = new HashSet<>();
            allCities.addAll(kscjByCity.keySet());
            allCities.addAll(wcxxByCity.keySet());

            for (String city : allCities) {
                List<Map<String, Object>> kscjCityStats = kscjByCity.getOrDefault(city, new ArrayList<>());
                List<Map<String, Object>> wcxxCityStats = wcxxByCity.getOrDefault(city, new ArrayList<>());

                Map<String, Object> cityComparison = compareCityStatistics(city, kscjCityStats, wcxxCityStats);
                differences.add(cityComparison);
            }
        } else {
            // 单个市州对比
            Map<String, Object> cityComparison = compareCityStatistics(szsmc, kscjStats, wcxxStats);
            differences.add(cityComparison);
        }

        comparison.put("differences", differences);

        // 总体一致性评估
        boolean isConsistent = differences.stream()
                .allMatch(diff -> (Boolean) ((Map<String, Object>) diff).get("isConsistent"));
        comparison.put("isConsistent", isConsistent);
        comparison.put("status", isConsistent ? "CONSISTENT" : "INCONSISTENT");
        comparison.put("message", isConsistent ? "数据一致" : "数据存在差异");

        return comparison;
    }

    /**
     * 对比单个市州的统计数据
     */
    private Map<String, Object> compareCityStatistics(String city,
                                                      List<Map<String, Object>> kscjStats,
                                                      List<Map<String, Object>> wcxxStats) {
        Map<String, Object> cityComparison = new HashMap<>();
        cityComparison.put("city", city);

        // 按等级分组
        Map<String, Map<String, Object>> kscjByGrade = kscjStats.stream()
                .collect(Collectors.toMap(
                        stat -> (String) stat.get("grade"),
                        stat -> stat,
                        (existing, replacement) -> existing));

        Map<String, Map<String, Object>> wcxxByGrade = wcxxStats.stream()
                .collect(Collectors.toMap(
                        stat -> (String) stat.get("grade"),
                        stat -> stat,
                        (existing, replacement) -> existing));

        Set<String> allGrades = new HashSet<>();
        allGrades.addAll(kscjByGrade.keySet());
        allGrades.addAll(wcxxByGrade.keySet());

        List<Map<String, Object>> gradeComparisons = new ArrayList<>();
        boolean cityIsConsistent = true;

        for (String grade : allGrades) {
            Map<String, Object> kscjGrade = kscjByGrade.get(grade);
            Map<String, Object> wcxxGrade = wcxxByGrade.get(grade);

            Map<String, Object> gradeComparison = new HashMap<>();
            gradeComparison.put("grade", grade);

            if (kscjGrade == null) {
                gradeComparison.put("status", "ONLY_IN_WCXX");
                gradeComparison.put("wcxxCount", wcxxGrade.get("count"));
                gradeComparison.put("isConsistent", false);
                cityIsConsistent = false;
            } else if (wcxxGrade == null) {
                gradeComparison.put("status", "ONLY_IN_KSCJ");
                gradeComparison.put("kscjCount", kscjGrade.get("count"));
                gradeComparison.put("isConsistent", false);
                cityIsConsistent = false;
            } else {
                // 对比人数
                Integer kscjCount = ((Number) kscjGrade.get("count")).intValue();
                Integer wcxxCount = ((Number) wcxxGrade.get("count")).intValue();
                boolean countMatches = Objects.equals(kscjCount, wcxxCount);

                gradeComparison.put("status", "IN_BOTH");
                gradeComparison.put("kscjCount", kscjCount);
                gradeComparison.put("wcxxCount", wcxxCount);
                gradeComparison.put("countDifference", kscjCount - wcxxCount);
                gradeComparison.put("countMatches", countMatches);
                gradeComparison.put("isConsistent", countMatches);

                if (!countMatches) {
                    cityIsConsistent = false;
                }
            }

            gradeComparisons.add(gradeComparison);
        }

        cityComparison.put("gradeComparisons", gradeComparisons);
        cityComparison.put("isConsistent", cityIsConsistent);

        // 计算总人数对比
        int kscjTotalCount = kscjStats.stream()
                .mapToInt(stat -> ((Number) stat.get("count")).intValue())
                .sum();
        int wcxxTotalCount = wcxxStats.stream()
                .mapToInt(stat -> ((Number) stat.get("count")).intValue())
                .sum();

        cityComparison.put("kscjTotalCount", kscjTotalCount);
        cityComparison.put("wcxxTotalCount", wcxxTotalCount);
        cityComparison.put("totalCountDifference", kscjTotalCount - wcxxTotalCount);
        cityComparison.put("totalCountMatches", kscjTotalCount == wcxxTotalCount);

        return cityComparison;
    }
}