package calypsox.tk.report;

import calypsox.util.collateral.SantCollateralConfigUtil;
import com.calypso.tk.report.CollateralConfigReportStyle;
import com.calypso.tk.report.ReportRow;

import java.util.Vector;

public class SantAgreementListByISINReportStyle extends CollateralConfigReportStyle {

    private static final long serialVersionUID = 1L;

    @Override
    public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors) {

        Object value = SantCollateralConfigUtil.overrideBookAndContractDirectionReportColumnValue(row, columnName, this);
        if (value != null) {
            return value;
        }
        return super.getColumnValue(row, columnName, errors);

    }
}
