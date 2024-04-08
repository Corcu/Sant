package calypsox.tk.anacredit.api.reportstyle;

import calypsox.tk.anacredit.api.copys.Copy3Columns;
import calypsox.tk.anacredit.api.copys.Copy3Record;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

import java.security.InvalidParameterException;
import java.util.Vector;

public class AnacreditCopy3ReportStyle extends ReportStyle {


    public static final String[] DEFAULT_COLUMNS = {AnacreditReportStyle.LINE};

    @Override
    public String[] getDefaultColumns() {
        return DEFAULT_COLUMNS;
    }

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
        Object result = null;

        String rowDataType = row.getProperty("ROW_DATA_TYPE");

        if (row.getProperty(rowDataType) instanceof Copy3Record) {
            Copy3Record record = row.getProperty(rowDataType);
            if (record != null) {
                if (AnacreditReportStyle.LINE.equals(columnName)) {
                    result = record.getLine();
                }  else {
                    for ( Copy3Columns col : Copy3Columns.values()) {
                        if (columnName.equals(col.name())) {
                            result = record.getValue(col);
                        }
                    }
                }
            }
        }
        return  result;

    }
}
