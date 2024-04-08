package calypsox.tk.report;

import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.ReportRow;

import java.util.Vector;

/**
 * @author aalonsop
 */
public class MCLyncsReportStyle extends CollateralConfigReportStyle {

    public static final String CONTRACT_CODE_CALYPSO="ContractCodeCalypso";

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {

        if(CONTRACT_CODE_CALYPSO.equals(columnName)){
            return CONTRACT_CODE_CALYPSO;
        }
        return super.getColumnValue(row, columnName, errors);
    }
}
