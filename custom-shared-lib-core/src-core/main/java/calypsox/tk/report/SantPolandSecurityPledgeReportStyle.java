package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

/**
 * This class customizes the value shown in the column "Reverse".
 * 
 * @author Carlos Cejudo
 *
 */
public class SantPolandSecurityPledgeReportStyle extends TradeReportStyle {

    public static final String COLUMN_NAME_REVERSE = "Reverse";

    /**
     * Serial Version UID
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object getColumnValue(ReportRow row, String columnId,
            Vector errors) {
        Object columnValue = null;

        if (COLUMN_NAME_REVERSE.equals(columnId)) {
            Object rawProperty = row.getProperty(
                    SantPolandSecurityPledgeReport.PROPERTY_REVERSE_MARK);
            if (rawProperty != null) {
                columnValue = rawProperty;
            } else {
                columnValue = Boolean.FALSE;
            }
        } else {
            columnValue = super.getColumnValue(row, columnId, errors);
        }

        return columnValue;
    }

}
