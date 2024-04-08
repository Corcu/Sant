package calypsox.tk.report;

import com.calypso.tk.report.ReportRow;

import java.security.InvalidParameterException;
import java.util.Vector;

/**
 * @author acd
 */
public class BODisponibleTransferPositionReportStyle extends BODisponibleSecurityPositionReportStyle {
    public static final String FROM_TO = "From-To";

    @Override
    public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
        if(FROM_TO.equalsIgnoreCase(columnId)){
            return null!=row ? row.getProperty("SelectedOption") : "";
        }

        return super.getColumnValue(row, columnId, errors);
    }

}

