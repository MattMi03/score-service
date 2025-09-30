package edu.qhjy.score_service.service;

import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.domain.dto.GradeQueryDTO;
import edu.qhjy.score_service.domain.vo.GradeQueryVO;

/**
 * 成绩查询服务接口
 */
public interface GradeService {

    /**
     * 分页查询成绩数据（支持级联查询）
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<GradeQueryVO> queryGradeData(GradeQueryDTO queryDTO);
}