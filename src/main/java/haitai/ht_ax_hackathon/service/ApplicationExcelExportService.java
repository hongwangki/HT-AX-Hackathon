package haitai.ht_ax_hackathon.service;

import haitai.ht_ax_hackathon.domain.AttachmentFile;
import haitai.ht_ax_hackathon.domain.HackathonApplication;
import haitai.ht_ax_hackathon.domain.TeamMember;
import haitai.ht_ax_hackathon.dto.ExcelDownload;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationExcelExportService {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final HackathonApplicationService applicationService;

    public ExcelDownload createDownload() {
        List<HackathonApplication> applications = applicationService.findAllApplicationsForExport();

        try (Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Styles styles = createStyles(workbook);
            createTeamSummarySheet(workbook, applications, styles);
            workbook.write(outputStream);

            return new ExcelDownload(
                    outputStream.toByteArray(),
                    "ht-ax-hackathon-applications-" + LocalDate.now().format(FILE_DATE_FORMAT) + ".xlsx"
            );
        } catch (IOException exception) {
            throw new IllegalStateException("엑셀 파일 생성에 실패했습니다.", exception);
        }
    }

    private void createTeamSummarySheet(
            Workbook workbook,
            List<HackathonApplication> applications,
            Styles styles
    ) {
        Sheet sheet = workbook.createSheet("팀별 신청 요약");
        sheet.setDisplayGridlines(false);
        sheet.setColumnWidth(0, 14 * 256);
        sheet.setColumnWidth(1, 9 * 256);
        sheet.setColumnWidth(2, 26 * 256);
        sheet.setColumnWidth(3, 18 * 256);
        sheet.setColumnWidth(4, 18 * 256);

        createMergedRow(sheet, 0, 0, 4, "HT AX 해커톤 팀별 신청 현황", styles.title, 32);
        createMergedRow(
                sheet,
                1,
                0,
                4,
                "다운로드 일시: " + formatDateTime(LocalDateTime.now()) + "  |  총 " + applications.size() + "팀",
                styles.summary,
                20
        );

        int rowIndex = 3;
        int teamIndex = 1;
        for (HackathonApplication application : applications) {
            createMergedRow(
                    sheet,
                    rowIndex++,
                    0,
                    4,
                    teamIndex++ + "팀  |  " + application.getTeamName(),
                    styles.teamTitle,
                    26
            );
            Row memberHeader = sheet.createRow(rowIndex++);
            writeCell(memberHeader, 0, "구성인원", styles.sectionHeader);
            writeCell(memberHeader, 1, "순번", styles.header);
            writeCell(memberHeader, 2, "소속", styles.header);
            writeCell(memberHeader, 3, "사번", styles.header);
            writeCell(memberHeader, 4, "이름", styles.header);

            int memberIndex = 1;
            for (TeamMember member : application.getMembers()) {
                Row memberRow = sheet.createRow(rowIndex++);
                writeCell(memberRow, 0, "", styles.sectionHeader);
                writeCell(memberRow, 1, memberIndex++, styles.body);
                writeCell(memberRow, 2, member.getDepartment(), styles.body);
                writeCell(memberRow, 3, member.getEmployeeNo(), styles.body);
                writeCell(memberRow, 4, member.getName(), styles.body);
            }

            createLabelValueRow(sheet, rowIndex++, "주제", application.getTopic(), styles);
            createLabelValueRow(sheet, rowIndex++, "아이디어 내용", application.getContent(), styles);
            createLabelValueRow(sheet, rowIndex++, "신청일시", formatDateTime(application.getCreatedAt()), styles);
            createLabelValueRow(sheet, rowIndex++, "첨부파일", summarizeFiles(application.getFiles()), styles);
            rowIndex++;
        }
    }

    private void createMergedRow(
            Sheet sheet,
            int rowIndex,
            int firstColumn,
            int lastColumn,
            String value,
            CellStyle style,
            int height
    ) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(height);
        for (int columnIndex = firstColumn; columnIndex <= lastColumn; columnIndex++) {
            writeCell(row, columnIndex, columnIndex == firstColumn ? value : "", style);
        }
        mergeAndApplyBorders(sheet, new CellRangeAddress(rowIndex, rowIndex, firstColumn, lastColumn));
    }

    private void createLabelValueRow(Sheet sheet, int rowIndex, String label, String value, Styles styles) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(calculateRowHeight(label, value));
        writeCell(row, 0, label, styles.sectionHeader);
        for (int columnIndex = 1; columnIndex <= 4; columnIndex++) {
            writeCell(row, columnIndex, columnIndex == 1 ? value : "", styles.wrappedBody);
        }
        mergeAndApplyBorders(sheet, new CellRangeAddress(rowIndex, rowIndex, 1, 4));
    }

    private int calculateRowHeight(String label, String value) {
        int lineCount = value == null || value.isBlank() ? 1 : value.split("\\R", -1).length;
        int estimatedWrappedLines = value == null ? 1 : Math.max(1, (value.length() / 72) + 1);
        int visibleLines = Math.max(lineCount, estimatedWrappedLines);
        int minimumHeight = "아이디어 내용".equals(label) ? 42 : 22;
        return Math.max(minimumHeight, Math.min(120, 18 * visibleLines));
    }

    private void mergeAndApplyBorders(Sheet sheet, CellRangeAddress region) {
        sheet.addMergedRegion(region);
        RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
        RegionUtil.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex(), region, sheet);
        RegionUtil.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex(), region, sheet);
        RegionUtil.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex(), region, sheet);
        RegionUtil.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex(), region, sheet);
    }

    private Styles createStyles(Workbook workbook) {
        CellStyle title = workbook.createCellStyle();
        setFillColor(title, 220, 232, 248);
        title.setAlignment(HorizontalAlignment.CENTER);
        title.setVerticalAlignment(VerticalAlignment.CENTER);
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        setFontColor(titleFont, 23, 59, 112);
        titleFont.setFontHeightInPoints((short) 15);
        title.setFont(titleFont);

        CellStyle summary = workbook.createCellStyle();
        setFillColor(summary, 255, 251, 240);
        Font summaryFont = workbook.createFont();
        summaryFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        summaryFont.setFontHeightInPoints((short) 10);
        summary.setFont(summaryFont);

        CellStyle header = workbook.createCellStyle();
        setFillColor(header, 237, 244, 251);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(header);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        setFontColor(headerFont, 23, 59, 112);
        header.setFont(headerFont);

        CellStyle body = workbook.createCellStyle();
        body.setVerticalAlignment(VerticalAlignment.TOP);
        applyBorders(body);

        CellStyle wrappedBody = workbook.createCellStyle();
        wrappedBody.cloneStyleFrom(body);
        wrappedBody.setWrapText(true);

        CellStyle teamTitle = workbook.createCellStyle();
        setFillColor(teamTitle, 218, 231, 247);
        teamTitle.setVerticalAlignment(VerticalAlignment.CENTER);
        Font teamTitleFont = workbook.createFont();
        teamTitleFont.setBold(true);
        setFontColor(teamTitleFont, 23, 59, 112);
        teamTitleFont.setFontHeightInPoints((short) 12);
        teamTitle.setFont(teamTitleFont);

        CellStyle sectionHeader = workbook.createCellStyle();
        sectionHeader.cloneStyleFrom(header);
        setFillColor(sectionHeader, 226, 237, 249);
        Font sectionHeaderFont = workbook.createFont();
        sectionHeaderFont.setBold(true);
        setFontColor(sectionHeaderFont, 23, 59, 112);
        sectionHeader.setFont(sectionHeaderFont);

        return new Styles(title, summary, header, body, wrappedBody, teamTitle, sectionHeader);
    }

    private void setFillColor(CellStyle style, int red, int green, int blue) {
        ((XSSFCellStyle) style).setFillForegroundColor(new XSSFColor(
                new byte[]{(byte) red, (byte) green, (byte) blue},
                null
        ));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    private void setFontColor(Font font, int red, int green, int blue) {
        ((XSSFFont) font).setColor(new XSSFColor(
                new byte[]{(byte) red, (byte) green, (byte) blue},
                null
        ));
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    }

    private void writeCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value == null ? "" : value.toString());
        }
        cell.setCellStyle(style);
    }

    private String summarizeFiles(List<AttachmentFile> files) {
        return files.stream()
                .map(AttachmentFile::getOriginalFileName)
                .collect(Collectors.joining("\n"));
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_TIME_FORMAT);
    }

    private record Styles(
            CellStyle title,
            CellStyle summary,
            CellStyle header,
            CellStyle body,
            CellStyle wrappedBody,
            CellStyle teamTitle,
            CellStyle sectionHeader
    ) {
    }
}
