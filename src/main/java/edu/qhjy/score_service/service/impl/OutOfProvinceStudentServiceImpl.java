package edu.qhjy.score_service.service.impl;

import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.domain.dto.OutOfProvinceStudentQueryDTO;
import edu.qhjy.score_service.domain.vo.OutOfProvinceStudentVO;
import edu.qhjy.score_service.mapper.primary.KsxxMapper;
import edu.qhjy.score_service.service.OutOfProvinceStudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 省外转入考生查询服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutOfProvinceStudentServiceImpl implements OutOfProvinceStudentService {

    private final KsxxMapper ksxxMapper;

    @Override
    public PageResult<OutOfProvinceStudentVO> queryOutOfProvinceStudents(OutOfProvinceStudentQueryDTO queryDTO) {
        log.info("开始查询省外转入考生数据，查询条件：{}", queryDTO);

        try {
            // 验证并设置默认值
            queryDTO.validateAndSetDefaults();

            // 查询总数
            Long total = ksxxMapper.countOutOfProvinceStudents(queryDTO);
            log.info("查询到省外转入考生数据总数：{}", total);

            if (total == 0) {
                return PageResult.empty(queryDTO.getPageNum(), queryDTO.getPageSize());
            }

            // 分页查询数据
            List<OutOfProvinceStudentVO> records = ksxxMapper.selectOutOfProvinceStudentsWithPagination(queryDTO);
            log.info("查询到省外转入考生数据记录数：{}", records.size());

            // 计算总页数
            int totalPages = (int) Math.ceil((double) total / queryDTO.getPageSize());

            return PageResult.<OutOfProvinceStudentVO>builder()
                    .pageNum(queryDTO.getPageNum())
                    .pageSize(queryDTO.getPageSize())
                    .total(total)
                    .pages(totalPages)
                    .records(records)
                    .build();

        } catch (Exception e) {
            log.error("查询省外转入考生数据失败", e);
            throw new RuntimeException("查询省外转入考生数据失败：" + e.getMessage(), e);
        }
    }

}