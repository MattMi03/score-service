package edu.qhjy.score_service.service.impl;

import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.domain.dto.StudentDataQueryDTO;
import edu.qhjy.score_service.domain.vo.StudentDataVO;
import edu.qhjy.score_service.mapper.primary.KscjMapper;
import edu.qhjy.score_service.service.StudentDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 学生数据查询服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentDataServiceImpl implements StudentDataService {

    private final KscjMapper kscjMapper;

    @Override
    public PageResult<StudentDataVO> queryStudentData(StudentDataQueryDTO queryDTO) {
        log.info("开始查询学生数据，查询条件：{}", queryDTO);

        // 参数校验
        validateQueryParams(queryDTO);

        // 设置分页参数
        queryDTO.validateAndSetDefaults();

        try {
            // 查询总数
            Long totalCount = kscjMapper.countStudentData(queryDTO);
            log.info("查询到学生数据总数：{}", totalCount);

            if (totalCount == 0) {
                return PageResult.empty(queryDTO.getPageNum(), queryDTO.getPageSize());
            }

            // 查询分页数据
            List<StudentDataVO> dataList = kscjMapper.selectStudentDataWithPagination(queryDTO);
            log.info("查询到学生数据条数：{}", dataList.size());

            // 处理科目类型相关的显示逻辑
            processSubjectTypeDisplay(dataList);

            return PageResult.success(dataList, totalCount, queryDTO.getPageNum(), queryDTO.getPageSize());

        } catch (Exception e) {
            log.error("查询学生数据失败，查询条件：{}，错误信息：{}", queryDTO, e.getMessage(), e);
            throw new RuntimeException("查询学生数据失败：" + e.getMessage(), e);
        }
    }

    /**
     * 参数校验
     */
    private void validateQueryParams(StudentDataQueryDTO queryDTO) {
        if (queryDTO == null) {
            throw new IllegalArgumentException("查询参数不能为空");
        }

        if (!StringUtils.hasText(queryDTO.getKsjhdm())) {
            throw new IllegalArgumentException("考试计划代码不能为空");
        }

        // 校验成绩范围
        if (queryDTO.getMinScore() != null && queryDTO.getMaxScore() != null) {
            if (queryDTO.getMinScore().compareTo(queryDTO.getMaxScore()) > 0) {
                throw new IllegalArgumentException("最小成绩不能大于最大成绩");
            }
        }
    }

    /**
     * 处理科目类型相关的显示逻辑
     * 根据KMLX字段决定显示合格评定还是等第
     */
    private void processSubjectTypeDisplay(List<StudentDataVO> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        for (StudentDataVO data : dataList) {
            // 检查data是否为null
            if (data == null) {
                log.warn("发现null的StudentDataVO对象，跳过处理");
                continue;
            }

            // 根据科目类型处理显示逻辑
            if (data.getKmlx() != null) {
                if (data.getKmlx() == 1) {
                    // 考察性考试：只显示合格评定，清空等第
                    data.setCjdjm(null);
                    // 合格性考试：两个都返回
                }
            }
        }
    }
}