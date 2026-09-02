package haitai.ht_ax_hackathon.service;

import haitai.ht_ax_hackathon.dto.AdminJudgeProgressRow;
import haitai.ht_ax_hackathon.dto.ExcelDownload;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PageMargin;
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

@Service
@RequiredArgsConstructor
public class JudgeProgressExcelExportService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AdminFinalResultService finalResultService;

    public ExcelDownload createDownload() {
        List<AdminJudgeProgressRow> rows = finalResultService.findJudgeProgress();
        int targetCount = rows.isEmpty() ? 0 : rows.get(0).totalCount();
        int totalAssignments = rows.size() * targetCount;
        int completedAssignments = rows.stream().mapToInt(AdminJudgeProgressRow::completedCount).sum();
        int remainingAssignments = Math.max(0, totalAssignments - completedAssignments);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Styles styles = createStyles(workbook);
            Sheet sheet = workbook.createSheet("심사 진행 현황");
            configureSheet(sheet);

            createMergedRow(sheet, 0, 0, 6, "HT AX 해커톤 심사 진행 현황", styles.title(), 34);
            createMergedRow(
                    sheet,
                    1,
                    0,
                    6,
                    "다운로드 일시  " + LocalDateTime.now().format(DATE_TIME_FORMAT)
                            + "   |   완료된 평가만 진행 건수에 반영됩니다.",
                    styles.subtitle(),
                    22
            );

            createSummary(sheet, 3, 0, "심사 참여 인원", rows.size() + "명", styles.summaryLabel(), styles.summaryJudge());
            createSummary(sheet, 3, 2, "전체 심사 건", totalAssignments + "건", styles.summaryLabel(), styles.summaryTotal());
            createSummary(sheet, 3, 4, "완료", completedAssignments + "건", styles.summaryLabel(), styles.summaryComplete());
            createSummarySingleColumn(sheet, 3, 6, "미진행", remainingAssignments + "건", styles.summaryLabel(), styles.summaryRemaining());

            int headerRowIndex = 6;
            Row header = sheet.createRow(headerRowIndex);
            header.setHeightInPoints(25);
            String[] headers = {"No.", "심사관", "완료 (팀)", "전체 (팀)", "미진행 (팀)", "진행률", "상태"};
            for (int column = 0; column < headers.length; column++) {
                writeCell(header, column, headers[column], styles.header());
            }

            int rowIndex = headerRowIndex + 1;
            for (int index = 0; index < rows.size(); index++) {
                AdminJudgeProgressRow progress = rows.get(index);
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(24);
                CellStyle body = index % 2 == 0 ? styles.body() : styles.alternateBody();

                writeCell(row, 0, index + 1, body);
                writeCell(row, 1, progress.judgeName(), styles.nameBody());
                writeCell(row, 2, progress.completedCount(), body);
                writeCell(row, 3, progress.totalCount(), body);
                writeCell(row, 4, progress.remainingCount(), body);
                writeProgressCell(row, 5, progress.progressRate(), styles.progress());
                writeCell(row, 6, progress.statusLabel(), statusStyle(progress, styles));
            }

            if (rows.isEmpty()) {
                createMergedRow(sheet, rowIndex++, 0, 6, "심사 참여 인원이 없습니다.", styles.empty(), 28);
            } else {
                sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, rowIndex - 1, 0, 6));
            }
            sheet.setPrintGridlines(false);
            sheet.getPrintSetup().setLandscape(true);
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);

            workbook.write(outputStream);
            return new ExcelDownload(
                    outputStream.toByteArray(),
                    "HT AX 해커톤 심사 진행 현황_" + LocalDate.now().format(FILE_DATE_FORMAT) + ".xlsx"
            );
        } catch (IOException exception) {
            throw new IllegalStateException("심사 진행 현황 엑셀 생성에 실패했습니다.", exception);
        }
    }

    private void configureSheet(Sheet sheet) {
        sheet.setDisplayGridlines(false);
        int[] widths = {8, 26, 15, 15, 15, 15, 15};
        for (int column = 0; column < widths.length; column++) {
            sheet.setColumnWidth(column, widths[column] * 256);
        }
        sheet.setMargin(PageMargin.LEFT, 0.3);
        sheet.setMargin(PageMargin.RIGHT, 0.3);
    }

    private void createSummary(
            Sheet sheet,
            int rowIndex,
            int firstColumn,
            String label,
            String value,
            CellStyle labelStyle,
            CellStyle valueStyle
    ) {
        mergeCells(sheet, rowIndex, rowIndex, firstColumn, firstColumn + 1, label, labelStyle, 20);
        mergeCells(sheet, rowIndex + 1, rowIndex + 1, firstColumn, firstColumn + 1, value, valueStyle, 28);
    }

    private void createSummarySingleColumn(
            Sheet sheet,
            int rowIndex,
            int column,
            String label,
            String value,
            CellStyle labelStyle,
            CellStyle valueStyle
    ) {
        writeSummaryCell(sheet, rowIndex, column, label, labelStyle, 20);
        writeSummaryCell(sheet, rowIndex + 1, column, value, valueStyle, 28);
    }

    private void writeSummaryCell(
            Sheet sheet,
            int rowIndex,
            int column,
            String value,
            CellStyle style,
            int height
    ) {
        Row row = sheet.getRow(rowIndex) == null ? sheet.createRow(rowIndex) : sheet.getRow(rowIndex);
        row.setHeightInPoints(height);
        writeCell(row, column, value, style);
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
        mergeCells(sheet, rowIndex, rowIndex, firstColumn, lastColumn, value, style, height);
    }

    private void mergeCells(
            Sheet sheet,
            int firstRow,
            int lastRow,
            int firstColumn,
            int lastColumn,
            String value,
            CellStyle style,
            int height
    ) {
        Row row = sheet.getRow(firstRow) == null ? sheet.createRow(firstRow) : sheet.getRow(firstRow);
        row.setHeightInPoints(height);
        for (int column = firstColumn; column <= lastColumn; column++) {
            writeCell(row, column, column == firstColumn ? value : "", style);
        }
        CellRangeAddress region = new CellRangeAddress(firstRow, lastRow, firstColumn, lastColumn);
        sheet.addMergedRegion(region);
        applyRegionBorders(sheet, region);
    }

    private CellStyle statusStyle(AdminJudgeProgressRow row, Styles styles) {
        return switch (row.statusLabel()) {
            case "완료" -> styles.statusComplete();
            case "진행 중" -> styles.statusProgress();
            default -> styles.statusWaiting();
        };
    }

    private Styles createStyles(Workbook workbook) {
        CellStyle title = baseStyle(workbook, HorizontalAlignment.LEFT);
        setFillColor(title, 23, 32, 51);
        title.setFont(font(workbook, true, 16, 255, 255, 255));
        title.setIndention((short) 1);

        CellStyle subtitle = baseStyle(workbook, HorizontalAlignment.LEFT);
        setFillColor(subtitle, 246, 248, 251);
        subtitle.setFont(font(workbook, false, 9, 106, 116, 135));
        subtitle.setIndention((short) 1);

        CellStyle summaryLabel = baseStyle(workbook, HorizontalAlignment.CENTER);
        setFillColor(summaryLabel, 247, 248, 251);
        summaryLabel.setFont(font(workbook, true, 9, 111, 121, 139));

        CellStyle summaryJudge = summaryValueStyle(workbook, 245, 240, 250, 87, 63, 137);
        CellStyle summaryTotal = summaryValueStyle(workbook, 237, 244, 252, 35, 83, 139);
        CellStyle summaryComplete = summaryValueStyle(workbook, 232, 247, 239, 19, 116, 81);
        CellStyle summaryRemaining = summaryValueStyle(workbook, 253, 246, 233, 150, 96, 13);

        CellStyle header = baseStyle(workbook, HorizontalAlignment.CENTER);
        setFillColor(header, 216, 31, 95);
        header.setFont(font(workbook, true, 10, 255, 255, 255));

        CellStyle body = baseStyle(workbook, HorizontalAlignment.CENTER);
        body.setFont(font(workbook, false, 10, 55, 65, 81));

        CellStyle alternateBody = baseStyle(workbook, HorizontalAlignment.CENTER);
        setFillColor(alternateBody, 249, 250, 252);
        alternateBody.setFont(font(workbook, false, 10, 55, 65, 81));

        CellStyle nameBody = baseStyle(workbook, HorizontalAlignment.LEFT);
        nameBody.setFont(font(workbook, true, 10, 30, 41, 59));
        nameBody.setIndention((short) 1);

        CellStyle progress = baseStyle(workbook, HorizontalAlignment.CENTER);
        setFillColor(progress, 250, 241, 245);
        progress.setFont(font(workbook, true, 10, 178, 25, 78));
        progress.setDataFormat(workbook.createDataFormat().getFormat("0%"));

        CellStyle statusComplete = statusStyle(workbook, 232, 247, 239, 19, 116, 81);
        CellStyle statusProgress = statusStyle(workbook, 253, 246, 233, 150, 96, 13);
        CellStyle statusWaiting = statusStyle(workbook, 242, 244, 247, 113, 124, 143);

        CellStyle empty = baseStyle(workbook, HorizontalAlignment.CENTER);
        setFillColor(empty, 248, 249, 251);
        empty.setFont(font(workbook, false, 10, 126, 136, 153));

        return new Styles(
                title, subtitle, summaryLabel, summaryJudge, summaryTotal, summaryComplete,
                summaryRemaining, header, body, alternateBody, nameBody, progress,
                statusComplete, statusProgress, statusWaiting, empty
        );
    }

    private CellStyle summaryValueStyle(
            Workbook workbook,
            int fillRed,
            int fillGreen,
            int fillBlue,
            int fontRed,
            int fontGreen,
            int fontBlue
    ) {
        CellStyle style = baseStyle(workbook, HorizontalAlignment.CENTER);
        setFillColor(style, fillRed, fillGreen, fillBlue);
        style.setFont(font(workbook, true, 15, fontRed, fontGreen, fontBlue));
        return style;
    }

    private CellStyle statusStyle(
            Workbook workbook,
            int fillRed,
            int fillGreen,
            int fillBlue,
            int fontRed,
            int fontGreen,
            int fontBlue
    ) {
        CellStyle style = baseStyle(workbook, HorizontalAlignment.CENTER);
        setFillColor(style, fillRed, fillGreen, fillBlue);
        style.setFont(font(workbook, true, 10, fontRed, fontGreen, fontBlue));
        return style;
    }

    private CellStyle baseStyle(Workbook workbook, HorizontalAlignment alignment) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private Font font(
            Workbook workbook,
            boolean bold,
            int size,
            int red,
            int green,
            int blue
    ) {
        Font font = workbook.createFont();
        font.setBold(bold);
        font.setFontHeightInPoints((short) size);
        ((XSSFFont) font).setColor(new XSSFColor(
                new byte[]{(byte) red, (byte) green, (byte) blue},
                null
        ));
        return font;
    }

    private void setFillColor(CellStyle style, int red, int green, int blue) {
        ((XSSFCellStyle) style).setFillForegroundColor(new XSSFColor(
                new byte[]{(byte) red, (byte) green, (byte) blue},
                null
        ));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    private void applyRegionBorders(Sheet sheet, CellRangeAddress region) {
        RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
        RegionUtil.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex(), region, sheet);
        RegionUtil.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex(), region, sheet);
        RegionUtil.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex(), region, sheet);
        RegionUtil.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex(), region, sheet);
    }

    private void writeProgressCell(Row row, int columnIndex, double value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
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

    private record Styles(
            CellStyle title,
            CellStyle subtitle,
            CellStyle summaryLabel,
            CellStyle summaryJudge,
            CellStyle summaryTotal,
            CellStyle summaryComplete,
            CellStyle summaryRemaining,
            CellStyle header,
            CellStyle body,
            CellStyle alternateBody,
            CellStyle nameBody,
            CellStyle progress,
            CellStyle statusComplete,
            CellStyle statusProgress,
            CellStyle statusWaiting,
            CellStyle empty
    ) {
    }
}
