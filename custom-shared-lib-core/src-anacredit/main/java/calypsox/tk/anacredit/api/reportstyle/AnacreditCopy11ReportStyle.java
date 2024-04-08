package calypsox.tk.anacredit.api.reportstyle;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.api.copys.Copy11Columns;
import calypsox.tk.anacredit.api.copys.Copy11Record;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

import java.security.InvalidParameterException;
import java.util.Vector;

public class AnacreditCopy11ReportStyle extends ReportStyle {
    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
        Object rowData = row.getProperty(AnacreditConstants.COPY_11);
        if (rowData instanceof Copy11Record) {
            Copy11Record record = (Copy11Record) rowData;
            if (record != null) {
                if (AnacreditReportStyle.LINE.equals(columnName)) {
                    return record.getLine();
                }  else {
                    for ( Copy11Columns col : Copy11Columns.values()) {
                        if (columnName.equals(col.name())) {
                            return record.getValue(col);
                        }
                    }
                }
            }
        }
        return null;
    }
}
