/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Vector;

import com.calypso.tk.core.Amount;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Rate;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.BondAssetBacked;
import com.calypso.tk.report.ReportRow;

import calypsox.util.collateral.CollateralUtilities;

public class Opt_BondStaticReportStyle extends BondReportStyle {

	public static final String FIXED = "Fixed";
	
	public static final String COUPON_CURRENCY = "Coupon_Currency";
	
	public static final String POOL_FACTOR = "Pool_Factor";
	
	protected DecimalFormat df;
	
	public Opt_BondStaticReportStyle() {
		super();
		 DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator(',');
		dfs.setGroupingSeparator('.');
		df = new DecimalFormat();
		df.setMaximumFractionDigits(10);
		df.setDecimalFormatSymbols(dfs);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Default columns.
	public static final String[] DEFAULTS_COLUMNS = { PRODUCT_CODE_PREFIX + "ISIN", PRODUCT_TYPE, PRODUCT_SUBTYPE,
			COUPON_CURRENCY, CURRENCY, DATED_DATE, DAY_COUNT, FIXED, COUPON_FREQUENCY, HOLIDAYS, RATE_INDEX, ISSUE_DATE, ISSUER,
			MATURITY_DATE, COUPON, RATE_INDEX_SPREAD, FACE_VALUE, COUPON_STUB_START, NOTIONAL_INDEX, CURRENT_NOTIONAL, NEXT_COUPON_DATE, POOL_FACTOR };

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(ReportRow row, String columnId, Vector errors) throws InvalidParameterException {

		Product product = getProduct(row);
		if (product == null) {
			return null;
		}

		if (!(product instanceof Bond)) {
			return null;
		}

		Bond bond = (Bond) product;
//		;
		// JLV 17/12/2014 -- Incidencia  HD 68880555 Optimizador
		if (columnId.equals(RATE_INDEX_SPREAD)) {
//			System.out.println("El Valor de RATE_INDEX_SPREAD: " + df.format(((Rate) super
//					.getColumnValue(row, columnId, errors)).get() * 100));
			//JRL 19/04/2016 Migration 14.4 
			Double tmp = CollateralUtilities.parseStringAmountToDouble(super.getColumnValue(row, columnId, errors).toString())*100;
			return df.format(tmp);
		// JLV 17/12/2014 -- Fin	
		} else if (columnId.equals(COUPON)) {
			return df.format(((Rate) super
					.getColumnValue(row, columnId, errors)).get());
		} else if (columnId.equals(CURRENT_NOTIONAL)) {
			return df.format(((Amount) super
					.getColumnValue(row, columnId, errors)).get());
		} else if (columnId.equals(COUPON_CURRENCY)) {
			return bond.getCouponCurrency();
		} else if (columnId.equals(FIXED)) {
			return bond.getFixedB() ? "1" : "0";
		} else if (columnId.equals(POOL_FACTOR)) {
			if (bond instanceof BondAssetBacked) {
				return ((BondAssetBacked) bond).getPoolFactor(JDate.getNow());
			} else
				return 1;
		} else {
			return super.getColumnValue(row, columnId, errors);
		}
	}
}