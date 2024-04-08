/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

public class SantThirdPartyInventoryViewReportTemplate extends SantInventoryViewReportTemplate {

	private static final long serialVersionUID = -7477386914034429784L;

	public static final String RATE = "RATE";

	public static final String DURATION = "DURATION";

	public static final String VALUE = "VALUE";

	// @Override
	// protected Vector<String> getDefaultColumns() {
	// Vector<String> columns = new Vector<String>();
	// columns.add("ISIN");
	// // columns.add("SEDOL");
	// // columns.add(BOSecurityPositionReportStyle.PO);
	// // columns.add(BOSecurityPositionReportStyle.PRODUCT_DESCRIPTION);
	// // columns.add(ProductReportStyle.PRODUCT_CODE_PREFIX + "ISIN");
	// // columns.add("NOMINAL");
	// // columns.add("VALUE");
	// // columns.add("VALUE_CCY");
	// return columns;
	// }

	@Override
	public void setDefaults() {
		super.setDefaults();
		setColumns(SantThirdPartyInventoryViewReportStyle.DEFAULTS_COLUMNS);

	}

	@Override
	public void setDefaultDateColumns() {

	}
}
