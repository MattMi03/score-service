package edu.qhjy.score_service.service.impl;

import edu.qhjy.score_service.domain.entity.BytjEntity;
import edu.qhjy.score_service.mapper.primary.BytjMapper;
import edu.qhjy.score_service.service.BytjService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 毕业条件设置服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BytjServiceImpl implements BytjService {

    private final BytjMapper bytjMapper;

    @Override
    public List<BytjEntity> selectAll() {
        log.info("查询所有毕业条件设置");
        try {
            List<BytjEntity> result = bytjMapper.selectAll();
            log.info("查询到{}条毕业条件设置记录", result.size());
            return result;
        } catch (Exception e) {
            log.error("查询所有毕业条件设置失败", e);
            throw new RuntimeException("查询毕业条件设置失败：" + e.getMessage(), e);
        }
    }

    @Override
    public BytjEntity selectById(Long bytjbs) {
        log.info("根据ID查询毕业条件设置，ID：{}", bytjbs);
        try {
            BytjEntity result = bytjMapper.selectById(bytjbs);
            if (result != null) {
                log.info("查询到毕业条件设置：{}", result);
            } else {
                log.warn("未找到ID为{}的毕业条件设置", bytjbs);
            }
            return result;
        } catch (Exception e) {
            log.error("根据ID查询毕业条件设置失败，ID：{}", bytjbs, e);
            throw new RuntimeException("查询毕业条件设置失败：" + e.getMessage(), e);
        }
    }

    @Override
    public int insert(BytjEntity bytjEntity) {
        log.info("新增毕业条件设置：{}", bytjEntity);
        try {
            int result = bytjMapper.insert(bytjEntity);
            log.info("新增毕业条件设置成功，影响行数：{}", result);
            return result;
        } catch (Exception e) {
            log.error("新增毕业条件设置失败", e);
            throw new RuntimeException("新增毕业条件设置失败：" + e.getMessage(), e);
        }
    }

    @Override
    public int updateById(BytjEntity bytjEntity) {
        log.info("更新毕业条件设置：{}", bytjEntity);
        try {
            int result = bytjMapper.updateById(bytjEntity);
            log.info("更新毕业条件设置成功，影响行数：{}", result);
            return result;
        } catch (Exception e) {
            log.error("更新毕业条件设置失败", e);
            throw new RuntimeException("更新毕业条件设置失败：" + e.getMessage(), e);
        }
    }

    @Override
    public int deleteById(Long bytjbs) {
        log.info("删除毕业条件设置，ID：{}", bytjbs);
        try {
            int result = bytjMapper.deleteById(bytjbs);
            log.info("删除毕业条件设置成功，影响行数：{}", result);
            return result;
        } catch (Exception e) {
            log.error("删除毕业条件设置失败，ID：{}", bytjbs, e);
            throw new RuntimeException("删除毕业条件设置失败：" + e.getMessage(), e);
        }
    }
}