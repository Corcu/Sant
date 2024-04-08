/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.util.Vector;

import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.BOSecurityPositionReportStyle;
import com.calypso.tk.report.BOSecurityPositionReportTemplate;
import com.calypso.tk.report.ProductReportStyle;

public class SantInventoryViewReportTemplate extends BOSecurityPositionReportTemplate {

	private static final long serialVersionUID = -7477386914034429784L;

	public static final String CONTRACTS = "CONTRACTS";

	public static final String HAIRCUTS = "HAIRCUTS";

	public static final String RATING_METHOD_SNP_EQV = "RATING_METHOD_SNP_EQV";
	public static final String RATING_METHOD_MOODY_EQV = "RATING_METHOD_MOODY_EQV";
	public static final String RATING_METHOD_FITCH_EQV = "RATING_METHOD_FITCH_EQV";

	public static final String RATING_METHOD_STRICTLY_SNP = "RATING_METHOD_STRICTLY_SNP";
	public static final String RATING_METHOD_STRICTLY_MOODY = "RATING_METHOD_STRICTLY_MOODY";
	public static final String RATING_METHOD_STRICTLY_FITCH = "RATING_METHOD_STRICTLY_FITCH";
	public static final String RATING_METHOD_STRICTLY_SC = "RATING_METHOD_STRICTLY_SC";

	public static final String SECURITY_IDS = "SECURITY_IDS";

	public static final String CURRENCIES = "Currencies";
	public static final String ACC_BOOK_LIST = "AccBook";

	@Override
	public void setDefaults() {
		super.setDefaults();
		JDate now = JDate.getNow();
		put(BOSecurityPositionReportTemplate.START_DATE, Util.dateToString(now.addDays(-5)));
		put(BOSecurityPositionReportTemplate.END_DATE, Util.dateToString(now.addDays(10)));
		put(BOSecurityPositionReportTemplate.MOVE, InventorySecurityPosition.BALANCE_DEFAULT);
	}

	@Override
	protected Vector<String> getDefaultColumns() {
		Vector<String> columns = new Vector<String>();
		columns.add(BOSecurityPositionReportStyle.PO);
		columns.add(BOSecurityPositionReportStyle.CLIENT);
		columns.add(BOSecurityPositionReportStyle.BOOK);
		columns.add(BOSecurityPositionReportStyle.PRODUCT_ID);
		columns.add(ProductReportStyle.PRODUCT_CODE_PREFIX + "ISIN");
		columns.add(BOSecurityPositionReportStyle.PRODUCT_DESCRIPTION);
		columns.add(BOSecurityPositionReportStyle.CURRENCY);
		columns.add(BOSecurityPositionReportStyle.AGENT);
		columns.add(BOSecurityPositionReportStyle.ACCOUNT);
		columns.add(BOSecurityPositionReportStyle.POSITION_TYPE);
		// columns.add(SantInventoryViewReportStyle.STOCK_LENDING_RATE);
		// columns.add(SantInventoryViewReportStyle.EXP_DATE_TYPE);
		// columns.add(SantInventoryViewReportStyle.EXP_DATE);
		// columns.add(SantInventoryViewReportStyle.ACTIVE_AVAILABLE_QTY);
		// columns.add(SantInventoryViewReportStyle.QTY_ON_LOAN);
		return columns;
	}

}
