package edu.qhjy.score_service.service.impl;

import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.dto.GraduationStatisticsQueryDTO;
import edu.qhjy.score_service.domain.vo.GraduationStatisticsVO;
import edu.qhjy.score_service.mapper.primary.KsxxMapper;
import edu.qhjy.score_service.service.GraduationStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 毕业生统计服务实现类
 *
 * @author dadalv
 * @date 2025-08-15
 */
@Slf4j
@Service
public class GraduationStatisticsServiceImpl implements GraduationStatisticsService {

    @Autowired
    private KsxxMapper ksxxMapper;

    @Override
    public Result<GraduationStatisticsVO> getGraduationStatistics(GraduationStatisticsQueryDTO queryDTO) {
        try {
            log.info("开始查询毕业生统计数据，毕业年度：{}，级别：{}", queryDTO.getBynd(), queryDTO.getRxnd());

            // 使用优化后的合并查询，一次性获取所有统计数据
            List<Map<String, Object>> optimizedStatistics = ksxxMapper.selectGraduationStatisticsOptimized(
                    queryDTO.getBynd(), queryDTO.getRxnd());

            // 构建响应数据
            GraduationStatisticsVO responseVO = new GraduationStatisticsVO();
            responseVO.setBynd(queryDTO.getBynd());
            responseVO.setRxnd(queryDTO.getRxnd());

            // 从合并查询结果中提取数据
            Integer byndTotalCount = 0;
            Integer rxndTotalCount = 0;
            List<GraduationStatisticsVO.CityStatistics> graduationStudentStatics = new ArrayList<>();

            if (!optimizedStatistics.isEmpty()) {
                // 从第一条记录中获取总数（所有记录的总数都相同）
                Map<String, Object> firstRecord = optimizedStatistics.get(0);
                byndTotalCount = ((Number) firstRecord.get("byndTotalCount")).intValue();
                rxndTotalCount = ((Number) firstRecord.get("rxndTotalCount")).intValue();

                // 转换市州统计数据
                graduationStudentStatics = optimizedStatistics.stream()
                        .map(map -> {
                            GraduationStatisticsVO.CityStatistics cityStats = new GraduationStatisticsVO.CityStatistics();
                            cityStats.setSzsmc((String) map.get("szsmc"));
                            cityStats.setTotalCount(((Number) map.get("totalCount")).intValue());
                            return cityStats;
                        })
                        .toList();
            }

            responseVO.setByndTotalCount(byndTotalCount);
            responseVO.setRxndTotalCount(rxndTotalCount);
            responseVO.setGraduationStudentStatics(graduationStudentStatics);

            log.info("毕业生统计数据查询完成（优化版本），共{}个市州，毕业年度总人数：{}，级别总人数：{}",
                    graduationStudentStatics.size(), byndTotalCount, rxndTotalCount);

            return Result.success("查询成功", responseVO);

        } catch (Exception e) {
            log.error("查询毕业生统计数据失败", e);
            return Result.error("查询毕业生统计数据失败：" + e.getMessage());
        }
    }
}