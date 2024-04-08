package calypsox.tk.anacredit.api.reportstyle;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.api.copys.*;

import calypsox.tk.report.AnacreditOperacionesReportTemplate;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

import java.security.InvalidParameterException;
import java.util.Vector;

public class AnacreditReportStyle  extends ReportStyle {
    public static final String LINE = "LINE";

    public static final String[] DEFAULT_COLUMNS = {LINE};
    private AnacreditCopy3ReportStyle copy3ReportStyle = new AnacreditCopy3ReportStyle();
    private AnacreditCopy4ReportStyle copy4ReportStyle = new AnacreditCopy4ReportStyle();
    private AnacreditCopy4AReportStyle copy4AReportStyle = new AnacreditCopy4AReportStyle();
    private AnacreditCopy11ReportStyle copy11ReportStyle = new AnacreditCopy11ReportStyle();
    private AnacreditCopy13ReportStyle copy13ReportStyle = new AnacreditCopy13ReportStyle();

    @Override
    public String[] getDefaultColumns() {
        return DEFAULT_COLUMNS;
    }

    public TreeList getTreeList() {
        TreeList treeList = super.getTreeList();
        for (Copy3Columns col : Copy3Columns.values()) {
            treeList.add("Anacredit.Copy3",col.name());
        }
        for (Copy4Columns col : Copy4Columns.values()) {
            treeList.add("Anacredit.Copy4",col.name());
        }
        for (Copy4AColumns col : Copy4AColumns.values()) {
            treeList.add("Anacredit.Copy4A",col.name());
        }
        for (Copy11Columns col : Copy11Columns.values()) {
            treeList.add("Anacredit.Copy11",col.name());
        }
        for (Copy13Columns col : Copy13Columns.values()) {
            treeList.add("Anacredit.Copy13",col.name());
        }
        return  treeList;
    }


    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {

        if ((row == null)) {
            return null;
        }

        String rowDataType = row.getProperty("ROW_DATA_TYPE");

        if (Util.isEmpty(rowDataType)) {
            rowDataType = "Copy3";
        }

        if (row.getProperty(rowDataType) == null) {
            return null;
        }

        if (AnacreditConstants.COPY_3.equalsIgnoreCase(rowDataType)) {
            return copy3ReportStyle.getColumnValue(row, columnName, errors );
        }
        if (AnacreditConstants.COPY_4.equalsIgnoreCase(rowDataType)) {
            return copy4ReportStyle.getColumnValue(row, columnName, errors );
        }
        if (AnacreditConstants.COPY_4A.equalsIgnoreCase(rowDataType)) {
            return copy4AReportStyle.getColumnValue(row, columnName, errors );
        }
        if (AnacreditConstants.COPY_11.equalsIgnoreCase(rowDataType)) {
            return copy11ReportStyle.getColumnValue(row, columnName, errors );
        }
        if (AnacreditConstants.COPY_13.equalsIgnoreCase(rowDataType)) {
            return copy13ReportStyle.getColumnValue(row, columnName, errors );
        }
        /*
        if (property != null && property instanceof Copy3Record) {
            return copy3ReportStyle.getColumnValue(row, columnName, errors );
        }
        if (property != null && property instanceof Copy4Record) {
            return copy4ReportStyle.getColumnValue(row, columnName, errors );
        }
        if (property != null && property instanceof Copy4ARecord) {
            return copy4AReportStyle.getColumnValue(row, columnName, errors );
        }
         */
        return null;
    }
}
