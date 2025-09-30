package edu.qhjy.score_service.service.impl;

import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.domain.dto.GradeQueryDTO;
import edu.qhjy.score_service.domain.handler.GradeQueryResultHandler;
import edu.qhjy.score_service.domain.vo.GradeQueryVO;
import edu.qhjy.score_service.mapper.primary.KscjMapper;
import edu.qhjy.score_service.mapper.primary.KskmxxMapper;
import edu.qhjy.score_service.service.GradeService;
import edu.qhjy.score_service.service.redis.SubjectCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * 成绩查询服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

    private final KscjMapper kscjMapper;
    private final SubjectCacheService subjectCacheService;

    @Override
    public PageResult<GradeQueryVO> queryGradeData(GradeQueryDTO queryDTO) {
        log.info("开始查询成绩数据，查询条件：{}", queryDTO);

        try {
            // 回退到传统查询方案，保留科目数据缓存优化
            log.info("使用传统查询方案");
            return queryWithTraditionalMethod(queryDTO);

        } catch (Exception e) {
            log.error("查询成绩数据失败", e);
            throw new RuntimeException("查询成绩数据失败：" + e.getMessage(), e);
        }
    }


    /**
     * 传统查询方案（原有逻辑）
     */
    private PageResult<GradeQueryVO> queryWithTraditionalMethod(GradeQueryDTO queryDTO) {
        // 查询总数
        Long total = kscjMapper.countGradeData(queryDTO);
        log.info("查询到成绩数据总数：{}", total);

        if (total == 0) {
            return PageResult.empty(queryDTO.getPageNum(), queryDTO.getPageSize());
        }

        // 使用ResultHandler分页查询数据
        GradeQueryResultHandler resultHandler = new GradeQueryResultHandler();
        kscjMapper.selectGradeDataWithResultHandler(queryDTO, resultHandler);
        List<GradeQueryVO> records = resultHandler.getResults();
        log.info("查询到成绩数据记录数：{}", records.size());

        // 补全科目数据，确保所有科目都在scores中返回
        completeSubjectScores(records, queryDTO);

        // 计算总页数
        int totalPages = (int) Math.ceil((double) total / queryDTO.getPageSize());

        return PageResult.<GradeQueryVO>builder()
                .pageNum(queryDTO.getPageNum())
                .pageSize(queryDTO.getPageSize())
                .total(total)
                .pages(totalPages)
                .records(records)
                .build();
    }


    /**
     * 补全科目数据，确保所有科目都在scores中返回
     * 根据kmlx查询对应的科目，没有成绩的科目设置为null
     *
     * @param records  成绩记录列表
     * @param queryDTO 查询条件
     */
    private void completeSubjectScores(List<GradeQueryVO> records, GradeQueryDTO queryDTO) {
        // 如果没有指定考试计划代码，则无法补全科目
        if (!StringUtils.hasText(queryDTO.getKsjhdm())) {
            log.warn("未指定考试计划代码，无法补全科目数据");
            return;
        }

        try {
            // 使用缓存服务查询该考试计划下指定科目类型的所有科目
            List<String> requiredSubjects = subjectCacheService.getSubjectNamesByKsjhdmAndKmlx(
                    queryDTO.getKsjhdm(), queryDTO.getKmlx());

            log.info("考试计划[{}]科目类型[{}]下共有{}个科目需要补全：{}",
                    queryDTO.getKsjhdm(), queryDTO.getKmlx(), requiredSubjects.size(), requiredSubjects);

            // 为每个学生补全缺失的科目，保持科目顺序
            for (GradeQueryVO record : records) {
                if (record.getScores() == null) {
                    continue;
                }

                // 创建新的LinkedHashMap来保持科目顺序
                LinkedHashMap<String, String> orderedScores = new LinkedHashMap<>();

                // 按照requiredSubjects的顺序添加科目
                for (String subjectName : requiredSubjects) {
                    if (record.getScores().containsKey(subjectName)) {
                        // 如果有成绩，使用原有成绩
                        orderedScores.put(subjectName, record.getScores().get(subjectName));
                    } else {
                        // 如果没有成绩，设置为null
                        orderedScores.put(subjectName, null);
                    }
                }

                // 替换原有的scores
                record.setScores(orderedScores);
            }

            log.info("科目补全完成，每个学生现在都包含{}个科目的成绩数据", requiredSubjects.size());

        } catch (Exception e) {
            log.error("补全科目数据失败", e);
            // 不抛出异常，避免影响主要查询功能
        }
    }
}