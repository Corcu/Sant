package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Vector;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.ControlMErrorLogger;

import com.calypso.tk.core.Attributes;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.report.BondReport;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMarketData;

@SuppressWarnings("serial")
public class ExpTLM_BondPricesReport extends BondReport {

	public static final String FEED_NAME = "Feed Name for TLM";
	private static final String QUOTENAME_DOMAIN_STRING = "Quote Set Name";

	@SuppressWarnings("unchecked")
	@Override
	public ReportOutput load(@SuppressWarnings("rawtypes") final Vector errorMsgsP) {

		Vector<QuoteValue> vQuotes = new Vector<QuoteValue>();
		final DefaultReportOutput output = new StandardReportOutput(this);
		final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();
		final DSConnection dsConn = getDSConnection();
		final RemoteMarketData remoteMarketData = dsConn.getRemoteMarketData();
		String feed = "";
		String price_type = "";

		// Get attributes
		final ReportTemplate reportTemp = getReportTemplate();
		final Attributes attributes = reportTemp.getAttributes();

		// ValueDate
		JDate jdate = reportTemp.getValDate();

		// Feed
		if (null == attributes.get(FEED_NAME)) {
			Log.error(this, "ExpTLM_BondPricesReport - FEED field is blank");
			ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");
			return null;
		} else {
			feed = attributes.get(FEED_NAME).toString();
		}

		// Price type
		if (null == attributes.get(QUOTENAME_DOMAIN_STRING)) {
			Log.error(this, "ExpTLM_BondPricesReport - QUOTE_SET field is blank");
			ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");
			return null;
		} else {
			price_type = attributes.get(QUOTENAME_DOMAIN_STRING).toString();
		}

		// Main process
		try {

			// get quote values
			final StringBuilder sb = new StringBuilder("quote_set_name = ").append(Util.string2SQLString(price_type))
					.append(" and quote_name like 'Bond%'  and TRUNC(quote_date) = ")
					.append(Util.date2SQLString(jdate)).append(" and close_quote is not NULL");

			vQuotes = remoteMarketData.getQuoteValues(sb.toString());

			if (vQuotes != null) {
				for (int i = 0; i < vQuotes.size(); i++) {

					final Vector<ExpTLM_BondPricesItem> expTLM_BondPricesItem = ExpTLM_BondPricesLogic.getReportRows(
							vQuotes.get(i), jdate.toString(), feed, price_type, dsConn, errorMsgsP);

					for (int j = 0; j < expTLM_BondPricesItem.size(); j++) {
						final ReportRow repRow = new ReportRow(expTLM_BondPricesItem.get(j));
						reportRows.add(repRow);
					}
				}
			}
			output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
			return output;
		} catch (final RemoteException e) {
			Log.error(this, "ExpTLM_BondPricesReport - " + e.getMessage());
			Log.error(this, e); //sonar
			ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");
		}

		return null;
	}
}
