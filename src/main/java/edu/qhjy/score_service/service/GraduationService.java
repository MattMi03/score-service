package edu.qhjy.score_service.service;

import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.dto.BatchGraduationDTO;
import edu.qhjy.score_service.domain.dto.GraduationQueryDTO;
import edu.qhjy.score_service.domain.vo.GraduationStudentVO;

/**
 * 毕业生花名册服务接口
 */
public interface GraduationService {

    /**
     * 毕业生条件查询（分页）
     * 根据szsmc（必填）以及可选的szxmc、xxmc、bynd、ksh进行级联查询，
     * 并根据考试科目和考察科目的合格数量以及考生状态判断是否满足毕业条件
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    Result<PageResult<GraduationStudentVO>> queryGraduationStudents(GraduationQueryDTO queryDTO);

    /**
     * 批量毕业审批
     * 修改ksxx表中学生的BYND为当前年份，KJZTMC更新为"毕业"
     *
     * @param batchDTO 批量审批条件
     * @return 审批结果
     */
    Result<String> batchGraduationApproval(BatchGraduationDTO batchDTO);

    /**
     * 检查学生是否满足毕业条件
     *
     * @param ksh   考生号
     * @param szsmc 所在市名称
     * @return 是否满足毕业条件
     */
    boolean checkGraduationQualification(String ksh, String szsmc);

    /**
     * 获取学生的毕业条件详情
     *
     * @param ksh   考生号
     * @param szsmc 所在市名称
     * @return 毕业条件详情
     */
    GraduationStudentVO getStudentGraduationDetails(String ksh, String szsmc);
}