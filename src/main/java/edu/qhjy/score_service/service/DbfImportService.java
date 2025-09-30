package edu.qhjy.score_service.service;

import edu.qhjy.score_service.domain.dto.DbfImportRequestDTO;
import edu.qhjy.score_service.domain.vo.DbfImportResponseVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * DBF文件导入服务接口
 *
 * @author dadalv
 * @since 2025-08-01
 */
public interface DbfImportService {

    /**
     * 导入DBF文件到成绩系统
     *
     * @param file   DBF文件
     * @param ksjhdm 考试计划代码
     * @return 导入结果
     */
    DbfImportResponseVO importDbfFile(MultipartFile file, String ksjhdm);

    /**
     * 验证导入请求参数
     *
     * @param request 导入请求
     * @return 验证结果，true表示验证通过
     */
    boolean validateImportRequest(DbfImportRequestDTO request);

    /**
     * 预览DBF文件内容（仅解析前几条记录用于预览）
     *
     * @param file         DBF文件
     * @param ksjhdm       考试计划代码
     * @param previewCount 预览记录数量，默认10条
     * @return 预览结果
     */
    DbfImportResponseVO previewDbfFile(MultipartFile file, String ksjhdm, int previewCount);

    /**
     * 获取导入进度（如果支持异步导入）
     *
     * @param taskId 任务ID
     * @return 导入进度信息
     */
    DbfImportResponseVO getImportProgress(String taskId);

    /**
     * 取消导入任务
     *
     * @param taskId 任务ID
     * @return 取消结果，true表示取消成功
     */
    boolean cancelImportTask(String taskId);

    /**
     * 调试DBF文件解析过程
     *
     * @param file   DBF文件
     * @param ksjhdm 考试计划代码
     * @return 调试信息
     */
    String debugDbfFile(MultipartFile file, String ksjhdm);
}
