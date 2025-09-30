package edu.qhjy.score_service.controller;

import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.dto.GradeBookQueryDTO;
import edu.qhjy.score_service.domain.vo.GradeBookVO;
import edu.qhjy.score_service.service.GradeBookPdfService;
import edu.qhjy.score_service.service.GradeBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 成绩等第册查询控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/grade-book")
@RequiredArgsConstructor
@Tag(name = "成绩等第册", description = "成绩等第册查询相关接口")
public class GradeBookController {

    private final GradeBookService gradeBookService;
    private final GradeBookPdfService gradeBookPdfService;

    /**
     * 分页查询成绩等第册
     */
    @Operation(summary = "分页查询成绩等第册", description = """
            以学校为单位生成成绩等第册，包含学生基本信息和所有科目成绩。\
            
            
            **查询流程建议：**\
            
            1. 首先调用 /exam-plans 获取考试计划列表\
            
            2. 调用 /cities 获取地市列表\
            
            3. 选择地市后调用 /areas?szsmc=xxx 获取考区列表\
            
            4. 选择考区后调用 /schools?szxmc=xxx 获取学校列表\
            
            5. 设置考试计划代码和学校后调用此接口进行查询""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = PageResult.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping(value = "/query", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Result<PageResult<GradeBookVO>> queryGradeBook(
            @Parameter(description = "考试计划代码（必填）", required = true, example = "202507001") @RequestParam("ksjhdm") String ksjhdm,

            @Parameter(description = "学校（必填）", required = true, example = "长沙市第一中学") @RequestParam("school") String school,

            @Parameter(description = "所在省名称", example = "湖南省") @RequestParam(value = "szsmc", required = false) String szsmc,

            @Parameter(description = "所在县名称（区县）", example = "岳麓区") @RequestParam(value = "szxmc", required = false) String szxmc,

            @Parameter(description = "分页页码（从1开始，默认1）", example = "1") @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,

            @Parameter(description = "分页大小（默认20，最大1000）", example = "20") @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {

        try {
            // 构建查询DTO
            GradeBookQueryDTO queryDTO = GradeBookQueryDTO.builder()
                    .ksjhdm(ksjhdm)
                    .school(school)
                    .szsmc(szsmc)
                    .kqmc(szxmc)
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .build();

            log.info("接收到成绩等第册查询请求：{}", queryDTO);
            PageResult<GradeBookVO> result = gradeBookService.queryGradeBook(queryDTO);
            log.info("成绩等第册查询成功，返回{}条记录，总计{}条", result.getRecords().size(), result.getTotal());
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("成绩等第册查询参数错误：{}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("成绩等第册查询失败：{}", e.getMessage(), e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 下载成绩等第册PDF文件
     */
    @Operation(summary = "下载成绩等第册PDF文件", description = """
            生成并下载成绩等第册PDF文件，包含完整的学生成绩信息。\
            
            
            **文件命名规则：** ksjhdm + xxmc（学校名称） + "等级册" + 生成日期 + "版.pdf"\
            
            
            **PDF内容包含：**\
            
            - 标题：KSJHDM + 等级册\
            
            - 页眉信息：考区、学校、打印日期\
            
            - 数据表格：序号、考籍号、姓名、身份证件号、性别、考试科目及等级\
            
            - 自动分页：每页约25条数据，适应A4纸张""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF文件下载成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "404", description = "未找到数据"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping(value = "/download-pdf", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<byte[]> downloadGradeBookPdf(
            @Parameter(description = "考试计划代码（必填）", required = true, example = "202507001") @RequestParam("ksjhdm") String ksjhdm,

            @Parameter(description = "学校（必填）", required = true, example = "长沙市第一中学") @RequestParam("school") String school,

            @Parameter(description = "所在省名称", example = "湖南省") @RequestParam(value = "szsmc", required = false) String szsmc,

            @Parameter(description = "所在县名称（区县）", example = "岳麓区") @RequestParam(value = "szxmc", required = false) String szxmc) {

        try {
            // 构建查询DTO
            GradeBookQueryDTO queryDTO = GradeBookQueryDTO.builder()
                    .ksjhdm(ksjhdm)
                    .school(school)
                    .szsmc(szsmc)
                    .kqmc(szxmc)
                    .build();

            log.info("接收到成绩等第册PDF下载请求：{}", queryDTO);

            // 生成PDF
            ByteArrayOutputStream pdfStream = gradeBookPdfService.generateGradeBookPdf(queryDTO);
            byte[] pdfBytes = pdfStream.toByteArray();

            // 生成文件名
            String fileName = gradeBookPdfService.generatePdfFileName(ksjhdm, school);
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            log.info("成绩等第册PDF生成成功，文件名：{}，文件大小：{}KB", fileName, pdfBytes.length / 1024);

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", encodedFileName);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (IllegalArgumentException e) {
            log.warn("成绩等第册PDF下载参数错误：{}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("成绩等第册PDF下载失败：{}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}