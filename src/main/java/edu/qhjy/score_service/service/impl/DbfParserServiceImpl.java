package edu.qhjy.score_service.service.impl;

import com.linuxense.javadbf.DBFReader;
import com.linuxense.javadbf.DBFRow;
import edu.qhjy.score_service.domain.dto.DbfRecordDTO;
import edu.qhjy.score_service.service.DbfParserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DBF文件解析服务实现类
 *
 * @author dadalv
 * @since 2025-08-01
 */
@Slf4j
@Service
public class DbfParserServiceImpl implements DbfParserService {

    /**
     * 科目名称提取正则表达式
     * 匹配格式：数字_科目名称_其他内容.dbf
     */
    private static final Pattern SUBJECT_PATTERN = Pattern.compile("\\d+_([^_]+)_.*\\.dbf$", Pattern.CASE_INSENSITIVE);

    /**
     * 成绩格式验证正则表达式
     * 匹配整数格式：最多4位整数（因为导入时已四舍五入为整数）
     */
    private static final Pattern SCORE_PATTERN = Pattern.compile("^\\d{1,4}$");

    @Override
    public List<DbfRecordDTO> parseDbfFile(MultipartFile file, String ksjhdm) throws Exception {
        log.info("开始解析DBF文件: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());

        List<DbfRecordDTO> records = new ArrayList<>();
        String kmmc = extractSubjectFromFileName(file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            // 使用GBK编码读取DBF文件（中文DBF文件通常使用GBK编码）
            DBFReader reader = new DBFReader(inputStream, Charset.forName("GBK"));

            // 验证字段结构
            validateRequiredFields(reader);

            DBFRow row;
            int rowNumber = 1;

            while ((row = reader.nextRow()) != null) {
                try {
                    DbfRecordDTO record = parseRow(row, ksjhdm, kmmc, rowNumber);
                    if (record != null && validateDbfRecord(record)) {
                        records.add(record);
                    } else {
                        log.warn("第{}行数据验证失败，跳过该记录", rowNumber);
                    }
                } catch (Exception e) {
                    log.error("解析第{}行数据时发生错误: {}", rowNumber, e.getMessage());
                }
                rowNumber++;
            }
        }

        log.info("DBF文件解析完成，共解析{}条有效记录", records.size());
        return records;
    }

    @Override
    public String extractSubjectFromFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "未知科目";
        }

        Matcher matcher = SUBJECT_PATTERN.matcher(fileName);
        if (matcher.find()) {
            String subject = matcher.group(1).trim();
            log.debug("从文件名 {} 中提取科目名称: {}", fileName, subject);
            return subject;
        }

        log.warn("无法从文件名 {} 中提取科目名称，使用默认值", fileName);
        return "未知科目";
    }

    @Override
    public boolean validateDbfRecord(DbfRecordDTO record) {
        if (record == null) {
            return false;
        }

        // 验证必填字段
        if (isBlank(record.getYjxh()) || isBlank(record.getKsxm()) ||
                isBlank(record.getKsjhdm()) || isBlank(record.getKmmc())) {
            log.debug("记录第{}行：必填字段为空", record.getRowNumber());
            return false;
        }

        // 验证成绩字段
        if (record.getZcj() == null || record.getKgtcj() == null || record.getZgtcj() == null) {
            log.debug("记录第{}行：成绩字段为空", record.getRowNumber());
            return false;
        }

        // 验证成绩格式
        if (!record.isAllScoresValid()) {
            log.debug("记录第{}行：成绩格式不符合decimal(5,1)要求", record.getRowNumber());
            return false;
        }

        return true;
    }

    @Override
    public boolean validateScoreFormat(String scoreStr) {
        if (scoreStr == null || scoreStr.trim().isEmpty()) {
            return false;
        }

        try {
            BigDecimal score = new BigDecimal(scoreStr.trim());
            // 检查是否符合decimal(5,1)格式
            return score.scale() <= 1 && score.precision() <= 5;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public int getDbfRecordCount(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            DBFReader reader = new DBFReader(inputStream, Charset.forName("GBK"));
            return reader.getRecordCount();
        }
    }

    @Override
    public boolean validateDbfFileStructure(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            DBFReader reader = new DBFReader(inputStream, Charset.forName("GBK"));

            // 检查必需的字段是否存在
            validateRequiredFields(reader);

            // 检查是否有数据记录
            return reader.getRecordCount() > 0;
        }
    }

    /**
     * 解析DBF行数据
     */
    private DbfRecordDTO parseRow(DBFRow row, String ksjhdm, String kmmc, int rowNumber) {
        try {
            DbfRecordDTO record = new DbfRecordDTO();
            record.setRowNumber(rowNumber);
            record.setKsjhdm(ksjhdm);
            record.setKmmc(kmmc);

            // 提取字段值（字段名可能大小写不同）
            record.setYjxh(getStringValue(row, "ksh", "KSH"));
            record.setKsxm(getStringValue(row, "xm", "XM"));

            // 解析成绩字段
            record.setZcj(parseScore(getStringValue(row, "zcj", "ZCJ")));
            record.setKgtcj(parseDecimalScore(getStringValue(row, "kgtcj", "KGTCJ")));
            record.setZgtcj(parseDecimalScore(getStringValue(row, "zgtcj", "ZGTCJ")));

            return record;
        } catch (Exception e) {
            log.error("解析第{}行数据失败: {}", rowNumber, e.getMessage());
            return null;
        }
    }

    /**
     * 获取字符串字段值（支持多种字段名）
     */
    private String getStringValue(DBFRow row, String... fieldNames) {
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

    /**
     * 解析FSLKSCJ字段，转换为Integer类型（四舍五入）
     */
    private Integer parseScore(String scoreStr) {
        if (scoreStr == null || scoreStr.trim().isEmpty()) {
            return null;
        }

        try {
            // 先解析为BigDecimal，然后四舍五入为整数
            BigDecimal decimal = new BigDecimal(scoreStr.trim());
            return decimal.setScale(0, RoundingMode.HALF_UP).intValue();
        } catch (NumberFormatException e) {
            log.warn("无法解析成绩值: {}", scoreStr);
            return null;
        }
    }

    /**
     * 解析其他成绩字段，保持原始BigDecimal类型
     */
    private BigDecimal parseDecimalScore(String scoreStr) {
        if (scoreStr == null || scoreStr.trim().isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(scoreStr.trim());
        } catch (NumberFormatException e) {
            log.warn("无法解析成绩值: {}", scoreStr);
            return null;
        }
    }

    /**
     * 验证DBF文件是否包含必需的字段
     */
    private void validateRequiredFields(DBFReader reader) throws Exception {
        int fieldCount = reader.getFieldCount();
        List<String> fieldNames = new ArrayList<>();

        for (int i = 0; i < fieldCount; i++) {
            fieldNames.add(reader.getField(i).getName().toLowerCase());
        }

        // 检查必需字段
        String[] requiredFields = {"ksh", "xm", "zcj", "kgtcj", "zgtcj"};
        for (String field : requiredFields) {
            if (!fieldNames.contains(field.toLowerCase())) {
                throw new IllegalArgumentException("DBF文件缺少必需字段: " + field);
            }
        }
    }

    /**
     * 检查字符串是否为空
     */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}