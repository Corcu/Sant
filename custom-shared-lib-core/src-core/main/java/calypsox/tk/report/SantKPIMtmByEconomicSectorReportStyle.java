package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.tk.report.generic.SantGenericKPIMtmReportStyle;

import com.calypso.tk.report.ReportRow;

public class SantKPIMtmByEconomicSectorReportStyle extends
	SantGenericKPIMtmReportStyle {

    private static final long serialVersionUID = 71036578L;

    public static final String ECONOMIC_SECTOR = "Economic Sector";

    public static final String[] DEFAULTS_COLUMNS = { REPORT_DATE,
	    AGREEMENT_OWNER, DEAL_OWNER, ECONOMIC_SECTOR, USD_MTM_SUM,
	    EUR_MTM_SUM };

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(final ReportRow row, final String columnName,
	    final Vector errors) throws InvalidParameterException {

	final KPIMtmReportItem item = (KPIMtmReportItem) row
		.getProperty(ReportRow.DEFAULT);

	if (columnName.equals(ECONOMIC_SECTOR)) {
	    return item.getEconomicSector();
	} else {
	    return super.getColumnValue(row, columnName, errors);
	}

    }

}
