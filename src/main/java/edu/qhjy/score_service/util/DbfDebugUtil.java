package edu.qhjy.score_service.util;

import com.linuxense.javadbf.DBFReader;
import com.linuxense.javadbf.DBFRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DBF文件调试工具
 * 用于诊断DBF文件解析过程中的问题
 *
 * @author dadalv
 * @since 2025-08-01
 */
@Slf4j
public class DbfDebugUtil {

    private static final Pattern SUBJECT_PATTERN = Pattern.compile("\\d+_([^_]+)_.*\\.dbf$", Pattern.CASE_INSENSITIVE);

    /**
     * 调试DBF文件解析过程
     *
     * @param file   DBF文件
     * @param ksjhdm 考试计划代码
     * @return 调试信息
     */
    public static String debugDbfFile(MultipartFile file, String ksjhdm) {
        StringBuilder debug = new StringBuilder();
        debug.append("=== DBF文件调试信息 ===\n");

        try {
            // 1. 检查基本参数
            debug.append("1. 基本参数检查:\n");
            debug.append("   文件名: ").append(file.getOriginalFilename()).append("\n");
            debug.append("   文件大小: ").append(file.getSize()).append(" bytes\n");
            debug.append("   考试计划代码(ksjhdm): ").append(ksjhdm != null ? ksjhdm : "null").append("\n");

            // 2. 检查科目名称提取
            debug.append("\n2. 科目名称提取:\n");
            String kmmc = extractSubjectFromFileName(file.getOriginalFilename());
            debug.append("   提取的科目名称(kmmc): ").append(kmmc).append("\n");

            // 3. 检查DBF文件结构
            debug.append("\n3. DBF文件结构检查:\n");
            try (InputStream inputStream = file.getInputStream()) {
                DBFReader reader = new DBFReader(inputStream, Charset.forName("GBK"));

                debug.append("   记录总数: ").append(reader.getRecordCount()).append("\n");
                debug.append("   字段总数: ").append(reader.getFieldCount()).append("\n");

                // 列出所有字段
                debug.append("   字段列表: ");
                List<String> fieldNames = new ArrayList<>();
                for (int i = 0; i < reader.getFieldCount(); i++) {
                    String fieldName = reader.getField(i).getName();
                    fieldNames.add(fieldName);
                    debug.append(fieldName).append(" ");
                }
                debug.append("\n");

                // 检查必需字段
                debug.append("\n4. 必需字段检查:\n");
                String[] requiredFields = {"ksh", "xm", "zcj", "kgtcj", "zgtcj"};
                for (String field : requiredFields) {
                    boolean exists = fieldNames.stream().anyMatch(f -> f.equalsIgnoreCase(field));
                    debug.append("   ").append(field).append(": ").append(exists ? "存在" : "缺失").append("\n");
                }

                // 5. 检查前几条记录的数据
                debug.append("\n5. 前3条记录数据检查:\n");
                DBFRow row;
                int rowNumber = 1;
                while ((row = reader.nextRow()) != null && rowNumber <= 3) {
                    debug.append("   第").append(rowNumber).append("行:\n");

                    // 检查各个字段值
                    String yjxh = getStringValue(row, "ksh", "KSH");
                    String ksxm = getStringValue(row, "xm", "XM");
                    String zcjStr = getStringValue(row, "zcj", "ZCJ");
                    String kgtcjStr = getStringValue(row, "kgtcj", "KGTCJ");
                    String zgtcjStr = getStringValue(row, "zgtcj", "ZGTCJ");

                    debug.append("     阅卷序号(yjxh): ").append(yjxh != null ? yjxh : "null").append("\n");
                    debug.append("     考生姓名(ksxm): ").append(ksxm != null ? ksxm : "null").append("\n");
                    debug.append("     总成绩(zcj): ").append(zcjStr != null ? zcjStr : "null");
                    if (zcjStr != null) {
                        BigDecimal zcj = parseScore(zcjStr);
                        debug.append(" -> 解析后: ").append(zcj != null ? zcj : "解析失败");
                    }
                    debug.append("\n");

                    debug.append("     客观题成绩(kgtcj): ").append(kgtcjStr != null ? kgtcjStr : "null");
                    if (kgtcjStr != null) {
                        BigDecimal kgtcj = parseScore(kgtcjStr);
                        debug.append(" -> 解析后: ").append(kgtcj != null ? kgtcj : "解析失败");
                    }
                    debug.append("\n");

                    debug.append("     主观题成绩(zgtcj): ").append(zgtcjStr != null ? zgtcjStr : "null");
                    if (zgtcjStr != null) {
                        BigDecimal zgtcj = parseScore(zgtcjStr);
                        debug.append(" -> 解析后: ").append(zgtcj != null ? zgtcj : "解析失败");
                    }
                    debug.append("\n");

                    // 检查必填字段验证
                    debug.append("     必填字段验证:\n");
                    debug.append("       yjxh为空: ").append(isBlank(yjxh)).append("\n");
                    debug.append("       ksxm为空: ").append(isBlank(ksxm)).append("\n");
                    debug.append("       ksjhdm为空: ").append(isBlank(ksjhdm)).append("\n");
                    debug.append("       kmmc为空: ").append(isBlank(kmmc)).append("\n");

                    BigDecimal zcj = parseScore(zcjStr);
                    BigDecimal kgtcj = parseScore(kgtcjStr);
                    BigDecimal zgtcj = parseScore(zgtcjStr);
                    debug.append("       zcj为null: ").append(zcj == null).append("\n");
                    debug.append("       kgtcj为null: ").append(kgtcj == null).append("\n");
                    debug.append("       zgtcj为null: ").append(zgtcj == null).append("\n");

                    // 总体验证结果
                    boolean isValid = !isBlank(yjxh) && !isBlank(ksxm) && !isBlank(ksjhdm) && !isBlank(kmmc) &&
                            zcj != null && kgtcj != null && zgtcj != null;
                    debug.append("     该行记录验证结果: ").append(isValid ? "通过" : "失败").append("\n\n");

                    rowNumber++;
                }
            }

        } catch (Exception e) {
            debug.append("\n调试过程中发生错误: ").append(e.getMessage()).append("\n");
            log.error("DBF文件调试失败", e);
        }

        return debug.toString();
    }

    private static String extractSubjectFromFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "未知科目";
        }

        Matcher matcher = SUBJECT_PATTERN.matcher(fileName);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "未知科目";
    }

    private static String getStringValue(DBFRow row, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                Object value = row.getObject(fieldName);
                if (value != null) {
                    return value.toString().trim();
                }
            } catch (Exception e) {
                // 字段不存在，尝试下一个字段名
            }
        }
        return null;
    }

    private static BigDecimal parseScore(String scoreStr) {
        if (scoreStr == null || scoreStr.trim().isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(scoreStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}