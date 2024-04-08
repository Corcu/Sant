package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.generic.SantGenericKPIMtmReportStyle;

import com.calypso.tk.core.Product;
import com.calypso.tk.report.ReportRow;

public class SantKPIMtmByInstrumentReportStyle extends SantGenericKPIMtmReportStyle {

	private static final long serialVersionUID = 71036578L;

	public static final String INSTRUMENT = "Instrument";

	public static final String[] DEFAULTS_COLUMNS = { REPORT_DATE, AGREEMENT_OWNER, DEAL_OWNER, INSTRUMENT,
			USD_MTM_SUM, EUR_MTM_SUM };

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		final KPIMtmReportItem item = (KPIMtmReportItem) row.getProperty(ReportRow.DEFAULT);

		if (columnName.equals(INSTRUMENT)) {
			if (item.getInstrument().equals(Product.REPO)) {
				return CollateralStaticAttributes.INSTRUMENT_TYPE_REPO;
			} else if (item.getInstrument().equals(Product.SEC_LENDING)) {
				return CollateralStaticAttributes.INSTRUMENT_TYPE_SEC_LENDING;
			} else {
				return item.getInstrument();
			}
		} else {
			return super.getColumnValue(row, columnName, errors);
		}

	}
}
