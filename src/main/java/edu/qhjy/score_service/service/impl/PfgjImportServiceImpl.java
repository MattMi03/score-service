package edu.qhjy.score_service.service.impl;

import com.linuxense.javadbf.DBFReader;
import com.linuxense.javadbf.DBFRow;
import edu.qhjy.score_service.domain.dto.PfgjImportDTO;
import edu.qhjy.score_service.domain.entity.PfgjEntity;
import edu.qhjy.score_service.domain.vo.ImportResultVO;
import edu.qhjy.score_service.mapper.primary.PfgjMapper;
import edu.qhjy.score_service.service.PfgjImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评分轨迹导入服务实现类
 *
 * @author dadalv
 * @since 2025-08-15
 */
@Slf4j
@Service
public class PfgjImportServiceImpl implements PfgjImportService {

    /**
     * 批量处理大小
     */
    private static final int BATCH_SIZE = 1000;
    @Autowired
    private PfgjMapper pfgjMapper;

    // /**
    // * 线程池配置 - 优化为高并发处理60万条数据
    // */
    // private static final int CORE_POOL_SIZE = 16;
    // private static final int MAX_POOL_SIZE = 24;
    // private static final int KEEP_ALIVE_TIME = 30;
    // private static final int QUEUE_CAPACITY = 200;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importPfgjFromDbf(MultipartFile file, String ksjhdm, String cjrxm, String cjrgzrym)
            throws Exception {
        log.info("开始导入评分轨迹DBF文件: {}, 考试计划代码: {}", file.getOriginalFilename(), ksjhdm);

        // 验证文件
        if (!validateDbfFileStructure(file)) {
            throw new IllegalArgumentException("DBF文件结构不符合要求");
        }

        // 解析DBF文件
        List<PfgjImportDTO> pfgjList = parseDbfFile(file, ksjhdm);
        if (pfgjList.isEmpty()) {
            return ImportResultVO.builder()
                    .success(false)
                    .message("DBF文件中没有有效的评分轨迹数据")
                    .totalCount(0)
                    .successCount(0)
                    .failCount(0)
                    .fileName(file.getOriginalFilename())
                    .build();
        }

        // 批量保存数据
        return batchSavePfgj(pfgjList, cjrxm, cjrgzrym, file.getOriginalFilename());
    }

    @Override
    public List<PfgjImportDTO> parseDbfFile(MultipartFile file, String ksjhdm) throws Exception {
        log.info("开始解析评分轨迹DBF文件: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());

        List<PfgjImportDTO> records = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream()) {
            // 使用GBK编码读取DBF文件
            DBFReader reader = new DBFReader(inputStream, Charset.forName("GBK"));

            // 验证字段结构
            validateRequiredFields(reader);

            DBFRow row;
            int rowNumber = 1;

            while ((row = reader.nextRow()) != null) {
                try {
                    PfgjImportDTO record = parseRow(row, ksjhdm, rowNumber);
                    if (record != null && validatePfgjData(record)) {
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

        log.info("评分轨迹DBF文件解析完成，共解析{}条有效记录", records.size());
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO batchSavePfgj(List<PfgjImportDTO> pfgjList, String cjrxm, String cjrgzrym) {
        return batchSavePfgj(pfgjList, cjrxm, cjrgzrym, null);
    }

    /**
     * 批量保存评分轨迹数据（带文件名）
     *
     * @param pfgjList 评分轨迹数据列表
     * @param cjrxm    创建人姓名
     * @param cjrgzrym 创建人工作人员码
     * @param fileName 文件名
     * @return 保存结果
     */
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO batchSavePfgj(List<PfgjImportDTO> pfgjList, String cjrxm, String cjrgzrym, String fileName) {
        log.info("开始批量保存评分轨迹数据，共{}条记录", pfgjList.size());

        int totalCount = pfgjList.size();
        int successCount = 0;
        int failCount = 0;
        List<String> errorMessages = new ArrayList<>();

        try {
            // // 创建线程池 - 高并发配置
            // ThreadPoolExecutor executor = new ThreadPoolExecutor(
            // CORE_POOL_SIZE,
            // MAX_POOL_SIZE,
            // KEEP_ALIVE_TIME,
            // TimeUnit.SECONDS,
            // new LinkedBlockingQueue<>(QUEUE_CAPACITY)
            // );

            // 分批处理数据
            List<List<PfgjImportDTO>> batches = partitionList(pfgjList, BATCH_SIZE);
            // List<CompletableFuture<Integer>> futures = new ArrayList<>();

            for (int i = 0; i < batches.size(); i++) {
                List<PfgjImportDTO> batch = batches.get(i);
                try {
                    int batchSuccessCount = processBatch(batch, cjrxm, cjrgzrym, i);
                    successCount += batchSuccessCount;
                    log.debug("批次{}处理完成，成功处理{}条记录", i, batchSuccessCount);
                } catch (Exception e) {
                    log.error("批次{}处理失败: {}", i, e.getMessage());
                    // 如果任何一个批次失败，抛出异常以触发事务回滚
                    throw new RuntimeException("批次" + i + "处理失败: " + e.getMessage(), e);
                }
            }

            failCount = totalCount - successCount;

        } catch (Exception e) {
            log.error("批量保存评分轨迹数据失败: {}", e.getMessage());
            errorMessages.add("批量保存失败: " + e.getMessage());
            // 重新抛出异常以确保事务回滚
            throw new RuntimeException("评分轨迹数据导入失败", e);
        }

        log.info("评分轨迹数据保存完成，总数: {}, 成功: {}, 失败: {}", totalCount, successCount, failCount);

        return ImportResultVO.builder()
                .success(true)
                .message("导入完成")
                .totalCount(totalCount)
                .successCount(successCount)
                .failCount(failCount)
                .errorMessages(errorMessages)
                .fileName(fileName)
                .build();
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

    @Override
    public int getDbfRecordCount(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            DBFReader reader = new DBFReader(inputStream, Charset.forName("GBK"));
            return reader.getRecordCount();
        }
    }

    @Override
    public boolean validatePfgjData(PfgjImportDTO pfgj) {
        if (pfgj == null) {
            return false;
        }

        // 使用DTO内置的验证方法
        return pfgj.isValid();
    }

    /**
     * 解析DBF行数据
     */
    private PfgjImportDTO parseRow(DBFRow row, String ksjhdm, int rowNumber) {
        try {
            PfgjImportDTO record = new PfgjImportDTO();
            record.setKsjhdm(ksjhdm);

            // 提取字段值（字段名可能大小写不同）
            record.setLsh(getStringValue(row, "lsh", "LSH"));

            // ksh -> yjxh
            String kshStr = getStringValue(row, "ksh", "KSH");
            if (kshStr != null && !kshStr.trim().isEmpty()) {
                try {
                    record.setYjxh(Long.parseLong(kshStr.trim()));
                } catch (NumberFormatException e) {
                    log.warn("第{}行：阅卷序号格式错误: {}", rowNumber, kshStr);
                    return null;
                }
            }

            // itemid -> itemid
            String itemidStr = getStringValue(row, "itemid", "ITEMID");
            if (itemidStr != null && !itemidStr.trim().isEmpty()) {
                try {
                    record.setItemid(Integer.parseInt(itemidStr.trim()));
                } catch (NumberFormatException e) {
                    log.warn("第{}行：大题号格式错误: {}", rowNumber, itemidStr);
                    return null;
                }
            }

            // mh -> mh
            record.setMh(getStringValue(row, "mh", "MH"));

            // marksum -> marksum
            record.setMarksum(getStringValue(row, "marksum", "MARKSUM"));

            // submark -> submark
            record.setSubmark(getStringValue(row, "submark", "SUBMARK"));

            // tasktype -> tasktype
            String tasktypeStr = getStringValue(row, "tasktype", "TASKTYPE");
            if (tasktypeStr != null && !tasktypeStr.trim().isEmpty()) {
                // 验证tasktype是否为有效数字字符串
                String trimmedTasktype = tasktypeStr.trim();
                if (trimmedTasktype.matches("^(1|2|3|21|22|23|24|25)$")) {
                    record.setTasktype(trimmedTasktype);
                } else {
                    log.warn("第{}行：评次值无效: {}", rowNumber, tasktypeStr);
                    return null;
                }
            }

            // teacherid -> teacherid
            String teacheridStr = getStringValue(row, "teacherid", "TEACHERID");
            if (teacheridStr != null && !teacheridStr.trim().isEmpty()) {
                try {
                    record.setTeacherid(Integer.parseInt(teacheridStr.trim()));
                } catch (NumberFormatException e) {
                    log.warn("第{}行：评卷用户号格式错误: {}", rowNumber, teacheridStr);
                    // teacherid不是必填字段，可以为空
                }
            }

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
     * 验证DBF文件是否包含必需的字段
     */
    private void validateRequiredFields(DBFReader reader) throws Exception {
        int fieldCount = reader.getFieldCount();
        List<String> fieldNames = new ArrayList<>();

        for (int i = 0; i < fieldCount; i++) {
            fieldNames.add(reader.getField(i).getName().toLowerCase());
        }

        // 检查必需字段
        String[] requiredFields = {"lsh", "ksh", "itemid", "tasktype"};
        for (String field : requiredFields) {
            if (!fieldNames.contains(field.toLowerCase())) {
                throw new IllegalArgumentException("DBF文件缺少必需字段: " + field);
            }
        }
    }

    /**
     * 处理单个批次
     */
    private int processBatch(List<PfgjImportDTO> batch, String cjrxm, String cjrgzrym, int batchIndex) {
        log.debug("开始处理批次{}, 记录数: {}", batchIndex, batch.size());

        try {
            // 转换为实体对象
            List<PfgjEntity> entities = batch.stream()
                    .map(dto -> convertToEntity(dto, cjrxm, cjrgzrym))
                    .collect(Collectors.toList());

            // 先删除已存在的记录（覆盖导入）
            if (!entities.isEmpty()) {
                List<String> lshList = entities.stream()
                        .map(PfgjEntity::getLsh)
                        .distinct()
                        .collect(Collectors.toList());

                // 按大题和评次分组删除
                entities.stream()
                        .collect(Collectors.groupingBy(e -> e.getItemid() + "_" + e.getTasktype()))
                        .forEach((key, group) -> {
                            PfgjEntity first = group.get(0);
                            pfgjMapper.deleteByConditions(first.getKsjhdm(), lshList,
                                    first.getItemid(), first.getTasktype());
                        });
            }

            // 批量插入新记录
            int insertCount = pfgjMapper.batchInsert(entities);
            log.debug("批次{}处理完成，插入{}条记录", batchIndex, insertCount);
            return insertCount;

        } catch (Exception e) {
            log.error("批次{}处理失败: {}", batchIndex, e.getMessage());
            return 0;
        }
    }

    /**
     * 将DTO转换为实体对象
     */
    private PfgjEntity convertToEntity(PfgjImportDTO dto, String cjrxm, String cjrgzrym) {
        PfgjEntity entity = new PfgjEntity();
        entity.setKsjhdm(dto.getKsjhdm());
        entity.setLsh(dto.getLsh());
        entity.setYjxh(dto.getYjxh());
        entity.setItemid(dto.getItemid());
        entity.setMh(dto.getMh());
        entity.setMarksum(dto.getMarksum());
        entity.setSubmark(dto.getSubmark());
        entity.setTasktype(dto.getTasktype());
        entity.setTeacherid(dto.getTeacherid());
        // 处理可能为空的创建人信息
        entity.setCjrxm(cjrxm != null && !cjrxm.trim().isEmpty() ? cjrxm : "系统导入");
        entity.setCjrgzrym(cjrgzrym != null && !cjrgzrym.trim().isEmpty() ? cjrgzrym : "000");
        entity.setCjsj(LocalDateTime.now());
        return entity;
    }

    /**
     * 将列表分割为指定大小的批次
     */
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }
}