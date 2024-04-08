package calypsox.tk.report;

import calypsox.util.collateral.SantCollateralConfigUtil;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.ReportRow;

import java.util.Vector;

/**
 * ReportStyle class for the custom MarginCallConfig report
 *
 * @author aela
 */
@SuppressWarnings("rawtypes")
public class SantMCConfigReportStyle extends CollateralConfigReportStyle {

    public static final String EXTRACTION_DATE = "Extraction Date";

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    public Object getColumnValue(ReportRow row, String columnName, Vector errors) {

        String value = SantCollateralConfigUtil.overrideBookAndContractDirectionReportColumnValue(row, columnName, this);
        if (!Util.isEmpty(value)) {
            return value;
        }

        if (EXTRACTION_DATE.equals(columnName)) {
            return row.getProperty(EXTRACTION_DATE);
        }
        return super.getColumnValue(row, columnName, errors);
    }
}
