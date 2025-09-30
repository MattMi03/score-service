package edu.qhjy.score_service.service;

import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.dto.GraduationStatisticsQueryDTO;
import edu.qhjy.score_service.domain.vo.GraduationStatisticsVO;

/**
 * 毕业生统计服务接口
 *
 * @author dadalv
 * @date 2025-08-15
 */
public interface GraduationStatisticsService {

    /**
     * 获取毕业生统计数据
     *
     * @param queryDTO 查询参数
     * @return 毕业生统计数据
     */
    Result<GraduationStatisticsVO> getGraduationStatistics(GraduationStatisticsQueryDTO queryDTO);
}