package calypsox.tk.anacredit.api.reportstyle;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.api.copys.Copy13Columns;
import calypsox.tk.anacredit.api.copys.Copy11Record;
import calypsox.tk.anacredit.api.copys.Copy13Record;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

import java.security.InvalidParameterException;
import java.util.Vector;

public class AnacreditCopy13ReportStyle extends ReportStyle {
    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
        Object rowData = row.getProperty(AnacreditConstants.COPY_13);
        if (rowData instanceof Copy13Record) {
            Copy13Record record = (Copy13Record) rowData;
            if (record != null) {
                if (AnacreditReportStyle.LINE.equals(columnName)) {
                    return record.getLine();
                }  else {
                    for ( Copy13Columns col : Copy13Columns.values()) {
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
