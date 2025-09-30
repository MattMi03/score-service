package edu.qhjy.score_service.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.domain.dto.GradeBookQueryDTO;
import edu.qhjy.score_service.domain.vo.GradeBookVO;
import edu.qhjy.score_service.mapper.primary.YjxhMapper;
import edu.qhjy.score_service.service.GradeBookPdfService;
import edu.qhjy.score_service.service.GradeBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 成绩等第册PDF生成服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeBookPdfServiceImpl implements GradeBookPdfService {

    // PDF页面设置（横向A4）
    private static final com.lowagie.text.Rectangle PAGE_SIZE = PageSize.A4.rotate();
    private static final float MARGIN_LEFT = 50f;
    private static final float MARGIN_RIGHT = 50f;
    private static final float MARGIN_TOP = 50f;
    private static final float MARGIN_BOTTOM = 50f;
    // 每页显示的数据行数（根据A4纸大小调整）
    private static final int ROWS_PER_PAGE = 25;
    // 字体大小
    private static final float TITLE_FONT_SIZE = 16f;
    private static final float HEADER_FONT_SIZE = 12f;
    private static final float TABLE_FONT_SIZE = 9f;
    private final GradeBookService gradeBookService;
    private final YjxhMapper yjxhMapper;

    @Override
    public ByteArrayOutputStream generateGradeBookPdf(GradeBookQueryDTO queryDTO) throws Exception {
        log.info("开始生成成绩等第册PDF，查询条件：{}", queryDTO);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PAGE_SIZE, MARGIN_LEFT, MARGIN_RIGHT, MARGIN_TOP, MARGIN_BOTTOM);

        try (document) {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // 提前查询考试计划名称，避免在每页都查询
            String ksjhmc = null;
            try {
                ksjhmc = yjxhMapper.selectKsjhmcByKsjhdm(queryDTO.getKsjhdm());
            } catch (Exception e) {
                log.warn("查询考试计划名称失败，ksjhdm: {}, 错误: {}", queryDTO.getKsjhdm(), e.getMessage());
            }

            // 获取所有数据（不分页）
            GradeBookQueryDTO allDataQuery = new GradeBookQueryDTO();
            allDataQuery.setKsjhdm(queryDTO.getKsjhdm());
            allDataQuery.setSchool(queryDTO.getSchool());
            allDataQuery.setPageNum(1);
            allDataQuery.setPageSize(10000); // 设置一个较大的值获取所有数据

            PageResult<GradeBookVO> result = gradeBookService.queryGradeBook(allDataQuery);

            if (result.getRecords().isEmpty()) {
                throw new RuntimeException("未查询到成绩等第册数据");
            }

            GradeBookVO gradeBookVO = result.getRecords().get(0);
            List<GradeBookVO.StudentGradeData> studentDataList = gradeBookVO.getStudentData();

            log.info("查询到学生数据总数：{}", studentDataList.size());

            // 分页处理数据
            int totalPages = (int) Math.ceil((double) studentDataList.size() / ROWS_PER_PAGE);

            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                if (pageIndex > 0) {
                    document.newPage();
                }

                // 添加页面内容，传入已查询的考试计划名称
                addPageContent(document, gradeBookVO, studentDataList, pageIndex, totalPages, queryDTO.getKsjhdm(),
                        ksjhmc);
            }

            log.info("成绩等第册PDF生成完成，总页数：{}", totalPages);

        }

        return baos;
    }

    /**
     * 添加页面内容
     */
    private void addPageContent(Document document, GradeBookVO gradeBookVO,
                                List<GradeBookVO.StudentGradeData> allStudentData,
                                int pageIndex, int totalPages, String ksjhdm, String ksjhmc) throws Exception {

        // 获取当前页的数据
        int startIndex = pageIndex * ROWS_PER_PAGE;
        int endIndex = Math.min(startIndex + ROWS_PER_PAGE, allStudentData.size());
        List<GradeBookVO.StudentGradeData> pageData = allStudentData.subList(startIndex, endIndex);

        // 添加标题和页眉信息
        addTitleAndHeader(document, gradeBookVO, ksjhdm, ksjhmc, pageIndex + 1, totalPages);

        // 添加表格
        addDataTable(document, pageData, startIndex);
    }

    /**
     * 添加标题和页眉信息
     */
    private void addTitleAndHeader(Document document, GradeBookVO gradeBookVO,
                                   String ksjhdm, String ksjhmc, int currentPage, int totalPages) throws Exception {

        // 创建中文字体
        BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        com.lowagie.text.Font titleFont = new com.lowagie.text.Font(baseFont, TITLE_FONT_SIZE,
                com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font headerFont = new com.lowagie.text.Font(baseFont, HEADER_FONT_SIZE,
                com.lowagie.text.Font.NORMAL);

        // 标题：使用传入的ksjhmc，拼接为"ksjhmc+等级册"
        String title = (ksjhmc != null && !ksjhmc.trim().isEmpty()) ? ksjhmc + "等级册" : ksjhdm + "等级册";
        Paragraph titleParagraph = new Paragraph(title, titleFont);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(10f);
        document.add(titleParagraph);

        // 页眉信息：考区、学校、打印日期
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/M/d"));

        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(15f);

        // 考区信息
        PdfPCell areaCell = new PdfPCell(
                new Phrase("考区：" + (gradeBookVO.getKqmc() != null ? gradeBookVO.getKqmc() : ""), headerFont));
        areaCell.setBorder(Rectangle.NO_BORDER);
        areaCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        headerTable.addCell(areaCell);

        // 学校信息
        PdfPCell schoolCell = new PdfPCell(
                new Phrase("学校：" + (gradeBookVO.getXxmc() != null ? gradeBookVO.getXxmc() : ""), headerFont));
        schoolCell.setBorder(Rectangle.NO_BORDER);
        schoolCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerTable.addCell(schoolCell);

        // 打印日期
        PdfPCell dateCell = new PdfPCell(new Phrase(currentDate, headerFont));
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTable.addCell(dateCell);

        document.add(headerTable);
    }

    /**
     * 添加数据表格
     */
    private void addDataTable(Document document, List<GradeBookVO.StudentGradeData> studentData, int startIndex)
            throws Exception {

        // 创建中文字体
        BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        com.lowagie.text.Font tableFont = new com.lowagie.text.Font(baseFont, TABLE_FONT_SIZE,
                com.lowagie.text.Font.NORMAL);
        com.lowagie.text.Font headerFont = new com.lowagie.text.Font(baseFont, TABLE_FONT_SIZE,
                com.lowagie.text.Font.BOLD);

        // 创建表格（6列：序号、考籍号、姓名、身份证件号、性别、考试科目及等级）
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);

        // 设置列宽比例
        float[] columnWidths = {8f, 20f, 12f, 25f, 8f, 27f};
        table.setWidths(columnWidths);

        // 添加表头
        addTableHeader(table, headerFont);

        // 添加数据行
        for (int i = 0; i < studentData.size(); i++) {
            GradeBookVO.StudentGradeData student = studentData.get(i);
            addTableRow(table, student, startIndex + i + 1, tableFont);
        }

        document.add(table);
    }

    /**
     * 添加表头
     */
    private void addTableHeader(PdfPTable table, Font headerFont) {
        String[] headers = {"序号", "考籍号", "姓名", "身份证件号", "性别", "考试科目及等级"};

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5f);
            cell.setBackgroundColor(new Color(240, 240, 240));
            table.addCell(cell);
        }
    }

    /**
     * 添加数据行
     */
    private void addTableRow(PdfPTable table, GradeBookVO.StudentGradeData student, int rowNumber, Font tableFont) {

        // 序号
        PdfPCell numberCell = new PdfPCell(new Phrase(String.valueOf(rowNumber), tableFont));
        numberCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        numberCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        numberCell.setPadding(3f);
        table.addCell(numberCell);

        // 考籍号
        PdfPCell kshCell = new PdfPCell(new Phrase(student.getKsh() != null ? student.getKsh() : "", tableFont));
        kshCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        kshCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        kshCell.setPadding(3f);
        table.addCell(kshCell);

        // 姓名
        PdfPCell nameCell = new PdfPCell(new Phrase(student.getXm() != null ? student.getXm() : "", tableFont));
        nameCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        nameCell.setPadding(3f);
        table.addCell(nameCell);

        // 身份证件号
        PdfPCell idCell = new PdfPCell(new Phrase(student.getSfzjh() != null ? student.getSfzjh() : "", tableFont));
        idCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        idCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        idCell.setPadding(3f);
        table.addCell(idCell);

        // 性别
        PdfPCell genderCell = new PdfPCell(new Phrase(student.getXb() != null ? student.getXb() : "", tableFont));
        genderCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        genderCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        genderCell.setPadding(3f);
        table.addCell(genderCell);

        // 考试科目及等级
        PdfPCell scoresCell = new PdfPCell(
                new Phrase(student.getScores() != null ? student.getScores() : "", tableFont));
        scoresCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        scoresCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        scoresCell.setPadding(3f);
        table.addCell(scoresCell);
    }

    @Override
    public String generatePdfFileName(String ksjhdm, String xxmc) {
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 清理学校名称中的特殊字符
        String cleanSchoolName = xxmc != null ? xxmc.replaceAll("[\\\\/:*?\"<>|]", "") : "";
        return ksjhdm + cleanSchoolName + "等级册" + currentDate + "版.pdf";
    }
}