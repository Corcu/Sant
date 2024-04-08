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

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Amount;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.AccountInterestConfig;
import com.calypso.tk.refdata.AccountInterestConfigRange;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.AccountReportStyle;
import com.calypso.tk.report.ReportRow;

public class Opt_ReferenceRateReportStyle extends AccountReportStyle {

	private static final long serialVersionUID = 123L;

	public static final String MARGIN_CALL_CONTRACT = "MARGIN_CALL_CONTRACT";

	public static final String RATE = "RATE";

	public static final String SPREAD = "SPREAD";

	public static final String OWNER = "OWNER";

	public static final String DAY_ACCOUNT = "DAY_ACCOUNT";

	public static final String QUOTE_SET = "QUOTE SET";
	
	public static final String ACCOUNT_CCY = "Account_CCY";
	
	public static final String REFERENCE_CCY = "Reference_CCY";
	
	public static final String IS_FLOOR = "Is Floor";

    public static final String FLOOR_VALUE = "Floor_Value";  
    //GSM: 05/11/14. Required change request
    public static final String CALYPSO_RATE = "CALYPSO_RATE";


	protected DecimalFormat df;

	// Default columns.
	public static final String[] DEFAULTS_COLUMNS = { MARGIN_CALL_CONTRACT, OWNER, ACCOUNT_CCY, RATE, SPREAD, DAY_ACCOUNT };

	public Opt_ReferenceRateReportStyle() {
		super();
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator(',');
		dfs.setGroupingSeparator('.');
		this.df = new DecimalFormat();
		this.df.setMaximumFractionDigits(10);
		this.df.setDecimalFormatSymbols(dfs);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(final ReportRow row, final String columnName, final Vector errors)
			throws InvalidParameterException {

		if (row == null) {
			return null;
		}
		AccountInterestConfig accConfig = (AccountInterestConfig) row.getProperty("AccountConfig");
		AccountInterestConfigRange range = (AccountInterestConfigRange) row.getProperty("Range");

		if (range == null) {
			return null;
		}

		CollateralConfig config = (CollateralConfig) row.getProperty(ReportRow.MARGIN_CALL_CONFIG);

		if (config == null) {
			return null;
		}

		if (columnName.equals(MARGIN_CALL_CONTRACT)) {
			return config.getName();
		}

		if (columnName.equals(OWNER)) {
			return config.getProcessingOrg().getCode();
		}

		// GSM: 05/04/2014. Fix to return the CCY of the Reference Rate, not the contract base
		if (columnName.equals(REFERENCE_CCY)) {
			if ((range.getRateIndex() != null) && (range.getRateIndex().getCurrency() != null)) {
				return range.getRateIndex().getCurrency();// config.getCurrency();
			}
			return "";
		}
		// GSM: 10/07/2014. Fix to return the CCY of the account
		if (columnName.equals(ACCOUNT_CCY)) {
			
			Account account = (Account) row.getProperty("Account");
			
			if (account == null || Util.isEmpty(account.getCurrency()))
					return "";
			return account.getCurrency();
		}
			
	
		if (columnName.equals(RATE)) {
			
			final String assetName = (String) row.getProperty("AssetName");
			if (assetName != null) {
				return assetName;
			}

		}
		
		if (columnName.equals(CALYPSO_RATE)) {
			
			if (!range.isFixed()){
				return range.getRateIndex();	
			}
			return range.getFixedRate();
		}

		if (columnName.equals(DAY_ACCOUNT)) {
			
			if (accConfig != null) {
				return accConfig.getDaycount().toString();
			}
			
//			if( range.isFixed()) {
//				if (accConfig != null) {
//					return accConfig.getDaycount().toString();
//				}
//			} else if (range.getRateIndex().getDayCount() != null) {
//				return range.getRateIndex().getDayCount().toString();
//			}
		}

		if (columnName.equals(SPREAD)) {
			
			if (!range.isFixed()){
				
				//return this.df.format(range.getSpread() * 100);
				return new Amount (range.getSpread() * 100, 4);
			}
			//GSM: 05/11/14. Required change request: if fixed, show fixed rate
			return new Amount (range.getFixedRate(), 4);

		}

		if (columnName.equals(QUOTE_SET)) {

			String qName = (String) row.getProperty("QuoteName");
			return qName;

		}
		
		if (columnName.equals(IS_FLOOR)) {

            if (range.getRateIndex() != null) {
                   return range.isFloor();
            }
            return false;
		}

     if (columnName.equals(FLOOR_VALUE)) {

            if (range.getRateIndex() != null && range.isFloor()) {
                   return this.df.format(range.getFloor());
            }
            return "";
     }


		return super.getColumnValue(row, columnName, errors);
	}
}
