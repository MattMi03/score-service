package edu.qhjy.score_service.controller;

import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.dto.GradeAdjustmentRequestDTO;
import edu.qhjy.score_service.domain.dto.ScoreSegmentDTO;
import edu.qhjy.score_service.domain.dto.ScoreSegmentQueryDTO;
import edu.qhjy.score_service.domain.vo.*;
import edu.qhjy.score_service.service.ScoreSegmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 一分一段表控制器
 * 提供一分一段表查询、等级调整、数据管理等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/score/segment")
@RequiredArgsConstructor
@Validated
@Tag(name = "一分一段表管理", description = "一分一段表查询、等级调整、数据管理相关接口")
public class ScoreSegmentController {

    private final ScoreSegmentService scoreSegmentService;

    @Operation(summary = "预览等级调整结果", description = "预览等级分界线调整后的影响，不保存数据。支持全省或指定市州的等级调整预览。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "预览成功", content = @Content(schema = @Schema(implementation = GradeAdjustmentResultVO.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误，如考试计划代码为空、科目名称为空、等级分界线格式错误等"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/grade-adjustment/preview")
    public Result<GradeAdjustmentResultVO> previewGradeAdjustment(
            @Parameter(description = "等级调整请求参数", required = true, example = "{\"adjustedszsmcs\":[{\"szsmc\":\"长沙市\",\"adjustedThresholds\":{\"A\":85.5,\"B\":75.0,\"C\":65.0,\"D\":55.0}}],\"ksjhdm\":\"2024001\",\"kmmc\":\"语文\",\"operatorName\":\"张三\",\"adjustmentReason\":\"根据考试难度调整\"}") @Valid @RequestBody GradeAdjustmentRequestDTO requestDTO) {
        try {
            GradeAdjustmentResultVO result = scoreSegmentService.previewGradeAdjustment(requestDTO);
            return Result.success(result);
        } catch (Exception e) {
            log.error("预览等级调整失败", e);
            return Result.error("预览等级调整失败: " + e.getMessage());
        }
    }

    /**
     * 预览等级调整后的一分一段数据变化
     */
    @Operation(summary = "预览一分一段数据变化", description = "预览等级调整后一分一段数据的变化情况")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "预览成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/grade-adjustment/score-segment-preview")
    public Result<ScoreSegmentChangePreviewVO> previewScoreSegmentChanges(
            @Parameter(description = "等级调整请求", required = true) @Valid @RequestBody GradeAdjustmentRequestDTO requestDTO) {
        try {
            log.info("预览一分一段数据变化: {}", requestDTO);
            ScoreSegmentChangePreviewVO result = scoreSegmentService.previewScoreSegmentChanges(requestDTO);
            return Result.success(result);
        } catch (Exception e) {
            log.error("预览一分一段数据变化失败", e);
            return Result.error("预览一分一段数据变化失败: " + e.getMessage());
        }
    }

    @Operation(summary = "确认并保存等级调整", description = "确认等级分界线调整并保存到数据库。此操作会同时更新WCXX表的等级分界线和KSCJ表的学生等级信息，支持事务回滚。返回等级调整结果和一分一段数据变化预览。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "调整成功，返回详细的调整结果统计和一分一段数据变化预览", content = @Content(schema = @Schema(implementation = GradeAdjustmentConfirmResultVO.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误，如必填字段为空、等级分界线不合理、数据格式错误等"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误，可能是数据库操作失败或事务回滚")
    })
    @PostMapping("/grade-adjustment/confirm")
    public Result<GradeAdjustmentConfirmResultVO> confirmGradeAdjustment(
            @Parameter(description = "等级调整确认请求参数", required = true, example = "{\"adjustedszsmcs\":[{\"szsmc\":\"长沙市\",\"adjustedThresholds\":{\"A\":88.0,\"B\":78.0,\"C\":68.0,\"D\":58.0}}],\"ksjhdm\":\"2024001\",\"kmmc\":\"数学\",\"operatorName\":\"李四\",\"operatorCode\":\"OP002\",\"adjustmentReason\":\"根据专家评议结果调整分界线\"}") @Valid @RequestBody GradeAdjustmentRequestDTO requestDTO) {
        try {
            GradeAdjustmentConfirmResultVO result = scoreSegmentService.confirmGradeAdjustment(requestDTO);
            return Result.success(result);
        } catch (Exception e) {
            log.error("确认等级调整失败", e);
            return Result.error("确认等级调整失败: " + e.getMessage());
        }
    }

    /**
     * 批量确认并保存等级调整（支持多市州）
     * <p>
     * 支持多个市州的等级调整，每个市州可以有不同的调整参数：
     * <p>
     * 批量调整请求示例：
     * {
     * "ksjhdm": "2024001",
     * "kmmc": "数学",
     * "adjustedszsmcs": [
     * {
     * "szsmc": "长沙市",
     * "adjustedThresholds": {
     * "A": 88.0,
     * "B": 78.0,
     * "C": 68.0,
     * "D": 58.0
     * }
     * },
     * {
     * "szsmc": "株洲市",
     * "adjustedThresholds": {
     * "A": 87.0,
     * "B": 77.0,
     * "C": 67.0,
     * "D": 57.0
     * }
     * }
     * ],
     * "operatorName": "李四",
     * "operatorCode": "OP002",
     * "adjustmentReason": "根据各市州情况分别调整分界线"
     * }
     * <p>
     * 注意事项：
     * - 此操作会批量处理多个市州的等级调整
     * - 每个市州的调整结果会单独返回
     * - 支持部分成功，即使某些市州调整失败，其他市州仍可成功
     * - 返回数据包含整体统计和各市州详细结果
     */
    @Operation(summary = "批量确认并保存等级调整", description = "批量处理多个市州的等级分界线调整并保存到数据库。支持并行处理，返回整体统计和各市州详细结果。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "批量调整完成，返回整体统计和各市州详细结果", content = @Content(schema = @Schema(implementation = BatchGradeAdjustmentResultVO.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误，如必填字段为空、等级分界线不合理、数据格式错误等"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误，可能是数据库操作失败")
    })
    @PostMapping("/grade-adjustment/batch-confirm")
    public Result<BatchGradeAdjustmentResultVO> batchConfirmGradeAdjustment(
            @Parameter(description = "批量等级调整确认请求参数", required = true, example = "{\"adjustedszsmcs\":[{\"szsmc\":\"长沙市\",\"adjustedThresholds\":{\"A\":88.0,\"B\":78.0,\"C\":68.0,\"D\":58.0}},{\"szsmc\":\"株洲市\",\"adjustedThresholds\":{\"A\":87.0,\"B\":77.0,\"C\":67.0,\"D\":57.0}}],\"ksjhdm\":\"2024001\",\"kmmc\":\"数学\",\"operatorName\":\"李四\",\"operatorCode\":\"OP002\",\"adjustmentReason\":\"根据各市州情况分别调整分界线\"}") @Valid @RequestBody GradeAdjustmentRequestDTO requestDTO) {
        try {
            BatchGradeAdjustmentResultVO result = scoreSegmentService.batchConfirmGradeAdjustment(requestDTO);
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量确认等级调整失败", e);
            return Result.error("批量确认等级调整失败: " + e.getMessage());
        }
    }

    /**
     * 获取历史考试计划列表
     */
    @Operation(summary = "获取历史考试计划列表", description = "获取系统中所有历史考试计划的统计信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/historical-exam-plans")
    public Result<List<ExamPlanSubjectStatisticsVO>> getHistoricalExamPlans() {
        try {
            List<ExamPlanSubjectStatisticsVO> examPlans = scoreSegmentService.getHistoricalExamPlans();
            return Result.success(examPlans);
        } catch (Exception e) {
            log.error("获取历史考试计划列表失败", e);
            return Result.error("获取历史考试计划列表失败: " + e.getMessage());
        }
    }

    // /**
    // * 预计算一分一段表数据
    // */
    // @Operation(summary = "预计算一分一段表数据", description = "预先计算指定考试计划和科目的一分一段表数据并缓存")
    // @ApiResponses(value = {
    // @ApiResponse(responseCode = "200", description = "预计算成功"),
    // @ApiResponse(responseCode = "400", description = "请求参数错误"),
    // @ApiResponse(responseCode = "500", description = "服务器内部错误")
    // })
    // @PostMapping("/pre-calculate")
    // public Result<Boolean> preCalculateScoreSegment(
    // @Parameter(description = "考试计划代码", required = true) @RequestParam String
    // ksjhdm,
    // @Parameter(description = "科目名称", required = true) @RequestParam String kmmc)
    // {
    // try {
    // Boolean success = scoreSegmentService.preCalculateScoreSegment(ksjhdm, kmmc);
    // return Result.success(success);
    // } catch (Exception e) {
    // log.error("预计算一分一段表数据失败", e);
    // return Result.error("删除一分一段数据失败: " + e.getMessage());
    // }
    // }

    /**
     * 手动同步学生等级
     * 将WCXX表中的等级分界线同步到KSCJ表的学生等级字段
     */
    @Operation(summary = "手动同步学生等级", description = "将WCXX表中的等级分界线同步到KSCJ表的学生等级字段。此操作会根据WCXX表中的等级分界线重新计算并更新KSCJ表中所有学生的等级信息。支持单市州同步和批量同步所有市州。")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "同步成功，返回详细的同步结果统计", content = @Content(schema = @Schema(implementation = GradeSyncResultVO.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误，如必填字段为空、等级分界线数据不完整等"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误，可能是数据库操作失败")
    })
    @PostMapping("/sync-student-grades")
    public Result<GradeSyncResultVO> syncStudentGradesFromWcxx(
            @Parameter(description = "考试计划代码", required = true) @RequestParam String ksjhdm,
            @Parameter(description = "科目名称", required = true) @RequestParam String kmmc,
            @Parameter(description = "市州名称（可选，为空时批量处理所有市州）") @RequestParam(required = false) String szsmc) {

        try {
            log.info("开始手动同步学生等级: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc);

            GradeSyncResultVO result = scoreSegmentService.syncStudentGradesFromWcxx(ksjhdm, kmmc, szsmc);

            if (result.isSuccess()) {
                log.info("学生等级同步成功: 同步学生数={}, 处理时间={}ms",
                        result.getSyncedStudentCount(), result.getProcessingTime());
                return Result.success(result);
            } else {
                log.warn("学生等级同步失败: {}", result.getMessage());
                return Result.error(result.getMessage());
            }

        } catch (Exception e) {
            log.error("手动同步学生等级异常: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);
            return Result.error("学生等级同步失败: " + e.getMessage());
        }
    }

    /**
     * 清除一分一段表缓存
     */
    @Operation(summary = "清除一分一段表缓存", description = "清除指定考试计划和科目的一分一段表缓存数据")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "清除成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @DeleteMapping("/cache")
    public Result<Void> clearScoreSegmentCache(
            @Parameter(description = "考试计划代码", required = true) @RequestParam String ksjhdm,
            @Parameter(description = "科目名称", required = true) @RequestParam String kmmc) {
        try {
            scoreSegmentService.clearScoreSegmentCache(ksjhdm, kmmc);
            return Result.success("缓存清除成功", null);
        } catch (Exception e) {
            log.error("清除一分一段表缓存失败", e);
            return Result.error("清除一分一段表缓存失败: " + e.getMessage());
        }
    }

    /**
     * 获取一分一段表计算状态
     */
    @Operation(summary = "获取一分一段表计算状态", description = "获取指定考试计划和科目的一分一段表计算状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/calculation-status")
    public Result<String> getCalculationStatus(
            @Parameter(description = "考试计划代码", required = true) @RequestParam String ksjhdm,
            @Parameter(description = "科目名称", required = true) @RequestParam String kmmc) {
        try {
            String status = scoreSegmentService.getCalculationStatus(ksjhdm, kmmc);
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取一分一段表计算状态失败", e);
            return Result.error("获取一分一段表计算状态失败: " + e.getMessage());
        }
    }

    /**
     * 导出一分一段表数据到Excel
     */
    @Operation(summary = "导出一分一段表数据到Excel", description = "导出指定条件的一分一段表数据为Excel文件")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "导出成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportScoreSegmentToExcel(
            @Parameter(description = "查询参数", required = true) @Valid @RequestBody ScoreSegmentQueryDTO queryDTO) {
        try {
            byte[] excelData = scoreSegmentService.exportScoreSegmentToExcel(queryDTO);

            String filename = String.format("一分一段表_%s_%s.xlsx",
                    queryDTO.getKsjhdm(), queryDTO.getKmmc());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelData);
        } catch (Exception e) {
            log.error("导出一分一段表数据失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 批量预计算多个考试计划的一分一段表
     */
    @Operation(summary = "批量预计算一分一段表", description = "批量预计算多个考试计划的一分一段表数据")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "批量预计算启动成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/batch-pre-calculate")
    public Result<String> batchPreCalculate(
            @Parameter(description = "考试计划列表", required = true) @RequestBody List<String> examPlans) {
        try {
            String taskId = scoreSegmentService.batchPreCalculate(examPlans);
            return Result.success("批量预计算任务已启动，任务ID: " + taskId, taskId);
        } catch (Exception e) {
            log.error("批量预计算一分一段表失败", e);
            return Result.error("批量预计算一分一段表失败: " + e.getMessage());
        }
    }

    /**
     * 获取批量预计算进度
     */
    @Operation(summary = "获取批量预计算进度", description = "获取批量预计算任务的执行进度")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "任务不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/batch-calculation-progress/{taskId}")
    public Result<GradeAssignmentProgressVO> getBatchCalculationProgress(
            @Parameter(description = "任务ID", required = true) @PathVariable String taskId) {
        try {
            GradeAssignmentProgressVO progress = scoreSegmentService.getBatchCalculationProgress(taskId);
            if (progress == null) {
                return Result.error("任务不存在或已过期");
            }
            return Result.success(progress);
        } catch (Exception e) {
            log.error("获取批量预计算进度失败", e);
            return Result.error("获取批量预计算进度失败: " + e.getMessage());
        }
    }

    /**
     * 保存一分一段数据到数据库
     */
    @Operation(summary = "生成一分一段数据到数据库", description = "计算并保存一分一段数据到数据库，用于持久化存储")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "保存成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/save-to-database")
    public Result<Boolean> saveScoreSegmentData(
            @Parameter(description = "考试计划代码", required = true) @RequestParam String ksjhdm,
            @Parameter(description = "科目名称", required = true) @RequestParam String kmmc,
            @Parameter(description = "市州名称") @RequestParam(required = false) String szsmc,
            @Parameter(description = "操作人姓名") @RequestParam(required = false) String operatorName,
            @Parameter(description = "操作人工作人员码") @RequestParam(required = false) String operatorCode) {
        try {
            log.info("保存一分一段数据到数据库: ksjhdm={}, kmmc={}, szsmc={}, operatorName={}",
                    ksjhdm, kmmc, szsmc, operatorName);
            Boolean result = scoreSegmentService.saveScoreSegmentData(ksjhdm, kmmc, szsmc, operatorName, operatorCode);
            return Result.success(result);
        } catch (Exception e) {
            log.error("保存一分一段数据失败: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * 从数据库查询一分一段数据
     */
    @Operation(summary = "从数据库查询一分一段数据", description = "从数据库中查询已保存的一分一段数据")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/from-database")
    public Result<List<ScoreSegmentDTO>> getScoreSegmentDataFromDB(
            @Parameter(description = "考试计划代码", required = true) @RequestParam String ksjhdm,
            @Parameter(description = "科目名称", required = true) @RequestParam String kmmc,
            @Parameter(description = "市州名称") @RequestParam(required = false) String szsmc) {
        try {
            log.info("从数据库查询一分一段数据: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc);
            List<ScoreSegmentDTO> result = scoreSegmentService.getScoreSegmentDataFromDB(ksjhdm, kmmc, szsmc);
            return Result.success(result);
        } catch (Exception e) {
            log.error("从数据库查询一分一段数据失败: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 删除一分一段数据
     */
    @Operation(summary = "删除一分一段数据", description = "删除指定条件的一分一段数据。支持灵活删除模式：" +
            "1. 当只传入kmmc时，删除该科目下的所有一分一段数据；" +
            "2. 当只传入szsmc时，删除该市州下的所有一分一段数据；" +
            "3. 当同时传入kmmc和szsmc时，删除指定科目和市州的一分一段数据")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @DeleteMapping("/data")
    public Result<Boolean> deleteScoreSegmentData(
            @Parameter(description = "考试计划代码", required = true) @RequestParam String ksjhdm,
            @Parameter(description = "科目名称（与市州名称至少传入一个）") @RequestParam(required = false) String kmmc,
            @Parameter(description = "市州名称（与科目名称至少传入一个）") @RequestParam(required = false) String szsmc) {
        try {
            log.info("删除一分一段数据: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc);
            Boolean result = scoreSegmentService.deleteScoreSegmentData(ksjhdm, kmmc, szsmc);
            if (result) {
                return Result.success("删除成功", true);
            } else {
                return Result.success("未找到匹配的数据", false);
            }
        } catch (Exception e) {
            log.error("删除一分一段数据失败: ksjhdm={}, kmmc={}, szsmc={}", ksjhdm, kmmc, szsmc, e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

}