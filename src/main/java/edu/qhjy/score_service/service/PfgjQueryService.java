package edu.qhjy.score_service.service;

import edu.qhjy.score_service.domain.dto.PfgjQueryRequestDTO;
import edu.qhjy.score_service.domain.vo.PfgjQueryResultVO;

import java.util.List;

/**
 * 评分轨迹查询服务接口
 *
 * @author dadalv
 * @since 2025-08-20
 */
public interface PfgjQueryService {

    /**
     * 根据考试计划代码、考生号和科目名称查询评分轨迹数据
     *
     * @param request 查询请求参数
     * @return 评分轨迹数据列表
     */
    List<PfgjQueryResultVO> queryPfgjData(PfgjQueryRequestDTO request);
}