package edu.qhjy.score_service.controller;

import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.dto.BatchGraduationDTO;
import edu.qhjy.score_service.domain.dto.GraduationPdfQueryDTO;
import edu.qhjy.score_service.domain.dto.GraduationQueryDTO;
import edu.qhjy.score_service.domain.vo.GraduationStudentVO;
import edu.qhjy.score_service.service.GraduationPdfService;
import edu.qhjy.score_service.service.GraduationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 毕业生花名册管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/graduation")
@RequiredArgsConstructor
@Validated
@Tag(name = "毕业生花名册管理", description = "毕业生条件查询和批量审批相关接口")
public class GraduationController {

    private final GraduationService graduationService;
    private final GraduationPdfService graduationPdfService;

    /**
     * 毕业生条件查询（分页）
     */
    @GetMapping("/students")
    @Operation(summary = "毕业生条件查询", description = "根据szsmc（必填）以及可选的kqmc、xxmc、bynd、ksh进行级联查询，支持分页和排序")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = PageResult.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<PageResult<GraduationStudentVO>> queryGraduationStudents(
            @Parameter(description = "所在市名称", required = true) @RequestParam("szsmc") @NotBlank(message = "所在市名称不能为空") String szsmc,
            @Parameter(description = "考区名称") @RequestParam(value = "szxmc", required = false) String szxmc,
            @Parameter(description = "学校名称") @RequestParam(value = "xxmc", required = false) String xxmc,
            @Parameter(description = "毕业年度") @RequestParam(value = "bynd", required = false) String bynd,
            @Parameter(description = "考生号") @RequestParam(value = "ksh", required = false) String ksh,
            @Parameter(description = "是否满足毕业条件") @RequestParam(value = "isQualified", required = false) Boolean isQualified,
            @Parameter(description = "页码") @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @Parameter(description = "排序字段") @RequestParam(value = "sortField", required = false) String sortField,
            @Parameter(description = "排序方向") @RequestParam(value = "sortOrder", required = false) String sortOrder) {

        try {
            log.info(
                    "开始查询毕业生信息，参数：szsmc={}, s={}, xxmc={}, bynd={}, ksh={}, isQualified={}, pageNum={}, pageSize={}",
                    szsmc, szxmc, xxmc, bynd, ksh, isQualified, pageNum, pageSize);

            // 构建查询DTO
            GraduationQueryDTO queryDTO = GraduationQueryDTO.builder()
                    .szsmc(szsmc)
                    .kqmc(szxmc)
                    .xxmc(xxmc)
                    .bynd(bynd)
                    .ksh(ksh)
                    .isQualified(isQualified)
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .sortField(sortField)
                    .sortOrder(sortOrder)
                    .build();

            Result<PageResult<GraduationStudentVO>> result = graduationService.queryGraduationStudents(queryDTO);
            log.info("查询毕业生信息完成");
            return result;

        } catch (Exception e) {
            log.error("查询毕业生信息失败", e);
            return Result.error("查询毕业生信息失败：" + e.getMessage());
        }
    }

    /**
     * 批量毕业审批
     */
    @PostMapping("/batch-approval")
    @Operation(summary = "批量毕业审批", description = "批量更新学生毕业状态，修改BYND为当前年份，KJZTMC更新为'毕业'")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "审批成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<String> batchGraduationApproval(@Valid @RequestBody BatchGraduationDTO batchDTO) {
        try {
            log.info("开始批量毕业审批，参数：szsmc={}, kqmc={}, xxmc={}, ksh={}, bynd={}, operatorName={}",
                    batchDTO.getSzsmc(), batchDTO.getKqmc(), batchDTO.getXxmc(),
                    batchDTO.getKsh(), batchDTO.getBynd(), batchDTO.getOperatorName());

            Result<String> result = graduationService.batchGraduationApproval(batchDTO);
            log.info("批量毕业审批完成");
            return result;

        } catch (Exception e) {
            log.error("批量毕业审批失败", e);
            return Result.error("批量毕业审批失败：" + e.getMessage());
        }
    }

    /**
     * 检查学生毕业条件
     */
    @GetMapping("/check-qualification")
    @Operation(summary = "检查学生毕业条件", description = "检查指定学生是否满足毕业条件")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "检查成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<Boolean> checkGraduationQualification(
            @Parameter(description = "考生号", required = true) @RequestParam("ksh") @NotBlank(message = "考生号不能为空") String ksh,
            @Parameter(description = "所在市名称", required = true) @RequestParam("szsmc") @NotBlank(message = "所在市名称不能为空") String szsmc) {

        try {
            log.info("开始检查学生毕业条件，参数：ksh={}, szsmc={}", ksh, szsmc);

            boolean isQualified = graduationService.checkGraduationQualification(ksh, szsmc);
            log.info("检查学生毕业条件完成，结果：{}", isQualified ? "满足条件" : "不满足条件");
            return Result.success(isQualified);

        } catch (Exception e) {
            log.error("检查学生毕业条件失败", e);
            return Result.error("检查学生毕业条件失败：" + e.getMessage());
        }
    }

    /**
     * 获取学生毕业条件详情
     */
    @GetMapping("/student-details")
    @Operation(summary = "获取学生毕业条件详情", description = "获取指定学生的详细毕业条件信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = GraduationStudentVO.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "404", description = "学生信息未找到"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<GraduationStudentVO> getStudentGraduationDetails(
            @Parameter(description = "考生号", required = true) @RequestParam("ksh") @NotBlank(message = "考生号不能为空") String ksh,
            @Parameter(description = "所在市名称", required = true) @RequestParam("szsmc") @NotBlank(message = "所在市名称不能为空") String szsmc) {

        try {
            log.info("开始获取学生毕业条件详情，参数：ksh={}, szsmc={}", ksh, szsmc);

            GraduationStudentVO studentDetails = graduationService.getStudentGraduationDetails(ksh, szsmc);
            if (studentDetails == null) {
                log.warn("未找到学生信息，ksh={}, szsmc={}", ksh, szsmc);
                return Result.error("未找到学生信息");
            }

            log.info("获取学生毕业条件详情完成，学生：{}", studentDetails.getXm());
            return Result.success(studentDetails);

        } catch (Exception e) {
            log.error("获取学生毕业条件详情失败", e);
            return Result.error("获取学生毕业条件详情失败：" + e.getMessage());
        }
    }

    /**
     * 下载毕业生花名册PDF文件
     */
    @Operation(summary = "下载毕业生花名册PDF文件", description = """
            生成并下载毕业生花名册PDF文件，包含完整的学生信息和所有科目成绩。\
            
            
            **文件命名规则：** 毕业年度 + 学校名称 + "毕业生花名册" + 生成日期 + "版.pdf"\
            
            
            **PDF内容包含：**\
            
            - 标题：毕业年度 + 毕业生花名册\
            
            - 页眉信息：地区、学校、打印日期\
            
            - 数据表格：序号、考生号、姓名、性别、所在市县、学校、各科目成绩\
            
            - 自动分页：每页约20条数据，适应A4纸张""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF文件下载成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "404", description = "未找到数据"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping(value = "/download-pdf", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<byte[]> downloadGraduationPdf(
            @Parameter(description = "毕业年度（必填）", required = true, example = "2024") @RequestParam("bynd") String bynd,

            @Parameter(description = "所在市名称", example = "长沙市") @RequestParam(value = "szsmc", required = false) String szsmc,

            @Parameter(description = "考区名称", example = "岳麓区") @RequestParam(value = "kqmc", required = false) String kqmc,

            @Parameter(description = "学校名称", example = "长沙市第一中学") @RequestParam(value = "xxmc", required = false) String xxmc,

            @Parameter(description = "考生号", example = "202401001") @RequestParam(value = "ksh", required = false) String ksh,

            @Parameter(description = "排序字段", example = "ksh") @RequestParam(value = "sortField", required = false) String sortField,

            @Parameter(description = "排序方向（asc/desc）", example = "asc") @RequestParam(value = "sortOrder", required = false) String sortOrder,

            @Parameter(description = "是否只查询满足毕业条件的学生（true/false）", example = "true") @RequestParam(value = "isQualified", required = false) Boolean isQualified) {

        try {
            // 构建查询DTO
            GraduationPdfQueryDTO queryDTO = GraduationPdfQueryDTO.builder()
                    .bynd(bynd)
                    .szsmc(szsmc)
                    .kqmc(kqmc)
                    .xxmc(xxmc)
                    .ksh(ksh)
                    .sortField(sortField)
                    .sortOrder(sortOrder)
                    .isQualified(isQualified)
                    .build();

            log.info("接收到毕业生花名册PDF下载请求：{}", queryDTO);

            // 生成PDF
            ByteArrayOutputStream pdfStream = graduationPdfService.generateGraduationPdf(queryDTO);
            byte[] pdfBytes = pdfStream.toByteArray();

            // 生成文件名
            String fileName = graduationPdfService.generatePdfFileName(queryDTO);
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            log.info("毕业生花名册PDF生成成功，文件名：{}，文件大小：{}KB", fileName, pdfBytes.length / 1024);

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", encodedFileName);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (IllegalArgumentException e) {
            log.warn("毕业生花名册PDF下载参数错误：{}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("毕业生花名册PDF下载失败：{}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}