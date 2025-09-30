package edu.qhjy.score_service.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 区域层级数据VO
 * 用于前端级联下拉选择框
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "区域层级数据")
public class AreaHierarchyVO {

    @Schema(description = "区域代码")
    private String code;

    @Schema(description = "区域名称")
    private String name;

    @Schema(description = "区域级别(city/county/school/class)")
    private String level;

    @Schema(description = "上级区域代码")
    private String parentCode;

    @Schema(description = "上级区域名称")
    private String parentName;

    @Schema(description = "是否有下级区域")
    private Boolean hasChildren;

    @Schema(description = "下级区域数量")
    private Integer childrenCount;

    /**
     * 创建地市级区域数据
     */
    public static AreaHierarchyVO createCity(String cityName) {
        return AreaHierarchyVO.builder()
                .code(cityName)
                .name(cityName)
                .level("city")
                .hasChildren(true)
                .build();
    }

    /**
     * 创建考区级区域数据
     */
    public static AreaHierarchyVO createCounty(String countyName, String parentCityName) {
        return AreaHierarchyVO.builder()
                .code(countyName)
                .name(countyName)
                .level("county")
                .parentCode(parentCityName)
                .parentName(parentCityName)
                .hasChildren(true)
                .build();
    }

    /**
     * 创建学校级区域数据
     */
    public static AreaHierarchyVO createSchool(String schoolName, String parentCountyName) {
        return AreaHierarchyVO.builder()
                .code(schoolName)
                .name(schoolName)
                .level("school")
                .parentCode(parentCountyName)
                .parentName(parentCountyName)
                .hasChildren(false)
                .build();
    }

    /**
     * 创建班级级区域数据
     */
    public static AreaHierarchyVO createClass(String className, String parentSchoolName) {
        return AreaHierarchyVO.builder()
                .code(className)
                .name(className)
                .level("class")
                .parentCode(parentSchoolName)
                .parentName(parentSchoolName)
                .hasChildren(false)
                .build();
    }
}