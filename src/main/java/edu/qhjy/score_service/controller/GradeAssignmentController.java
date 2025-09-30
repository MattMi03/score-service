package edu.qhjy.score_service.controller;

import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.dto.GradeAssignmentRequestDTO;
import edu.qhjy.score_service.domain.vo.GradeAssignmentResultVO;
import edu.qhjy.score_service.service.GradeAssignmentService;
import edu.qhjy.score_service.service.redis.GradeAssignmentProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 等级赋分控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/grade-assignment")
@RequiredArgsConstructor
@Validated
@Tag(name = "等级赋分管理", description = "等级赋分相关接口")
public class GradeAssignmentController {

    private final GradeAssignmentService gradeAssignmentService;
    private final GradeAssignmentProgressService progressService;

    @Operation(summary = "执行等级赋分", description = "根据指定的考试计划和科目执行等级赋分")
    @PostMapping("/assign")
    public ResponseEntity<GradeAssignmentResultVO> assignGrades(
            @Valid @RequestBody GradeAssignmentRequestDTO request) {

        try {
            log.info("接收等级赋分请求: {}", request);

            // 参数验证提示
            if (request.getKsjhdm() == null || request.getKsjhdm().trim().isEmpty()) {
                String errorMsg = "参数错误: 考试计划代码(ksjhdm)不能为空";
                log.warn(errorMsg);
                return ResponseEntity.badRequest().body(
                        GradeAssignmentResultVO.builder()
                                .status("FAILED")
                                .message(errorMsg)
                                .processedStudentCount(0)
                                .processedCityCount(0)
                                .build());
            }

            if (request.getKmmc() == null || request.getKmmc().trim().isEmpty()) {
                String errorMsg = "参数错误: 科目名称(kmmc)不能为空";
                log.warn(errorMsg);
                return ResponseEntity.badRequest().body(
                        GradeAssignmentResultVO.builder()
                                .status("FAILED")
                                .message(errorMsg)
                                .processedStudentCount(0)
                                .processedCityCount(0)
                                .build());
            }

            GradeAssignmentResultVO result = gradeAssignmentService.assignGrades(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("等级赋分执行失败", e);
            String errorMessage = e.getMessage();

            // 针对常见错误提供更友好的提示
            if (errorMessage != null && errorMessage.contains("已存在等级赋分记录")) {
                // 错误信息已经在Service层格式化过了，直接使用
                return ResponseEntity.badRequest().body(
                        GradeAssignmentResultVO.builder()
                                .status("FAILED")
                                .message(errorMessage)
                                .processedStudentCount(0)
                                .processedCityCount(0)
                                .build());
            } else {
                return ResponseEntity.badRequest().body(
                        GradeAssignmentResultVO.builder()
                                .status("FAILED")
                                .message("等级赋分执行失败: " + errorMessage)
                                .processedStudentCount(0)
                                .processedCityCount(0)
                                .build());
            }
        }
    }

    @Operation(summary = "查询等级赋分进度", description = "查询指定任务的等级赋分进度")
    @GetMapping("/progress/{taskId}")
    public ResponseEntity<Map<String, Object>> getProgress(
            @Parameter(description = "任务ID") @PathVariable String taskId) {

        try {
            Map<String, Object> progress = progressService.getProgress(taskId);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            log.error("查询等级赋分进度失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "查询等级赋分统计", description = "查询指定考试计划和科目的等级分布统计")
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @Parameter(description = "考试计划代码") @RequestParam @NotBlank String ksjhdm,
            @Parameter(description = "科目名称") @RequestParam @NotBlank String kmmc,
            @Parameter(description = "市州名称（可选）") @RequestParam(required = false) String szsmc) {

        try {
            Map<String, Object> statistics = gradeAssignmentService.getGradeAssignmentStatistics(
                    ksjhdm, kmmc, szsmc);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("查询等级赋分统计失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "获取可用城市列表", description = "获取指定考试计划和科目可用于等级赋分的城市列表")
    @GetMapping("/cities")
    public Result<List<String>> getAvailableCities(
            @Parameter(description = "考试计划代码") @RequestParam @NotBlank String ksjhdm,
            @Parameter(description = "科目名称") @RequestParam @NotBlank String kmmc) {

        try {
            List<String> cities = gradeAssignmentService.getAvailableCities(ksjhdm, kmmc);
            return Result.success(cities);
        } catch (Exception e) {
            log.error("获取可用城市列表失败", e);
            return Result.error("获取可用城市列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "清除等级赋分记录", description = "清除指定条件的等级赋分记录")
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearGradeAssignment(
            @Valid @RequestBody GradeAssignmentRequestDTO request) {

        try {
            Map<String, Object> result = gradeAssignmentService.clearGradeAssignment(
                    request.getKsjhdm(), request.getKmmc(), request.getSzsmc());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("清除等级赋分记录失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "清除等级赋分记录失败: " + e.getMessage()));
        }
    }


    @Operation(summary = "验证等级赋分配置", description = "验证等级赋分的配置参数是否正确")
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateConfiguration(
            @Valid @RequestBody GradeAssignmentRequestDTO request) {

        try {
            log.info("验证等级赋分配置: 考试计划={}, 科目={}, 城市={}",
                    request.getKsjhdm(), request.getKmmc(), request.getSzsmc());

            // 获取城市列表
            List<String> cities = gradeAssignmentService.getAvailableCities(
                    request.getKsjhdm(), request.getKmmc());

            if (cities.isEmpty()) {
                log.warn("未找到可用城市数据: 考试计划={}, 科目={}", request.getKsjhdm(), request.getKmmc());
                return ResponseEntity.badRequest().body(Map.of(
                        "valid", false,
                        "message", "未找到可用于等级赋分的城市数据",
                        "availableCities", cities));
            }

            // 检查是否已存在等级赋分记录
            Map<String, Object> statistics = gradeAssignmentService.getGradeAssignmentStatistics(
                    request.getKsjhdm(), request.getKmmc(), request.getSzsmc());

            boolean hasExistingGrades = false;
            int existingGradeCount = 0;
            if (statistics.containsKey("gradeDistribution")) {
                Object gradeDistribution = statistics.get("gradeDistribution");
                if (gradeDistribution instanceof List<?> gradeList) {
                    hasExistingGrades = !gradeList.isEmpty();
                    existingGradeCount = gradeList.size();
                }
            }

            String message;
            if (hasExistingGrades) {
                if (request.getSzsmc() != null && !request.getSzsmc().trim().isEmpty()) {
                    message = String.format("城市 %s 已存在等级赋分记录（%d个等级），执行新的赋分将覆盖现有记录",
                            request.getSzsmc(), existingGradeCount);
                } else {
                    message = String.format("已存在等级赋分记录（%d个等级），执行新的赋分将覆盖现有记录",
                            existingGradeCount);
                }
            } else {
                message = "配置验证通过，可以执行等级赋分";
            }

            Map<String, Object> result = Map.of(
                    "valid", true,
                    "availableCities", cities,
                    "hasExistingGrades", hasExistingGrades,
                    "existingGradeCount", existingGradeCount,
                    "targetCity", request.getSzsmc() != null ? request.getSzsmc() : "全部城市",
                    "message", message);

            log.info("验证结果: {}", result);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("验证等级赋分配置失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "message", "配置验证失败: " + e.getMessage()));
        }
    }
}