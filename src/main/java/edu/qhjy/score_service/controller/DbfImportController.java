package edu.qhjy.score_service.controller;

import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.dto.DbfImportRequestDTO;
import edu.qhjy.score_service.domain.dto.PfgjQueryRequestDTO;
import edu.qhjy.score_service.domain.vo.DbfImportResponseVO;
import edu.qhjy.score_service.domain.vo.ImportResultVO;
import edu.qhjy.score_service.domain.vo.PfgjQueryResultVO;
import edu.qhjy.score_service.service.DbfImportService;
import edu.qhjy.score_service.service.PfgjImportService;
import edu.qhjy.score_service.service.PfgjQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * DBF文件导入控制器
 *
 * @author dadalv
 * @since 2025-08-01
 */
@Slf4j
@RestController
@RequestMapping("/api/dbf")
@RequiredArgsConstructor
@Validated
@Tag(name = "DBF文件导入", description = "DBF文件导入相关接口")
public class DbfImportController {

    private final DbfImportService dbfImportService;
    private final PfgjImportService pfgjImportService;
    private final PfgjQueryService pfgjQueryService;

    /**
     * 导入DBF文件
     *
     * @param file   DBF文件
     * @param ksjhdm 考试计划代码
     * @return 导入结果
     */
    @PostMapping(value = "/import/kscj", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入成绩数据DBF文件", description = "上传DBF文件并导入成绩数据到系统中")
    public ResponseEntity<DbfImportResponseVO> importDbfFile(
            @Parameter(description = "DBF文件，最大300MB", required = true) @RequestParam("file") @NotNull MultipartFile file,

            @Parameter(description = "考试计划代码", required = true, example = "202507") @RequestParam("ksjhdm") @NotBlank String ksjhdm) {

        // ==================== 【新增验证日志】 ====================
        log.debug("【计时点 B' - Controller 开始】Controller方法体开始执行。");
        // =======================================================

        log.debug("接收DBF文件导入请求: 文件名={}, 大小={} bytes, 考试计划代码={}",
                file.getOriginalFilename(), file.getSize(), ksjhdm);

        try {
            // 验证请求参数
            DbfImportRequestDTO request = new DbfImportRequestDTO();
            request.setFile(file);
            request.setKsjhdm(ksjhdm);

            if (!dbfImportService.validateImportRequest(request)) {
                DbfImportResponseVO errorResponse = DbfImportResponseVO.createFailureResponse(
                        file.getOriginalFilename(),
                        "请求参数验证失败：请检查文件格式(.dbf)和文件大小(≤300MB)",
                        file.getSize(), ksjhdm, null);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 执行导入
            DbfImportResponseVO response = dbfImportService.importDbfFile(file, ksjhdm);

            if (response.isSuccess()) {
                log.debug("DBF文件导入成功: 文件名={}, 成功记录数={}, 失败记录数={}",
                        file.getOriginalFilename(), response.getSuccessCount(), response.getFailedCount());
                return ResponseEntity.ok(response);
            } else {
                log.warn("DBF文件导入失败: 文件名={}, 错误信息={}",
                        file.getOriginalFilename(), response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("DBF文件导入过程中发生异常: 文件名={}, 错误={}",
                    file.getOriginalFilename(), e.getMessage(), e);

            DbfImportResponseVO errorResponse = DbfImportResponseVO.createFailureResponse(
                    file.getOriginalFilename(),
                    "导入过程中发生系统错误: " + e.getMessage(),
                    file.getSize(), ksjhdm, null);
            errorResponse.setErrorDetails(e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 预览DBF文件内容
     *
     * @param file         DBF文件
     * @param ksjhdm       考试计划代码
     * @param previewCount 预览记录数量，默认10条
     * @return 预览结果
     */
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "预览DBF文件", description = "预览DBF文件内容，不执行实际导入")
    public ResponseEntity<DbfImportResponseVO> previewDbfFile(
            @Parameter(description = "DBF文件", required = true) @RequestParam("file") @NotNull MultipartFile file,

            @Parameter(description = "考试计划代码", required = true, example = "202507") @RequestParam("ksjhdm") @NotBlank String ksjhdm,

            @Parameter(description = "预览记录数量", example = "10") @RequestParam(value = "previewCount", defaultValue = "10") int previewCount) {

        log.debug("接收DBF文件预览请求: 文件名={}, 预览数量={}", file.getOriginalFilename(), previewCount);

        try {
            DbfImportResponseVO response = dbfImportService.previewDbfFile(file, ksjhdm, previewCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("DBF文件预览失败: 文件名={}, 错误={}", file.getOriginalFilename(), e.getMessage(), e);

            DbfImportResponseVO errorResponse = DbfImportResponseVO.createFailureResponse(
                    file.getOriginalFilename(),
                    "预览失败: " + e.getMessage(),
                    file.getSize(), ksjhdm, null);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 获取导入任务进度
     *
     * @param taskId 任务ID
     * @return 任务进度信息
     */
    @GetMapping("/progress/{taskId}")
    @Operation(summary = "获取导入进度", description = "查询异步导入任务的进度信息")
    public ResponseEntity<DbfImportResponseVO> getImportProgress(
            @Parameter(description = "任务ID", required = true) @PathVariable @NotBlank String taskId) {

        log.debug("查询导入任务进度: taskId={}", taskId);

        try {
            DbfImportResponseVO progress = dbfImportService.getImportProgress(taskId);

            if (progress != null) {
                return ResponseEntity.ok(progress);
            } else {
                DbfImportResponseVO notFoundResponse = DbfImportResponseVO.createFailureResponse(
                        null, "未找到指定的导入任务", 0L, null, null);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("查询导入进度失败: taskId={}, 错误={}", taskId, e.getMessage(), e);

            DbfImportResponseVO errorResponse = DbfImportResponseVO.createFailureResponse(
                    null, "查询进度失败: " + e.getMessage(), 0L, null, null);

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 取消导入任务
     *
     * @param taskId 任务ID
     * @return 取消结果
     */
    @DeleteMapping("/cancel/{taskId}")
    @Operation(summary = "取消导入任务", description = "取消正在进行的异步导入任务")
    public ResponseEntity<String> cancelImportTask(
            @Parameter(description = "任务ID", required = true) @PathVariable @NotBlank String taskId) {

        log.debug("接收取消导入任务请求: taskId={}", taskId);

        try {
            boolean cancelled = dbfImportService.cancelImportTask(taskId);

            if (cancelled) {
                return ResponseEntity.ok("任务已成功取消");
            } else {
                return ResponseEntity.badRequest().body("任务取消失败，可能任务已完成或不存在");
            }

        } catch (Exception e) {
            log.error("取消导入任务失败: taskId={}, 错误={}", taskId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("取消任务时发生错误: " + e.getMessage());
        }
    }

    /**
     * 调试DBF文件解析
     *
     * @param file   DBF文件
     * @param ksjhdm 考试计划代码
     * @return 调试信息
     */
    @PostMapping(value = "/debug", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "调试DBF文件", description = "调试DBF文件解析过程，用于排查问题")
    public ResponseEntity<String> debugDbfFile(
            @Parameter(description = "DBF文件", required = true) @RequestParam("file") @NotNull MultipartFile file,
            @Parameter(description = "考试计划代码", required = true, example = "202507") @RequestParam("ksjhdm") @NotBlank String ksjhdm) {

        log.debug("接收DBF文件调试请求: 文件名={}, 大小={} bytes, 考试计划代码={}",
                file.getOriginalFilename(), file.getSize(), ksjhdm);

        try {
            String debugInfo = dbfImportService.debugDbfFile(file, ksjhdm);
            return ResponseEntity.ok(debugInfo);

        } catch (Exception e) {
            log.error("DBF文件调试失败: 文件名={}, 错误={}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.badRequest().body("调试失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查接口
     *
     * @return 服务状态
     */
    /**
     * 导入评分轨迹DBF文件
     *
     * @param file     DBF文件
     * @param ksjhdm   考试计划代码
     * @param cjrxm    创建人姓名
     * @param cjrgzrym 创建人工作人员码
     * @return 导入结果
     */
    @PostMapping(value = "/import/pfgj", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入评分轨迹DBF文件", description = "上传DBF文件并导入评分轨迹数据到系统中")
    public ResponseEntity<ImportResultVO> importPfgjDbfFile(
            @Parameter(description = "DBF文件，最大300MB", required = true) @RequestParam("file") @NotNull MultipartFile file,

            @Parameter(description = "考试计划代码", required = true, example = "202507") @RequestParam("ksjhdm") @NotBlank String ksjhdm,

            @Parameter(description = "创建人姓名", required = false, example = "张三") @RequestParam(value = "cjrxm", required = false) String cjrxm,

            @Parameter(description = "创建人工作人员码", required = false, example = "001") @RequestParam(value = "cjrgzrym", required = false) String cjrgzrym) {

        log.debug("接收评分轨迹DBF文件导入请求: 文件名={}, 大小={} bytes, 考试计划代码={}, 创建人={}",
                file.getOriginalFilename(), file.getSize(), ksjhdm, cjrxm);

        try {
            // 验证文件格式
            if (!file.getOriginalFilename().toLowerCase().endsWith(".dbf")) {
                return ResponseEntity.badRequest().body(
                        ImportResultVO.builder()
                                .success(false)
                                .message("文件格式错误，请上传.dbf格式文件")
                                .totalCount(0)
                                .successCount(0)
                                .failCount(0)
                                .fileName(file.getOriginalFilename())
                                .build());
            }

            // 验证文件大小（300MB限制）
            if (file.getSize() > 300 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(
                        ImportResultVO.builder()
                                .success(false)
                                .message("文件大小超过300MB限制")
                                .totalCount(0)
                                .successCount(0)
                                .failCount(0)
                                .build());
            }

            // 执行导入
            ImportResultVO result = pfgjImportService.importPfgjFromDbf(file, ksjhdm, cjrxm, cjrgzrym);

            if (result.isSuccess()) {
                log.debug("评分轨迹DBF文件导入成功: 文件名={}, 成功记录数={}, 失败记录数={}",
                        file.getOriginalFilename(), result.getSuccessCount(), result.getFailCount());
                return ResponseEntity.ok(result);
            } else {
                log.warn("评分轨迹DBF文件导入失败: 文件名={}, 错误信息={}",
                        file.getOriginalFilename(), result.getMessage());
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            log.error("评分轨迹DBF文件导入过程中发生异常: 文件名={}, 错误={}",
                    file.getOriginalFilename(), e.getMessage(), e);

            ImportResultVO errorResult = ImportResultVO.builder()
                    .success(false)
                    .message("导入过程中发生系统错误: " + e.getMessage())
                    .totalCount(0)
                    .successCount(0)
                    .failCount(0)
                    .fileName(file.getOriginalFilename())
                    .build();

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 验证评分轨迹DBF文件结构
     *
     * @param file DBF文件
     * @return 验证结果
     */
    @PostMapping(value = "/validate/pfgj", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "验证评分轨迹DBF文件结构", description = "验证DBF文件是否包含必需的字段")
    public ResponseEntity<ImportResultVO> validatePfgjDbfFile(
            @Parameter(description = "DBF文件", required = true) @RequestParam("file") @NotNull MultipartFile file) {

        log.debug("接收评分轨迹DBF文件验证请求: 文件名={}, 大小={} bytes",
                file.getOriginalFilename(), file.getSize());

        try {
            // 验证文件格式
            if (!file.getOriginalFilename().toLowerCase().endsWith(".dbf")) {
                return ResponseEntity.badRequest().body(
                        ImportResultVO.builder()
                                .success(false)
                                .message("文件格式错误，请上传.dbf格式文件")
                                .totalCount(0)
                                .successCount(0)
                                .failCount(0)
                                .build());
            }

            // 验证文件结构
            boolean isValid = pfgjImportService.validateDbfFileStructure(file);
            int recordCount = pfgjImportService.getDbfRecordCount(file);

            ImportResultVO result = ImportResultVO.builder()
                    .success(isValid)
                    .message(isValid ? "DBF文件结构验证通过" : "DBF文件结构验证失败，缺少必需字段")
                    .totalCount(recordCount)
                    .successCount(isValid ? recordCount : 0)
                    .failCount(isValid ? 0 : recordCount)
                    .fileName(file.getOriginalFilename())
                    .build();

            log.debug("评分轨迹DBF文件验证完成: 文件名={}, 验证结果={}, 记录数={}",
                    file.getOriginalFilename(), isValid, recordCount);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("评分轨迹DBF文件验证过程中发生异常: 文件名={}, 错误={}",
                    file.getOriginalFilename(), e.getMessage(), e);

            ImportResultVO errorResult = ImportResultVO.builder()
                    .success(false)
                    .message("验证过程中发生系统错误: " + e.getMessage())
                    .totalCount(0)
                    .successCount(0)
                    .failCount(0)
                    .fileName(file.getOriginalFilename())
                    .build();

            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    /**
     * 查询评分轨迹数据
     *
     * @param ksjhdm 考试计划代码
     * @param ksh    考生号
     * @param kmmc   科目名称
     * @return 评分轨迹数据列表
     */
    @GetMapping("/query/pfgj")
    @Operation(summary = "查询评分轨迹数据", description = "根据考试计划代码、考生号和科目名称查询评分轨迹数据")
    public Result<List<PfgjQueryResultVO>> queryPfgjData(
            @Parameter(description = "考试计划代码", required = true, example = "202501") @RequestParam("ksjhdm") @NotBlank String ksjhdm,
            @Parameter(description = "考生号", required = true, example = "20250101001") @RequestParam("ksh") @NotBlank String ksh,
            @Parameter(description = "科目名称", required = true, example = "语文") @RequestParam("kmmc") @NotBlank String kmmc) {

        log.debug("接收到查询评分轨迹数据请求，参数：ksjhdm={}, ksh={}, kmmc={}", ksjhdm, ksh, kmmc);

        try {
            // 构建查询请求DTO
            PfgjQueryRequestDTO request = new PfgjQueryRequestDTO();
            request.setKsjhdm(ksjhdm);
            request.setKsh(ksh);
            request.setKmmc(kmmc);

            // 执行查询
            List<PfgjQueryResultVO> resultList = pfgjQueryService.queryPfgjData(request);

            if (resultList.isEmpty()) {
                log.debug("查询评分轨迹数据完成，未找到匹配的记录，参数：ksjhdm={}, ksh={}, kmmc={}", ksjhdm, ksh, kmmc);
                return Result.success("未找到匹配的评分轨迹数据", resultList);
            }

            log.debug("查询评分轨迹数据成功，返回{}条记录", resultList.size());
            return Result.success(resultList);

        } catch (Exception e) {
            log.error("查询评分轨迹数据失败，参数：ksjhdm={}, ksh={}, kmmc={}", ksjhdm, ksh, kmmc, e);
            return Result.error("查询评分轨迹数据失败：" + e.getMessage());
        }
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查DBF导入服务的健康状态")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("DBF导入服务运行正常");
    }
}