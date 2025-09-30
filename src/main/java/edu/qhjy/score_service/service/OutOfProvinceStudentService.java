package edu.qhjy.score_service.service;

import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.domain.dto.OutOfProvinceStudentQueryDTO;
import edu.qhjy.score_service.domain.vo.OutOfProvinceStudentVO;

/**
 * 省外转入考生查询服务接口
 */
public interface OutOfProvinceStudentService {

    /**
     * 分页查询省外转入考生数据（支持级联查询）
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<OutOfProvinceStudentVO> queryOutOfProvinceStudents(OutOfProvinceStudentQueryDTO queryDTO);

    // /**
    // * 获取省外转入考生统计信息
    // *
    // * @return 统计信息
    // */
    // Map<String, Object> getStatistics();
}