package edu.qhjy.score_service.service.impl;

import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.domain.dto.GradeBookQueryDTO;
import edu.qhjy.score_service.domain.vo.GradeBookVO;
import edu.qhjy.score_service.mapper.primary.KscjMapper;
import edu.qhjy.score_service.service.GradeBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 成绩等第册服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeBookServiceImpl implements GradeBookService {

    private final KscjMapper kscjMapper;

    @Override
    public PageResult<GradeBookVO> queryGradeBook(GradeBookQueryDTO queryDTO) {
        log.info("开始查询成绩等第册，查询条件：{}", queryDTO);

        // 参数校验
        validateQueryParams(queryDTO);

        // 设置分页参数
        queryDTO.validateAndSetDefaults();

        try {
            // 查询学校基本信息
            Map<String, String> schoolInfo = kscjMapper.selectSchoolInfo(queryDTO);
            if (schoolInfo == null || schoolInfo.isEmpty()) {
                log.warn("未找到学校信息，查询条件：{}", queryDTO);
                return PageResult.empty(queryDTO.getPageNum(), queryDTO.getPageSize());
            }

            // 查询总数
            Long totalCount = kscjMapper.countGradeBookData(queryDTO);
            log.info("查询到成绩等第册学生总数：{}", totalCount);

            if (totalCount == 0) {
                return PageResult.empty(queryDTO.getPageNum(), queryDTO.getPageSize());
            }

            // 查询分页数据
            List<GradeBookVO.StudentGradeData> studentDataList = kscjMapper.selectGradeBookWithPagination(queryDTO);
            log.info("查询到成绩等第册学生数据条数：{}", studentDataList.size());

            // 构建响应数据
            GradeBookVO gradeBookVO = GradeBookVO.builder()
                    .kqmc(schoolInfo.get("kqmc"))
                    .xxmc(schoolInfo.get("xxmc"))
                    .studentData(studentDataList)
                    .build();

            return PageResult.success(List.of(gradeBookVO), totalCount, queryDTO.getPageNum(), queryDTO.getPageSize());

        } catch (Exception e) {
            log.error("查询成绩等第册失败，查询条件：{}，错误信息：{}", queryDTO, e.getMessage(), e);
            throw new RuntimeException("查询成绩等第册失败：" + e.getMessage(), e);
        }
    }

    /**
     * 验证查询参数
     */
    private void validateQueryParams(GradeBookQueryDTO queryDTO) {
        if (queryDTO == null) {
            throw new IllegalArgumentException("查询参数不能为空");
        }

        if (queryDTO.getKsjhdm() == null || queryDTO.getKsjhdm().trim().isEmpty()) {
            throw new IllegalArgumentException("考试计划代码不能为空");
        }

        if (queryDTO.getSchool() == null || queryDTO.getSchool().trim().isEmpty()) {
            throw new IllegalArgumentException("学校不能为空");
        }

        if (!queryDTO.isValid()) {
            throw new IllegalArgumentException("分页参数无效");
        }
    }
}