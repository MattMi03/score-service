package edu.qhjy.score_service.service;

import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.domain.dto.GradeBookQueryDTO;
import edu.qhjy.score_service.domain.vo.GradeBookVO;

/**
 * 成绩等第册服务接口
 */
public interface GradeBookService {

    /**
     * 分页查询成绩等第册
     * 以学校为单位生成成绩等第册，包含学生基本信息和所有科目成绩
     *
     * @param queryDTO 查询条件和分页参数
     * @return 分页结果
     */
    PageResult<GradeBookVO> queryGradeBook(GradeBookQueryDTO queryDTO);
}