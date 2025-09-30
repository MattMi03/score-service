package edu.qhjy.score_service.controller;

import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.dto.StudentDataQueryDTO;
import edu.qhjy.score_service.domain.vo.StudentDataVO;
import edu.qhjy.score_service.service.StudentDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生数据查询控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/student-data")
@RequiredArgsConstructor
@Tag(name = "学生数据查询", description = "学生成绩数据查询和导出接口")
public class StudentDataController {

    private final StudentDataService studentDataService;

    /**
     * 分页查询学生数据
     */
    @Operation(summary = "分页查询学生数据", description = """
            支持多条件筛选的学生成绩数据分页查询，使用application/x-www-form-urlencoded格式。\
            
            
            **查询流程建议：**\
            
            1. 首先调用 /exam-plans 获取考试计划列表\
            
            2. 选择考试计划后调用 /subjects?ksjhmc=xxx 获取科目列表\
            
            3. 调用 /cities 获取地市列表\
            
            4. 选择地市后调用 /areas?szsmc=xxx 获取考区列表\
            
            5. 选择考区后调用 /schools?szxmc=xxx 获取学校列表\
            
            6. 设置其他筛选条件后调用此接口进行查询""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = PageResult.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping(value = "/query", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Result<PageResult<StudentDataVO>> queryStudentData(
            @Parameter(description = "考试计划代码（必填）", required = true, example = "202507001") @RequestParam("ksjhdm") String ksjhdm,

            @Parameter(description = "科目名称（可选）", example = "语文") @RequestParam(value = "kmmc", required = false) String kmmc,

            @Parameter(description = "地市名称（可选）", example = "长沙市") @RequestParam(value = "szsmc", required = false) String szsmc,

            @Parameter(description = "考区名称/区县（可选）", example = "岳麓区") @RequestParam(value = "szxmc", required = false) String szxmc,

            @Parameter(description = "学校名称（可选）", example = "长沙市第一中学") @RequestParam(value = "xxmc", required = false) String xxmc,

            @Parameter(description = "考籍号（可选）", example = "202501001") @RequestParam(value = "ksh", required = false) String ksh,

            @Parameter(description = "考生姓名（可选，支持模糊查询）", example = "张三") @RequestParam(value = "ksxm", required = false) String ksxm,

            @Parameter(description = "班级名称（可选）", example = "高三1班") @RequestParam(value = "bjmc", required = false) String bjmc,

            @Parameter(description = "页码（从1开始，默认1）", example = "1") @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,

            @Parameter(description = "每页大小（默认20）", example = "20") @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize,

            @Parameter(description = "是否只查询有成绩的学生（true/false，默认false）", example = "false") @RequestParam(value = "onlyWithScore", required = false, defaultValue = "false") Boolean onlyWithScore,

            @Parameter(description = "成绩范围过滤-最低分（可选）", example = "60") @RequestParam(value = "minScore", required = false) Integer minScore,

            @Parameter(description = "成绩范围过滤-最高分（可选）", example = "100") @RequestParam(value = "maxScore", required = false) Integer maxScore,

            @Parameter(description = "合格状态过滤（可选：合格/不合格）", example = "合格") @RequestParam(value = "cjhgm", required = false) String cjhgm,

            @Parameter(description = "等第过滤（可选：A/B/C/D/E）", example = "A") @RequestParam(value = "cjdjm", required = false) String cjdjm,

            @Parameter(description = "排序字段（可选：ksh-考籍号, kscjbs-考生成绩标识, fslkscj-分数，默认ksh）", example = "ksh") @RequestParam(value = "sortField", required = false, defaultValue = "ksh") String sortField,

            @Parameter(description = "排序方向（可选：asc-升序, desc-降序，默认asc）", example = "asc") @RequestParam(value = "sortOrder", required = false, defaultValue = "asc") String sortOrder) {

        try {
            // 构建查询DTO
            StudentDataQueryDTO queryDTO = StudentDataQueryDTO.builder()
                    .ksjhdm(ksjhdm)
                    .kmmc(kmmc)
                    .szsmc(szsmc)
                    .kqmc(szxmc)
                    .xxmc(xxmc)
                    .ksh(ksh)
                    .ksxm(ksxm)
                    .bjmc(bjmc)
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .onlyWithScore(onlyWithScore)
                    .minScore(minScore)
                    .maxScore(maxScore)
                    .cjhgm(cjhgm)
                    .cjdjm(cjdjm)
                    .sortField(sortField)
                    .sortOrder(sortOrder)
                    .build();

            log.info("接收到学生数据查询请求：{}", queryDTO);
            PageResult<StudentDataVO> result = studentDataService.queryStudentData(queryDTO);
            log.info("学生数据查询成功，返回{}条记录，总计{}条", result.getRecords().size(), result.getTotal());
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.warn("学生数据查询参数错误：{}", e.getMessage());
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("学生数据查询失败：{}", e.getMessage(), e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

}