package edu.qhjy.score_service.service;

import edu.qhjy.score_service.domain.dto.GradeBookQueryDTO;

import java.io.ByteArrayOutputStream;

/**
 * 成绩等第册PDF生成服务接口
 */
public interface GradeBookPdfService {

    /**
     * 生成成绩等第册PDF文件
     *
     * @param queryDTO 查询条件
     * @return PDF文件的字节数组输出流
     * @throws Exception 生成PDF过程中的异常
     */
    ByteArrayOutputStream generateGradeBookPdf(GradeBookQueryDTO queryDTO) throws Exception;

    /**
     * 生成PDF文件名
     * 命名规则：ksjhdm + xxmc（学校名称） + "等级册" + 生成日期 + "版"
     *
     * @param ksjhdm 考试计划代码
     * @param xxmc   学校名称
     * @return PDF文件名
     */
    String generatePdfFileName(String ksjhdm, String xxmc);
}