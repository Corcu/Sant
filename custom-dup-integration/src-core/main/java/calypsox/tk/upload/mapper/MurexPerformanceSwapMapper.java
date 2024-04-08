package calypsox.tk.upload.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import javax.xml.datatype.XMLGregorianCalendar;

import com.calypso.tk.bo.BOException;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Frequency;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Util;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.BondAssetBacked;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.jaxb.AmortizationSchedule;
import com.calypso.tk.upload.jaxb.BondAssetDetailsLeg;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.jaxb.Cashflow;
import com.calypso.tk.upload.jaxb.CurrencyConversionLeg;
import com.calypso.tk.upload.jaxb.DateLagType;
import com.calypso.tk.upload.jaxb.IncomePaymentsLeg;
import com.calypso.tk.upload.jaxb.PerformanceSwap;
import com.calypso.tk.upload.jaxb.PriceFixingLeg;
import com.calypso.tk.upload.jaxb.PrimaryLeg;
import com.calypso.tk.upload.jaxb.PrincipalStructure;
import com.calypso.tk.upload.jaxb.SecondaryLeg;
import com.calypso.tk.upload.jaxb.SwapLeg;
import com.calypso.tk.upload.jaxb.UnderlyingProductDefinition;

public class MurexPerformanceSwapMapper extends MurexTradeMapper {
	
	protected PerformanceSwap performanceSwap;
	protected Product underlyingProduct; 
	
	public static final String TYPE_INCOME_PAYMENT = "Type_IncomePayment";
	public static final String TYPE_PRICE_FIXING = "Type_PriceFixing";
	
	public static final String FREQUENCY = "Frequency";
	public static final String RATE_INDEX = "RateIndex";
	public static final String RATE_INDEX_SOURCE = "RateIndexSource";
	public static final String RATE_INDEX_TENOR = "RateIndexTenor";
	public static final String AMORTIZATION_TYPE = "AmortizationType";
	public static final String STUB_PERIOD = "StubPeriod";
	public static final String FX_RESET = "FXReset";
	
	public static final String COUPON_SCHEDULE = "REF_ASSET_CPN_SCHEDULE";
	public static final String CUSTOM_SCHEDULE = "CUSTOM_SCHEDULE";
	public static final String AT_MATURITY = "AT_MATURITY";
	
	public static final String KW_CAPITAL_FACTOR = "CapitalFactor";
	public static final String KW_CAPITAL_AMENDMENT = "CapitalAmendment";
	public static final String KW_PREVIOUS_CAPITAL_AMENDMENT = "PreviousCapitalAmendment";
	public static final String KW_CAPITAL_HANDLING = "CapitalHandling";
	public static final String KW_CAPITAL_HANDLING_INCREASE = "Increase";
	public static final String KW_CAPITAL_HANDLING_DECREASE = "Decrease";
	
/*	protected void updateCashFlows(CalypsoTrade trade) {
		CurrencyConversionLeg ccyLeg = trade.getProduct().getPerformanceSwap().getPrimaryLeg().getCurrencyConversionLeg();
		
		if(ccyLeg.isFixedB() || !(Util.isEmpty(ccyLeg.getFXReset()) || isFalse(ccyLeg.getFXReset(), false))){
			String payCcy = ccyLeg.getPayCurrency();
			if(!Util.isEmpty(payCcy)) {
				for(Cashflow cashFlow : trade.getCashFlows().getCashFlow()) {
					String legType = cashFlow.getLegType();
					if("Primary".equals(legType)) {
						for(Column column : cashFlow.getColumn()) {
							if("Interest Amt".equals(column.getName())) {
								column.setName("Interest Amt" + " " + payCcy);
							}
						}
					}
				}
			}
		}
	}*/
	
	protected void mapPerformanceSwap(PerformanceSwap perfSwap) {
		this.performanceSwap=perfSwap;
		if(perfSwap!=null) {
			mapPrimaryLeg(perfSwap.getPrimaryLeg());
			mapSecondaryLeg(perfSwap.getSecondaryLeg());
		}
		
	}
	
	protected void mapPrimaryLeg(PrimaryLeg primaryLeg) {
		if(primaryLeg!=null) {	
			if(isMaturityExtended())
				primaryLeg.setEndDate(getCalypsoTrade().getMaturityDate());
			mapUnderlyingProductDefinition(primaryLeg.getUnderlyingProductDefinition());
			mapIncomePaymentsLeg(primaryLeg.getIncomePaymentsLeg());
			mapPriceFixingLeg(primaryLeg.getPriceFixingLeg());
			mapCurrencyConversionLeg(primaryLeg.getCurrencyConversionLeg());
			
		}
	}
	protected void mapUnderlyingProductDefinition(List<UnderlyingProductDefinition> underlyingProductDefinitionList) {
		if(underlyingProductDefinitionList!=null) {
			for(UnderlyingProductDefinition definition : underlyingProductDefinitionList) {
				mapUnderlyingProductDefinition(definition);
			}
		}
	}
	
	protected void mapUnderlyingProductDefinition(UnderlyingProductDefinition definition) {
		if(definition!=null) {

			if(existingTrade != null && existingTrade.getKeywordValue(KW_CAPITAL_AMENDMENT)!=null) {
				String previousCapitalAmendment = existingTrade.getKeywordValue(KW_CAPITAL_AMENDMENT);
				setKeyWordValue(KW_PREVIOUS_CAPITAL_AMENDMENT,previousCapitalAmendment);
			}
			
			BigDecimal totalCapitalAmendment = getIncomingCapitalAmendment().add(getExistingCapitalAmendment());
			definition.setQuantity(definition.getQuantity() + totalCapitalAmendment.doubleValue());	
			setKeyWordValue(KW_CAPITAL_AMENDMENT, totalCapitalAmendment.toPlainString());
			mapBondAssetDetailsLeg(definition.getBondAssetDetailsLeg());
		}
	}
	
	protected BigDecimal getIncomingCapitalAmendment() {
		if(this.calypsoTrade==null || isEventActionCancel())
			return BigDecimal.ZERO;
		BigDecimal incomingCapitalAmendment = getKeyWordValueAsBigDecimal(KW_CAPITAL_AMENDMENT);
		String capitalHandling = getKeyWordValue(KW_CAPITAL_HANDLING);
		if(KW_CAPITAL_HANDLING_INCREASE.equals(capitalHandling)) 
			return incomingCapitalAmendment;
		else 
			return incomingCapitalAmendment.multiply(new BigDecimal(-1));
		
	}
	
	protected BigDecimal getExistingCapitalAmendment() {
		if(existingTrade==null)
			return BigDecimal.ZERO;
		
		String existingCapitalAmountString = null;
		
		if(isEventActionCancel() || isEventActionCancelReissue()) 
			existingCapitalAmountString = existingTrade.getKeywordValue(KW_PREVIOUS_CAPITAL_AMENDMENT);
		else
			existingCapitalAmountString = existingTrade.getKeywordValue(KW_CAPITAL_AMENDMENT);
		
		
		BigDecimal existingCapitalAmendment = new BigDecimal(0);
		if(existingCapitalAmountString!=null)
			existingCapitalAmendment = new BigDecimal(existingCapitalAmountString);

		return existingCapitalAmendment;

	}
	
	protected void mapBondAssetDetailsLeg(BondAssetDetailsLeg bondAssetDetailsLeg) {
		if(bondAssetDetailsLeg!=null) {
			bondAssetDetailsLeg.setUseAssetScheduleB(false);
		}
	}

	protected void mapIncomePaymentsLeg(IncomePaymentsLeg incomePaymentLeg) {
		if(incomePaymentLeg!=null) {
			incomePaymentLeg.setDateRoll(getCalypsoValue(DATE_ROLL,incomePaymentLeg.getDateRoll()));
			incomePaymentLeg.setFrequency(getCalypsoValue(FREQUENCY,incomePaymentLeg.getFrequency()));
			incomePaymentLeg.setType(getCalypsoValueWithDefault(TYPE_INCOME_PAYMENT,incomePaymentLeg.getType()));
			
			// holidays
			mapHolidayCode(incomePaymentLeg.getHolidayCode());
		}
	}
	
	protected Product getUnderlyingProduct() {
		if(underlyingProduct==null) {
			PrimaryLeg primaryLeg = performanceSwap.getPrimaryLeg();
			if(primaryLeg!=null && primaryLeg.getUnderlyingProductDefinition().size()>0) {
				UnderlyingProductDefinition productDefinition = primaryLeg.getUnderlyingProductDefinition().get(0);
				String bondCode=productDefinition.getProductCodeValue();
				String codeType=productDefinition.getProductCodeType();
				try {
					underlyingProduct=DSConnection.getDefault().getRemoteProduct().getProductByCode(codeType, bondCode);
					if(underlyingProduct==null)
						Log.error("UPLOADER", "Underlying product not retreived correctly for ISIN "+bondCode);
				} catch (CalypsoServiceException e) {
					Log.error("UPLOADER",e);
				}
			}
		}
		return underlyingProduct;
	}
	
	protected String getBondFrequency() {
		Product product = getUnderlyingProduct();
		
		if (product instanceof Bond) {
			Bond bond = (Bond)product;
			Frequency frequency = bond.getCouponFrequency();
			return frequency.toString();
		}
		
		return null;
		
	}
	
	public String getPriceFixingType(PriceFixingLeg priceFixingLeg) {
		
		String interfaceType = getCalypsoValue(FREQUENCY,priceFixingLeg.getType());
		if(AT_MATURITY.equals(interfaceType))
			return AT_MATURITY;
		
		/*String bondFrequency = getBondFrequency();
		
		if(bondFrequency!=null && bondFrequency.equals(interfaceType)) {
			return COUPON_SCHEDULE;
		}*/
		return CUSTOM_SCHEDULE;
	}

	
	protected void mapPriceFixingLeg(PriceFixingLeg priceFixingLeg) {
		if(priceFixingLeg!=null) {
			
			// date roll
			priceFixingLeg.setFixingDateRoll(getCalypsoValue(DATE_ROLL,priceFixingLeg.getFixingDateRoll()));
			priceFixingLeg.setPaymentDateRoll(getCalypsoValue(DATE_ROLL,priceFixingLeg.getPaymentDateRoll()));
			
			priceFixingLeg.setType(getPriceFixingType(priceFixingLeg));

			priceFixingLeg.setFrequency(getCalypsoValue(FREQUENCY,priceFixingLeg.getFrequency()));
			
			if(!Util.isEmpty(priceFixingLeg.getFixingLag()))
				priceFixingLeg.setFixingLag(""+(-Math.abs(Integer.parseInt(priceFixingLeg.getFixingLag()))));
			
			// holidays
			mapHolidayCodeType(priceFixingLeg.getFixingHolidays());
			mapHolidayCodeType(priceFixingLeg.getPaymentHolidays());
			
		}
	}
	
	protected void mapDateLagType(DateLagType dateLagType) {
		if(dateLagType!=null) {
			dateLagType.setDateRoll(getCalypsoValue(DATE_ROLL,dateLagType.getDateRoll()));
			
			//holidays
			mapHolidayCodeType(dateLagType.getHolidays());
		}
	}
	
	protected void mapCurrencyConversionLeg(CurrencyConversionLeg currencyConversionLeg) {
		
		if(currencyConversionLeg!=null) {
			mapDateLagType(currencyConversionLeg.getIncomeFXResetSchedule());
			mapDateLagType(currencyConversionLeg.getReturnFXResetSchedule());
			
			currencyConversionLeg.setFXReset(getCalypsoValue(FX_RESET,currencyConversionLeg.getFXReset()));
		}
	}
	
	protected void mapSecondaryLeg(SecondaryLeg secondaryLeg) {
			if(secondaryLeg!=null) {	
				mapSwapLeg(secondaryLeg.getSwapLeg());
			}
		}
	
	
	protected void mapSwapLeg(SwapLeg swapLeg) {
		
		if(swapLeg!=null) {

			if(isMaturityExtended())
				swapLeg.setEndDate(getCalypsoTrade().getMaturityDate());
			
			// date roll
			swapLeg.setDateRollConvention(getCalypsoValue(DATE_ROLL,swapLeg.getDateRollConvention()));
			swapLeg.setResetRoll(getCalypsoValue(DATE_ROLL,swapLeg.getResetRoll()));
			
			// rate index
			swapLeg.setRateIndex(getCalypsoValue(RATE_INDEX,swapLeg.getRateIndex()));
			swapLeg.setRateIndexSource(getCalypsoValue(RATE_INDEX_SOURCE,swapLeg.getRateIndexSource()));
			swapLeg.setTenor(getCalypsoValue(RATE_INDEX_TENOR,swapLeg.getTenor()));
			
			// holidays
			mapHolidayCode(swapLeg.getHolidayCode());
			mapHolidayCodeType(swapLeg.getResetHolidays());
			
			swapLeg.setPaymentFrequency(getCalypsoValue(FREQUENCY,swapLeg.getPaymentFrequency()));
			swapLeg.setResetFrequency(getCalypsoValue(FREQUENCY,swapLeg.getResetFrequency()));
			swapLeg.setDayCountConvention(getCalypsoValue(DAY_COUNT,swapLeg.getDayCountConvention()));
				
			mapPrincipalStructure(swapLeg.getPrincipalStructure());
			
			swapLeg.setStubPeriod(getCalypsoValue(STUB_PERIOD,swapLeg.getStubPeriod()));
			
			swapLeg.setInterestCompoundingFrequency(getCalypsoValue(FREQUENCY,swapLeg.getInterestCompoundingFrequency()));
			
			if(Util.isTrue(swapLeg.getInterestCompounding())) {
				if(Util.isEmpty(swapLeg.getResetFrequency()))
					swapLeg.setResetFrequency(getCalypsoValue(FREQUENCY,swapLeg.getTenor()));
				if(Util.isEmpty(swapLeg.getInterestCompoundingFrequency()))
					swapLeg.setInterestCompoundingFrequency(getCalypsoValue(FREQUENCY,swapLeg.getTenor()));
			}
			mapAmortizationSchedule(swapLeg.getAmortizationSchedule());
		}
		
	}
	
	
	protected void mapAmortizationSchedule(List<AmortizationSchedule> amortScheduleList) {
		if(amortScheduleList!=null ) {
			amortScheduleList.sort(new Comparator<AmortizationSchedule>() {
				@Override
				public int compare(AmortizationSchedule o1, AmortizationSchedule o2) {
					return compareAmortizationScheduleDates(o1,o2);
				}
			});
			ListIterator<AmortizationSchedule> itAmortSchedule = amortScheduleList.listIterator();	
			HashSet<String> scheduleDates = new HashSet<String>();
			
			//remove duplicated amortization schedule keep the 1st one of each date
			while(itAmortSchedule.hasNext()) {
				AmortizationSchedule amortSchedule = itAmortSchedule.next();
				XMLGregorianCalendar amortDate = amortSchedule.getAmortizationDate();
				if(amortDate!=null) {
					if(scheduleDates.contains(amortDate.toString()))
						itAmortSchedule.remove();
					else
						scheduleDates.add(amortDate.toString());
				}
			}
			
			//handle cash flow with insertioneventId as Amort schedule
			ArrayList<AmortizationSchedule> newAmortizationSchedules = new ArrayList<AmortizationSchedule>();
			if( this.calypsoTrade.getCashFlows()!=null) {
				for(Cashflow cashflow : this.calypsoTrade.getCashFlows().getCashFlow()) {
					if(MurexTradeMapper.getColumnValue(cashflow, "InsertionEventId")!=null && !Util.isEmpty(cashflow.getLegType()) && cashflow.getLegType().equals("Secondary")) {
						
						AmortizationSchedule amortSchedule = new AmortizationSchedule();
						amortSchedule.setAmortizationDate(stringToCalendarNoTime(MurexTradeMapper.getColumnValue(cashflow,"Pmt Begin")));
						amortSchedule.setAmortizationAmount(Double.parseDouble(MurexTradeMapper.getColumnValue(cashflow,"Notional")));
						newAmortizationSchedules.add(amortSchedule);
					}
					
				}
			}	
			if(newAmortizationSchedules!=null && newAmortizationSchedules.size()>0 && newAmortizationSchedules.size()>0) {
				// merge schedules 
				itAmortSchedule = amortScheduleList.listIterator();
				ListIterator<AmortizationSchedule> itNewAmortSchedule = newAmortizationSchedules.listIterator();
				AmortizationSchedule currentAmortSchedule = itAmortSchedule.next();
				AmortizationSchedule nextAmortSchedule = currentAmortSchedule;
				while(itNewAmortSchedule.hasNext()) {
					AmortizationSchedule newAmortizationSchedule = itNewAmortSchedule.next();
					
					// search next amort schedule by date
					while(itAmortSchedule.hasNext() && 
							(compareAmortizationScheduleDates(newAmortizationSchedule,nextAmortSchedule))>=0) {
						currentAmortSchedule = nextAmortSchedule;
						nextAmortSchedule =itAmortSchedule.next(); 
					}
					if(compareAmortizationScheduleDates(newAmortizationSchedule,nextAmortSchedule)>=0) {
						currentAmortSchedule=nextAmortSchedule;
					}
					
					if(compareAmortizationScheduleDates(newAmortizationSchedule,currentAmortSchedule)==0) {
						currentAmortSchedule.setAmortizationAmount(currentAmortSchedule.getAmortizationAmount()+newAmortizationSchedule.getAmortizationAmount());
					}
					else {
						if(itAmortSchedule.hasPrevious()) itAmortSchedule.previous();
						newAmortizationSchedule.setAmortizationAmount(currentAmortSchedule.getAmortizationAmount()+newAmortizationSchedule.getAmortizationAmount());
						itAmortSchedule.add(newAmortizationSchedule);
						nextAmortSchedule=newAmortizationSchedule;
					}
				}
			}
		}	
	}
	
	protected int compareAmortizationScheduleDates(AmortizationSchedule amort1, AmortizationSchedule amort2) {
		return amort1.getAmortizationDate().toGregorianCalendar().compareTo(amort2.getAmortizationDate().toGregorianCalendar());
	}
	
	
	protected void mapPrincipalStructure(PrincipalStructure principalStructure ) {
		if(principalStructure!=null) {
			principalStructure.setAmortizationType(getCalypsoValueWithDefault(AMORTIZATION_TYPE,principalStructure.getAmortizationType()));
		}
		
	}	
	
	/**
	 * Map fields from Murex to Calypso.
	 * mapping of Date Roll
	 * + mapping of rateIndex
	 * @param doc
	 */
	protected void mapProduct(CalypsoTrade trade, Vector<BOException> errors) {
		if(trade.getProduct().getPerformanceSwap()!=null) {
			mapPerformanceSwap(trade.getProduct().getPerformanceSwap());
			//updateCashFlows(trade);
		}
	}
	
	public static boolean isFalse(String s, boolean valueIfMissing) {
		if (Util.isEmpty(s)) {
			return valueIfMissing;
		}
		else
		{
			String val = s.toUpperCase();
			return val.equals("NO") || val.equals("N") || val.equals("FALSE") || val.equals("F");
		}
	}
		
	
}

