package edu.qhjy.score_service.controller;

import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.dto.OutOfProvinceStudentQueryDTO;
import edu.qhjy.score_service.domain.dto.ScoreSaveDTO;
import edu.qhjy.score_service.domain.vo.OutOfProvinceStudentVO;
import edu.qhjy.score_service.domain.vo.StudentInfoVO;
import edu.qhjy.score_service.service.OutOfProvinceScoreService;
import edu.qhjy.score_service.service.OutOfProvinceStudentService;
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
import org.springframework.web.bind.annotation.*;

/**
 * 省外转入考生管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/out-of-province-student")
@RequiredArgsConstructor
@Tag(name = "省外转入考生管理", description = "省外转入考生查询和成绩登记相关接口")
public class OutOfProvinceStudentController {

    private final OutOfProvinceStudentService outOfProvinceStudentService;
    private final OutOfProvinceScoreService outOfProvinceScoreService;

    /**
     * 省外转入考生查询
     */
    @Operation(summary = "省外转入考生查询", description = "支持级联查询和分页的省外转入考生查询接口，级联顺序：szsmc→szxmc→xxmc→ksh")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = PageResult.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/query")
    public Result<PageResult<OutOfProvinceStudentVO>> queryOutOfProvinceStudents(
            @Parameter(description = "省市名称") @RequestParam(value = "szsmc", required = false) String szsmc,
            @Parameter(description = "市县名称") @RequestParam(value = "szxmc", required = false) String szxmc,
            @Parameter(description = "学校名称") @RequestParam(value = "xxmc", required = false) String xxmc,
            @Parameter(description = "考生号") @RequestParam(value = "ksh", required = false) String ksh,
            @Parameter(description = "页码") @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize,
            @Parameter(description = "排序字段") @RequestParam(value = "sortField", required = false) String sortField,
            @Parameter(description = "排序方向：asc-升序，desc-降序") @RequestParam(value = "sortOrder", defaultValue = "asc") String sortOrder) {

        try {
            // 构建查询DTO
            OutOfProvinceStudentQueryDTO queryDTO = OutOfProvinceStudentQueryDTO.builder()
                    .szsmc(szsmc)
                    .kqmc(szxmc)
                    .xxmc(xxmc)
                    .ksh(ksh)
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .sortField(sortField)
                    .sortOrder(sortOrder)
                    .build();

            log.info("开始查询省外转入考生数据，查询条件：{}", queryDTO);
            PageResult<OutOfProvinceStudentVO> result = outOfProvinceStudentService
                    .queryOutOfProvinceStudents(queryDTO);
            log.info("查询省外转入考生数据成功，返回{}条记录，总计{}条", result.getRecords().size(), result.getTotal());

            return Result.success(result);
        } catch (Exception e) {
            log.error("查询省外转入考生数据失败", e);
            return Result.error("查询省外转入考生数据失败：" + e.getMessage());
        }
    }

    /**
     * 查询考籍信息
     */
    @Operation(summary = "查询考籍信息", description = "根据考生号查询考生基本信息和科目列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = StudentInfoVO.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/student-info")
    public Result<StudentInfoVO> getStudentInfo(
            @Parameter(description = "考生号", required = true) @RequestParam("ksh") String ksh) {

        log.info("查询考籍信息，考生号：{}", ksh);
        return outOfProvinceScoreService.getStudentInfo(ksh);
    }

    /**
     * 保存省外转入成绩
     */
    @Operation(summary = "保存省外转入成绩", description = "保存省外转入考生的成绩信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "保存成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/scores")
    public Result<String> saveScores(@Valid @RequestBody ScoreSaveDTO scoreSaveDTO) {

        log.info("保存省外转入成绩，考生号：{}", scoreSaveDTO.getKsh());
        return outOfProvinceScoreService.saveScores(scoreSaveDTO);
    }

    /**
     * 删除省外转入成绩
     */
    @Operation(summary = "删除省外转入成绩", description = "删除省外转入成绩，kmmc为空时删除该考生所有科目成绩，不为空时删除指定科目成绩")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "404", description = "成绩记录不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @DeleteMapping("/scores/{ksh}")
    public Result<String> deleteScore(
            @Parameter(description = "考生号", required = true) @PathVariable("ksh") String ksh,
            @Parameter(description = "科目名称，为空时删除所有科目") @RequestParam(value = "kmmc", required = false) String kmmc) {
        log.info("删除省外转入成绩，考生号：{}，科目：{}", ksh, kmmc);
        return outOfProvinceScoreService.deleteScore(ksh, kmmc);
    }

    /**
     * 修改省外转入成绩
     */
    @Operation(summary = "修改省外转入成绩", description = "修改省外转入考生的成绩信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "404", description = "成绩记录不存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PutMapping("/scores")
    public Result<String> updateScore(@Valid @RequestBody ScoreSaveDTO scoreSaveDTO) {

        log.info("修改省外转入成绩，考生号：{}", scoreSaveDTO.getKsh());
        return outOfProvinceScoreService.updateScore(scoreSaveDTO);
    }

}