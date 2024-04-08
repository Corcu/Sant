/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Hashtable;
import java.util.Vector;

import com.calypso.tk.bo.Inventory;
import com.calypso.tk.bo.InventorySecurityPosition;
import com.calypso.tk.core.Amount;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.BOPositionReport;
import com.calypso.tk.report.BOPositionReportStyle;
import com.calypso.tk.report.ProductReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;

public class SantThirdPartyInventoryViewReportStyle extends SantInventoryViewReportStyle {

	private static final long serialVersionUID = 6881928559332741996L;

	public static final String NOMINAL = TradeReportStyle.NOMINAL;

	// columns.add("ISIN");
	// // columns.add("SEDOL");
	// // columns.add(BOSecurityPositionReportStyle.PO);
	// // columns.add(BOSecurityPositionReportStyle.PRODUCT_DESCRIPTION);
	// // columns.add(ProductReportStyle.PRODUCT_CODE_PREFIX + "ISIN");
	// // columns.add("NOMINAL");
	// // columns.add("VALUE");
	// // columns.add("VALUE_CCY");

	// Default columns.
	public static final String[] DEFAULTS_COLUMNS = { ProductReportStyle.PRODUCT_CODE_PREFIX + "ISIN",
			ProductReportStyle.PRODUCT_CODE_PREFIX + "SEDOL", BOSecurityPositionReportStyle.PRODUCT_DESCRIPTION,
			NOMINAL, BOSecurityPositionReportStyle.CLEANPRICE_VALUE, BOPositionReportStyle.CURRENCY };

	@Override
	@SuppressWarnings("rawtypes")
	public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {
		Inventory inventory = (Inventory) row.getProperty(ReportRow.INVENTORY);

		if (columnId.equals(NOMINAL)) {

			Hashtable positions = (Hashtable) row.getProperty(BOPositionReport.POSITIONS);
			if (positions == null) {
				return null;
			}

			String s = Util.dateToMString(inventory.getPositionDate());
			Vector datedPositions = (Vector) positions.get(s);
			if ((datedPositions == null) || (datedPositions.size() == 0)) {
				return null;
			}
			InventorySecurityPosition invSecPos = (InventorySecurityPosition) datedPositions.get(0);
			Product product = getProduct(invSecPos.getSecurityId());
			//MIG_V14.4
			return new Amount(getTotal(datedPositions, InventorySecurityPosition.BALANCE_DEFAULT,null)
					* product.getPrincipal(inventory.getPositionDate()));

		}
		return super.getColumnValue(row, columnId, errors);

	}
}
