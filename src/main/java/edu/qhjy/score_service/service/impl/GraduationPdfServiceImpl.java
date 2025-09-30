package edu.qhjy.score_service.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import edu.qhjy.score_service.common.PageResult;
import edu.qhjy.score_service.common.Result;
import edu.qhjy.score_service.domain.dto.GraduationPdfQueryDTO;
import edu.qhjy.score_service.domain.dto.GraduationQueryDTO;
import edu.qhjy.score_service.domain.vo.GraduationStudentVO;
import edu.qhjy.score_service.mapper.primary.KmxxMapper;
import edu.qhjy.score_service.mapper.primary.KscjMapper;
import edu.qhjy.score_service.service.GraduationPdfService;
import edu.qhjy.score_service.service.GraduationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 毕业生花名册PDF生成服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraduationPdfServiceImpl implements GraduationPdfService {

    // PDF页面设置（横向A4）
    private static final com.lowagie.text.Rectangle PAGE_SIZE = PageSize.A4.rotate();
    private static final float MARGIN_LEFT = 30f;
    private static final float MARGIN_RIGHT = 30f;
    private static final float MARGIN_TOP = 50f;
    private static final float MARGIN_BOTTOM = 50f;
    // 每页显示的数据行数（根据A4纸大小和科目数量调整）
    private static final int ROWS_PER_PAGE = 20;
    // 字体大小
    private static final float TITLE_FONT_SIZE = 16f;
    private static final float HEADER_FONT_SIZE = 12f;
    private static final float TABLE_FONT_SIZE = 8f;
    private final GraduationService graduationService;
    private final KscjMapper kscjMapper;
    private final KmxxMapper kmxxMapper;

    @Override
    public ByteArrayOutputStream generateGraduationPdf(GraduationPdfQueryDTO queryDTO) throws Exception {
        log.info("开始生成毕业生花名册PDF，查询条件：{}", queryDTO);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PAGE_SIZE, MARGIN_LEFT, MARGIN_RIGHT, MARGIN_TOP, MARGIN_BOTTOM);

        try (document) {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // 查询毕业生数据（不分页，获取所有数据）
            List<GraduationStudentVO> graduationStudents = queryGraduationStudentsForPdf(queryDTO);

            if (CollectionUtils.isEmpty(graduationStudents)) {
                throw new RuntimeException("未查询到毕业生数据");
            }

            // 获取所有科目名称，用于构建表头
            List<String> allSubjects = getAllSubjects();

            log.info("查询到毕业生数据总数：{}，科目总数：{}", graduationStudents.size(), allSubjects.size());

            // 分页处理数据
            int totalPages = (int) Math.ceil((double) graduationStudents.size() / ROWS_PER_PAGE);

            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                if (pageIndex > 0) {
                    document.newPage();
                }

                // 添加页面内容
                addPageContent(document, graduationStudents, allSubjects, pageIndex, totalPages, queryDTO);
            }

            log.info("毕业生花名册PDF生成完成，总页数：{}", totalPages);

        }

        return baos;
    }

    /**
     * 查询毕业生数据用于PDF生成
     * 修改scores字段返回逻辑，包含所有科目
     */
    private List<GraduationStudentVO> queryGraduationStudentsForPdf(GraduationPdfQueryDTO queryDTO) {
        try {
            // 1. 转换查询条件
            GraduationQueryDTO graduationQueryDTO = convertToGraduationQueryDTO(queryDTO);

            // 2. 调用现有的毕业生查询服务获取基本数据（不分页，获取所有数据）
            graduationQueryDTO.setPageNum(1);
            graduationQueryDTO.setPageSize(Integer.MAX_VALUE);

            Result<PageResult<GraduationStudentVO>> result = graduationService
                    .queryGraduationStudents(graduationQueryDTO);
            if (result.getCode() == null || !result.getCode().equals(200) || result.getData() == null
                    || CollectionUtils.isEmpty(result.getData().getRecords())) {
                log.warn("未查询到毕业生数据：{}", result.getMessage());
                return new ArrayList<>();
            }

            List<GraduationStudentVO> students = result.getData().getRecords();

            // 3. 获取所有科目列表
            List<String> allSubjects = getAllSubjects();
            if (CollectionUtils.isEmpty(allSubjects)) {
                log.warn("未查询到科目信息");
                return students; // 返回原始数据，不修改scores
            }

            // 4. 为每个学生重新构建完整的scores Map（包含所有科目）
            for (GraduationStudentVO student : students) {
                enrichStudentScoresWithAllSubjects(student, allSubjects);
            }

            return students;

        } catch (Exception e) {
            log.error("查询毕业生数据失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 转换查询条件
     */
    private GraduationQueryDTO convertToGraduationQueryDTO(GraduationPdfQueryDTO pdfQueryDTO) {
        GraduationQueryDTO queryDTO = new GraduationQueryDTO();
        queryDTO.setSzsmc(pdfQueryDTO.getSzsmc());
        queryDTO.setKqmc(pdfQueryDTO.getKqmc());
        queryDTO.setXxmc(pdfQueryDTO.getXxmc());
        queryDTO.setBynd(pdfQueryDTO.getBynd());
        queryDTO.setKsh(pdfQueryDTO.getKsh());
        queryDTO.setIsQualified(pdfQueryDTO.getIsQualified());
        queryDTO.setSortField(pdfQueryDTO.getSortField());
        queryDTO.setSortOrder(pdfQueryDTO.getSortOrder());
        return queryDTO;
    }

    /**
     * 为学生补全所有科目的成绩信息
     * 参考OutOfProvinceScoreServiceImpl的scoresList构建逻辑
     */
    private void enrichStudentScoresWithAllSubjects(GraduationStudentVO student, List<String> allSubjects) {
        try {
            // 查询学生的实际成绩数据（参考OutOfProvinceScoreServiceImpl.getStudentInfo方法）
            List<Map<String, String>> scoresMapList = kscjMapper.selectScoresByKsh(student.getKsh());
            Map<String, String> actualScores = new HashMap<>();
            if (scoresMapList != null && !scoresMapList.isEmpty()) {
                for (Map<String, String> scoreMap : scoresMapList) {
                    String key = scoreMap.get("key");
                    String value = scoreMap.get("value");
                    if (key != null && value != null) {
                        actualScores.put(key, value);
                    }
                }
            }

            // 构建包含所有科目的scores Map
            Map<String, String> completeScores = new HashMap<>();
            for (String subject : allSubjects) {
                // 如果有成绩记录则返回CJHGM，否则返回null
                completeScores.put(subject, actualScores.get(subject));
            }

            student.setScores(completeScores);

        } catch (Exception e) {
            log.error("为学生 {} 补全科目成绩失败", student.getKsh(), e);
            // 设置空的scores Map
            student.setScores(new HashMap<>());
        }
    }

    /**
     * 获取所有科目名称
     */
    private List<String> getAllSubjects() {
        try {
            return kmxxMapper.selectAllKmmc();
        } catch (Exception e) {
            log.error("查询所有科目名称失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 添加页面内容
     */
    private void addPageContent(Document document, List<GraduationStudentVO> allStudents,
                                List<String> allSubjects, int pageIndex, int totalPages,
                                GraduationPdfQueryDTO queryDTO) throws Exception {

        // 获取当前页的数据
        int startIndex = pageIndex * ROWS_PER_PAGE;
        int endIndex = Math.min(startIndex + ROWS_PER_PAGE, allStudents.size());
        List<GraduationStudentVO> pageData = allStudents.subList(startIndex, endIndex);

        // 添加标题和页眉信息
        addTitleAndHeader(document, queryDTO, pageIndex + 1, totalPages);

        // 添加表格
        addDataTable(document, pageData, allSubjects, startIndex);
    }

    /**
     * 添加标题和页眉信息
     */
    private void addTitleAndHeader(Document document, GraduationPdfQueryDTO queryDTO,
                                   int currentPage, int totalPages) throws Exception {

        // 创建中文字体
        BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        com.lowagie.text.Font titleFont = new com.lowagie.text.Font(baseFont, TITLE_FONT_SIZE,
                com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font headerFont = new com.lowagie.text.Font(baseFont, HEADER_FONT_SIZE,
                com.lowagie.text.Font.NORMAL);

        // 标题
        String title = "毕业生花名册";
        if (queryDTO.getBynd() != null && !queryDTO.getBynd().trim().isEmpty()) {
            title = queryDTO.getBynd() + "年" + title;
        }

        Paragraph titleParagraph = new Paragraph(title, titleFont);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(10f);
        document.add(titleParagraph);

        // 页眉信息：市州、县区、学校、打印日期、页码
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/M/d"));

        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingAfter(15f);

        // 左侧信息：市州、县区、学校
        StringBuilder leftInfo = new StringBuilder();
        if (queryDTO.getSzsmc() != null && !queryDTO.getSzsmc().trim().isEmpty()) {
            leftInfo.append("市州：").append(queryDTO.getSzsmc()).append("  ");
        }
        if (queryDTO.getKqmc() != null && !queryDTO.getKqmc().trim().isEmpty()) {
            leftInfo.append("考区：").append(queryDTO.getKqmc()).append("  ");
        }
        if (queryDTO.getXxmc() != null && !queryDTO.getXxmc().trim().isEmpty()) {
            leftInfo.append("学校：").append(queryDTO.getXxmc());
        }

        PdfPCell leftCell = new PdfPCell(new Phrase(leftInfo.toString(), headerFont));
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        headerTable.addCell(leftCell);

        // 右侧信息：打印日期和页码
        String rightInfo = currentDate + "  第" + currentPage + "/" + totalPages + "页";
        PdfPCell rightCell = new PdfPCell(new Phrase(rightInfo, headerFont));
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        headerTable.addCell(rightCell);

        document.add(headerTable);
    }

    /**
     * 添加数据表格
     */
    private void addDataTable(Document document, List<GraduationStudentVO> studentData,
                              List<String> allSubjects, int startIndex) throws Exception {

        // 创建中文字体
        BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        com.lowagie.text.Font tableFont = new com.lowagie.text.Font(baseFont, TABLE_FONT_SIZE,
                com.lowagie.text.Font.NORMAL);
        com.lowagie.text.Font headerFont = new com.lowagie.text.Font(baseFont, TABLE_FONT_SIZE,
                com.lowagie.text.Font.BOLD);

        // 计算列数：毕业年度 + 考生号 + 姓名 + 性别 + 市州 + 县区 + 学校 + 考籍状态 + 各科目
        int columnCount = 8 + allSubjects.size();
        PdfPTable table = new PdfPTable(columnCount);
        table.setWidthPercentage(100);

        // 设置列宽比例（根据内容调整）
        float[] columnWidths = new float[columnCount];
        columnWidths[0] = 6f; // 毕业年度
        columnWidths[1] = 12f; // 考生号
        columnWidths[2] = 8f; // 姓名
        columnWidths[3] = 4f; // 性别
        columnWidths[4] = 8f; // 市州
        columnWidths[5] = 8f; // 县区
        columnWidths[6] = 12f; // 学校
        columnWidths[7] = 8f; // 考籍状态

        // 科目列宽度
        float subjectWidth = Math.max(3f, 60f / allSubjects.size()); // 动态计算科目列宽
        for (int i = 8; i < columnCount; i++) {
            columnWidths[i] = subjectWidth;
        }

        table.setWidths(columnWidths);

        // 添加表头
        addTableHeader(table, headerFont, allSubjects);

        // 添加数据行
        for (GraduationStudentVO student : studentData) {
            addTableRow(table, student, allSubjects, tableFont);
        }

        document.add(table);
    }

    /**
     * 添加表头
     */
    private void addTableHeader(PdfPTable table, com.lowagie.text.Font headerFont, List<String> allSubjects) {
        // 基本信息列
        String[] basicHeaders = {"毕业年度", "考生号", "姓名", "性别", "市州", "县区", "学校", "考籍状态"};

        for (String header : basicHeaders) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(3f);
            cell.setBackgroundColor(new Color(240, 240, 240));
            table.addCell(cell);
        }

        // 科目列
        for (String subject : allSubjects) {
            PdfPCell cell = new PdfPCell(new Phrase(subject, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(3f);
            cell.setBackgroundColor(new Color(240, 240, 240));
            table.addCell(cell);
        }
    }

    /**
     * 添加数据行
     */
    private void addTableRow(PdfPTable table, GraduationStudentVO student,
                             List<String> allSubjects, com.lowagie.text.Font tableFont) {

        // 毕业年度
        addTableCell(table, student.getBynd(), tableFont, Element.ALIGN_CENTER);

        // 考生号
        addTableCell(table, student.getKsh(), tableFont, Element.ALIGN_CENTER);

        // 姓名
        addTableCell(table, student.getXm(), tableFont, Element.ALIGN_CENTER);

        // 性别
        addTableCell(table, student.getXb(), tableFont, Element.ALIGN_CENTER);

        // 市州
        addTableCell(table, student.getSzsmc(), tableFont, Element.ALIGN_CENTER);

        // 县区
        addTableCell(table, student.getKqmc(), tableFont, Element.ALIGN_CENTER);

        // 学校
        addTableCell(table, student.getXxmc(), tableFont, Element.ALIGN_CENTER);

        // 考籍状态
        addTableCell(table, student.getKjztmc(), tableFont, Element.ALIGN_CENTER);

        // 各科目成绩
        Map<String, String> scores = student.getScores();
        for (String subject : allSubjects) {
            String score = scores.get(subject);
            addTableCell(table, score != null ? score : "", tableFont, Element.ALIGN_CENTER);
        }
    }

    /**
     * 添加表格单元格
     */
    private void addTableCell(PdfPTable table, String content, com.lowagie.text.Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(content != null ? content : "", font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(2f);
        table.addCell(cell);
    }

    @Override
    public String generatePdfFileName(GraduationPdfQueryDTO queryDTO) {
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        StringBuilder fileName = new StringBuilder();

        // 添加毕业年度
        if (queryDTO.getBynd() != null && !queryDTO.getBynd().trim().isEmpty()) {
            fileName.append(queryDTO.getBynd()).append("年");
        }

        // 添加所在市名称
        if (queryDTO.getSzsmc() != null && !queryDTO.getSzsmc().trim().isEmpty()) {
            String cleanSzsmc = queryDTO.getSzsmc().replaceAll("[\\\\/:*?\"<>|]", "");
            fileName.append(cleanSzsmc);
        }

        // 添加考区名称
        if (queryDTO.getKqmc() != null && !queryDTO.getKqmc().trim().isEmpty()) {
            String cleanKqmc = queryDTO.getKqmc().replaceAll("[\\\\/:*?\"<>|]", "");
            fileName.append(cleanKqmc);
        }

        // 添加学校名称
        if (queryDTO.getXxmc() != null && !queryDTO.getXxmc().trim().isEmpty()) {
            String cleanXxmc = queryDTO.getXxmc().replaceAll("[\\\\/:*?\"<>|]", "");
            fileName.append(cleanXxmc);
        }

        fileName.append("毕业生花名册").append(currentDate).append("版.pdf");

        return fileName.toString();
    }
}