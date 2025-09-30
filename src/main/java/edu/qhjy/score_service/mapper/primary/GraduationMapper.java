package edu.qhjy.score_service.mapper.primary;

import edu.qhjy.score_service.domain.dto.GraduationQueryDTO;
import edu.qhjy.score_service.domain.entity.BytjEntity;
import edu.qhjy.score_service.domain.vo.GraduationStudentVO;
import edu.qhjy.score_service.domain.vo.StudentScoreVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 毕业生花名册Mapper接口
 */
@Mapper
public interface GraduationMapper {

    /**
     * 根据所在市名称查询毕业条件
     *
     * @param szsmc 所在市名称
     * @return 毕业条件实体
     */
    BytjEntity selectBytjBySzsmc(@Param("szsmc") String szsmc);

    /**
     * 毕业生条件查询（分页）
     *
     * @param queryDTO 查询条件
     * @return 毕业生信息列表
     */
    List<GraduationStudentVO> selectGraduationStudents(@Param("query") GraduationQueryDTO queryDTO);

    /**
     * 统计毕业生总数
     *
     * @param queryDTO 查询条件
     * @return 总数
     */
    int countGraduationStudents(@Param("query") GraduationQueryDTO queryDTO);

    /**
     * 查询学生的所有科目成绩
     *
     * @param ksh 考生号
     * @return 学生成绩列表
     */
    List<StudentScoreVO> selectStudentScores(@Param("ksh") String ksh);

    /**
     * 批量更新学生毕业状态
     *
     * @param kshList      考生号列表
     * @param bynd         毕业年度
     * @param operatorName 操作人姓名
     * @param operatorCode 操作人工作人员码
     * @return 更新记录数
     */
    int batchUpdateGraduationStatus(@Param("kshList") List<String> kshList,
                                    @Param("bynd") String bynd,
                                    @Param("operatorName") String operatorName,
                                    @Param("operatorCode") String operatorCode);

    /**
     * 根据条件查询符合毕业条件的考生号列表
     *
     * @param szsmc 所在市名称（必填）
     * @param kqmc  考区名称（可选）
     * @param xxmc  学校名称（可选）
     * @param ksh   考生号（可选）
     * @param bynd  毕业年度（可选）
     * @return 考生号列表
     */
    List<String> selectQualifiedStudentKsh(@Param("szsmc") String szsmc,
                                           @Param("kqmc") String kqmc,
                                           @Param("xxmc") String xxmc,
                                           @Param("ksh") String ksh,
                                           @Param("bynd") String bynd);

    /**
     * 统计考试科目合格数量
     *
     * @param ksh 考生号
     * @return 考试科目合格数量
     */
    int countExamSubjectPass(@Param("ksh") String ksh);

    /**
     * 统计考察科目合格数量
     *
     * @param ksh 考生号
     * @return 考察科目合格数量
     */
    int countAssessmentSubjectPass(@Param("ksh") String ksh);

    /**
     * 根据考生号查询学生考籍状态
     *
     * @param ksh 考生号
     * @return 考籍状态名称
     */
    String selectStudentKjztmc(@Param("ksh") String ksh);

    /**
     * 批量查询学生成绩
     *
     * @param kshList 考生号列表
     * @return 学生成绩列表
     */
    List<StudentScoreVO> selectStudentScoresBatch(@Param("kshList") List<String> kshList);

    /**
     * 查询满足毕业条件的学生（数据库层面筛选）
     *
     * @param queryDTO 查询条件
     * @param kskm     考试科目最低数量
     * @param kckm     考查科目最低数量
     * @return 满足毕业条件的学生列表
     */
    List<GraduationStudentVO> selectQualifiedGraduationStudents(@Param("query") GraduationQueryDTO queryDTO,
                                                                @Param("kskm") Integer kskm,
                                                                @Param("kckm") Integer kckm);

    /**
     * 统计满足毕业条件的学生总数（数据库层面筛选）
     *
     * @param queryDTO 查询条件
     * @param kskm     考试科目最低数量
     * @param kckm     考查科目最低数量
     * @return 满足毕业条件的学生总数
     */
    int countQualifiedGraduationStudents(@Param("query") GraduationQueryDTO queryDTO,
                                         @Param("kskm") Integer kskm,
                                         @Param("kckm") Integer kckm);
}