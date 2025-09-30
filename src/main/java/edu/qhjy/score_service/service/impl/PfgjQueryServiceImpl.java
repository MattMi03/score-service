package edu.qhjy.score_service.service.impl;

import edu.qhjy.score_service.domain.dto.PfgjQueryRequestDTO;
import edu.qhjy.score_service.domain.entity.PfgjEntity;
import edu.qhjy.score_service.domain.entity.YjxhEntity;
import edu.qhjy.score_service.domain.vo.PfgjQueryResultVO;
import edu.qhjy.score_service.mapper.primary.PfgjMapper;
import edu.qhjy.score_service.mapper.primary.YjxhMapper;
import edu.qhjy.score_service.service.PfgjQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 评分轨迹查询服务实现类
 *
 * @author dadalv
 * @since 2025-08-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PfgjQueryServiceImpl implements PfgjQueryService {

    private final YjxhMapper yjxhMapper;
    private final PfgjMapper pfgjMapper;

    @Override
    public List<PfgjQueryResultVO> queryPfgjData(PfgjQueryRequestDTO request) {
        // 参数验证
        if (request == null) {
            throw new IllegalArgumentException("查询请求参数不能为空");
        }
        if (request.getKsjhdm() == null || request.getKsjhdm().trim().isEmpty()) {
            throw new IllegalArgumentException("考试计划代码不能为空");
        }
        if (request.getKsh() == null || request.getKsh().trim().isEmpty()) {
            throw new IllegalArgumentException("考生号不能为空");
        }
        if (request.getKmmc() == null || request.getKmmc().trim().isEmpty()) {
            throw new IllegalArgumentException("科目名称不能为空");
        }

        log.info("开始查询评分轨迹数据，参数：ksjhdm={}, ksh={}, kmmc={}",
                request.getKsjhdm(), request.getKsh(), request.getKmmc());

        long startTime = System.currentTimeMillis();

        try {
            // 第一步：根据ksjhdm、ksh、kmmc查询yjxh
            YjxhEntity yjxhEntity = yjxhMapper.selectByKsjhdmAndKshAndKmmc(
                    request.getKsjhdm().trim(), request.getKsh().trim(), request.getKmmc().trim());

            if (yjxhEntity == null) {
                log.info("未找到对应的阅卷序号，参数：ksjhdm={}, ksh={}, kmmc={}",
                        request.getKsjhdm(), request.getKsh(), request.getKmmc());
                return new ArrayList<>();
            }

            log.debug("查询到阅卷序号：{}", yjxhEntity.getYjxh());

            // 第二步：根据yjxh和ksjhdm查询评分轨迹数据
            String yjxh = yjxhEntity.getYjxh();
            if (yjxh == null || yjxh.trim().isEmpty()) {
                log.warn("阅卷序号为空，无法查询评分轨迹数据");
                return new ArrayList<>();
            }

            List<PfgjEntity> pfgjList = pfgjMapper.selectByYjxhAndKsjhdm(yjxh.trim(), request.getKsjhdm().trim());

            if (CollectionUtils.isEmpty(pfgjList)) {
                log.info("未找到对应的评分轨迹数据，yjxh={}, ksjhdm={}", yjxh, request.getKsjhdm());
                return new ArrayList<>();
            }

            // 第三步：转换为VO对象
            List<PfgjQueryResultVO> resultList = pfgjList.stream()
                    .map(this::convertToVO)
                    .filter(Objects::nonNull) // 过滤掉转换失败的记录
                    .collect(Collectors.toList());

            long endTime = System.currentTimeMillis();
            log.info("查询评分轨迹数据完成，共查询到{}条记录，耗时：{}ms",
                    resultList.size(), endTime - startTime);

            return resultList;

        } catch (IllegalArgumentException e) {
            // 参数异常直接抛出，不包装
            throw e;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("查询评分轨迹数据失败，参数：ksjhdm={}, ksh={}, kmmc={}, 耗时：{}ms",
                    request.getKsjhdm(), request.getKsh(), request.getKmmc(),
                    endTime - startTime, e);
            throw new RuntimeException("查询评分轨迹数据失败：" + e.getMessage(), e);
        }
    }

    /**
     * 将PfgjEntity转换为PfgjQueryResultVO
     *
     * @param entity 评分轨迹实体
     * @return 评分轨迹查询结果VO
     */
    private PfgjQueryResultVO convertToVO(PfgjEntity entity) {
        if (entity == null) {
            log.warn("评分轨迹实体为空，跳过转换");
            return null;
        }

        try {
            PfgjQueryResultVO vo = new PfgjQueryResultVO();
            vo.setPfgjbs(entity.getPfgjbs());
            vo.setLsh(entity.getLsh());
            vo.setItemid(entity.getItemid());
            vo.setMh(entity.getMh());
            vo.setMarksum(entity.getMarksum());
            vo.setSubmark(entity.getSubmark());
            vo.setTasktype(entity.getTasktype());
            vo.setTeacherid(entity.getTeacherid());
            return vo;
        } catch (Exception e) {
            log.error("转换评分轨迹实体到VO失败，entity={}", entity, e);
            return null;
        }
    }
}