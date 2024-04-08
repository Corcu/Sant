package calypsox.tk.report;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.report.TransferReportStyle;

import java.security.InvalidParameterException;
import java.util.Vector;

public class STPTradeReportStyle extends TradeReportStyle {
    public static final String IS_STP = "IS_STP";
    public static final String COMMENT = "COMMENT";
    public static final String ACTION_PERFORMED = "ACTION_PERFORMED";
    public static final String RESULTING_STATUS = "RESULTING_STATUS";
    public static final String PREVIOUS_STATUS = "PREVIOUS_STATUS";
    public static final String TASK_OWNER = "TASK_OWNER";
    public static final String MODIFICATION_DATE = "MODIFICATION_DATE";

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {

        if (columnId.equals(IS_STP)) {
            return row.getProperty("STP") == null ? true : row.getProperty("STP");
        }
        if (columnId.equals(COMMENT)) {
            return row.getProperty("COMMENT");
        }
        if (columnId.equals(ACTION_PERFORMED)) {
            return row.getProperty("ACTION_PERFORMED");
        }
        if (columnId.equals(RESULTING_STATUS)) {
            return row.getProperty("RESULTING_STATUS");
        }
        if (columnId.equals(PREVIOUS_STATUS)) {
            return row.getProperty("PREVIOUS_STATUS");
        }
        if (columnId.equals(TASK_OWNER)) {
            return row.getProperty("TASK_OWNER");
        }
        if (columnId.equals(MODIFICATION_DATE)) {
            return row.getProperty("MODIFICATION_DATE");
        }
        
        return super.getColumnValue(row, columnId, errors);
    }
}
