package edu.qhjy.score_service.service;

import edu.qhjy.score_service.domain.dto.PfgjImportDTO;
import edu.qhjy.score_service.domain.vo.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 评分轨迹导入服务接口
 *
 * @author dadalv
 * @since 2025-08-15
 */
public interface PfgjImportService {

    /**
     * 导入DBF文件中的评分轨迹数据
     *
     * @param file     DBF文件
     * @param ksjhdm   考试计划代码
     * @param cjrxm    创建人姓名
     * @param cjrgzrym 创建人工作人员码
     * @return 导入结果
     * @throws Exception 导入异常
     */
    ImportResultVO importPfgjFromDbf(MultipartFile file, String ksjhdm, String cjrxm, String cjrgzrym) throws Exception;

    /**
     * 解析DBF文件获取评分轨迹数据
     *
     * @param file   DBF文件
     * @param ksjhdm 考试计划代码
     * @return 评分轨迹数据列表
     * @throws Exception 解析异常
     */
    List<PfgjImportDTO> parseDbfFile(MultipartFile file, String ksjhdm) throws Exception;

    /**
     * 批量保存评分轨迹数据
     *
     * @param pfgjList 评分轨迹数据列表
     * @param cjrxm    创建人姓名
     * @param cjrgzrym 创建人工作人员码
     * @return 保存结果
     */
    ImportResultVO batchSavePfgj(List<PfgjImportDTO> pfgjList, String cjrxm, String cjrgzrym);

    /**
     * 验证DBF文件结构
     *
     * @param file DBF文件
     * @return 是否有效
     * @throws Exception 验证异常
     */
    boolean validateDbfFileStructure(MultipartFile file) throws Exception;

    /**
     * 获取DBF文件记录数
     *
     * @param file DBF文件
     * @return 记录数
     * @throws Exception 获取异常
     */
    int getDbfRecordCount(MultipartFile file) throws Exception;

    /**
     * 验证评分轨迹数据
     *
     * @param pfgj 评分轨迹数据
     * @return 是否有效
     */
    boolean validatePfgjData(PfgjImportDTO pfgj);
}