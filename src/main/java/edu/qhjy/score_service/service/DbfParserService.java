package edu.qhjy.score_service.service;

import edu.qhjy.score_service.domain.dto.DbfRecordDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * DBF文件解析服务接口
 *
 * @author dadalv
 * @since 2025-08-01
 */
public interface DbfParserService {

    /**
     * 解析DBF文件，提取成绩数据
     *
     * @param file   DBF文件
     * @param ksjhdm 考试计划代码
     * @return 解析后的记录列表
     * @throws Exception 解析异常
     */
    List<DbfRecordDTO> parseDbfFile(MultipartFile file, String ksjhdm) throws Exception;

    /**
     * 从文件名中提取科目名称
     * 文件名格式：5_化学_单科成绩(46877人).dbf
     * 提取结果：化学
     *
     * @param fileName 文件名
     * @return 科目名称
     */
    String extractSubjectFromFileName(String fileName);

    /**
     * 验证DBF记录数据的完整性和格式
     *
     * @param record DBF记录
     * @return 验证结果，true表示验证通过
     */
    boolean validateDbfRecord(DbfRecordDTO record);

    /**
     * 验证成绩格式是否符合decimal(5,1)要求
     *
     * @param scoreStr 成绩字符串
     * @return 验证结果，true表示格式正确
     */
    boolean validateScoreFormat(String scoreStr);

    /**
     * 获取DBF文件的记录总数（不解析内容，仅统计数量）
     *
     * @param file DBF文件
     * @return 记录总数
     * @throws Exception 读取异常
     */
    int getDbfRecordCount(MultipartFile file) throws Exception;

    /**
     * 验证DBF文件格式和结构
     *
     * @param file DBF文件
     * @return 验证结果，true表示文件格式正确
     * @throws Exception 验证异常
     */
    boolean validateDbfFileStructure(MultipartFile file) throws Exception;
}