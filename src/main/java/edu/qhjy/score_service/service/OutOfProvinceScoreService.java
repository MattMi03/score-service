package edu.qhjy.score_service.service;

import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.dto.ScoreSaveDTO;
import edu.qhjy.score_service.domain.vo.StudentInfoVO;

/**
 * 省外转入成绩登记服务接口
 *
 * @author dadalv
 * @date 2025-08-15
 */
public interface OutOfProvinceScoreService {

    /**
     * 根据考生号查询考籍信息
     *
     * @param ksh 考生号
     * @return 考籍信息（包含考生基本信息和科目列表）
     */
    Result<StudentInfoVO> getStudentInfo(String ksh);

    /**
     * 保存省外转入成绩
     *
     * @param scoreSaveDTO 成绩保存DTO
     * @return 保存结果
     */
    Result<String> saveScores(ScoreSaveDTO scoreSaveDTO);

    /**
     * 删除省外转入成绩
     *
     * @param ksh  考生号
     * @param kmmc 科目名称
     * @return 删除结果
     */
    Result<String> deleteScore(String ksh, String kmmc);

    /**
     * 修改省外转入成绩
     *
     * @param scoreSaveDTO 成绩保存DTO
     * @return 修改结果
     */
    Result<String> updateScore(ScoreSaveDTO scoreSaveDTO);
}