package edu.qhjy.score_service.service;

import edu.qhjy.score_service.domain.dto.GraduationStatisticsQueryDTO;

import java.io.ByteArrayOutputStream;

/**
 * 毕业生统计表PDF生成服务接口
 */
public interface GraduationStatisticsPdfService {

    /**
     * 生成毕业生统计表PDF文件
     *
     * @param queryDTO 查询条件
     * @return PDF文件的字节数组输出流
     * @throws Exception 生成PDF过程中的异常
     */
    ByteArrayOutputStream generateGraduationStatisticsPdf(GraduationStatisticsQueryDTO queryDTO) throws Exception;

    /**
     * 生成PDF文件名
     * 命名规则：毕业年度 + 级别 + "毕业生统计表" + 生成日期 + "版"
     *
     * @param queryDTO 查询条件，包含毕业年度和级别信息
     * @return PDF文件名
     */
    String generatePdfFileName(GraduationStatisticsQueryDTO queryDTO);
}