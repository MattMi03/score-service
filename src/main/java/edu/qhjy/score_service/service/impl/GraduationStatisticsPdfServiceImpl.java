package edu.qhjy.score_service.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.dto.GraduationStatisticsQueryDTO;
import edu.qhjy.score_service.domain.vo.GraduationStatisticsVO;
import edu.qhjy.score_service.service.GraduationStatisticsPdfService;
import edu.qhjy.score_service.service.GraduationStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 毕业生统计表PDF生成服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraduationStatisticsPdfServiceImpl implements GraduationStatisticsPdfService {

    // PDF页面设置（纵向A4）
    private static final com.lowagie.text.Rectangle PAGE_SIZE = PageSize.A4;
    private static final float MARGIN_LEFT = 50f;
    private static final float MARGIN_RIGHT = 50f;
    private static final float MARGIN_TOP = 50f;
    private static final float MARGIN_BOTTOM = 50f;
    // 字体大小
    private static final float TITLE_FONT_SIZE = 18f;
    private static final float HEADER_FONT_SIZE = 14f;
    private static final float TABLE_FONT_SIZE = 12f;
    private final GraduationStatisticsService graduationStatisticsService;

    @Override
    public ByteArrayOutputStream generateGraduationStatisticsPdf(GraduationStatisticsQueryDTO queryDTO) throws Exception {
        log.info("开始生成毕业生统计表PDF，查询条件：{}", queryDTO);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PAGE_SIZE, MARGIN_LEFT, MARGIN_RIGHT, MARGIN_TOP, MARGIN_BOTTOM);

        try (document) {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // 获取统计数据
            Result<GraduationStatisticsVO> result = graduationStatisticsService.getGraduationStatistics(queryDTO);

            if (result.getCode() == null || !result.getCode().equals(200) || result.getData() == null) {
                throw new RuntimeException("未查询到毕业生统计数据：" + result.getMessage());
            }

            GraduationStatisticsVO statisticsVO = result.getData();

            log.info("查询到毕业生统计数据，共{}个市州", statisticsVO.getGraduationStudentStatics().size());

            // 添加标题
            addTitle(document, queryDTO);

            // 添加统计表格
            addStatisticsTable(document, statisticsVO);

            // 添加汇总信息
            addSummaryInfo(document, statisticsVO);

            log.info("毕业生统计表PDF生成完成");

        }

        return baos;
    }

    /**
     * 添加标题
     */
    private void addTitle(Document document, GraduationStatisticsQueryDTO queryDTO) throws DocumentException {
        // 创建中文字体
        BaseFont baseFont;
        try {
            baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            log.warn("无法加载中文字体，使用默认字体");
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            } catch (Exception ex) {
                log.error("创建默认字体失败", ex);
                throw new RuntimeException("字体创建失败", ex);
            }
        }

        Font titleFont = new Font(baseFont, TITLE_FONT_SIZE, Font.BOLD);

        // 构建标题文本
        String titleText = queryDTO.getBynd() + "年度" + queryDTO.getRxnd() + "级毕业生统计表";

        Paragraph title = new Paragraph(titleText, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20f);

        document.add(title);
    }

    /**
     * 添加统计表格
     */
    private void addStatisticsTable(Document document, GraduationStatisticsVO statisticsVO) throws DocumentException {
        // 创建中文字体
        BaseFont baseFont;
        try {
            baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            log.warn("无法加载中文字体，使用默认字体");
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            } catch (Exception ex) {
                log.error("创建默认字体失败", ex);
                throw new RuntimeException("字体创建失败", ex);
            }
        }

        Font headerFont = new Font(baseFont, HEADER_FONT_SIZE, Font.BOLD);
        Font tableFont = new Font(baseFont, TABLE_FONT_SIZE, Font.NORMAL);

        // 创建表格（4列：毕业年度、级别、地市、人数）
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingAfter(20f);

        // 设置列宽
        float[] columnWidths = {20f, 20f, 40f, 20f};
        table.setWidths(columnWidths);

        // 添加表头
        addTableHeader(table, headerFont);

        // 添加数据行
        List<GraduationStatisticsVO.CityStatistics> cityStatistics = statisticsVO.getGraduationStudentStatics();
        for (GraduationStatisticsVO.CityStatistics cityStats : cityStatistics) {
            addTableRow(table, statisticsVO, cityStats, tableFont);
        }

        document.add(table);
    }

    /**
     * 添加表头
     */
    private void addTableHeader(PdfPTable table, Font headerFont) {
        String[] headers = {"毕业年度", "级别", "地市", "人数"};

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(8f);
            cell.setBackgroundColor(new Color(240, 240, 240));
            table.addCell(cell);
        }
    }

    /**
     * 添加数据行
     */
    private void addTableRow(PdfPTable table, GraduationStatisticsVO statisticsVO,
                             GraduationStatisticsVO.CityStatistics cityStats, Font tableFont) {
        // 毕业年度
        PdfPCell byndCell = new PdfPCell(new Phrase(statisticsVO.getBynd() != null ? statisticsVO.getBynd().toString() : "", tableFont));
        byndCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        byndCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        byndCell.setPadding(5f);
        table.addCell(byndCell);

        // 级别
        PdfPCell rxndCell = new PdfPCell(new Phrase(statisticsVO.getRxnd() != null ? statisticsVO.getRxnd().toString() : "", tableFont));
        rxndCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        rxndCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        rxndCell.setPadding(5f);
        table.addCell(rxndCell);

        // 地市
        PdfPCell cityCell = new PdfPCell(new Phrase(cityStats.getSzsmc() != null ? cityStats.getSzsmc() : "", tableFont));
        cityCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cityCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cityCell.setPadding(5f);
        table.addCell(cityCell);

        // 人数
        PdfPCell countCell = new PdfPCell(new Phrase(cityStats.getTotalCount() != null ? cityStats.getTotalCount().toString() : "0", tableFont));
        countCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        countCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        countCell.setPadding(5f);
        table.addCell(countCell);
    }

    /**
     * 添加汇总信息
     */
    private void addSummaryInfo(Document document, GraduationStatisticsVO statisticsVO) throws DocumentException {
        // 创建中文字体
        BaseFont baseFont;
        try {
            baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            log.warn("无法加载中文字体，使用默认字体");
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            } catch (Exception ex) {
                log.error("创建默认字体失败", ex);
                throw new RuntimeException("字体创建失败", ex);
            }
        }

        Font summaryFont = new Font(baseFont, HEADER_FONT_SIZE, Font.BOLD);

        // 添加汇总表格
        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(20f);

        // 设置列宽，与上方表格保持一致
        float[] summaryWidths = {20f, 20f, 40f, 20f};
        summaryTable.setWidths(summaryWidths);

        // 毕业年度合计行
        PdfPCell byndLabelCell = new PdfPCell(new Phrase((statisticsVO.getBynd() != null ? statisticsVO.getBynd().toString() : "") + "年度合计", summaryFont));
        byndLabelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        byndLabelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        byndLabelCell.setPadding(8f);
        byndLabelCell.setBackgroundColor(new Color(240, 240, 240));
        byndLabelCell.setColspan(3); // 跨越前三列
        summaryTable.addCell(byndLabelCell);

        PdfPCell byndCountCell = new PdfPCell(new Phrase(statisticsVO.getByndTotalCount() != null ? statisticsVO.getByndTotalCount().toString() : "0", summaryFont));
        byndCountCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        byndCountCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        byndCountCell.setPadding(8f);
        summaryTable.addCell(byndCountCell);

        // 级别合计行
        PdfPCell rxndLabelCell = new PdfPCell(new Phrase((statisticsVO.getRxnd() != null ? statisticsVO.getRxnd().toString() : "") + "级合计", summaryFont));
        rxndLabelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        rxndLabelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        rxndLabelCell.setPadding(8f);
        rxndLabelCell.setBackgroundColor(new Color(240, 240, 240));
        rxndLabelCell.setColspan(3); // 跨越前三列
        summaryTable.addCell(rxndLabelCell);

        PdfPCell rxndCountCell = new PdfPCell(new Phrase(statisticsVO.getRxndTotalCount() != null ? statisticsVO.getRxndTotalCount().toString() : "0", summaryFont));
        rxndCountCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        rxndCountCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        rxndCountCell.setPadding(8f);
        summaryTable.addCell(rxndCountCell);

        document.add(summaryTable);

        // 添加生成时间
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"));
        Paragraph dateInfo = new Paragraph("生成时间：" + currentDate, new Font(baseFont, 10f, Font.NORMAL));
        dateInfo.setAlignment(Element.ALIGN_RIGHT);
        dateInfo.setSpacingBefore(30f);
        document.add(dateInfo);
    }

    @Override
    public String generatePdfFileName(GraduationStatisticsQueryDTO queryDTO) {
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return queryDTO.getBynd().toString() + "年度" + queryDTO.getRxnd() + "级毕业生统计表" + currentDate + "版.pdf";
    }
}