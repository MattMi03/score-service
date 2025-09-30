package edu.qhjy.score_service.service.impl;

import edu.qhjy.score_service.domain.dto.DbfImportRequestDTO;
import edu.qhjy.score_service.domain.dto.DbfRecordDTO;
import edu.qhjy.score_service.domain.entity.KscjEntity;
import edu.qhjy.score_service.domain.entity.YjxhEntity;
import edu.qhjy.score_service.domain.vo.DbfImportResponseVO;
import edu.qhjy.score_service.domain.vo.FailedRecordVO;
import edu.qhjy.score_service.domain.vo.StudentDataVO;
import edu.qhjy.score_service.mapper.primary.KscjMapper;
import edu.qhjy.score_service.mapper.primary.YjxhMapper;
import edu.qhjy.score_service.service.DbfImportService;
import edu.qhjy.score_service.service.DbfParserService;
import edu.qhjy.score_service.util.DbfDebugUtil;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * DBF文件导入服务实现类
 *
 * @author dadalv
 * @since 2025-08-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DbfImportServiceImpl implements DbfImportService {

    private final DbfParserService dbfParserService;
    private final YjxhMapper yjxhMapper;
    private final KscjMapper kscjMapper;

    // @Qualifier("primaryDataSource")
    // private final HikariDataSource primaryDataSource;

    private final TransactionTemplate transactionTemplate;
    // 存储导入任务进度的缓存
    private final Map<String, DbfImportResponseVO> importProgressCache = new ConcurrentHashMap<>();
    // 存储可取消的任务
    private final Map<String, CompletableFuture<DbfImportResponseVO>> importTasks = new ConcurrentHashMap<>();
    @Value("${dbf.import.batch-size:500}")
    private int batchSize;
    @Value("${dbf.import.thread-pool.core-size:8}")
    private int corePoolSize;
    @Value("${dbf.import.thread-pool.max-size:16}")
    private int maxPoolSize;
    @Value("${dbf.import.thread-pool.queue-capacity:200}")
    private int queueCapacity;
    @Value("${dbf.import.thread-pool.keep-alive-time:60}")
    private long keepAliveTime;
    // 线程池执行器
    private volatile ThreadPoolExecutor threadPoolExecutor;

    // 初始化线程池
    private ThreadPoolExecutor getThreadPoolExecutor() {
        if (threadPoolExecutor == null) {
            synchronized (this) {
                if (threadPoolExecutor == null) {
                    threadPoolExecutor = new ThreadPoolExecutor(
                            corePoolSize,
                            maxPoolSize,
                            keepAliveTime,
                            TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(queueCapacity),
                            new ThreadFactory() {
                                private final AtomicInteger threadNumber = new AtomicInteger(1);

                                @Override
                                public Thread newThread(Runnable r) {
                                    Thread t = new Thread(r, "dbf-import-" + threadNumber.getAndIncrement());
                                    t.setDaemon(false);
                                    return t;
                                }
                            },
                            new ThreadPoolExecutor.CallerRunsPolicy());
                }
            }
        }
        return threadPoolExecutor;
    }

    @PreDestroy
    public void shutdown() {
        if (threadPoolExecutor != null && !threadPoolExecutor.isShutdown()) {
            log.info("正在关闭DBF导入线程池...");
            threadPoolExecutor.shutdown();
            try {
                if (!threadPoolExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    threadPoolExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPoolExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("DBF导入线程池已关闭");
        }
    }

    @Override
    public DbfImportResponseVO importDbfFile(MultipartFile file, String ksjhdm) {
        long overallStartTime = System.currentTimeMillis();
        long lastCheckpointTime = overallStartTime;
        log.info("【性能分析】================== 开始导入任务 ==================");
        log.info("【性能分析】文件名: {}, 文件大小: {} bytes", file.getOriginalFilename(), file.getSize());



        String taskId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();

        log.info("开始导入DBF文件: {}", file.getOriginalFilename());

        // // 记录导入前连接池状态
        // if (primaryDataSource != null && primaryDataSource.getHikariPoolMXBean() !=
        // null) {
        // log.info("导入前连接池状态: active={}, idle={}, total={}, waiting={}",
        // primaryDataSource.getHikariPoolMXBean().getActiveConnections(),
        // primaryDataSource.getHikariPoolMXBean().getIdleConnections(),
        // primaryDataSource.getHikariPoolMXBean().getTotalConnections(),
        // primaryDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        // }

        try {
            // 1. 验证文件和参数
            DbfImportRequestDTO request = new DbfImportRequestDTO();
            request.setFile(file);
            request.setKsjhdm(ksjhdm);

            if (!validateImportRequest(request)) {
                return DbfImportResponseVO.createFailureResponse(
                        "参数验证失败", file.getOriginalFilename(), file.getSize(), ksjhdm, startTime);
            }

            log.info("【性能分析-阶段1】开始 -> 解析DBF文件...");
            // 2. 解析DBF文件
            List<DbfRecordDTO> dbfRecords = dbfParserService.parseDbfFile(file, ksjhdm);

            // ==================== 【性能分析代码-节点1】 ====================
            long currentTime = System.currentTimeMillis();
            log.info("【性能分析-阶段1】结束 -> 文件解析完成。耗时: {} 毫秒", currentTime - lastCheckpointTime);
            lastCheckpointTime = currentTime;
            // =============================================================


            if (dbfRecords.isEmpty()) {
                return DbfImportResponseVO.createFailureResponse(
                        "DBF文件中没有有效数据", file.getOriginalFilename(), file.getSize(), ksjhdm, startTime);
            }

            // 3. 从DBF记录中获取科目名称（所有记录的科目名称应该相同）
            String kmmc = dbfRecords.get(0).getKmmc();


            log.info("【性能分析-阶段2】开始 -> 数据校验与关联...");
            // 4. 使用批量查询优化的方式处理记录
            ProcessResult processResult = processRecords(dbfRecords, ksjhdm, kmmc);

            // ==================== 【性能分析代码-节点2】 ====================
            currentTime = System.currentTimeMillis();
            log.info("【性能分析-阶段2】结束 -> 数据校验与关联完成。耗时: {} 毫秒", currentTime - lastCheckpointTime);
            lastCheckpointTime = currentTime;
            // =============================================================


            // 5. 批量导入成功记录（覆盖模式）
            log.info("【性能分析-阶段3】开始 -> 写入数据库 (先删后插)...");
            if (!processResult.getValidRecords().isEmpty()) {
                batchInsertScores(processResult.getValidRecords(), ksjhdm);
            }

            // ==================== 【性能分析代码-节点3】 ====================
            currentTime = System.currentTimeMillis();
            log.info("【性能分析-阶段3】结束 -> 写入数据库完成。耗时: {} 毫秒", currentTime - lastCheckpointTime);
            // =============================================================


            // 6. 构建响应结果
            String ksjhmc = yjxhMapper.selectKsjhmcByKsjhdm(ksjhdm);
            DbfImportResponseVO response = DbfImportResponseVO.createSuccessResponse(
                    file.getOriginalFilename(), file.getSize(), ksjhdm, ksjhmc,
                    dbfParserService.extractSubjectFromFileName(file.getOriginalFilename()),
                    startTime);

            response.setStatistics(
                    dbfRecords.size(),
                    processResult.getValidRecords().size(),
                    processResult.getYjxhNotFoundCount(),
                    processResult.getNameMismatchCount(),
                    0 // 数据验证失败数（在解析阶段已过滤）
            );

            response.setFailedRecords(processResult.getFailedRecords());
            response.setSuccessPreview(processResult.getValidRecords().stream()
                    .limit(10)
                    .map(this::convertToPreviewMap)
                    .collect(Collectors.toList()));

            log.info("DBF文件导入完成，成功: {}, 失败: {}",
                    processResult.getValidRecords().size(), processResult.getFailedRecords().size());

            // // 记录导入后连接池状态
            // if (primaryDataSource != null && primaryDataSource.getHikariPoolMXBean() !=
            // null) {
            // log.info("导入后连接池状态: active={}, idle={}, total={}, waiting={}",
            // primaryDataSource.getHikariPoolMXBean().getActiveConnections(),
            // primaryDataSource.getHikariPoolMXBean().getIdleConnections(),
            // primaryDataSource.getHikariPoolMXBean().getTotalConnections(),
            // primaryDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
            // }

            // ==================== 【性能分析代码-终点】 ====================
            log.info("【性能分析】================== 导入任务成功结束 ==================");
            log.info("【性能分析】总耗时: {} 毫秒", System.currentTimeMillis() - overallStartTime);
            // =============================================================


            return response;

        } catch (Exception e) {
            log.error("DBF文件导入失败: {}", e.getMessage());
            String ksjhmc = null;
            try {
                ksjhmc = yjxhMapper.selectKsjhmcByKsjhdm(ksjhdm);
            } catch (Exception ex) {
                // 忽略获取考试计划名称失败的警告日志
            }
            DbfImportResponseVO response = DbfImportResponseVO.createFailureResponse(
                    file.getOriginalFilename(),
                    "导入过程中发生错误: " + e.getMessage(),
                    file.getSize(), ksjhdm, ksjhmc, startTime);
            response.setErrorDetails(e.getMessage());
            return response;
        }
    }

    @Override
    public boolean validateImportRequest(DbfImportRequestDTO request) {
        if (request == null || request.getFile() == null) {
            log.warn("导入请求或文件为空");
            return false;
        }

        return request.isValidDbfFile() && request.isValidFileSize();
    }

    @Override
    public DbfImportResponseVO previewDbfFile(MultipartFile file, String ksjhdm, int previewCount) {
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // 解析前几条记录用于预览
            List<DbfRecordDTO> allRecords = dbfParserService.parseDbfFile(file, ksjhdm);
            List<DbfRecordDTO> previewRecords = allRecords.stream()
                    .limit(previewCount)
                    .toList();

            String ksjhmc = yjxhMapper.selectKsjhmcByKsjhdm(ksjhdm);
            DbfImportResponseVO response = DbfImportResponseVO.createSuccessResponse(
                    file.getOriginalFilename(), file.getSize(), ksjhdm, ksjhmc,
                    dbfParserService.extractSubjectFromFileName(file.getOriginalFilename()),
                    startTime);

            response.setMessage("预览模式 - 仅显示前" + previewCount + "条记录");
            response.setSuccessPreview(previewRecords.stream()
                    .map(this::convertToPreviewMap)
                    .collect(Collectors.toList()));

            return response;

        } catch (Exception e) {
            log.error("预览DBF文件失败: {}", e.getMessage(), e);
            String ksjhmc = null;
            try {
                ksjhmc = yjxhMapper.selectKsjhmcByKsjhdm(ksjhdm);
            } catch (Exception ex) {
                log.warn("获取考试计划名称失败: {}", ex.getMessage());
            }
            return DbfImportResponseVO.createFailureResponse(
                    "预览失败: " + e.getMessage(),
                    file.getOriginalFilename(), file.getSize(), ksjhdm, ksjhmc, startTime);
        }
    }

    @Override
    public DbfImportResponseVO getImportProgress(String taskId) {
        return importProgressCache.get(taskId);
    }

    @Override
    public boolean cancelImportTask(String taskId) {
        CompletableFuture<DbfImportResponseVO> task = importTasks.get(taskId);
        if (task != null && !task.isDone()) {
            boolean cancelled = task.cancel(true);
            if (cancelled) {
                importTasks.remove(taskId);
                importProgressCache.remove(taskId);
                log.info("导入任务已取消: {}", taskId);
            }
            return cancelled;
        }
        return false;
    }

    @Override
    public String debugDbfFile(MultipartFile file, String ksjhdm) {
        return DbfDebugUtil.debugDbfFile(file, ksjhdm);
    }

    /**
     * 根据DBF记录批次按需获取阅卷序号映射关系（分批查询优化）
     */
    private Map<String, YjxhEntity> getYjxhMappingByBatch(List<DbfRecordDTO> batch, String ksjhdm, String kmmc) {
        Set<String> yjxhSet = batch.stream()
                .map(DbfRecordDTO::getYjxh)
                .collect(Collectors.toSet());

        List<String> yjxhList = new ArrayList<>(yjxhSet);
        List<YjxhEntity> entities = yjxhMapper.selectByYjxhList(yjxhList, ksjhdm, kmmc);

        return entities.stream()
                .collect(Collectors.toMap(YjxhEntity::getYjxh, entity -> entity));
    }

    /**
     * 处理记录，分类为有效记录和失败记录（分批查询优化版本）
     */
    private ProcessResult processRecords(List<DbfRecordDTO> dbfRecords, String ksjhdm, String kmmc) {
        // 分批处理记录，避免一次性加载所有阅卷序号映射
        List<List<DbfRecordDTO>> batches = partitionList(dbfRecords, batchSize);

        List<DbfRecordDTO> allValidRecords = new ArrayList<>();
        List<FailedRecordVO> allFailedRecords = new ArrayList<>();
        int totalYjxhNotFoundCount = 0;
        int totalNameMismatchCount = 0;

        for (List<DbfRecordDTO> batch : batches) {
            // 为当前批次获取阅卷序号映射
            Map<String, YjxhEntity> yjxhMap = getYjxhMappingByBatch(batch, ksjhdm, kmmc);

            // 处理当前批次（改为普通for循环，5w条<1秒）
            List<ProcessedRecord> processedRecords = new ArrayList<>(batch.size());
            for (DbfRecordDTO record : batch) {
                ProcessedRecord processed = processRecord(record, yjxhMap);
                processedRecords.add(processed);
            }

            // 汇总当前批次结果
            for (ProcessedRecord processed : processedRecords) {
                if (processed.isValid()) {
                    allValidRecords.add(processed.getRecord());
                }
                if (processed.getFailedRecord() != null) {
                    allFailedRecords.add(processed.getFailedRecord());
                    if (processed.isYjxhNotFound()) {
                        totalYjxhNotFoundCount++;
                    } else if (processed.isNameMismatch()) {
                        totalNameMismatchCount++;
                    }
                }
            }
        }

        return new ProcessResult(allValidRecords, allFailedRecords, totalYjxhNotFoundCount, totalNameMismatchCount);
    }

    /**
     * 处理单条记录
     */
    private ProcessedRecord processRecord(DbfRecordDTO record, Map<String, YjxhEntity> yjxhMap) {
        YjxhEntity yjxhEntity = yjxhMap.get(record.getYjxh());

        if (yjxhEntity == null) {
            // 阅卷序号不存在 - 统计错误且不导入
            return new ProcessedRecord(record, false, FailedRecordVO.createYjxhNotFound(record), true, false);
        } else if (!yjxhEntity.getKsxm().equals(record.getKsxm())) {
            // 姓名不匹配 - 统计错误但仍要导入成绩数据
            record.setRealKsh(yjxhEntity.getKsh());
            return new ProcessedRecord(record, true, FailedRecordVO.createNameMismatch(record, yjxhEntity), false,
                    true);
        } else {
            // 验证通过，设置真实考生号
            record.setRealKsh(yjxhEntity.getKsh());
            return new ProcessedRecord(record, true, null, false, false);
        }
    }

    /**
     * 批量插入成绩数据（覆盖模式）- 多线程并发处理，预查询KSJHMC和KMLX
     */
// 请将 DbfImportServiceImpl.java 文件中的 batchInsertScores 方法完整替换为以下代码
//    private void batchInsertScores(List<DbfRecordDTO> validRecords, String ksjhdm) {
//        // ==================== 【性能分析代码-阶段3.1】 ====================
//        long methodStartTime = System.currentTimeMillis();
//        long phaseStartTime = methodStartTime;
//        log.info("【性能分析-阶段3】进入 batchInsertScores 方法，待处理记录数: {}", validRecords.size());
//        // ===============================================================
//
//        // 预查询KSJHMC和KMLX，避免在每个批次中重复查询
//        String ksjhmc = null;
//        Integer kmlx = null;
//        try {
//            ksjhmc = yjxhMapper.selectKsjhmcByKsjhdm(ksjhdm);
//            if (!validRecords.isEmpty()) {
//                String yjxh = validRecords.get(0).getYjxh();
//                YjxhEntity yjxhEntity = yjxhMapper.selectByYjxhAndKsjhdm(yjxh, ksjhdm);
//                if (yjxhEntity != null) {
//                    kmlx = yjxhEntity.getKmlx();
//                } else {
//                    kmlx = 0; // 默认为合格性考试
//                }
//            }
//        } catch (Exception e) {
//            log.warn("预查询KSJHMC或KMLX失败: {}", e.getMessage());
//            kmlx = 0; // 默认为合格性考试
//        }
//
//        // --- 批量删除阶段 ---
////        try {
////            log.info("【性能分析-阶段3.2】开始 -> 批量删除旧数据...");
////            List<String> allKshList = validRecords.stream()
////                    .map(DbfRecordDTO::getRealKsh)
////                    .distinct()
////                    .collect(Collectors.toList());
////
////            String kmmc = validRecords.get(0).getKmmc();
////
////            // 分批删除，每批1000条
////            List<List<String>> kshBatches = partitionList(allKshList, 1000);
////            int totalDeleted = 0;
////
////            for (List<String> batch : kshBatches) {
////                log.info("正在删除旧数据，当前批次大小: {}", batch.size());
////                int deletedCount = kscjMapper.deleteByKshListAndKsjhdmAndKmmc(batch, ksjhdm, kmmc);
////                totalDeleted += deletedCount;
////            }
////
////            log.info("分批删除完成，总删除记录数: {}, 分{}批执行", totalDeleted, kshBatches.size());
////        } catch (Exception e) {
////            log.warn("批量删除失败: {}", e.getMessage());
////        }
//
//        // ==================== 【性能分析代码-阶段3.2】 ====================
//        long currentTime = System.currentTimeMillis();
//        log.info("【性能分析-阶段3.2】结束 -> 批量删除完成。耗时: {} 毫秒", currentTime - phaseStartTime);
//        phaseStartTime = currentTime; // 重置阶段计时器
//        // ===============================================================
//
//        // --- 批量插入阶段 ---
//        log.info("【性能分析-阶段3.3】开始 -> 并发批量插入新数据...");
//        List<List<DbfRecordDTO>> batches = partitionList(validRecords, batchSize);
//
//        // 用于统计处理进度
//        AtomicInteger completedBatches = new AtomicInteger(0);
//        AtomicInteger successCount = new AtomicInteger(0);
//        AtomicInteger failureCount = new AtomicInteger(0);
//
//        // 创建CompletableFuture任务列表
//        List<CompletableFuture<BatchResult>> futures = new ArrayList<>();
//        final String finalKsjhmc = ksjhmc;
//        final Integer finalKmlx = kmlx;
//
//        for (int i = 0; i < batches.size(); i++) {
//            final int batchIndex = i;
//            final List<DbfRecordDTO> batch = batches.get(i);
//
//            log.info("提交批次 {}/{} 进行处理，记录数: {}", batchIndex + 1, batches.size(), batch.size());
//            CompletableFuture<BatchResult> future = CompletableFuture.supplyAsync(
//                    () -> processBatch(batch, batchIndex + 1, batches.size(), ksjhdm, completedBatches, successCount,
//                            failureCount, finalKsjhmc, finalKmlx),
//                    getThreadPoolExecutor());
//
//            futures.add(future);
//        }
//
//        // 等待所有批次处理完成
//        try {
//            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
//            allOf.get(); // 等待所有任务完成
//
//            // 统计最终结果
//            int totalSuccess = successCount.get();
//            int totalFailure = failureCount.get();
//
//            log.debug("批量插入完成！成功: {}, 失败: {}", totalSuccess, totalFailure);
//
//        } catch (InterruptedException | ExecutionException e) {
//            log.error("批量插入过程中发生异常", e);
//            Thread.currentThread().interrupt();
//            throw new RuntimeException("批量插入失败: " + e.getMessage(), e);
//        }
//
//        // ==================== 【性能分析代码-阶段3.3】 ====================
//        currentTime = System.currentTimeMillis();
//        log.info("【性能分析-阶段3.3】结束 -> 并发批量插入完成。耗时: {} 毫秒", currentTime - phaseStartTime);
//        log.info("【性能分析-阶段3】方法 batchInsertScores 总耗时: {} 毫秒", currentTime - methodStartTime);
//        // ===============================================================
//    }

    private void batchInsertScores(List<DbfRecordDTO> validRecords, String ksjhdm) {
        long methodStartTime = System.currentTimeMillis();
        log.info("【性能分析-阶段3】进入 batchInsertScores [高性能模式]，待处理记录数: {}", validRecords.size());

        // 预查询KSJHMC和KMLX (逻辑保留)
        String ksjhmc = yjxhMapper.selectKsjhmcByKsjhdm(ksjhdm);
        Integer kmlx = 0;
        if (!validRecords.isEmpty()) {
            try {
                YjxhEntity yjxhEntity = yjxhMapper.selectByYjxhAndKsjhdm(validRecords.get(0).getYjxh(), ksjhdm);
                if (yjxhEntity != null) { kmlx = yjxhEntity.getKmlx(); }
            } catch (Exception e) {
                log.warn("查询KMLX失败，使用默认值0。错误: {}", e.getMessage());
            }
        }
        final String finalKsjhmc = ksjhmc;
        final Integer finalKmlx = kmlx;

        // 使用TransactionTemplate确保整个ETL过程的原子性
        transactionTemplate.execute(status -> {
            try {
                long phaseStartTime = System.currentTimeMillis();

                // 步骤1：清空临时表
                log.info("【性能分析-阶段3.1】开始 -> 清空临时表...");
                kscjMapper.clearTempImportTable();
                log.info("【性能分析-阶段3.1】结束 -> 临时表已清空。耗时: {} ms", System.currentTimeMillis() - phaseStartTime);
                phaseStartTime = System.currentTimeMillis();

                // 步骤2：全量数据高速写入临时表
                log.info("【性能分析-阶段3.2】开始 -> 批量插入数据到临时表...");
                List<KscjEntity> allEntities = validRecords.stream()
                        .map(record -> convertToKscjEntity(record, finalKsjhmc, finalKmlx))
                        .collect(Collectors.toList());

                List<List<KscjEntity>> batches = partitionList(allEntities, batchSize); // batchSize来自yml配置
                for (int i = 0; i < batches.size(); i++) {
                    kscjMapper.batchInsertIntoTempTable(batches.get(i));
                }
                log.info("【性能分析-阶段3.2】结束 -> 所有数据已插入临时表。耗时: {} ms", System.currentTimeMillis() - phaseStartTime);
                phaseStartTime = System.currentTimeMillis();

                // 步骤3：执行一次性的MERGE操作
                log.info("【性能分析-阶段3.3】开始 -> 从临时表MERGE数据到目标表...");
                int affectedRows = kscjMapper.mergeFromTempTable();
                log.info("【性能分析-阶段3.3】结束 -> MERGE操作完成，影响行数: {}。耗时: {} ms", affectedRows, System.currentTimeMillis() - phaseStartTime);

            } catch (Exception e) {
                log.error("高性能导入过程中发生严重错误，事务将回滚!", e);
                status.setRollbackOnly();
                throw new RuntimeException("导入失败: " + e.getMessage(), e);
            }
            return null;
        });

        log.info("【性能分析-阶段3】方法 batchInsertScores [高性能模式] 总耗时: {} ms", System.currentTimeMillis() - methodStartTime);
    }

    /**
     * 处理单个批次的数据（使用预查询的KSJHMC和KMLX）
     */
    private BatchResult processBatch(List<DbfRecordDTO> batch, int batchIndex, int totalBatches,
                                     String ksjhdm, AtomicInteger completedBatches,
                                     AtomicInteger successCount, AtomicInteger failureCount,
                                     String ksjhmc, Integer kmlx) {
        long startTime = System.currentTimeMillis();

        // 使用TransactionTemplate执行独立事务
        return transactionTemplate.execute(status -> {
            try {
                log.info("开始处理批次 {}/{}，记录数: {}", batchIndex, totalBatches, batch.size());
                // 转换为KscjEntity对象
                List<KscjEntity> kscjEntities = batch.stream()
                        .map(record -> convertToKscjEntity(record, ksjhmc, kmlx))
                        .collect(Collectors.toList());

                // 批量插入新记录（使用优化版本）
                int insertedCount = kscjMapper.batchInsertOptimized(kscjEntities);

                // 更新统计信息
                successCount.addAndGet(insertedCount);
                completedBatches.incrementAndGet();

                long duration = System.currentTimeMillis() - startTime;
                log.info("批次{}插入完成，插入{}条记录", batchIndex, insertedCount);
                return new BatchResult(batchIndex, insertedCount, 0, duration);

            } catch (Exception e) {
                failureCount.addAndGet(batch.size());
                completedBatches.incrementAndGet();
                log.error("批次{}处理失败: {}", batchIndex, e.getMessage());
                status.setRollbackOnly();
                return new BatchResult(batchIndex, 0, batch.size(), System.currentTimeMillis() - startTime);
            }
        });
    }

    /**
     * 将DbfRecordDTO转换为KscjEntity
     */
    private KscjEntity convertToKscjEntity(DbfRecordDTO record, String ksjhmc, Integer kmlx) {
        KscjEntity entity = new KscjEntity();
        entity.setKsh(record.getRealKsh());
        entity.setYjxh(record.getYjxh());
        entity.setFslkscj(record.getZcj()); // zcj -> fslkscj
        entity.setCjfx1(record.getKgtcj()); // kgtcj -> cjfx1
        entity.setCjfx2(record.getZgtcj()); // zgtcj -> cjfx2
        entity.setKsjhdm(record.getKsjhdm());
        entity.setKmmc(record.getKmmc());

        // 直接使用传入的KSJHMC，避免重复查询
        entity.setKsjhmc(ksjhmc);

        // 设置科目类型
        entity.setKmlx(kmlx);

        // 根据FSLKSCJ设置开考类型名称：0为缺考，非0为正考
        if (record.getZcj() != null && record.getZcj().equals(0)) {
            entity.setKklxmc("缺考");
        } else {
            entity.setKklxmc("正考");
        }

        return entity;
    }

    /**
     * 转换为预览数据格式
     */
    private StudentDataVO convertToPreviewMap(DbfRecordDTO record) {
        return StudentDataVO.builder()
                .ksh(record.getRealKsh())
                .ksxm(record.getKsxm())
                .kmmc(record.getKmmc())
                .fslkscj(record.getZcj() != null ? new BigDecimal(record.getZcj()) : null)
                .ksjhdm(record.getKsjhdm())
                .build();
    }

    /**
     * 将列表分割为指定大小的批次
     */
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }

    /**
     * 批次处理结果
     */
    @Getter
    private static class BatchResult {
        private final int batchIndex;
        private final int successCount;
        private final int failureCount;
        private final long duration;

        public BatchResult(int batchIndex, int successCount, int failureCount, long duration) {
            this.batchIndex = batchIndex;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.duration = duration;
        }

    }

    /**
     * 处理结果内部类
     */
    @Getter
    private static class ProcessResult {
        private final List<DbfRecordDTO> validRecords;
        private final List<FailedRecordVO> failedRecords;
        private final int yjxhNotFoundCount;
        private final int nameMismatchCount;

        public ProcessResult(List<DbfRecordDTO> validRecords, List<FailedRecordVO> failedRecords,
                             int yjxhNotFoundCount, int nameMismatchCount) {
            this.validRecords = validRecords;
            this.failedRecords = failedRecords;
            this.yjxhNotFoundCount = yjxhNotFoundCount;
            this.nameMismatchCount = nameMismatchCount;
        }

    }

    /**
     * 单条记录处理结果封装类
     */
    private static class ProcessedRecord {
        private final DbfRecordDTO record;
        private final boolean valid;
        private final FailedRecordVO failedRecord;
        private final boolean yjxhNotFound;
        private final boolean nameMismatch;

        public ProcessedRecord(DbfRecordDTO record, boolean valid, FailedRecordVO failedRecord,
                               boolean yjxhNotFound, boolean nameMismatch) {
            this.record = record;
            this.valid = valid;
            this.failedRecord = failedRecord;
            this.yjxhNotFound = yjxhNotFound;
            this.nameMismatch = nameMismatch;
        }

        public DbfRecordDTO getRecord() {
            return record;
        }

        public boolean isValid() {
            return valid;
        }

        public FailedRecordVO getFailedRecord() {
            return failedRecord;
        }

        public boolean isYjxhNotFound() {
            return yjxhNotFound;
        }

        public boolean isNameMismatch() {
            return nameMismatch;
        }
    }
}