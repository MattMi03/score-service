package edu.qhjy.score_service.domain.handler;

import edu.qhjy.score_service.domain.vo.GradeQueryVO;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

import java.util.*;

/**
 * 成绩查询结果处理器
 * 用于将查询结果中的科目成绩数据聚合到GradeQueryVO的scores Map中
 */
public class GradeQueryResultHandler implements ResultHandler<Map<String, Object>> {

    private final Map<String, GradeQueryVO> resultMap = new LinkedHashMap<>();

    @Override
    public void handleResult(ResultContext<? extends Map<String, Object>> resultContext) {
        Map<String, Object> row = resultContext.getResultObject();

        String ksh = (String) row.get("ksh");
        String kmmc = (String) row.get("kmmc");
        String scoreValue = (String) row.get("score_value");

        // 获取或创建GradeQueryVO对象
        GradeQueryVO gradeVO = resultMap.computeIfAbsent(ksh, k -> {
            GradeQueryVO vo = new GradeQueryVO();
            vo.setKsh((String) row.get("ksh"));
            vo.setXm((String) row.get("xm"));
            vo.setSfzjh((String) row.get("sfzjh"));
            vo.setGrade((String) row.get("grade"));
            vo.setBj((String) row.get("bj"));
            vo.setXb((String) row.get("xb"));
            vo.setMz((String) row.get("mz"));
            vo.setSzsmc((String) row.get("szsmc"));
            vo.setKqmc((String) row.get("kqmc"));
            vo.setXxmc((String) row.get("xxmc"));
            vo.setScores(new HashMap<>());
            return vo;
        });

        // 添加科目成绩到scores Map中
        if (kmmc != null && scoreValue != null) {
            gradeVO.getScores().put(kmmc, scoreValue);
        }
    }

    /**
     * 获取处理后的结果列表
     */
    public List<GradeQueryVO> getResults() {
        return new ArrayList<>(resultMap.values());
    }

    /**
     * 清空结果
     */
    public void clear() {
        resultMap.clear();
    }
}