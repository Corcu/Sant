/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

public class SantListOfTradesWithNoMtmVariationReportStyle extends TradeReportStyle {

	private static final long serialVersionUID = -6445312121908972842L;

	public static final String PROCESS_DATE = "Process Date";
	public static final String MTM = "MtM";
	public static final String MTM_BASE = "MtM Base";
	public static final String PRODUCT = "Product";
	public static final String CONTRACT = "Contract";
	public static final String OWNER = "Owner";
	public static final String EXCLUDE = "Exclude";

	public static final String[] DEFAULT_COLUMNS = { PROCESS_DATE, CONTRACT, PRODUCT, OWNER, MTM, EXCLUDE };

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
		Trade trade = (Trade) row.getProperty(ReportRow.TRADE);
		JDate processDate = (JDate) row.getProperty("ProcessDate");
		MarginCallConfig mcc = (MarginCallConfig) row.getProperty(ReportRow.MARGIN_CALL_CONFIG);
		Double mtm = (Double) row.getProperty("MTM");
		Double mtmBase = (Double) row.getProperty("MTM_BASE");
		Boolean isExcluded = (Boolean) row.getProperty("Excluded");

		// PROCESS DATE
		if (PROCESS_DATE.equals(columnName)) {
			return processDate;
		}
		// MTM
		else if (MTM.equals(columnName)) {
			return new Amount(mtm, 2);
		}
		// MTM
		else if (MTM_BASE.equals(columnName)) {
			return new Amount(mtmBase, 2);
		}
		// PRODUCT
		else if (PRODUCT.equals(columnName)) {
			if ("CollateralExposure".equals(trade.getProductType())) {
				return trade.getProductSubType();
			}
			return trade.getProductType();
		}
		// EXCLUDE
		else if (EXCLUDE.equals(columnName)) {
			return isExcluded ? "Yes" : "No";
		}
		// OWNER
		else if (OWNER.equals(columnName)) {
			return (isExcluded && (mcc == null)) ? trade.getBook().getLegalEntity().getCode() : mcc.getProcessingOrg()
					.getCode();
		}
		// CONTRACT
		else if (CONTRACT.equals(columnName)) {
			return (isExcluded && (mcc == null)) ? trade.getCounterParty().getCode() : mcc.getName();
		}

		return super.getColumnValue(row, columnName, errors);
	}
}
