package calypsox.tk.anacredit.api.reportstyle;

import calypsox.tk.anacredit.api.AnacreditConstants;
import calypsox.tk.anacredit.api.copys.Copy4AColumns;
import calypsox.tk.anacredit.api.copys.Copy4ARecord;
import calypsox.tk.anacredit.api.copys.Copy4Columns;
import calypsox.tk.anacredit.api.copys.Copy4Record;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportStyle;

import java.security.InvalidParameterException;
import java.util.Vector;

public class AnacreditCopy4ReportStyle extends ReportStyle {
    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
        Object rowData = row.getProperty(AnacreditConstants.COPY_4);
        if (rowData instanceof Copy4Record) {
            Copy4Record record = (Copy4Record) rowData;
            if (record != null) {
                if (AnacreditReportStyle.LINE.equals(columnName)) {
                    return record.getLine();
                }  else {
                    for ( Copy4Columns col : Copy4Columns.values()) {
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
