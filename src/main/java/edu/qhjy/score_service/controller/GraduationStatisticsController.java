package edu.qhjy.score_service.controller;

import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.dto.GraduationStatisticsQueryDTO;
import edu.qhjy.score_service.domain.vo.GraduationStatisticsVO;
import edu.qhjy.score_service.service.GraduationStatisticsPdfService;
import edu.qhjy.score_service.service.GraduationStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 毕业生统计控制器
 *
 * @author dadalv
 * @date 2025-8-15
 */
@Slf4j
@RestController
@RequestMapping("/api/graduation/graduation-statistics")
@RequiredArgsConstructor
@Tag(name = "毕业生统计", description = "毕业生统计相关接口")
public class GraduationStatisticsController {

    private final GraduationStatisticsService graduationStatisticsService;
    private final GraduationStatisticsPdfService graduationStatisticsPdfService;

    /**
     * 获取毕业生统计数据
     */
    @Operation(summary = "获取毕业生统计数据", description = "根据毕业年度和级别获取各市州毕业生统计数据")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping
    public Result<GraduationStatisticsVO> getGraduationStatistics(
            @Parameter(description = "毕业年度", required = true, example = "2025") @RequestParam("bynd") Integer bynd,
            @Parameter(description = "级别", required = true, example = "2022") @RequestParam("rxnd") Integer rxnd) {

        log.info("查询毕业生统计数据，毕业年度：{}，级别：{}", bynd, rxnd);

        GraduationStatisticsQueryDTO queryDTO = new GraduationStatisticsQueryDTO();
        queryDTO.setBynd(bynd);
        queryDTO.setRxnd(rxnd);

        return graduationStatisticsService.getGraduationStatistics(queryDTO);
    }

    /**
     * 下载毕业生统计表PDF文件
     */
    @Operation(summary = "下载毕业生统计表PDF文件", description = """
            生成并下载毕业生统计表PDF文件，包含各市州毕业生统计数据。\
            
            
            **文件命名规则：** 毕业年度 + 级别 + "毕业生统计表" + 生成日期 + "版.pdf"\
            
            
            **PDF内容包含：**\
            
            - 标题：毕业年度 + 级别 + 毕业生统计表\
            
            - 统计表格：毕业年度、级别、地市、人数\
            
            - 汇总信息：毕业年度合计、级别合计\
            
            - 生成时间""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF文件下载成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "404", description = "未找到数据"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/download-pdf")
    public ResponseEntity<byte[]> downloadGraduationStatisticsPdf(
            @Parameter(description = "毕业年度（必填）", required = true, example = "2025") @RequestParam("bynd") Integer bynd,

            @Parameter(description = "级别（必填）", required = true, example = "2022") @RequestParam("rxnd") Integer rxnd) {

        try {
            log.info("毕业生统计表PDF下载请求，毕业年度：{}，级别：{}", bynd, rxnd);

            // 构建查询条件
            GraduationStatisticsQueryDTO queryDTO = new GraduationStatisticsQueryDTO();
            queryDTO.setBynd(bynd);
            queryDTO.setRxnd(rxnd);

            // 生成PDF
            ByteArrayOutputStream pdfStream = graduationStatisticsPdfService.generateGraduationStatisticsPdf(queryDTO);
            byte[] pdfBytes = pdfStream.toByteArray();

            // 生成文件名
            String fileName = graduationStatisticsPdfService.generatePdfFileName(queryDTO);
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", encodedFileName);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            log.info("毕业生统计表PDF下载成功，文件名：{}，文件大小：{}字节", fileName, pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (IllegalArgumentException e) {
            log.warn("毕业生统计表PDF下载参数错误：{}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("毕业生统计表PDF下载失败：{}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}