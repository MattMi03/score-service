package edu.qhjy.score_service.controller;

import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.dto.ExamScoreQueryDTO;
import edu.qhjy.score_service.domain.dto.GradeQueryDTO;
import edu.qhjy.score_service.domain.dto.InitializeExamStudentsDTO;
import edu.qhjy.score_service.domain.vo.ExamPlanStatisticsVO;
import edu.qhjy.score_service.domain.vo.ExamScoreVO;
import edu.qhjy.score_service.domain.vo.GradeQueryVO;
import edu.qhjy.score_service.domain.vo.InitializeResultVO;
import edu.qhjy.score_service.service.GradeService;
import edu.qhjy.score_service.service.ScoreService;
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
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 成绩管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/score")
@RequiredArgsConstructor
@Tag(name = "成绩管理", description = "成绩管理相关接口")
public class ScoreController {

    private final ScoreService scoreService;
    private final GradeService gradeService;

    /**
     * 根据考试计划代码和科目类型查询科目列表
     */
    @Operation(summary = "查询科目列表", description = "用于前端下拉选择，查询指定考试计划下的考查性科目列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/subjects")
    public Result<List<String>> getSubjectsByKsjhdmAndKmlx(
            @Parameter(description = "考试计划代码", required = true, example = "202507001") @RequestParam("ksjhdm") String ksjhdm,
            @Parameter(description = "科目类型（0表示合格性科目，1表示考查性科目，不传则查询所有类型）", example = "1") @RequestParam(value = "kmlx", required = false) Integer kmlx) {

        try {
            log.info("开始查询科目列表，考试计划代码：{}，科目类型：{}", ksjhdm, kmlx);
            List<String> subjects = scoreService.getSubjectsByKsjhdmAndKmlx(ksjhdm, kmlx);
            log.info("查询科目列表成功，返回{}个科目", subjects.size());
            return Result.success(subjects);
        } catch (Exception e) {
            log.error("查询科目列表失败", e);
            return Result.error("查询科目列表失败：" + e.getMessage());
        }
    }

    /**
     * 查询考试计划科目统计信息
     */
    @Operation(summary = "查询考试计划科目统计信息", description = "列出对应考试计划和科目下的报考人数")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = ExamPlanStatisticsVO.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/exam-plan-statistics")
    public Result<List<ExamPlanStatisticsVO>> getExamPlanStatistics(
            @Parameter(description = "考试计划代码") @RequestParam(value = "ksjhdm", required = false) String ksjhdm) {

        try {
            log.info("开始查询考试计划科目统计信息，考试计划代码：{}", ksjhdm);
            List<ExamPlanStatisticsVO> statistics = scoreService.getExamPlanStatistics(ksjhdm);
            log.info("查询考试计划科目统计信息成功，返回{}条记录", statistics.size());
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("查询考试计划科目统计信息失败", e);
            return Result.error("查询考试计划科目统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 成绩查询接口（支持级联查询和分页）
     */
    @Operation(summary = "成绩查询", description = "支持级联查询和分页的成绩查询接口，级联顺序：szsmc→kqmc→xxmc→grade→bjmc→ksh")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = PageResult.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/query")
    public Result<PageResult<GradeQueryVO>> queryGrades(
            @Parameter(description = "考试计划代码", required = true) @RequestParam("ksjhdm") String ksjhdm,
            @Parameter(description = "地市名称") @RequestParam(value = "szsmc", required = false) String szsmc,
            @Parameter(description = "考区名称") @RequestParam(value = "szxmc", required = false) String szxmc,
            @Parameter(description = "学校名称") @RequestParam(value = "xxmc", required = false) String xxmc,
            @Parameter(description = "年级（入学年度）") @RequestParam(value = "grade", required = false) String grade,
            @Parameter(description = "班级名称") @RequestParam(value = "bjmc", required = false) String bjmc,
            @Parameter(description = "考籍号") @RequestParam(value = "ksh", required = false) String ksh,
            @Parameter(description = "科目类型：0-合格性考试科目，1-考察性考试科目") @RequestParam(value = "kmlx", defaultValue = "0") Integer kmlx,
            @Parameter(description = "页码") @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @Parameter(description = "排序字段") @RequestParam(value = "sortField", required = false) String sortField,
            @Parameter(description = "排序方向：asc-升序，desc-降序") @RequestParam(value = "sortOrder", defaultValue = "asc") String sortOrder,
            @Parameter(description = "是否只返回有成绩的考生：true-仅返回正考记录，false-返回所有考生包括正考和缺考") @RequestParam(value = "onlyWithScores", defaultValue = "false") Boolean onlyWithScores) {

        try {
            // 构建查询DTO
            GradeQueryDTO queryDTO = GradeQueryDTO.builder()
                    .ksjhdm(ksjhdm)
                    .szsmc(szsmc)
                    .kqmc(szxmc)
                    .xxmc(xxmc)
                    .grade(grade)
                    .bjmc(bjmc)
                    .ksh(ksh)
                    .kmlx(kmlx)
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .sortField(sortField)
                    .sortOrder(sortOrder)
                    .onlyWithScores(onlyWithScores)
                    .build();

            log.info("开始查询成绩数据，查询条件：{}", queryDTO);
            PageResult<GradeQueryVO> result = gradeService.queryGradeData(queryDTO);
            log.info("查询成绩数据成功，返回{}条记录，总计{}条", result.getRecords().size(), result.getTotal());

            return Result.success(result);
        } catch (Exception e) {
            log.error("查询成绩数据失败", e);
            return Result.error("查询成绩数据失败：" + e.getMessage());
        }
    }

    /**
     * 考查科目成绩查询接口
     */
    @Operation(summary = "考查科目成绩查询", description = "分页查询考查科目成绩数据，支持按市、考区、学校、考籍号级联筛选")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = PageResult.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/exam-scores")
    public Result<PageResult<ExamScoreVO>> getExamScores(
            @Parameter(description = "考试计划代码", required = true) @RequestParam("ksjhdm") String ksjhdm,
            @Parameter(description = "科目名称", required = true) @RequestParam(value = "kmmc") String kmmc,
            @Parameter(description = "所在市名称") @RequestParam(value = "szsmc", required = false) String szsmc,
            @Parameter(description = "考区名称") @RequestParam(value = "kqmc", required = false) String kqmc,
            @Parameter(description = "学校名称") @RequestParam(value = "xxmc", required = false) String xxmc,
            @Parameter(description = "考籍号") @RequestParam(value = "ksh", required = false) String ksh,
            @Parameter(description = "是否只查询有成绩的记录：true-只返回有成绩数据的记录，false-只返回没有成绩数据的记录，不传-返回所有记录") @RequestParam(value = "withScores", required = false) Boolean withScores,
            @Parameter(description = "页码") @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        try {
            // 构建查询DTO
            ExamScoreQueryDTO query = new ExamScoreQueryDTO();
            query.setKsjhdm(ksjhdm);
            query.setKmmc(kmmc);
            query.setSzsmc(szsmc);
            query.setKqmc(kqmc);
            query.setXxmc(xxmc);
            query.setKsh(ksh);
            query.setWithScores(withScores);
            query.setPageNum(pageNum);
            query.setPageSize(pageSize);

            log.info("开始查询考查科目成绩，查询条件：{}", query);
            PageResult<ExamScoreVO> result = scoreService.getExamScoresWithPagination(query);
            log.info("查询考查科目成绩成功，返回{}条记录，总计{}条", result.getRecords().size(), result.getTotal());
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询考查科目成绩失败: {}", e.getMessage(), e);
            return Result.error("查询考查科目成绩失败: " + e.getMessage());
        }
    }

    /**
     * 导入考试科目考生（初始化接口）
     */
    @Operation(summary = "导入考试科目考生（初始化接口）", description = "根据考试计划、科目、地区等条件初始化考生成绩记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "初始化成功", content = @Content(schema = @Schema(implementation = InitializeResultVO.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/initialize-students")
    public Result<InitializeResultVO> initializeExamStudents(@Valid @RequestBody InitializeExamStudentsDTO request) {
        try {
            log.info("开始初始化考试科目考生，请求参数：{}", request);
            InitializeResultVO result = scoreService.initializeExamStudents(request);
            log.info("初始化考试科目考生成功，结果：{}", result);
            return Result.success(result);
        } catch (Exception e) {
            log.error("初始化考试科目考生失败: {}", e.getMessage(), e);
            return Result.error("初始化考试科目考生失败: " + e.getMessage());
        }
    }

    /**
     * 生成Excel导入模板接口
     */
    @Operation(summary = "生成Excel导入模板", description = "根据考试计划代码和科目名称生成Excel导入模板，预填入学生基本信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "模板生成成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/generate-excel-template")
    public ResponseEntity<byte[]> generateExcelTemplate(
            @Parameter(description = "考试计划代码", required = true) @RequestParam("ksjhdm") String ksjhdm,
            @Parameter(description = "科目名称", required = true) @RequestParam("kmmc") String kmmc,
            @Parameter(description = "地市名称", required = true) @RequestParam("szsmc") String szsmc,
            @Parameter(description = "考区名称", required = true) @RequestParam("kqmc") String kqmc,
            @Parameter(description = "学校名称", required = true) @RequestParam("xxmc") String xxmc) {

        try {
            log.info("开始生成Excel导入模板，考试计划代码: {}, 科目名称: {}, 地市: {}, 考区: {}, 学校: {}",
                    ksjhdm, kmmc, szsmc, kqmc, xxmc);

            byte[] excelBytes = scoreService.generateExcelTemplate(ksjhdm, kmmc, szsmc, kqmc, xxmc);

            // 生成文件名，格式：{ksjhdm}{kqmc}{xxmc}{kmmc}导入模板.xlsx
            String fileName = String.format("%s%s%s%s导入模板.xlsx",
                    ksjhdm != null ? ksjhdm : "",
                    kqmc != null ? kqmc : "",
                    xxmc != null ? xxmc : "",
                    kmmc != null ? kmmc : "");

            // 对文件名进行URL编码以支持中文字符
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");
            headers.setContentLength(excelBytes.length);

            log.info("Excel导入模板生成成功，文件名: {}, 文件大小: {}KB", fileName, excelBytes.length / 1024);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);

        } catch (Exception e) {
            log.error("生成Excel导入模板失败，考试计划代码: {}, 科目名称: {}", ksjhdm, kmmc, e);
            return ResponseEntity.status(500)
                    .body(null);
        }
    }

    /**
     * 导入Excel成绩文件接口
     */
    @Operation(summary = "导入Excel成绩文件", description = "读取Excel文件并批量更新成绩数据")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "导入成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/import-excel-scores")
    public Result<Object> importExcelScores(
            @Parameter(description = "考试计划代码", required = true) @RequestParam("ksjhdm") String ksjhdm,
            @Parameter(description = "科目名称", required = true) @RequestParam("kmmc") String kmmc,
            @Parameter(description = "Excel文件", required = true) @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

        try {
            log.info("开始导入Excel成绩文件，考试计划代码：{}，科目名称：{}，文件名：{}",
                    ksjhdm, kmmc, file.getOriginalFilename());

            // 验证文件
            if (file.isEmpty()) {
                return Result.error("上传的文件为空");
            }

            // 调用服务导入Excel
            Object result = scoreService.importExcelScores(ksjhdm, kmmc, file);

            log.info("Excel成绩文件导入完成");

            return Result.success(result);

        } catch (Exception e) {
            log.error("导入Excel成绩文件失败，考试计划代码：{}，科目名称：{}，文件名：{}",
                    ksjhdm, kmmc, file.getOriginalFilename(), e);
            return Result.error("导入失败：" + e.getMessage());
        }
    }

}