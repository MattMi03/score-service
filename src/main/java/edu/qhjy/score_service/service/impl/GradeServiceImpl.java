package edu.qhjy.score_service.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import edu.qhjy.score_service.aop.UserContext;
import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.domain.dto.GradeQueryDTO;
import edu.qhjy.score_service.domain.handler.GradeQueryResultHandler;
import edu.qhjy.score_service.domain.vo.GradeQueryVO;
import edu.qhjy.score_service.mapper.primary.KscjMapper;
import edu.qhjy.score_service.service.GradeService;
import edu.qhjy.score_service.service.redis.SubjectCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            // --- 最终权限逻辑 ---
            // 1. 从 UserContext 获取当前登录用户的 DM 码
            UserContext.UserInfo user = UserContext.get();
            if (user != null) {
                String userDm = user.getDm();
                log.info("设置最终数据范围权限，用户DM: {}", userDm);
                // 2. 将用户权限DM设置到查询DTO中，由SQL进行最终的数据范围限定
                queryDTO.setPermissionDm(userDm);
            }
            // --- 权限逻辑结束 ---

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
        // 步骤 1: 使用 PageHelper 对学生ID进行精确分页
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        List<String> kshList = kscjMapper.selectPaginatedStudentKsh(queryDTO);

        // PageHelper 执行后，kshList 实际上是一个 Page 对象，包含了总数等信息
        PageInfo<String> kshPageInfo = new PageInfo<>(kshList);
        long total = kshPageInfo.getTotal(); // 获取正确的、按学生统计的总数
        log.info("查询到符合条件的学生总数：{}", total);

        if (total == 0 || kshList.isEmpty()) {
            return PageResult.empty(queryDTO.getPageNum(), queryDTO.getPageSize());
        }

        // 步骤 2: 根据分页得到的 kshList，查询这些考生的所有详细成绩信息
        List<Map<String, Object>> flatStudentScores = kscjMapper.selectGradeDataByKshList(kshList, queryDTO);
        log.info("查询到本页学生的详细成绩条目数：{}", flatStudentScores.size());

        // 步骤 3: 在内存中对详细数据进行分组和组装
        // 按KSH对结果进行分组
        Map<String, List<Map<String, Object>>> groupedByKsh = flatStudentScores.stream()
                .collect(Collectors.groupingBy(row -> (String) row.get("ksh")));

        List<GradeQueryVO> records = new ArrayList<>();
        // 按照分页查询出的kshList的顺序来组装，确保排序正确
        for (String ksh : kshList) {
            List<Map<String, Object>> studentRows = groupedByKsh.get(ksh);
            if (studentRows == null || studentRows.isEmpty()) continue;

            // 以第一条记录为基础，构建VO对象
            Map<String, Object> firstRow = studentRows.get(0);
            GradeQueryVO vo = new GradeQueryVO();
            vo.setKsh((String) firstRow.get("ksh"));
            vo.setXm((String) firstRow.get("xm"));
            vo.setSfzjh((String) firstRow.get("sfzjh"));
            vo.setGrade((String) firstRow.get("grade"));
            vo.setBj((String) firstRow.get("bj"));
            vo.setXb((String) firstRow.get("xb"));
            vo.setMz((String) firstRow.get("mz"));
            vo.setSzsmc((String) firstRow.get("szsmc"));
            vo.setKqmc((String) firstRow.get("kqmc"));
            vo.setXxmc((String) firstRow.get("xxmc"));
            vo.setXxdm((String) firstRow.get("xxdm"));

            // 将该学生的所有科目成绩聚合到scores Map中
            LinkedHashMap<String, String> scores = new LinkedHashMap<>();
            for (Map<String, Object> row : studentRows) {
                scores.put((String) row.get("kmmc"), (String) row.get("score_value"));
            }
            vo.setScores(scores);
            records.add(vo);
        }
        log.info("组装后，本页实际返回学生记录数：{}", records.size());

        // 补全科目数据
        completeSubjectScores(records, queryDTO);

        // 步骤 4: 构建最终的 PageResult 对象
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