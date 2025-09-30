package edu.qhjy.score_service.service.impl;

import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.dto.BatchGraduationDTO;
import edu.qhjy.score_service.domain.dto.GraduationQueryDTO;
import edu.qhjy.score_service.domain.entity.BytjEntity;
import edu.qhjy.score_service.domain.vo.GraduationStudentVO;
import edu.qhjy.score_service.domain.vo.StudentScoreVO;
import edu.qhjy.score_service.mapper.primary.GraduationMapper;
import edu.qhjy.score_service.service.GraduationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 毕业生花名册服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraduationServiceImpl implements GraduationService {

    private final GraduationMapper graduationMapper;

    @Override
    @Transactional(readOnly = true)
    public Result<PageResult<GraduationStudentVO>> queryGraduationStudents(GraduationQueryDTO queryDTO) {
        try {
            // 参数校验
            if (!StringUtils.hasText(queryDTO.getSzsmc())) {
                return Result.error("所在市名称不能为空");
            }

            // 获取毕业条件
            BytjEntity graduationRequirement = graduationMapper.selectBytjBySzsmc(queryDTO.getSzsmc());
            if (graduationRequirement == null) {
                return Result.error("未找到该市的毕业条件设置");
            }

            // 如果需要根据isQualified筛选，需要先获取所有符合基本条件的学生进行判断
            if (queryDTO.getIsQualified() != null && queryDTO.getIsQualified()) {
                // 使用数据库层面的毕业条件筛选，避免内存中处理大量数据
                List<GraduationStudentVO> students = graduationMapper.selectQualifiedGraduationStudents(
                        queryDTO, graduationRequirement.getKskm(), graduationRequirement.getKckm());

                // 计算满足毕业条件的学生总数
                int totalCount = graduationMapper.countQualifiedGraduationStudents(
                        queryDTO, graduationRequirement.getKskm(), graduationRequirement.getKckm());
                Long total = (long) totalCount;

                if (CollectionUtils.isEmpty(students)) {
                    return Result.success(
                            PageResult.of(students, queryDTO.getPageNum(), queryDTO.getPageSize(), total));
                }

                // 批量查询学生成绩并设置详细信息
                List<String> kshList = students.stream()
                        .map(GraduationStudentVO::getKsh)
                        .collect(Collectors.toList());

                List<StudentScoreVO> allScores = graduationMapper.selectStudentScoresBatch(kshList);

                // 按考生号分组成绩数据
                Map<String, List<StudentScoreVO>> scoresMap = allScores.stream()
                        .collect(Collectors.groupingBy(StudentScoreVO::getKsh));

                // 为每个学生设置成绩和毕业条件判定
                for (GraduationStudentVO student : students) {
                    enrichStudentGraduationInfoBatch(student, graduationRequirement,
                            scoresMap.getOrDefault(student.getKsh(), Collections.emptyList()));
                }

                return Result.success(PageResult.of(students, queryDTO.getPageNum(), queryDTO.getPageSize(), total));

            } else {
                // 不需要根据isQualified筛选，使用原有逻辑
                // 查询学生列表（不使用PageHelper，在Mapper中实现分页）
                List<GraduationStudentVO> students = graduationMapper.selectGraduationStudents(queryDTO);

                // 计算总数（需要单独查询）
                int totalCount = graduationMapper.countGraduationStudents(queryDTO);
                Long total = (long) totalCount;

                if (CollectionUtils.isEmpty(students)) {
                    return Result
                            .success(PageResult.of(students, queryDTO.getPageNum(), queryDTO.getPageSize(), total));
                }

                // 批量查询学生成绩并判断毕业条件
                List<String> kshList = students.stream()
                        .map(GraduationStudentVO::getKsh)
                        .collect(Collectors.toList());

                // 批量查询所有学生的成绩
                List<StudentScoreVO> allScores = graduationMapper.selectStudentScoresBatch(kshList);

                // 按考生号分组成绩数据
                Map<String, List<StudentScoreVO>> scoresMap = allScores.stream()
                        .collect(Collectors.groupingBy(StudentScoreVO::getKsh));

                // 为每个学生设置成绩和毕业条件判定
                for (GraduationStudentVO student : students) {
                    enrichStudentGraduationInfoBatch(student, graduationRequirement,
                            scoresMap.getOrDefault(student.getKsh(), Collections.emptyList()));
                }

                return Result.success(PageResult.of(students, queryDTO.getPageNum(), queryDTO.getPageSize(), total));
            }

        } catch (Exception e) {
            log.error("查询毕业生信息失败", e);
            return Result.error("查询毕业生信息失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> batchGraduationApproval(BatchGraduationDTO batchDTO) {
        try {
            // 参数校验
            if (!StringUtils.hasText(batchDTO.getSzsmc())) {
                return Result.error("所在市名称不能为空");
            }
            if (!StringUtils.hasText(batchDTO.getOperatorName())) {
                return Result.error("操作人姓名不能为空");
            }
            // operatorCode为可选参数，如果传入则验证长度不超过10个字符
            if (StringUtils.hasText(batchDTO.getOperatorCode()) && batchDTO.getOperatorCode().length() > 10) {
                return Result.error("操作人工作人员码长度不能超过10个字符");
            }

            // 获取毕业年度：优先使用传入的bynd参数，否则使用当前年份
            String currentYear;
            if (StringUtils.hasText(batchDTO.getBynd())) {
                currentYear = batchDTO.getBynd();
                log.info("使用传入的毕业年度：{}", currentYear);
            } else {
                currentYear = String.valueOf(LocalDate.now().getYear());
                log.info("使用系统当前年份作为毕业年度：{}", currentYear);
            }

            // 查询符合条件的考生号列表
            List<String> qualifiedKshList = graduationMapper.selectQualifiedStudentKsh(
                    batchDTO.getSzsmc(),
                    batchDTO.getKqmc(),
                    batchDTO.getXxmc(),
                    batchDTO.getKsh(),
                    batchDTO.getBynd());

            if (CollectionUtils.isEmpty(qualifiedKshList)) {
                return Result.error("未找到符合条件的学生");
            }

            log.info("找到符合基本条件的学生数量：{}", qualifiedKshList.size());

            // 获取毕业条件
            BytjEntity graduationRequirement = graduationMapper.selectBytjBySzsmc(batchDTO.getSzsmc());
            if (graduationRequirement == null) {
                return Result.error("未找到该市的毕业条件设置");
            }

            // 使用优化后的批量检查方法筛选真正满足毕业条件的学生
            List<String> actualQualifiedKshList = batchCheckGraduationQualification(
                    qualifiedKshList, graduationRequirement);

            if (CollectionUtils.isEmpty(actualQualifiedKshList)) {
                return Result.error("没有学生满足毕业条件");
            }

            log.info("满足毕业条件的学生数量：{}", actualQualifiedKshList.size());

            // 批量更新毕业状态
            // 如果operatorCode为空，传入null避免数据库字段长度限制
            String operatorCode = StringUtils.hasText(batchDTO.getOperatorCode()) ? batchDTO.getOperatorCode() : null;
            int updateCount = graduationMapper.batchUpdateGraduationStatus(
                    actualQualifiedKshList,
                    currentYear,
                    batchDTO.getOperatorName(),
                    operatorCode);

            log.info("批量毕业审批完成，更新学生数量：{}", updateCount);
            return Result.success(String.format("批量毕业审批成功，共处理 %d 名学生", updateCount));

        } catch (Exception e) {
            log.error("批量毕业审批失败", e);
            return Result.error("批量毕业审批失败：" + e.getMessage());
        }
    }

    @Override
    public boolean checkGraduationQualification(String ksh, String szsmc) {
        try {
            // 获取毕业条件
            BytjEntity graduationRequirement = graduationMapper.selectBytjBySzsmc(szsmc);
            if (graduationRequirement == null) {
                log.warn("未找到市 {} 的毕业条件设置", szsmc);
                return false;
            }

            // 检查学生考籍状态
            String kjztmc = graduationMapper.selectStudentKjztmc(ksh);
            if (!"正常在校".equals(kjztmc) && !"毕业".equals(kjztmc)) {
                log.debug("学生 {} 考籍状态为 {}，不满足毕业条件", ksh, kjztmc);
                return false;
            }

            // 统计考试科目合格数量
            int examSubjectPassCount = graduationMapper.countExamSubjectPass(ksh);
            // 统计考察科目合格数量
            int assessmentSubjectPassCount = graduationMapper.countAssessmentSubjectPass(ksh);

            // 判断是否满足毕业条件
            boolean examQualified = examSubjectPassCount >= graduationRequirement.getKskm();
            boolean assessmentQualified = assessmentSubjectPassCount >= graduationRequirement.getKckm();

            return examQualified && assessmentQualified;

        } catch (Exception e) {
            log.error("检查学生 {} 毕业条件失败", ksh, e);
            return false;
        }
    }

    /**
     * 批量检查毕业条件（优化版本，解决N+1查询问题）
     *
     * @param kshList               考生号列表
     * @param graduationRequirement 毕业条件
     * @return 满足毕业条件的考生号列表
     */
    private List<String> batchCheckGraduationQualification(List<String> kshList, BytjEntity graduationRequirement) {
        if (CollectionUtils.isEmpty(kshList)) {
            return new ArrayList<>();
        }

        try {
            // 批量查询所有学生的成绩
            List<StudentScoreVO> allScores = graduationMapper.selectStudentScoresBatch(kshList);

            // 按考生号分组成绩数据
            Map<String, List<StudentScoreVO>> scoresMap = allScores.stream()
                    .collect(Collectors.groupingBy(StudentScoreVO::getKsh));

            List<String> qualifiedKshList = new ArrayList<>();

            // 遍历每个学生，检查毕业条件
            for (String ksh : kshList) {
                List<StudentScoreVO> studentScores = scoresMap.getOrDefault(ksh, new ArrayList<>());

                // 统计考试科目和考察科目的合格数量
                int examSubjectPassCount = 0;
                int assessmentSubjectPassCount = 0;

                Set<String> examSubjects = new HashSet<>();
                Set<String> assessmentSubjects = new HashSet<>();

                for (StudentScoreVO score : studentScores) {
                    if ("合格".equals(score.getCjhgm())) {
                        Integer kmlx = score.getKmlx();
                        if (kmlx != null) {
                            if (kmlx == 0) {
                                // 考试科目（KMLX=0）
                                examSubjects.add(score.getKmmc());
                            } else if (kmlx == 1) {
                                // 考察科目（KMLX=1）
                                assessmentSubjects.add(score.getKmmc());
                            }
                        }
                    }
                }

                examSubjectPassCount = examSubjects.size();
                assessmentSubjectPassCount = assessmentSubjects.size();

                // 判断是否满足毕业条件
                boolean examQualified = examSubjectPassCount >= graduationRequirement.getKskm();
                boolean assessmentQualified = assessmentSubjectPassCount >= graduationRequirement.getKckm();

                if (examQualified && assessmentQualified) {
                    qualifiedKshList.add(ksh);
                }
            }

            log.info("批量检查毕业条件完成，输入学生数：{}，满足条件学生数：{}", kshList.size(), qualifiedKshList.size());
            return qualifiedKshList;

        } catch (Exception e) {
            log.error("批量检查毕业条件失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public GraduationStudentVO getStudentGraduationDetails(String ksh, String szsmc) {
        try {
            // 构造查询条件
            GraduationQueryDTO queryDTO = new GraduationQueryDTO();
            queryDTO.setSzsmc(szsmc);
            queryDTO.setKsh(ksh);
            queryDTO.setPageNum(1);
            queryDTO.setPageSize(1);

            // 查询学生信息
            List<GraduationStudentVO> students = graduationMapper.selectGraduationStudents(queryDTO);
            if (CollectionUtils.isEmpty(students)) {
                return null;
            }

            GraduationStudentVO student = students.get(0);

            // 获取毕业条件
            BytjEntity graduationRequirement = graduationMapper.selectBytjBySzsmc(szsmc);
            if (graduationRequirement != null) {
                enrichStudentGraduationInfo(student, graduationRequirement);
            }

            return student;

        } catch (Exception e) {
            log.error("获取学生 {} 毕业条件详情失败", ksh, e);
            return null;
        }
    }

    /**
     * 丰富学生毕业信息（批量优化版本）
     *
     * @param student               学生信息
     * @param graduationRequirement 毕业条件
     * @param scores                学生成绩列表（预先查询好的）
     */
    private void enrichStudentGraduationInfoBatch(GraduationStudentVO student, BytjEntity graduationRequirement,
                                                  List<StudentScoreVO> scores) {
        try {
            // 转换成绩为Map格式
            Map<String, String> scoresMap = new HashMap<>();
            int examSubjectPassCount = 0;
            int assessmentSubjectPassCount = 0;

            for (StudentScoreVO score : scores) {
                // 设置成绩显示值（根据科目类型区分：kmlx=0返回CJHGM(CJDJM)拼接形式，kmlx=1返回CJHGM）
                String displayScore;
                Integer kmlx = score.getKmlx();
                if (kmlx != null && kmlx == 0) {
                    // 考试科目返回合格评定(等级码)的拼接形式
                    String cjhgm = score.getCjhgm();
                    String cjdjm = score.getCjdjm();
                    if (StringUtils.hasText(cjhgm) && StringUtils.hasText(cjdjm)) {
                        displayScore = cjhgm + "(" + cjdjm + ")";
                    } else if (StringUtils.hasText(cjhgm)) {
                        // CJDJM为空时，返回CJHGM()格式
                        displayScore = cjhgm + "()";
                    } else {
                        // CJHGM为空时，直接返回CJDJM
                        displayScore = cjdjm;
                    }
                } else if (kmlx != null && kmlx == 1) {
                    // 考察科目仅返回合格评定
                    displayScore = score.getCjhgm();
                } else {
                    // 兜底逻辑：如果kmlx为空，仍使用原优先级策略
                    displayScore = StringUtils.hasText(score.getCjdjm()) ? score.getCjdjm() : score.getCjhgm();
                }
                scoresMap.put(score.getKmmc(), displayScore);

                // 统计合格科目数量
                if ("合格".equals(score.getCjhgm())) {
                    if (kmlx != null) {
                        if (kmlx == 0) {
                            examSubjectPassCount++;
                        } else if (kmlx == 1) {
                            assessmentSubjectPassCount++;
                        }
                    }
                }

                // 设置是否通过标志
                score.setIsPass("合格".equals(score.getCjhgm()));
            }

            // 设置学生信息
            student.setScores(scoresMap);
            student.setExamSubjectPassCount(examSubjectPassCount);
            student.setAssessmentSubjectPassCount(assessmentSubjectPassCount);
            student.setRequiredExamSubjectCount(graduationRequirement.getKskm());
            student.setRequiredAssessmentSubjectCount(graduationRequirement.getKckm());

            // 判断是否满足毕业条件（用于内部筛选逻辑）
            boolean examQualified = examSubjectPassCount >= graduationRequirement.getKskm();
            boolean assessmentQualified = assessmentSubjectPassCount >= graduationRequirement.getKckm();
            boolean statusQualified = "正常在校".equals(student.getKjztmc()) || "毕业".equals(student.getKjztmc());
            // 将毕业条件判断结果存储在临时字段中，用于后续筛选
            student.setIsQualifiedInternal(examQualified && assessmentQualified && statusQualified);

        } catch (Exception e) {
            log.error("丰富学生 {} 毕业信息失败", student.getKsh(), e);
            student.setIsQualifiedInternal(false);
        }
    }

    /**
     * 丰富学生毕业信息（单个查询版本，保留用于单个学生查询）
     *
     * @param student               学生信息
     * @param graduationRequirement 毕业条件
     */
    private void enrichStudentGraduationInfo(GraduationStudentVO student, BytjEntity graduationRequirement) {
        try {
            // 查询学生成绩
            List<StudentScoreVO> scores = graduationMapper.selectStudentScores(student.getKsh());

            // 调用批量处理方法
            enrichStudentGraduationInfoBatch(student, graduationRequirement, scores);

        } catch (Exception e) {
            log.error("丰富学生 {} 毕业信息失败", student.getKsh(), e);
            student.setIsQualifiedInternal(false);
        }
    }
}