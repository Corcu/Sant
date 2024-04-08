/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Tenor;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Bond;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.util.Aggregation;

public class EligibleProductReportStyle extends com.calypso.tk.report.EligibleProductReportStyle {

	private static final long serialVersionUID = 7424060259834197985L;

	static final public String REMAIN_MATURITY = "Remaining Maturity";

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(ReportRow row, String columnId, Vector errors) {
		if (columnId.equals(REMAIN_MATURITY)) {
			if (row == null) {
				return null;
			}

			Product product = getProduct(row);
			if ((product != null) && (product instanceof Bond)) {
				Bond bond = (Bond) product;
				JDate matDate = bond.getMaturityDate();
				if (matDate == null) {
					return "NA";
				}

				JDatetime valDatetime = (JDatetime) row.getProperty(ReportRow.VALUATION_DATETIME);
				if (valDatetime == null) {
					valDatetime = new JDatetime();
				}
				JDate valDate = null;
				PricingEnv env = (PricingEnv) row.getProperty(ReportRow.PRICING_ENV);
				if (env != null) {
					valDate = valDatetime.getJDate(env.getTimeZone());
				} else {
					valDate = valDatetime.getJDate(TimeZone.getDefault());
				}

				if (valDate == null) {
					Log.error("BondReportStyle", "null valDate passed in getValue");
					valDate = JDate.getNow();
				}
				if (matDate.before(valDate)) {
					return "Matured";
				}
				Vector buckets = Aggregation.getTenorsFromAggStr(columnId);
				if (buckets == null) {
					JDate endDt = valDate.addTenor(new Tenor(360));
					if (matDate.before(endDt)) {
						return "0D-1Y";
					}
					int i = 0;
					while (matDate.gte(endDt)) {
						endDt = endDt.addYears(1);
						i++;
					}
					return Integer.toString(i) + "Y-" + Integer.toString(i + 1) + "Y";
				}
				String start = "0D";
				String end = "";
				for (int i = 0; i < buckets.size(); i++) {
					Tenor tenor = new Tenor((String) buckets.elementAt(i));
					JDate endDt = valDate.addTenor(tenor);
					if (matDate.before(endDt)) {
						end = tenor.toString();
						break;
					}
					if (matDate.gte(endDt)) {
						start = tenor.toString();
					}
				}
				return start + "-" + end;
			}

			@SuppressWarnings("unused")
			Trade trade = (Trade) row.getProperty(ReportRow.TRADE);
		}

		return super.getColumnValue(row, columnId, errors);
	}
}
