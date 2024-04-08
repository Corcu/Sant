package calypsox.tk.report;

import calypsox.tk.report.util.UtilReport;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.*;
import com.calypso.tk.refdata.CurrencyDefault;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.report.PerformanceSwapReportStyle;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.LocalCache;

import java.security.InvalidParameterException;
import java.util.TimeZone;
import java.util.Vector;

public class PerSwapContingenciaReportStyle extends TradeReportStyle {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String PRODUCT_DEFINITION = "Product Definition";
	public static final String DIRECTION = "Direction";
	public static final String AGENCY_INDICATOR = "Agency Indicator";
	public static final String REFERENCE_TYPE = "Reference Type";
	public static final String TENOR_DAY_MONTH_YEAR = "Tenor Day Month Year";
	public static final String TENOR_NUMBER = "Tenor Number";
	public static final String AMORTIZATION_TYPE = "Amortization Type";
	public static final String NEXT_NOMINAL_LIQUIDATION_DATE = "Next Nominal Liquidation Date";
	public static final String NEXT_INTEREST_LIQUIDATION_DATE = "Next Interest Liquidation Date";
	public static final String NEXT_RESET_DATE = "Next Reset Date";
	public static final String LAST_RESET_DATE = "Last Reset Date";
	public static final String NOMINAL_PAYMENT_FREQUENCY = "Nominal Payment Frequency";
	public static final String PAYMENT_FREQUENCY = "Payment Frequency";
	public static final String RATE_INDEX_TENOR = "Rate Index Tenor";
	public static final String CURRENT_RATE = "Current Rate";
	public static final String LEG_CURRENCY = "Leg Currency";
	public static final String LEG_CURRENCY_DESCRIPTION = "Leg Currency Description";
	public static final String TRADE_DATE_NO_TIME = "Trade Date No Time";
	public static final String VALUE_DATE = "Value Date";
	public static final String LEG_NOMINAL = "Leg Nominal";
	public static final String INTEREST_TYPE = "Interest Type";
	public static final String LEG_DAY_COUNT = "Leg Day Count";
	public static final String LEG_SPREAD = "Leg Spread";
	public static final String INTRUMENT_TYPE = "Instrument Type";
	public static final String COLLATERAL_NAME = "Collateral Name";
	public static final String ACCOUNTING_CENTER = "Accounting Center";
	public static final String SEGMENT_CODE = "Segment Code";
	public static final String SEGMENT_CODE_DESCRIPTION = "Segment Code Description";
	public static final String ACCOUTING_CENTER_DESCRIPTION = "Accounting Center Description";
	public static final String ACTIVITY_SECTOR = "Activity Sector";
	public static final String ACCOUNTING_VALUE = "Accounting Value";
	public static final String CAP = "Cap";
	public static final String FLOOR = "Floor";
	public static final String MARKET_VALUE = "Market Value";
	public static final String STC_TRADEID = "STC TradeId";

	protected PerformanceSwappableLeg getReportingLeg(ReportRow row) {
		Trade trade = (Trade) row.getProperty(ReportRow.TRADE);
		Product product = trade.getProduct();
		PerformanceSwappableLeg reportingLeg = null;

		if (product instanceof PerformanceSwap) {
			PerformanceSwap perfSwap = (PerformanceSwap) trade.getProduct();

			String reportingLegValue = trade.getKeywordValue(PerformanceSwapTradeReportExplode.REPORTING_LEG);

			if (PerformanceSwapTradeReportExplode.REPORTING_LEG_PRIMARY.equals(reportingLegValue))
				reportingLeg = perfSwap.getPrimaryLeg();
			else if (PerformanceSwapTradeReportExplode.REPORTING_LEG_SECONDARY.equals(reportingLegValue))
				reportingLeg = perfSwap.getSecondaryLeg();

		}
		return reportingLeg;
	}

	protected String getMarketValuePricerMeasure(ReportRow row) {
		Trade trade = (Trade) row.getProperty(ReportRow.TRADE);
		String pricerMeasure = "Pricer.";
		String reportingLegValue = trade.getKeywordValue(PerformanceSwapTradeReportExplode.REPORTING_LEG);
		if (PerformanceSwapTradeReportExplode.REPORTING_LEG_PRIMARY.equals(reportingLegValue)) {
			pricerMeasure += "NPV_LEG1";
		} else {
			pricerMeasure += "NPV_LEG2";
		}
		return pricerMeasure;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getColumnValue(ReportRow row, String columnName, Vector errors) throws InvalidParameterException {
		Trade trade = (Trade) row.getProperty(ReportRow.TRADE);
		JDatetime valDateTime = (JDatetime) row.getProperty(ReportRow.VALUATION_DATETIME);
		PricingEnv pricingEnv = (PricingEnv) row.getProperty(ReportRow.PRICING_ENV);
		JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());

		PerformanceSwappableLeg reportingLeg = this.getReportingLeg(row);

		if (reportingLeg != null) {
			PerformanceSwap perfSwap = (PerformanceSwap) trade.getProduct();

			if (columnName.equals(PRODUCT_DEFINITION)) {
				return "BOND RETURN SWAP";
			} else if (columnName.equals(TRADE_DATE_NO_TIME)) {
				Object tradeDate = super.getColumnValue(row, TRADE_DATE, errors);
				if (tradeDate instanceof JDatetime) {
					JDatetime tradeDateTime = (JDatetime) tradeDate;
					return tradeDateTime.getJDate(TimeZone.getDefault());
				}
				return tradeDate;
			} else if (columnName.equals(DIRECTION)) {
				Boolean isBuy = (Boolean) super.getColumnValue(row, IS_BUY, errors);
				if (perfSwap.getPrimaryLeg().equals(reportingLeg)) {
					if (isBuy)
						return 1;
					else
						return 0;
				} else {
					if (isBuy)
						return 0;
					else
						return 1;
				}
			} else if (columnName.equals(AGENCY_INDICATOR)) {
				Object isInternalDeal = super.getColumnValue(row, IS_INTERNAL_DEAL, errors);
				if (Util.isTrue(isInternalDeal, false))
					return 0;// 0:INTERNA
				return 1; // 1:MERCADO
			} else if (columnName.equals(REFERENCE_TYPE)) {
				return UtilReport.getReferenceType(reportingLeg);
			} else if (columnName.equals(TENOR_DAY_MONTH_YEAR)) {
				RateIndex index = UtilReport.getRateIndex(reportingLeg);
				if (index == null)
					return 0;
				String tenor = index.getTenor().toString();
				if (tenor.length() > 1) {
					char dmy = tenor.charAt(1);
					if (dmy == 'D')
						return 0;
					if (dmy == 'M')
						return 1;
					if (dmy == 'Y')
						return 2;
				}
				return 0;
			} else if (columnName.equals(TENOR_NUMBER)) {
				RateIndex index = UtilReport.getRateIndex(reportingLeg);
				if (index == null)
					return 0;
				String tenor = index.getTenor().toString();
				if (tenor.length() > 1) {
					return tenor.charAt(0);
				}
				return 0;
			} else if (columnName.equals(AMORTIZATION_TYPE)) {
				return UtilReport.getAmortization(reportingLeg);
			} else if (columnName.equals(NEXT_NOMINAL_LIQUIDATION_DATE)) {
				return UtilReport.getNextPrincipalLiquidation(reportingLeg, valDate);
			} else if (columnName.equals(NEXT_INTEREST_LIQUIDATION_DATE)) {
				return UtilReport.getCashFlowDate(reportingLeg, valDate, CashFlow.INTEREST, "Date", true);
			} else if (columnName.equals(NEXT_RESET_DATE)) {
				return UtilReport.getCashFlowDate(reportingLeg, valDate, CashFlow.INTEREST, "ResetDate", true);
			} else if (columnName.equals(LAST_RESET_DATE)) {
				return UtilReport.getCashFlowDate(reportingLeg, valDate, CashFlow.INTEREST, "ResetDate", false);
			} else if (columnName.equals(PAYMENT_FREQUENCY)) {
				Frequency freq =  UtilReport.getPrincipalPmtFrequency(reportingLeg, trade);
				if(freq==null)
					return null;
				return freq.getTenor();
			} else if (columnName.equals(RATE_INDEX_TENOR)) {
				RateIndex index = UtilReport.getRateIndex(reportingLeg);
				if (index == null)
					return null;
				String tenor = index.getTenor().toString();
				return tenor;
			} else if (columnName.equals(CURRENT_RATE)) {
				return UtilReport.getCurrentRate(reportingLeg, valDate);
			} else if (columnName.equals(LEG_CURRENCY)) {
				return reportingLeg.getCurrency();
			} else if (columnName.equals(LEG_CURRENCY_DESCRIPTION)) {
				String ccy = reportingLeg.getCurrency();
				CurrencyDefault ccyDefault = LocalCache.getCurrencyDefault(ccy);
				if (ccyDefault == null)
					return null;
				return ccyDefault.getDescription();
			} else if (columnName.equals(VALUE_DATE)) {
				return UtilReport.getStartDate(reportingLeg);
			} else if (columnName.equals(LEG_NOMINAL)) {
				return UtilReport.getNominal(reportingLeg, valDate);
			} else if (columnName.equals(INTEREST_TYPE)) {
				return UtilReport.getInterestype(reportingLeg);
			} else if (columnName.equals(LEG_DAY_COUNT)) {
				if (reportingLeg == perfSwap.getSecondaryLeg()) {
					return UtilReport.getDayCount(reportingLeg);
				} else
					return "";
			} else if (columnName.equals(LEG_SPREAD)) {
				if (reportingLeg == perfSwap.getSecondaryLeg()) {
					return super.getColumnValue(row, PerformanceSwapReportStyle.RATE_INDEX_SPREAD, errors);
				} else
					return UtilReport.getCurrentSpread(reportingLeg, valDate);
			} else if (columnName.equals(COLLATERAL_NAME)) {
				return "CSA";
			} else if (columnName.equals(MARKET_VALUE)) {
				String pricerMeasure = getMarketValuePricerMeasure(row);
				return super.getColumnValue(row, pricerMeasure, errors);
			}
			else if (columnName.equals(NOMINAL_PAYMENT_FREQUENCY)) {
				if(isNoBullet(trade, reportingLeg)) {
//					return UtilReport.getPrincipalPmtFrequency(reportingLeg, trade);
					Frequency freq = UtilReport.getPrincipalPmtFrequency(reportingLeg, trade);
					if (freq == null)
						return null;
					return freq.getTenor();
				}
				return null;
			}
			 else if (columnName.equals(ACCOUNTING_VALUE)) {
					String pricerMeasure = getMarketValuePricerMeasure(row);
					return super.getColumnValue(row, pricerMeasure, errors);
				}
			else if (columnName.equals(ACCOUNTING_CENTER) || columnName.equals(SEGMENT_CODE)
					|| columnName.equals(SEGMENT_CODE_DESCRIPTION) || columnName.equals(ACCOUTING_CENTER_DESCRIPTION)
					|| columnName.equals(ACTIVITY_SECTOR)
					|| columnName.equals(INTRUMENT_TYPE)) {
				return "";
			} else if (columnName.equals(CAP) || columnName.equals(FLOOR)) {
				return "";
			}else if (columnName.equals(STC_TRADEID)) {
				String strTradeId=String.valueOf(trade.getLongId());
				return strTradeId+"_STC";
			}

		}
		return super.getColumnValue(row, columnName, errors);
	}


	@SuppressWarnings("rawtypes")
	@Override
	public void precalculateColumnValues(ReportRow row, String[] columns, Vector errors) {
		boolean hasMarketValueColumn = false;
		String marketValuePM = getMarketValuePricerMeasure(row);
		String[] columnsPM = new String[columns.length + 1];
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].equals(MARKET_VALUE)) {
				hasMarketValueColumn = true;
			}
			columnsPM[i] = columns[i];
		}
		columnsPM[columnsPM.length - 1] = marketValuePM;

		super.precalculateColumnValues(row, hasMarketValueColumn ? columnsPM : columns, errors);
		
	}

	private boolean isNoBullet(Trade tradeM, PerformanceSwappableLeg leg){

		PerformanceSwap product = (PerformanceSwap) tradeM.getProduct();

		if (leg instanceof SwapLeg) {
			String amortStructure = ((SwapLeg) product.getSecondaryLeg()).getPrincipalStructure();
			if (!Util.isEmpty(amortStructure) && "Bullet".equalsIgnoreCase(amortStructure)) {
				return false;
			} else if (!Util.isEmpty(amortStructure) && "Schedule".equalsIgnoreCase(amortStructure)) {
				return true;
			}
		} else if (leg instanceof PerformanceSwapLeg) {
			PerformanceSwapLeg productPrimaryLeg = (PerformanceSwapLeg) product.getPrimaryLeg();
			Security sec = productPrimaryLeg.getReferenceAsset();
			if (sec instanceof Bond) {
				Bond bond = ((Bond) sec);
				if (bond.getAmortizingB() && "Schedule".equals(bond.getPrincipalStructure())) {
					return true;
				}
				else {
					return false;
				}
			}
		}
		return false;
	}

}
