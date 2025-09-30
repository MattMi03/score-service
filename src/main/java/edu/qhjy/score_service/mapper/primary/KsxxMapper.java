package edu.qhjy.score_service.mapper.primary;

import edu.qhjy.score_service.domain.dto.OutOfProvinceStudentQueryDTO;
import edu.qhjy.score_service.domain.entity.KsxxEntity;
import edu.qhjy.score_service.domain.vo.OutOfProvinceStudentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考生信息Mapper (edu_exam数据库)
 */
@Mapper
public interface KsxxMapper {

    /**
     * 根据考生号查询考生信息
     */
    KsxxEntity selectByKsh(@Param("ksh") String ksh);

    /**
     * 根据考生号列表批量查询考生信息
     */
    List<KsxxEntity> selectByKshList(@Param("kshList") List<String> kshList);

    /**
     * 根据学校和班级查询考生信息
     */
    List<KsxxEntity> selectByXxmcAndBjmc(@Param("xxmc") String xxmc, @Param("bjmc") String bjmc);

    /**
     * 查询所有考生信息
     */
    List<KsxxEntity> selectAll();

    /**
     * 分页查询省外转入考生数据（支持级联查询）
     *
     * @param queryDTO 查询条件
     * @return 省外转入考生列表
     */
    List<OutOfProvinceStudentVO> selectOutOfProvinceStudentsWithPagination(
            @Param("query") OutOfProvinceStudentQueryDTO queryDTO);

    /**
     * 统计省外转入考生数据总数（支持级联查询）
     *
     * @param queryDTO 查询条件
     * @return 总数
     */
    Long countOutOfProvinceStudents(@Param("query") OutOfProvinceStudentQueryDTO queryDTO);

    /**
     * 根据考生号查询考生基本信息（用于考籍信息查询）
     *
     * @param ksh 考生号
     * @return 考生基本信息（包含KSH、SFZJH、XM字段）
     */
    KsxxEntity selectStudentInfoByKsh(@Param("ksh") String ksh);

    /**
     * 验证考生信息（考生号、身份证件号、姓名）
     *
     * @param ksh   考生号
     * @param sfzjh 身份证件号
     * @param xm    姓名
     * @return 考生信息
     */
    KsxxEntity validateStudentInfo(@Param("ksh") String ksh, @Param("sfzjh") String sfzjh, @Param("xm") String xm);

    /**
     * 查询各市州毕业生统计数据
     *
     * @param bynd 毕业年度
     * @param rxnd 级别
     * @return 各市州毕业生统计数据
     */
    List<java.util.Map<String, Object>> selectGraduationStatisticsByCity(@Param("bynd") Integer bynd,
                                                                         @Param("rxnd") Integer rxnd);

    /**
     * 根据毕业年度查询总人数
     *
     * @param bynd 毕业年度
     * @return 总人数
     */
    Integer selectTotalCountByBynd(@Param("bynd") Integer bynd);

    /**
     * 根据级别查询总人数
     *
     * @param rxnd 级别
     * @return 总人数
     */
    Integer selectTotalCountByRxnd(@Param("rxnd") Integer rxnd);

    /**
     * 获取毕业生统计数据（合并查询优化版本）
     * 同时获取按市州分组统计、毕业年度总数、级别总数
     *
     * @param bynd 毕业年度
     * @param rxnd 级别
     * @return 包含市州统计、毕业年度总数、级别总数的统计数据
     */
    List<java.util.Map<String, Object>> selectGraduationStatisticsOptimized(@Param("bynd") Integer bynd,
                                                                            @Param("rxnd") Integer rxnd);

}
