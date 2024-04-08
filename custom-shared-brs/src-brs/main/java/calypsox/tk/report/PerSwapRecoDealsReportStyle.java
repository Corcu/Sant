package calypsox.tk.report;

import java.security.InvalidParameterException;
import java.util.Vector;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.PerformanceSwapLeg;
import com.calypso.tk.product.Security;
import com.calypso.tk.product.SwapLeg;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.report.PerformanceSwapReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.report.util.UtilReport;

public class PerSwapRecoDealsReportStyle extends TradeReportStyle {

	public static final String REPORT_DATE = "Report Date";
	public static final String INTERNAL = "Internal";
	public static final String INSTRUMENT = "Instrument";
	public static final String EFFECTIVE_DATE = "Effective Date";
	
	public static final String PERFORMANCE_PAY_REC = "Performance Pay Rec";
	public static final String PERFORMANCE_BOND = "Performance Bond";
	public static final String PERFORMANCE_START_DATE = "Performance Start Date";
	public static final String PERFORMANCE_END_DATE = "Performance End Date";
	public static final String PERFORMANCE_QUANTITY = "Performance Quantity";
	public static final String PERFORMANCE_CURRENCY = "Performance Currency";
	public static final String PERFORMANCE_FIXING_TYPE = "Performance Fixing Type";
	public static final String PERFORMANCE_NOTIONAL_PRICE = "Performance Notional Price";
	public static final String PERFORMANCE_NOMINAL = "Performance Nominal";
	public static final String PERFORMANCE_LAST_RESET_DATE = "Performance Last Reset Date";
	public static final String PERFORMANCE_LAST_RESET_PRICE = "Performance Last Reset Price";
	public static final String PERFORMANCE_LATEST_PRICE_DATE = "Performance Latest Price Date";
	public static final String PERFORMANCE_LATEST_PRICE = "Performance Latest Price";
	public static final String PERFORMANCE_TYPE_SCHEDULE = "Performance Type Schedule";
	public static final String PERFORMANCE_ROLL_CONVENTION_FIXING = "Performance Roll Convention Fixing";
	public static final String PERFORMANCE_FIXING_LAG = "Performance Fixing Lag";
	public static final String PERFORMANCE_PAYMENT_LAG = "Performance Payment Lag";
	public static final String PERFORMANCE_ROLL_CONV_FIXING = "Roll Convention Fixing";
	public static final String PERFORMANCE_FREQUENCY = "Performance Frequency";
	public static final String PERFORMANCE_ROLL = "Performance Roll";
	public static final String PERFORMANCE_CALCULATION_BASIS = "Performance Calculation Basis";
	public static final String PERFORMANCE_CALENDAR = "Performance Calendar";
	public static final String PERFORMANCE_CALENDAR_1 = "Performance Calendar 1";
	
	public static final String FINANCING_PAY_REC = "Financing Pay Rec";
	public static final String FINANCING_FIXED_FLOAT = "Financing Fixed Float";
	public static final String FINANCING_START_DATE = "Financing Start Date";
	public static final String FINANCING_END_DATE = "Financing End Date";
	public static final String FINANCING_CURRENCY = "Financing Currency";
	public static final String FINANCING_NOMINAL = "Financing Nominal";
	public static final String FINANCING_INDEX = "Financing Index";
	public static final String FINANCING_HAIRCUT = "Financing Haircut";
	public static final String FINANCING_HAIRCUT_RATE = "Financing Haircut Rate";
	public static final String FINANCING_CONVENTION = "Financing Convention";
	public static final String FINANCING_FIRST_RATE = "Financing 1st Rate";
	public static final String FINANCING_MARGIN = "Financing Margin";
	public static final String FINANCING_FIXING_AV = "Financing Fixing (A/V)";
	public static final String FINANCING_PAYMENT_AV = "Financing Payment (A/V)";
	public static final String FINANCING_ROLLCONV_PAYMENT = "Financing RollConv Payment";
	public static final String FINANCING_ROLL = "Financing Roll";
	public static final String FINANCING_DAY_CONV = "Financing Day Convention";
	public static final String FINANCING_PAYMENT_CALENDAR = "Financing Payment Calendar";
	public static final String FINANCING_FIXED_RATE = "Financing Fixed Rate";
	
	private static final String FIXED = "Fixed";
	private static final String FLOAT = "Float";
	private static final String PAY = "PAY";
	private static final String REC = "REC";
	private static final String BOND_RETURN_SWAP = "BOND RETURN SWAP";
	private static final String YES = "Y";
	private static final String NO = "N";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
		
		Trade trade = row.getProperty(ReportRow.TRADE);
		Product product = trade.getProduct();
		JDatetime valDateTime = (JDatetime) row.getProperty(ReportRow.VALUATION_DATETIME);
		PricingEnv pricingEnv = (PricingEnv) row.getProperty(ReportRow.PRICING_ENV);
		JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());
		
		if(product instanceof PerformanceSwap) {
			PerformanceSwap perfSwap = (PerformanceSwap)product;
		
			if(columnName.equals(REPORT_DATE)) {
				return ReportRow.getValuationDateTime(row).getJDate(ReportRow.getPricingEnv(row).getTimeZone());
			}
			else if (columnName.equals(INTERNAL)) {
				Object isInternalDeal = super.getColumnValue(row, IS_INTERNAL_DEAL, errors);
				if (Util.isTrue(isInternalDeal, false))
					return YES;
				return NO;
			}
			else if (columnName.equals(INSTRUMENT)) {
				return BOND_RETURN_SWAP;
			}
			else if (columnName.equals(EFFECTIVE_DATE)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				return perfSwapLeg.getStartDate();
			}
			else if (columnName.equals(PERFORMANCE_PAY_REC)) {
				Boolean isBuy = (Boolean) super.getColumnValue(row, IS_BUY, errors);
				if(isBuy) return REC;
				else return PAY;
			}
			else if (columnName.equals(FINANCING_PAY_REC)) {
				Boolean isBuy = !((Boolean) super.getColumnValue(row, IS_BUY, errors));
				if(isBuy) return REC;
				else return PAY;
			}
			else if (columnName.equals(PERFORMANCE_BOND)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				Security sec = perfSwapLeg.getReferenceAsset();
				if (sec instanceof Bond) {
					Bond bond = (Bond) sec;
					return bond.getName();
				}
			}
			else if (columnName.equals(PERFORMANCE_START_DATE)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				return perfSwapLeg.getStartDate();
			}
			else if (columnName.equals(PERFORMANCE_END_DATE)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				return perfSwapLeg.getMaturityDate();
			}
			else if (columnName.equals(PERFORMANCE_CURRENCY)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				return perfSwapLeg.getCurrency();
			}
			else if (columnName.equals(PERFORMANCE_FIXING_TYPE)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				
				Boolean isFixed = UtilReport.isFixedInterestType(perfSwapLeg);
				if(isFixed==null)
					return null;
				
				if(isFixed)
					return FIXED;
				else
					return FLOAT;

			}
			else if (columnName.equals(PERFORMANCE_NOTIONAL_PRICE)) {
				return formatResult(super.getColumnValue(row, PerformanceSwapReportStyle.INITIAL_PRICE, errors));
			}
			else if (columnName.equals(PERFORMANCE_NOMINAL)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				return formatResult(UtilReport.getNominal(perfSwapLeg,valDate));
			}
			
			else if (columnName.equals(PERFORMANCE_QUANTITY)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				return formatResult(perfSwapLeg.getPrincipal(valDate));
			} 
			else if (columnName.equals(PERFORMANCE_LAST_RESET_DATE)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				return formatResult(UtilReport.getLastPriceFixingDate(perfSwapLeg, valDate));
			}
			else if (columnName.equals(PERFORMANCE_LAST_RESET_PRICE)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				return formatResult(UtilReport.getLastPriceFixing(perfSwapLeg, valDate)*100);
			}
			else if (columnName.equals(PERFORMANCE_LATEST_PRICE_DATE)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				Security sec = perfSwapLeg.getReferenceAsset();
				if (sec instanceof Bond) {
					Bond bond = ((Bond) sec);
					QuoteValue value = new QuoteValue();
			        value.setQuoteSetName(pricingEnv.getQuoteSetName());
			        value.setName(bond.getQuoteName());
			        value.setQuoteType(bond.getQuoteType());
			        value.setDate(valDate);
			        QuoteValue quoteValue;
					try {
						quoteValue = DSConnection.getDefault().getRemoteMarketData().getLatestQuoteValue(value);
			            if(null!=quoteValue){
			                return quoteValue.getDate();
			            }
					} catch (CalypsoServiceException e) {
						Log.error("PerSwapRecoDealsReportStyle", e);
					}

					return null;
	
				}
			}
			else if (columnName.equals(PERFORMANCE_LATEST_PRICE)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				Security sec = perfSwapLeg.getReferenceAsset();
				if (sec instanceof Bond) {
					Bond bond = ((Bond) sec);
					QuoteValue value = new QuoteValue();
			        value.setQuoteSetName(pricingEnv.getQuoteSetName());
			        value.setName(bond.getQuoteName());
			        value.setQuoteType(bond.getQuoteType());
			        value.setDate(valDate);
			        QuoteValue quoteValue;
					try {
						quoteValue = DSConnection.getDefault().getRemoteMarketData().getLatestQuoteValue(value);
			            if(null!=quoteValue){
			                return formatResult(quoteValue.getClose()*100);
			            }
					} catch (CalypsoServiceException e) {
						Log.error("PerSwapRecoDealsReportStyle", e);
					}

					return null;
	
				}
			}	
			else if(columnName.equals(PERFORMANCE_TYPE_SCHEDULE)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				Security sec = perfSwapLeg.getReferenceAsset();
				if (sec instanceof Bond) {
					Bond bond = ((Bond) sec);
					return bond.getPrincipalStructure();
	
				}
			}
			else if(columnName.equals(PERFORMANCE_FIXING_LAG)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				return perfSwapLeg.getReturnResetSchedule().getOffset();
			}
			else if(columnName.equals(PERFORMANCE_ROLL_CONVENTION_FIXING)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				return perfSwapLeg.getReturnResetSchedule().getDateRoll();
			}
			
			else if(columnName.equals(PERFORMANCE_PAYMENT_LAG)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				return perfSwapLeg.getReturnPmtSchedule().getOffset();
			}
			else if(columnName.equals(PERFORMANCE_FREQUENCY)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				return perfSwapLeg.getReturnPmtSchedule().getFrequency();
			}
			else if(columnName.equals(PERFORMANCE_ROLL)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				return perfSwapLeg.getReturnPmtSchedule().getDateRoll();
			}
			else if(columnName.equals(PERFORMANCE_CALENDAR)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
				if(perfSwapLeg.getReturnPmtSchedule().getHolidays()!=null) {
					return String.join(", ", perfSwapLeg.getReturnPmtSchedule().getHolidays());
				}
				return null;
			}
			else if(columnName.equals(PERFORMANCE_CALCULATION_BASIS)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg)  perfSwap.getPrimaryLeg();
				Security sec = perfSwapLeg.getReferenceAsset();
				if (sec instanceof Bond) {
					Bond bond = (Bond) sec;
					return bond.getCouponFrequency();		
				}
				return null;
			}
			else if(columnName.equals(PERFORMANCE_CALENDAR_1)) {
				PerformanceSwapLeg perfSwapLeg = (PerformanceSwapLeg)  perfSwap.getPrimaryLeg();
				Security sec = perfSwapLeg.getReferenceAsset();
				if (sec instanceof Bond) {
					Bond bond = (Bond) sec;
					if(bond.getHolidays()!=null) {
						return String.join(", ", bond.getHolidays());
					}
				}
				return null;
			}
			else if(columnName.equals(FINANCING_FIXED_FLOAT))
			{
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
				if (swapLeg.isFixed())
					return FIXED;
				else
					return FLOAT;
				
			}
			else if (columnName.equals(FINANCING_START_DATE)) {
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
				return swapLeg.getStartDate();
			}
			else if (columnName.equals(FINANCING_END_DATE)) {
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
				return swapLeg.getEndDate();
			}
			else if (columnName.equals(FINANCING_CURRENCY)) {
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
				return swapLeg.getCurrency();
			}
			else if (columnName.equals(FINANCING_NOMINAL)) {
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
				return formatResult(UtilReport.getNominal(swapLeg,valDate));
			}
			else if (columnName.equals(FINANCING_INDEX)) {
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
				RateIndex index = UtilReport.getRateIndex(swapLeg);
				return index;
			}
			else if (columnName.equals(FINANCING_HAIRCUT)) {
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
			}
			else if (columnName.equals(FINANCING_HAIRCUT_RATE)) {
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
			}
			else if (columnName.equals(FINANCING_CONVENTION)) {
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
				RateIndex index = UtilReport.getRateIndex(swapLeg);
				if(index!=null)
					return index.getDayCount();
			}
			else if (columnName.equals(FINANCING_FIRST_RATE)) {
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
				return swapLeg.getFirstResetRate();
			}
			else if (columnName.equals(FINANCING_MARGIN)) {
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
			}
			else if (columnName.equals(FINANCING_FIXING_AV)) {
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
			}
			else if (columnName.equals(FINANCING_PAYMENT_AV)) {
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
			}
			else if (columnName.equals(FINANCING_ROLLCONV_PAYMENT)) {
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
				return swapLeg.getCouponDateRoll();
			}
			else if (columnName.equals(FINANCING_ROLL)) {
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
			}
			else if (columnName.equals(FINANCING_DAY_CONV)) {
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
				return swapLeg.getDayCount();
			}
			else if (columnName.equals(FINANCING_PAYMENT_CALENDAR)) {
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
				if(swapLeg.getCouponHolidays()!=null) {
					return String.join(", ", swapLeg.getCouponHolidays());
				}
				return null;
			}
			else if (columnName.equals(FINANCING_FIXED_RATE)) {
				SwapLeg swapLeg = (SwapLeg) perfSwap.getSecondaryLeg();
				
				return formatResult(swapLeg.getFixedRate());
			}
		}
			return formatResult(super.getColumnValue(row, columnName, errors)) ;
	}
	

	public static Object formatResult(Object o) {
		return UtilReport.formatResult(o, ',');
	}
	
}
