package edu.qhjy.score_service.domain.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考生信息表实体类 (edu_exam数据库)
 */
@Data
public class KsxxEntity {

    /**
     * 学生标识id
     */
    private Long ksbs;

    /**
     * 考生号
     */
    private String ksh;

    /**
     * 学籍号
     */
    private String xjh;

    /**
     * 姓名
     */
    private String xm;

    /**
     * 性别 (男或女)
     */
    private String xb;

    /**
     * 民族
     */
    private String mz;

    /**
     * 出生日期
     */
    private LocalDate csrq;

    /**
     * 首次申报落户地省名称
     */
    private String scsblhdqmc;

    /**
     * 首次申报落户地地市州名称
     */
    private String scsblhdsmc;

    /**
     * 首次申报落户地区县名称
     */
    private String scsblhdxmc;

    /**
     * 身份证件类型名称
     */
    private String sfzjlxmc;

    /**
     * 身份证件号
     */
    private String sfzjh;

    /**
     * 家庭详细地址省名称
     */
    private String jtxxdzqmc;

    /**
     * 家庭详细地址市州名称
     */
    private String jtxxdzsmc;

    /**
     * 家庭详细地址区县名称
     */
    private String jtxxdzxmc;

    /**
     * 现户籍所在省名称（考务系统：所在省名称）
     */
    private String szqmc;

    /**
     * 现户籍所在市名称（考务系统：所在市名称）
     */
    private String szsmc;

    /**
     * 现户籍所在区县名称
     */
    private String szxmc;

    /**
     * 考区名称（区县）
     */
    private String kqmc;

    /**
     * 原户籍所在省名称
     */
    private String yhjszqmc;

    /**
     * 原户籍所在市名称
     */
    private String yhjszsmc;

    /**
     * 原户籍所在县名称
     */
    private String yhjszxmc;

    /**
     * 迁入现址时间
     */
    private String qrxzsj;

    /**
     * 迁入原因
     */
    private String qryy;

    /**
     * 准迁证号
     */
    private String zqzh;

    /**
     * 迁移证号
     */
    private String qyzh;

    /**
     * 照片地址 (文件路径或URL)
     */
    private String zpdz;

    /**
     * 政治面貌
     */
    private String zzmm;

    /**
     * 移动电话
     */
    private String yddh;

    /**
     * 应试语种
     */
    private String ysyz;

    /**
     * 民族语言授课语种
     */
    private String mzyyyskyz;

    /**
     * 考生类型： 0：省内考生，1：省外转入，2：社会考生
     */
    private Integer kslx;

    /**
     * 考生类型状态名称
     */
    private String kjztmc;

    /**
     * 学校名称（考务系统使用）
     */
    private String xxmc;

    /**
     * 班级名称（考务系统使用）
     */
    private String bjmc;

    /**
     * 入学年度
     */
    private Integer rxnd;

    /**
     * 毕业年度
     */
    private Integer bynd;

    /**
     * 审核阶段
     */
    private String shjd;

    /**
     * 审核状态
     */
    private String shzt;

    /**
     * 审核时间
     */
    private LocalDateTime shsj;

    /**
     * 审核人姓名
     */
    private String shrxm;

    /**
     * 审核意见
     */
    private String shyj;

    /**
     * 创建人姓名
     */
    private String cjrxm;

    /**
     * 创建人工作人员码
     */
    private String cjrgzrym;

    /**
     * 创建时间
     */
    private LocalDateTime cjsj;

    /**
     * 更新人姓名
     */
    private String gxrxm;

    /**
     * 更新人工作人员码
     */
    private String gxrgzrym;

    /**
     * 更新时间
     */
    private LocalDateTime gxsj;
}