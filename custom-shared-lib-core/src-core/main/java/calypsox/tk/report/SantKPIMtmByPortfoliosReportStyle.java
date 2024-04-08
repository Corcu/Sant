package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.tk.report.generic.SantGenericKPIMtmReportStyle;

import com.calypso.tk.report.ReportRow;

public class SantKPIMtmByPortfoliosReportStyle extends SantGenericKPIMtmReportStyle {

    private static final long serialVersionUID = 71036578L;

    public static final String PORTFOLIO = "Portfolio";

    public static final String[] DEFAULTS_COLUMNS = { REPORT_DATE,
	    AGREEMENT_OWNER, DEAL_OWNER, PORTFOLIO, USD_MTM_SUM, EUR_MTM_SUM };

    @SuppressWarnings("rawtypes")
    @Override
    public Object getColumnValue(final ReportRow row, final String columnName,
	    final Vector errors) throws InvalidParameterException {

	final KPIMtmReportItem item = (KPIMtmReportItem) row
		.getProperty(ReportRow.DEFAULT);

	if (columnName.equals(PORTFOLIO)) {
	    return item.getPortfolio();
	} else {
	    return super.getColumnValue(row, columnName, errors);
	}

    }

}
