/**
 *
 */
package calypsox.tk.report;

import calypsox.util.SantReportFormattingUtil;
import com.calypso.tk.core.*;
import com.calypso.tk.report.*;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author aela
 *
 */
@SuppressWarnings("rawtypes")
public class SantExcelReportViewer implements ReportViewer {
    private Workbook wb = null;
    private Sheet sheet = null;
    private Map<String, CellStyle> styles = null;
    private int rowNum = 0;
    protected DataFormat format = null;
    private String sheetName = null;

    @Override
    public void init(DefaultReportOutput output, ReportTemplate template, ReportView view, boolean forceInit) {
        this.rowNum = 0;
        this.wb = new XSSFWorkbook();
        this.styles = createStyles(this.wb);

        if (this.sheetName != null) {
            this.sheet = this.wb.createSheet(this.sheetName);
        } else {
            this.sheet = this.wb.createSheet("Underlyings");
        }
        PrintSetup printSetup = this.sheet.getPrintSetup();
        printSetup.setLandscape(true);
        this.sheet.setFitToPage(true);
        this.sheet.setHorizontallyCenter(true);

    }

    /**
     * @param sheetName
     *            the sheetName to set
     */
    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    @Override
    public void setHeading(String[] headings) {
        Row headerRow = this.sheet.createRow(this.rowNum++);
        Cell headerCell;
        for (int i = 0; i < headings.length; i++) {
            headerCell = headerRow.createCell(i);
            headerCell.setCellValue(headings[i]);
            headerCell.setCellStyle(this.styles.get("header"));
        }
    }

    @Override
    public void setSubheading(ReportRow row, Object[] subheadings, Vector comparatorKeys) {
    }

    @Override
    public void setRow(ReportRow row, Object[] data) {
        Row xslRow = this.sheet.createRow(this.rowNum++);
        // xslRow.setHeightInPoints(40);
        Cell rowCell;
        for (int i = 0; i < data.length; i++) {
            rowCell = xslRow.createCell(i);
            setCellValueAndStyle(data[i], rowCell);
        }
    }

    @Override
    public void setTotals(Object[] data, String title) {
    }

    @Override
    public void setSubtotals(Object[] data, Vector comparatorKeys, int depth) {
    }

    @Override
    public void setTrailer(Object[] data, String title) {
        // here we will add the processing post excel creation
        if (this.sheet.getRow(0) != null) {
            for (int i = 0; i < this.sheet.getRow(0).getLastCellNum(); i++) {
                this.sheet.autoSizeColumn(i);

            }
        }
    }

    @Override
    public void addFilter(ReportViewerFilter filter) {
    }

    @Override
    public void removeFilter(ReportViewerFilter filter) {
    }

    @Override
    public boolean accept(ReportRow row) {
        return true;
    }

    /**
     * Create a library of cell styles
     */
    private Map<String, CellStyle> createStyles(Workbook wb) {
        Map<String, CellStyle> styles = new HashMap<String, CellStyle>();
        CellStyle style;
        Font titleFont = wb.createFont();
        titleFont.setFontHeightInPoints((short) 10);
        titleFont.setBold(true);
        style = wb.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFont(titleFont);
        styles.put("title", style);

        Font headerFont = wb.createFont();
        headerFont.setFontHeightInPoints((short) 10);
        headerFont.setColor(IndexedColors.BLACK.getIndex());
        headerFont.setBold(true);
        headerFont.setFontName(HSSFFont.FONT_ARIAL);

        style = wb.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());

        // style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        // style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setFont(headerFont);
        style.setWrapText(true);
        styles.put("header", style);

        style = wb.createCellStyle();
        Font cellFont = wb.createFont();
        cellFont.setFontHeightInPoints((short) 10);
        cellFont.setColor(IndexedColors.BLACK.getIndex());
        cellFont.setFontName(HSSFFont.FONT_ARIAL);

        style.setWrapText(true);
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setFont(cellFont);

        styles.put("cell", style);

        this.format = wb.createDataFormat();

        CellStyle styleNumber = wb.createCellStyle();
        styleNumber.cloneStyleFrom(style);
        CellStyle styleInteger = wb.createCellStyle();
        styleInteger.cloneStyleFrom(style);
        CellStyle styleDate = wb.createCellStyle();
        styleDate.cloneStyleFrom(style);
        CellStyle styleDatetime = wb.createCellStyle();
        styleDatetime.cloneStyleFrom(style);

        styleNumber.setDataFormat(this.format.getFormat("#,##0.00"));
        styleInteger.setDataFormat(this.format.getFormat("#,##0"));
        // styleDate.setDataFormat(format.getFormat("0.0"));
        // styleDatetime.setDataFormat(format.getFormat("0.0"));

        styles.put("cellNumber", styleNumber);
        styles.put("cellInteger", styleInteger);
        styles.put("cellDate", styleDate);
        styles.put("cellDatetime", styleDatetime);

        return styles;
    }

    private void setCellValueAndStyle(Object value, Cell rowCell) {
        String fromattedValue = "";
        CellStyle style = this.styles.get("cell");
        value = SantReportFormattingUtil.getInstance().formatEmptyCollectionForReporting(value);
        if (value instanceof String) {
            fromattedValue = (String) value;
            rowCell.setCellValue(fromattedValue);
        } else if (value instanceof Number) {
            rowCell.setCellType(Cell.CELL_TYPE_NUMERIC);
            if (value instanceof Integer) {
                rowCell.setCellValue(((Integer) value));
                style = this.styles.get("cellInteger");
            } else if (value instanceof Long) {
                rowCell.setCellValue(((Long) value));
                style = this.styles.get("cellInteger");
            } else {
                rowCell.setCellValue(((Number) value).doubleValue());
                style = this.styles.get("cellNumber");
            }
        } else if (value instanceof JDatetime) {
            DateFormat df = Util.getDateFormatWithAppliedPattern(Locale.getDefault(), TimeZone.getDefault());
            if (df instanceof SimpleDateFormat) {
                String datePattern = ((SimpleDateFormat) df).toPattern();
                if (!Util.isEmpty(datePattern)) {
                    datePattern += " HH:mm";
                }
                style = this.styles.get("cellDatetime");
                style.setDataFormat(this.format.getFormat(datePattern));
                rowCell.setCellValue(((JDatetime) value));
            } else {
                fromattedValue = Util.dateToString((JDatetime) value);
                style = this.styles.get("cellDatetime");
            }
        } else if (value instanceof Rate) {
            rowCell.setCellType(Cell.CELL_TYPE_NUMERIC);
            rowCell.setCellValue(((Rate) value).get() * 100);
            style = this.styles.get("cellNumber");
        } else if (value instanceof BondPrice) {
            rowCell.setCellType(Cell.CELL_TYPE_NUMERIC);
            style = this.styles.get("cellNumber");
            if (((BondPrice) value).getBase() == 100) {
                rowCell.setCellValue(((BondPrice) value).get() * 100);
            } else {
                rowCell.setCellValue(((BondPrice) value).get());
            }
        } else if (value instanceof Spread) {
            rowCell.setCellType(Cell.CELL_TYPE_NUMERIC);
            style = this.styles.get("cellNumber");
            rowCell.setCellValue(((Spread) value).get());
        } else if (value instanceof DisplayValue) {
            style = this.styles.get("cellNumber");
            rowCell.setCellValue(((DisplayValue) value).get());
        } else if (value instanceof Date) {
            DateFormat df = Util.getDateFormatWithAppliedPattern(Locale.getDefault(), TimeZone.getDefault());
            if (df instanceof SimpleDateFormat) {
                String datePattern = ((SimpleDateFormat) df).toPattern();
                style = this.styles.get("cellDate");
                style.setDataFormat(this.format.getFormat(datePattern));
                rowCell.setCellValue(((JDate) value).getDate());
            } else {
                rowCell.setCellValue(((DisplayValue) value).get());
                fromattedValue = Util.dateToString((Date) value);
                rowCell.setCellValue(fromattedValue);
            }
        } else if (value instanceof JDate) {
            DateFormat df = Util.getDateFormatWithAppliedPattern(Locale.getDefault(), TimeZone.getDefault());
            if (df instanceof SimpleDateFormat) {
                String datePattern = ((SimpleDateFormat) df).toPattern();
                style = this.styles.get("cellDate");
                style.setDataFormat(this.format.getFormat(datePattern));
                rowCell.setCellValue(((JDate) value).getDate());
            } else {
                JDatetime jdt = ((JDate) value).getJDatetime();
                fromattedValue = Util.dateToString(jdt);
                rowCell.setCellValue(fromattedValue);
            }
        }
        rowCell.setCellStyle(style);
    }

    /**
     * @return the content of the current excel file in an array of bytes.
     * @throws IOException
     */
    public byte[] getContentAsBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        this.wb.write(out);
        out.flush();
        out.close();
        return out.toByteArray();
    }
}