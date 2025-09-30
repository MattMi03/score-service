package edu.qhjy.score_service.service;

import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.domain.dto.StudentDataQueryDTO;
import edu.qhjy.score_service.domain.vo.StudentDataVO;

/**
 * 学生数据查询服务接口
 */
public interface StudentDataService {

    /**
     * 分页查询学生数据
     * 支持多条件筛选，包括考试计划、科目、地市、考区、学校等
     *
     * @param queryDTO 查询条件和分页参数
     * @return 分页结果
     */
    PageResult<StudentDataVO> queryStudentData(StudentDataQueryDTO queryDTO);

}