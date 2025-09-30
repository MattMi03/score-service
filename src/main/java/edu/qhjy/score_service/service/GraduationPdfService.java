package edu.qhjy.score_service.service;

import edu.qhjy.score_service.domain.dto.GraduationPdfQueryDTO;

import java.io.ByteArrayOutputStream;

/**
 * 毕业生花名册PDF生成服务接口
 */
public interface GraduationPdfService {

    /**
     * 生成毕业生花名册PDF文件
     *
     * @param queryDTO 查询条件
     * @return PDF文件的字节数组输出流
     * @throws Exception 生成PDF过程中的异常
     */
    ByteArrayOutputStream generateGraduationPdf(GraduationPdfQueryDTO queryDTO) throws Exception;

    /**
     * 生成PDF文件名
     * 命名规则：bynd + szsmc（如果有）+ szxmc（如果有）+ xxmc（如果有）+ "毕业生花名册" + 生成日期 + "版"
     *
     * @param queryDTO 查询条件，包含地区和学校信息
     * @return PDF文件名
     */
    String generatePdfFileName(GraduationPdfQueryDTO queryDTO);
}