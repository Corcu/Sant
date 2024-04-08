package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.SignedAmount;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.flow.CashFlowPriceChange;
import com.calypso.tk.report.CashFlowReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.util.CurrencyUtil;

import calypsox.tk.report.util.UtilReport;

public class PerSwapRecoFlowsReportStyle extends CashFlowReportStyle {
	
	public static final String REPORT_DATE = "Report Date";
	public static final String INSTRUMENT = "Instrument";
	public static final String INTERNAL = "Internal";
	
	public static final String START_NOTIONAL = "Start Notional";
	public static final String END_NOTIONAL = "End Notional";
	public static final String FIXING_DATE = "Fixing Date";
	
	private static final String BOND_RETURN_SWAP = "BOND RETURN SWAP";

	private static final String YES = "Y";
	private static final String NO = "N";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private SignedAmount roundAmount(CashFlow cf, Double amount) {
		   if (amount != null && !amount.isNaN()) {
		      double amt = amount;
		      String cur = cf.getCurrency();
		      amt = CurrencyUtil.roundAmount(amt, cur);
		      int digits = CurrencyUtil.getRoundingUnit(cur);
		      return new SignedAmount(amt, digits);
		   } else {
		      return null;
		   }
		}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
		
		CashFlow cf = (CashFlow)row.getProperty(ReportRow.CASHFLOW);
		
		if(columnName.equals(REPORT_DATE)) {
			return ReportRow.getValuationDateTime(row).getJDate(ReportRow.getPricingEnv(row).getTimeZone());
		}
		else if(columnName.equals(INSTRUMENT)) {
			return BOND_RETURN_SWAP;
		}
		
		else if (columnName.equals(INTERNAL)) {
			Object isInternalDeal = super.getColumnValue(row, IS_INTERNAL_DEAL, errors);
			if (Util.isTrue(isInternalDeal, false))
				return YES;
			return NO;
		}
		else if(columnName.equals(START_NOTIONAL)) {
			if(cf instanceof CashFlowPriceChange)  {
				CashFlowPriceChange cfPc = (CashFlowPriceChange)cf;
				return formatResult(roundAmount(cf, cfPc.getStartNotional()));
				
			}
		}
		else if(columnName.equals(END_NOTIONAL)) {
			if(cf instanceof CashFlowPriceChange)  {
				CashFlowPriceChange cfPc = (CashFlowPriceChange)cf;
				return formatResult(roundAmount(cf, cfPc.getEndNotional()));
			}
		}
		else if(columnName.equals(FIXING_DATE)) {
			if(cf instanceof CashFlowPriceChange)  {
				CashFlowPriceChange cfPc = (CashFlowPriceChange)cf;
				return cfPc.getPriceFixingDate();
				
			}
		}
		
		return formatResult(super.getColumnValue(row, columnName, errors));
	}
	
	public static Object formatResult(Object o) {
		return UtilReport.formatResult(o, ',');
	}

}
