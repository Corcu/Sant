/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.report.ReportRow;

public class SantCollateralPositionSendBalanceReportStyle extends BOSecurityPositionReportStyle {

	public static final String CONTRACT_ID = "Contract ID";
	public static final String CONTRACT_TYPE = "Contract Type";
	public static final String NAME = "Name";
	public static final String POSITION_TYPE = "Position Type";
	public static final String ISIN = "ISIN";
	public static final String NOMINAL = "Nominal";
	public static final String DIRTY_PRICE = "Dirty Price";
	public static final String CURRENCY = "Currency";
	public static final String HAIRCUT = "Haircut";
	public static final String VALUE = "Value";
	public static final String FX_RATE = "FX Rate";
	public static final String BASE_CCY = "Base CCy";
	public static final String VALUE_IN_BASE_CCY = "Value in Base CCY";
	public static final String MATURITY = "Maturity";
	public static final String NEXT_COUPON_DATE = "Next Coupon Date";

	private static final long serialVersionUID = 1L;

	public static final String[] DEFAULT_COLUMN_NAMES = new String[] { CONTRACT_ID, CONTRACT_TYPE, NAME, POSITION_TYPE,
			ISIN, NOMINAL, DIRTY_PRICE, CURRENCY, HAIRCUT, VALUE, FX_RATE, BASE_CCY, VALUE_IN_BASE_CCY, MATURITY,
			NEXT_COUPON_DATE };

	@SuppressWarnings({ "rawtypes" })
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		final SantCollateralPositionSendBalanceItem item = (SantCollateralPositionSendBalanceItem) row
				.getProperty(SantCollateralPositionSendBalanceItem.SEND_BALANCE_ITEM);

		if (CONTRACT_ID.equals(columnName)) {
			return item.getContractID();
		} else if (CONTRACT_TYPE.equals(columnName)) {
			return item.getContractType();
		} else if (NAME.equals(columnName)) {
			return item.getName();
		} else if (POSITION_TYPE.equals(columnName)) {
			return item.getPositionType();
		} else if (ISIN.equals(columnName)) {
			return item.getIsin();
		} else if (NOMINAL.equals(columnName)) {
			return item.getNominal();
		} else if (DIRTY_PRICE.equals(columnName)) {
			return item.getDirtyPrice();
		} else if (CURRENCY.equals(columnName)) {
			return item.getCurrency();
		} else if (HAIRCUT.equals(columnName)) {
			return item.getHaircut();
		} else if (VALUE.equals(columnName)) {
			return item.getValue();
		} else if (FX_RATE.equals(columnName)) {
			return item.getFxRate();
		} else if (BASE_CCY.equals(columnName)) {
			return item.getBaseCCY();
		} else if (VALUE_IN_BASE_CCY.equals(columnName)) {
			return item.getValueInBaseCCY();
		} else if (MATURITY.equals(columnName)) {
			return item.getMaturity();
		} else if (NEXT_COUPON_DATE.equals(columnName)) {
			return item.getNextCouponDate();
		}

		return null;
	}
}