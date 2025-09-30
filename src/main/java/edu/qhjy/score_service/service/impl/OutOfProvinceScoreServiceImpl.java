package edu.qhjy.score_service.service.impl;

import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.dto.ScoreSaveDTO;
import edu.qhjy.score_service.domain.entity.KmxxEntity;
import edu.qhjy.score_service.domain.entity.KscjEntity;
import edu.qhjy.score_service.domain.entity.KsxxEntity;
import edu.qhjy.score_service.domain.vo.StudentInfoVO;
import edu.qhjy.score_service.mapper.primary.KmxxMapper;
import edu.qhjy.score_service.mapper.primary.KscjMapper;
import edu.qhjy.score_service.mapper.primary.KsxxMapper;
import edu.qhjy.score_service.service.OutOfProvinceScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 省外转入成绩登记服务实现类
 *
 * @author dadalv
 * @date 2025-08-15
 */
@Service
public class OutOfProvinceScoreServiceImpl implements OutOfProvinceScoreService {

    @Autowired
    private KsxxMapper ksxxMapper;

    @Autowired
    private KmxxMapper kmxxMapper;

    @Autowired
    private KscjMapper kscjMapper;

    @Override
    public Result<StudentInfoVO> getStudentInfo(String ksh) {
        // 参数校验
        if (!StringUtils.hasText(ksh)) {
            return Result.error("考生号不能为空");
        }

        try {
            // 查询考生基本信息
            KsxxEntity studentInfo = ksxxMapper.selectStudentInfoByKsh(ksh);
            if (studentInfo == null) {
                return Result.error("未找到该考生信息");
            }

            // 查询所有科目名称
            List<String> allSubjects = kmxxMapper.selectAllKmmc();
            if (allSubjects == null || allSubjects.isEmpty()) {
                return Result.error("未找到科目信息");
            }

            // 查询考生的成绩数据
            List<Map<String, String>> scoresMapList = kscjMapper.selectScoresByKsh(ksh);
            Map<String, String> studentScores = new HashMap<>();
            if (scoresMapList != null && !scoresMapList.isEmpty()) {
                for (Map<String, String> scoreMap : scoresMapList) {
                    String key = scoreMap.get("key");
                    String value = scoreMap.get("value");
                    if (key != null && value != null) {
                        studentScores.put(key, value);
                    }
                }
            }

            // 构建科目成绩映射
            Map<String, String> scoresList = new HashMap<>();
            for (String subject : allSubjects) {
                // 如果有成绩记录则返回CJHGM，否则返回null
                scoresList.put(subject, studentScores.get(subject));
            }

            // 构建返回结果
            StudentInfoVO studentInfoVO = new StudentInfoVO();
            studentInfoVO.setKsh(studentInfo.getKsh());
            studentInfoVO.setSfzjh(studentInfo.getSfzjh());
            studentInfoVO.setXm(studentInfo.getXm());
            studentInfoVO.setScoresList(scoresList);

            return Result.success("考籍信息查询成功", studentInfoVO);
        } catch (Exception e) {
            return Result.error("查询考籍信息失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> saveScores(ScoreSaveDTO scoreSaveDTO) {
        // 参数校验
        if (scoreSaveDTO == null) {
            return Result.error("请求参数不能为空");
        }
        if (!StringUtils.hasText(scoreSaveDTO.getKsh())) {
            return Result.error("考生号不能为空");
        }
        if (!StringUtils.hasText(scoreSaveDTO.getSfzjh())) {
            return Result.error("身份证件号不能为空");
        }
        if (!StringUtils.hasText(scoreSaveDTO.getXm())) {
            return Result.error("姓名不能为空");
        }
        if (scoreSaveDTO.getScores() == null || scoreSaveDTO.getScores().isEmpty()) {
            return Result.error("成绩信息不能为空");
        }

        try {
            // 验证考生信息
            KsxxEntity validationResult = ksxxMapper.validateStudentInfo(
                    scoreSaveDTO.getKsh(),
                    scoreSaveDTO.getSfzjh(),
                    scoreSaveDTO.getXm());
            if (validationResult == null) {
                return Result.error("考生信息验证失败，请检查考生号、身份证件号和姓名是否匹配");
            }

            // 准备成绩记录列表
            List<KscjEntity> scoreEntities = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            // 遍历成绩数据
            for (Map.Entry<String, String> entry : scoreSaveDTO.getScores().entrySet()) {
                String subjectName = entry.getKey();
                String score = entry.getValue();

                // 跳过空成绩
                if (score == null) {
                    continue;
                }

                // 查询科目信息获取KMLX
                KmxxEntity subjectInfo = kmxxMapper.selectByKmmc(subjectName);
                if (subjectInfo == null) {
                    return Result.error("未找到科目：" + subjectName);
                }

                // 检查是否已存在该考生该科目的成绩记录
                KscjEntity existingScore = kscjMapper.selectByKshAndKmmc(scoreSaveDTO.getKsh(), subjectName);

                // 创建或更新成绩记录
                KscjEntity scoreEntity;
                if (existingScore != null) {
                    // 覆盖模式：使用现有记录
                    scoreEntity = existingScore;
                } else {
                    // 新建记录
                    scoreEntity = new KscjEntity();
                    scoreEntity.setKsh(scoreSaveDTO.getKsh());
                }

                scoreEntity.setKsjhdm("0000"); // 省外转入标识
                scoreEntity.setKsjhmc("省外转入"); // 考试计划名称
                scoreEntity.setKmmc(subjectName);
                scoreEntity.setKklxmc("正考");

                // 设置KMLX字段
                Integer kmlx = subjectInfo.getKmlx();
                scoreEntity.setKmlx(kmlx);

                // 根据科目类型设置成绩
                if (kmlx != null && kmlx == 0) {
                    // 合格性考试科目 (KMLX=0)
                    if ("合格".equals(score)) {
                        scoreEntity.setCjhgm("合格");
                        scoreEntity.setCjdjm("P");
                    } else if ("不合格".equals(score)) {
                        scoreEntity.setCjhgm("不合格");
                        scoreEntity.setCjdjm("F");
                    } else {
                        return Result.error("合格性考试科目[" + subjectName + "]成绩只能为'合格'或'不合格'");
                    }
                } else {
                    // 考察性考试科目 (KMLX=1)，只填入CJHGM字段
                    scoreEntity.setCjhgm(score);
                    scoreEntity.setCjdjm(null); // 考察性考试科目不设置CJDJM
                }

                // 设置审计字段
                // TODO: 需要从登陆账号中获取
                scoreEntity.setCjrxm("系统");
                scoreEntity.setCjrgzrym("系统");
                scoreEntity.setCjsj(now);
                scoreEntity.setGxrxm("系统");
                scoreEntity.setGxrgzrym("系统");
                scoreEntity.setGxsj(now);

                scoreEntities.add(scoreEntity);
            }

            // 分离新增和更新的记录
            List<KscjEntity> newEntities = new ArrayList<>();
            List<KscjEntity> updateEntities = new ArrayList<>();

            for (KscjEntity entity : scoreEntities) {
                if (entity.getKscjbs() == null) {
                    // 新记录
                    newEntities.add(entity);
                } else {
                    // 更新记录
                    updateEntities.add(entity);
                }
            }

            // 批量新增成绩
            if (!newEntities.isEmpty()) {
                int insertResult = kscjMapper.batchInsertOutOfProvinceScores(newEntities);
                if (insertResult <= 0) {
                    return Result.error("新增成绩保存失败");
                }
            }

            // 批量更新成绩
            if (!updateEntities.isEmpty()) {
                for (KscjEntity entity : updateEntities) {
                    int updateResult = kscjMapper.updateById(entity);
                    if (updateResult <= 0) {
                        return Result.error("更新成绩保存失败：考生号" + entity.getKsh() + "，科目" + entity.getKmmc());
                    }
                }
            }

            return Result.success("成绩保存成功");
        } catch (Exception e) {
            return Result.error("保存成绩失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteScore(String ksh, String kmmc) {
        try {
            // 参数校验
            if (!StringUtils.hasText(ksh)) {
                return Result.error("考生号不能为空");
            }

            // 验证考生信息和类型
            KsxxEntity student = ksxxMapper.selectByKsh(ksh);
            if (student == null) {
                return Result.error("未找到考生信息");
            }

            int deleteResult;

            if (!StringUtils.hasText(kmmc)) {
                // 科目名称为空时，删除该考生的所有省外转入成绩
                // 先检查是否存在省外转入成绩记录
                List<KscjEntity> existingScores = kscjMapper.selectByKsh(ksh);
                boolean hasOutOfProvinceScores = existingScores.stream()
                        .anyMatch(score -> "0000".equals(score.getKsjhdm()));

                if (!hasOutOfProvinceScores) {
                    return Result.error("未找到该考生的省外转入成绩记录");
                }

                // 删除该考生的所有省外转入成绩
                deleteResult = kscjMapper.deleteAllOutOfProvinceScoresByKsh(ksh);
                if (deleteResult <= 0) {
                    return Result.error("删除成绩失败");
                }

                return Result.success("成功删除该考生的所有省外转入成绩，共删除" + deleteResult + "条记录");
            } else {
                // 科目名称不为空时，删除指定科目的成绩
                // 检查成绩记录是否存在
                KscjEntity existingScore = kscjMapper.selectByKshAndKmmc(ksh, kmmc);
                if (existingScore == null || !"0000".equals(existingScore.getKsjhdm())) {
                    return Result.error("未找到该考生的省外转入成绩记录");
                }

                // 删除指定科目的成绩记录
                deleteResult = kscjMapper.deleteOutOfProvinceScore(ksh, kmmc);
                if (deleteResult <= 0) {
                    return Result.error("删除成绩失败");
                }

                return Result.success("成绩删除成功");
            }
        } catch (Exception e) {
            return Result.error("删除成绩失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> updateScore(ScoreSaveDTO scoreSaveDTO) {
        try {
            // 参数校验
            if (!StringUtils.hasText(scoreSaveDTO.getKsh())) {
                return Result.error("考生号不能为空");
            }
            if (scoreSaveDTO.getScores() == null || scoreSaveDTO.getScores().isEmpty()) {
                return Result.error("成绩数据不能为空");
            }

            // 验证考生信息
            KsxxEntity student = ksxxMapper.selectByKsh(scoreSaveDTO.getKsh());
            if (student == null) {
                return Result.error("未找到考生信息");
            }

            // 处理每个科目的成绩更新
            for (Map.Entry<String, String> entry : scoreSaveDTO.getScores().entrySet()) {
                String subjectName = entry.getKey();
                String score = entry.getValue();

                // 跳过空成绩
                if (score == null) {
                    continue;
                }

                // 查询科目信息获取KMLX
                KmxxEntity subjectInfo = kmxxMapper.selectByKmmc(subjectName);
                if (subjectInfo == null) {
                    return Result.error("未找到科目：" + subjectName);
                }

                // 检查是否存在该考生该科目的省外转入成绩记录
                KscjEntity existingScore = kscjMapper.selectByKshAndKmmc(scoreSaveDTO.getKsh(), subjectName);
                boolean isExistingRecord = existingScore != null && "0000".equals(existingScore.getKsjhdm());

                // 创建成绩实体
                KscjEntity scoreEntity = new KscjEntity();
                scoreEntity.setKsh(scoreSaveDTO.getKsh());
                scoreEntity.setKmmc(subjectName);
                scoreEntity.setKsjhdm("0000"); // 省外转入标识
                scoreEntity.setKsjhmc("省外转入"); // 考试计划名称
                scoreEntity.setKklxmc("正考");

                // 设置KMLX字段
                Integer kmlx = subjectInfo.getKmlx();
                scoreEntity.setKmlx(kmlx);

                // 根据科目类型设置成绩
                if (kmlx != null && kmlx == 0) {
                    // 合格性考试科目 (KMLX=0)
                    if ("合格".equals(score)) {
                        scoreEntity.setCjhgm("合格");
                        scoreEntity.setCjdjm("P");
                    } else if ("不合格".equals(score)) {
                        scoreEntity.setCjhgm("不合格");
                        scoreEntity.setCjdjm("F");
                    } else {
                        return Result.error("合格性考试科目[" + subjectName + "]成绩只能为'合格'或'不合格'");
                    }
                } else {
                    // 考察性考试科目 (KMLX=1)，只填入CJHGM字段
                    scoreEntity.setCjhgm(score);
                    scoreEntity.setCjdjm(null); // 考察性考试科目不设置CJDJM
                }

                // 设置审计字段
                LocalDateTime now = LocalDateTime.now();
                scoreEntity.setCjrxm("系统");
                scoreEntity.setCjrgzrym("系统");
                scoreEntity.setCjsj(now);
                scoreEntity.setGxrxm("系统");
                scoreEntity.setGxrgzrym("系统");
                scoreEntity.setGxsj(now);

                int result;
                if (isExistingRecord) {
                    // 更新现有记录
                    result = kscjMapper.updateOutOfProvinceScore(scoreEntity);
                    if (result <= 0) {
                        return Result.error("更新成绩失败：考生号" + scoreSaveDTO.getKsh() + "，科目" + subjectName);
                    }
                } else {
                    // 新增成绩记录
                    result = kscjMapper.insertOutOfProvinceScore(scoreEntity);
                    if (result <= 0) {
                        return Result.error("新增成绩失败：考生号" + scoreSaveDTO.getKsh() + "，科目" + subjectName);
                    }
                }
            }

            return Result.success("成绩保存成功");
        } catch (Exception e) {
            return Result.error("保存成绩失败：" + e.getMessage());
        }
    }
}