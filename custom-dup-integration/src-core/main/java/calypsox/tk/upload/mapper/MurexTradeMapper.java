package calypsox.tk.upload.mapper;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOException;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.upload.jaxb.CalypsoObject;
import com.calypso.tk.upload.jaxb.CalypsoTrade;
import com.calypso.tk.upload.jaxb.CashFlows;
import com.calypso.tk.upload.jaxb.Cashflow;
import com.calypso.tk.upload.jaxb.Column;
import com.calypso.tk.upload.jaxb.ColumnName;
import com.calypso.tk.upload.jaxb.Fee;
import com.calypso.tk.upload.jaxb.HolidayCode;
import com.calypso.tk.upload.jaxb.HolidayCodeType;
import com.calypso.tk.upload.jaxb.Keyword;
import com.calypso.tk.upload.jaxb.Termination;
import com.calypso.tk.upload.jaxb.TradeFee;
import com.calypso.tk.upload.jaxb.TradeKeywords;
import com.calypso.tk.upload.mapper.DefaultMapper;
import com.calypso.tk.util.DataUploaderUtil;

import calypsox.tk.upload.validator.ValidatorUtil;

public abstract class MurexTradeMapper extends DefaultMapper {
	
	private String source = null;
	
	public static final String LOG_CATEGORY = "MurexTradeMapper";
	
	public static final String CUSTOM_FLOWS = "CustomFlows";
	public static final String CUSTOM_FLOWS_ENABLED = "Enabled";
	public static final String PRODUCT_TYPE = "ProductType";
	public static final String ACTION = "Action";
	public static final String HOLIDAY = "Holiday";
	public static final String DEFAULT = "Default";
	public static final String FEE_DIRECTION = "FeeDirection";
	public static final String FEE_TYPE_EVENT_PREMIUM = "EVENT_PREMIUM";
	
	public static final String MUREX_TRADE_VERSION_KW = "MurexVersionNumber";
	public static final String KW_EVENT_ACTION = "EventAction";
	public static final String KW_EVENT_ACTION_NEW = "New";
	public static final String KW_EVENT_ACTION_CANCEL = "Cancel";
	public static final String KW_EVENT_ACTION_CANCELREEVENT = "CancelReevent";
	public static final String KW_MX_LAST_EVENT = "MxLastEvent";
	public static final String KW_MX_LAST_EVENT_MATURITY_EXTENSION = "mxContractEventRatesIMATURITY_EXTENSION";
	public static final String KW_MATURITY_EXTENDED = "MaturityExtended";
	
	public static final String LIST_SEPARATOR = ",";
	
	public Boolean isMaturityExtended = null;
	
	CalypsoTrade calypsoTrade;
	
	Trade existingTrade;

	public CalypsoTrade getCalypsoTrade() {
		return calypsoTrade;
	}

	public String getSource() {
		return this.source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void ReMap(CalypsoObject calypObject, String source, Vector<BOException> errors, Object dbConn) {
		this.reMap(calypObject, source, errors);
	}

	
	public boolean isPerformanceSwapTrade(CalypsoTrade trade) {
		return trade.getProduct().getPerformanceSwap()!=null;
	}
	
	public void reMap(CalypsoObject calypObject, String source, Vector<BOException> errors) {
		

		
		if(Log.isDebug())
			Log.debug("MUREX_SOURCE_MESSAGE",DataUploaderUtil.marshallCalypsoObject(calypObject));
		
		Log.debug("UPLOADER", "inside DefaultMapper.remap() Source = " + source);
		this.setSource(source);
		
		if (calypObject instanceof CalypsoTrade) {
			calypsoTrade = (CalypsoTrade) calypObject;
		}
		if (calypsoTrade != null) {
		if(!isPerformanceSwapTrade(calypsoTrade)) {
			super.reMap(calypObject, source, errors);
		}
		else {
				String tradeCp = calypsoTrade.getTradeCounterParty();
				String tradeBook;
				if (tradeCp != null) {
					try {
						tradeBook = getCalypsoValue( COUNTER_PARTY, tradeCp);
						if (!Util.isEmpty(tradeBook)) {
							calypsoTrade.setTradeCounterParty(tradeBook);
						}
					} catch (Exception arg7) {
						Log.error("UPLOADER",
								" Remote Exception Occured while Processing Remapping in MWBaseMapper.ReMap()", arg7);
					}
				}
	
				tradeBook = calypsoTrade.getTradeBook();
				Book book = BOCache.getBook(DSConnection.getDefault(), tradeBook);
				String mappedValue;
				if (!Util.isEmpty(tradeBook) && book == null) {
					mappedValue = getCalypsoValue(BOOK, tradeBook);
					if (!Util.isEmpty(mappedValue)) {
						calypsoTrade.setTradeBook(mappedValue);
						book = BOCache.getBook(DSConnection.getDefault(), mappedValue);
					}
				}
	
				if (calypsoTrade.getTraderName() != null) {
					mappedValue = getCalypsoValue(TRADERS, calypsoTrade.getTraderName());
					if (mappedValue != null) {
						calypsoTrade.setTraderName(mappedValue);
					}
				}
	
				if (!Util.isEmpty(calypsoTrade.getProductType())) {
					mappedValue = getCalypsoValue( PRODUCT_TYPE, calypsoTrade.getProductType());
					calypsoTrade.setProductType(mappedValue);
				}
				
				existingTrade = ValidatorUtil.getExistingTrade(calypsoTrade, null);
				
				if (!Util.isEmpty(calypsoTrade.getAction())) {
					mappedValue = getCalypsoValue( ACTION, calypsoTrade.getAction());
					
					// AMEND is received with NEW but different murex trade version
					if(mappedValue.equals(Action.S_NEW) && existingTrade!=null) {
						String incomingMurexVersionId=getKeyWordValue(MUREX_TRADE_VERSION_KW);
						if(incomingMurexVersionId!=null) {	
								String existingMurexVersionID = existingTrade.getKeywordValue(MUREX_TRADE_VERSION_KW);
								if(incomingMurexVersionId.equals(existingMurexVersionID))
									mappedValue=Action.S_AMEND;
								calypsoTrade.setAction(mappedValue);
						}
					}
					
					
					
				}
				
				mapHolidayCode(calypsoTrade.getHolidayCode());
				
				sortCashFlows(calypsoTrade.getCashFlows());
				
				
				if(calypsoTrade.getCashFlows()!=null) {
					mappedValue= getCalypsoValue(this.getSource(),CUSTOM_FLOWS,CUSTOM_FLOWS_ENABLED,false);
					if(mappedValue!=null)
						calypsoTrade.getCashFlows().setCustomFlows(mappedValue);
				}
				
				
	
				this.mapTermination(calypsoTrade.getTermination());
				
				if(isCancelMaturityExtended()) {
					setKeyWordValue(KW_MATURITY_EXTENDED, "");
				}
				else
				{
					if(isMaturityExtended()) {
						setKeyWordValue(KW_MATURITY_EXTENDED, "true");
					}
				}
				
				this.mapProduct(calypsoTrade, errors);
				
				removeColumn(calypsoTrade.getCashFlows(), "Notional");
				LegalEntity processingOrg = BOCache.getLegalEntity(DSConnection.getDefault(),book.getProcessingOrgBasedId());
				
				if(processingOrg!=null)
					this.mapTradeFee(calypsoTrade.getTradeFee(),processingOrg.getCode());
				
			
				Log.info("UPLOADER", "End of DefaultMapper.ReMap() ");
			}
		}
	}
	
	
	public static String getColumnValue(Cashflow cf, String columnName) {
		for(Column c : cf.getColumn()) {
			if(c.getName().equals(columnName))
					return c.getValue();
		}
		return null;
		
	}
	
	public void removeColumn(CashFlows cashFlows, String columnNameToRemove) {
		if(cashFlows!=null && cashFlows.getLockColumns()!=null) {
			for(ColumnName column : cashFlows.getLockColumns()) {
				Iterator<String> columnNameIt = column.getName().iterator();
				while (columnNameIt.hasNext()) {
					String columnName = columnNameIt.next();
					if(columnName.equals(columnNameToRemove))
						columnNameIt.remove();
				}
			}
		
			
			for(Cashflow cashFlow : cashFlows.getCashFlow()) {
				Iterator<Column> columnIt = cashFlow.getColumn().iterator();
				while(columnIt.hasNext()) {
					Column column = columnIt.next();
					if(column.getName().equals(columnNameToRemove))
						columnIt.remove();
				}
			}
		}

	}
	
	public static Date stringToDate(String source) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		try {
			return df.parse(source);
		} catch (ParseException e) {
			Log.error(LOG_CATEGORY, e);
			return null;
		}
	}
	
	public void sortCashFlows(CashFlows cashFlows) {
	
	
		
		if(cashFlows!=null) {
			ArrayList<Cashflow> cashFlowsList = new ArrayList<Cashflow>();
			for(Cashflow cashFlow : cashFlows.getCashFlow()) {
				cashFlowsList.add(cashFlow);
			}
			
			Collections.sort(cashFlowsList, new Comparator<Cashflow>() {

				@Override
				public int compare(Cashflow o1, Cashflow o2) {
					int c=0;
					c = o1.getLegType().compareTo(o2.getLegType());
					if(c!=0) return c;

					String o1pmDateStr=getColumnValue(o1,"Pmt Dt");
					String o2pmDateStr=getColumnValue(o2,"Pmt Dt");
					if(o1pmDateStr!=null && o2pmDateStr!=null) {
						Date o1PmDate = stringToDate(o1pmDateStr);
						Date o2PmDate = stringToDate(o2pmDateStr);
						if(o1PmDate!=null && o2PmDate!=null) {
							c = o1PmDate.compareTo(o2PmDate);
						}
					}
					
					if(c!=0) return c;
					
					String o1Type = getColumnValue(o1,"Type");
					String o2Type = getColumnValue(o2,"Type");
					if(o1Type!=null && o2Type!=null) {
						c = o1Type.compareTo(o2Type);
					}
						
					return c;
					
				}
				
			});
			
			cashFlows.getCashFlow().clear();
			for(Cashflow cashFlow:cashFlowsList) {
				cashFlows.getCashFlow().add(cashFlow);
			}
			
		}

			
	}
	
	
	public void mapTradeFee(TradeFee tradeFee,String processingOrg) {
		if(tradeFee!=null) {
			
			String eventAction = getKeyWordValue(KW_EVENT_ACTION);
			boolean removeEventPremiumFees = false;
			if(KW_EVENT_ACTION_CANCEL.equals(eventAction)) {
				removeEventPremiumFees=true;
			}
			
			Iterator<Fee> feeIt = tradeFee.getFee().iterator();
			while(feeIt.hasNext()) {
				Fee fee = feeIt.next();
				if(removeEventPremiumFees && fee.getFeeType().equals(FEE_TYPE_EVENT_PREMIUM)) {
					feeIt.remove();
				}
				else {
					if(fee.getFeeCounterParty()==null || Util.isEmpty(fee.getFeeCounterParty()))
						fee.setFeeCounterParty(processingOrg);
				}
			}
			
		}
		
	}
	
	public void mapTermination(Termination termination) {
		if(termination!=null) {
			if(termination.getTerminationDateTime()==null) {
				termination.setTerminationDateTime(dateToCalendar(new JDatetime()));
			}
			if(termination.getTerminationEffectiveDate()==null) {
				termination.setTerminationEffectiveDate(dateToCalendar(new JDatetime()));
			}			
		}
		
	}
	
    
    public static XMLGregorianCalendar dateToCalendar(Date date) {
        if (date != null) {
            GregorianCalendar greg = new GregorianCalendar();
            greg.setTime(date);
            try {
				return DatatypeFactory.newInstance().newXMLGregorianCalendar(greg);
			} catch (DatatypeConfigurationException e) {
				Log.error(LOG_CATEGORY, e);
			}
        }
        return null;
    }
    
    public static XMLGregorianCalendar dateToCalendarNoTime(Date date) {
        if (date != null) {
            GregorianCalendar greg = new GregorianCalendar();
            greg.setTime(date);
            try {
				return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(greg.get(Calendar.YEAR),greg.get(Calendar.MONTH)+1,greg.get(Calendar.DAY_OF_MONTH), DatatypeConstants.FIELD_UNDEFINED);
			} catch (DatatypeConfigurationException e) {
				Log.error(LOG_CATEGORY, e);
			}
        }
        return null;
    }
    
    
    public static XMLGregorianCalendar stringToCalendar(String date) {
    	return dateToCalendar(stringToDate(date));
    }
    
    public static XMLGregorianCalendar stringToCalendarNoTime(String date) {
    	return dateToCalendarNoTime(stringToDate(date));
    }
	
	public void mapFee(Fee fee) {
		if(fee!=null) {
			fee.setFeeDirection(getCalypsoValueWithDefault(FEE_DIRECTION, fee.getFeeDirection()));
		}
	}
	
	public void mapHolidayCode(HolidayCode holidayCode) {
		if(holidayCode!=null && holidayCode.getHoliday().size()!=0) {
			String mappedValue = getCalypsoValue( HOLIDAY, holidayCode.getHoliday().get(0));
			String[] allHolidays = mappedValue.split(LIST_SEPARATOR);
			holidayCode.getHoliday().clear();
			for(int i=0;i<allHolidays.length;i++) {
				holidayCode.getHoliday().add(allHolidays[i]);
			}			
		}

	}
	
	public void mapHolidayCodeType(HolidayCodeType holidayCode) {
		
		if(holidayCode!=null && holidayCode.getHoliday().size()!=0) {
			String mappedValue = getCalypsoValue( HOLIDAY, holidayCode.getHoliday().get(0));
			String[] allHolidays = mappedValue.split(LIST_SEPARATOR);
			holidayCode.getHoliday().clear();
			for(int i=0;i<allHolidays.length;i++) {
				holidayCode.getHoliday().add(allHolidays[i]);
			}			
		}

	}
	
	public String getCalypsoValue(String typeName, String interfaceValue) {
		return getCalypsoValue(this.getSource(),typeName,interfaceValue,null);
	}
	
	public String getCalypsoValueWithDefault(String typeName, String interfaceValue) {
		String calypsoValue = getCalypsoValue(this.getSource(),typeName,interfaceValue,false);

		if(calypsoValue==null) {
			String defaultValue = getCalypsoValue(this.getSource(),typeName,DEFAULT,false);
			if(defaultValue==null)
				return interfaceValue;
			else
				return defaultValue;
		}
		return calypsoValue;
	}
	
	public String getCalypsoValue(String typeName, String interfaceValue, String defaultValue) {
		String calypsoValue = getCalypsoValue(this.getSource(),typeName,interfaceValue,false);
		
		if(calypsoValue==null)
			return defaultValue;
		return calypsoValue;
	}
	
	public String getKeyWordValue(String keywordName) {
		if(calypsoTrade.getTradeKeywords()!=null) {
			for(Keyword keyword : calypsoTrade.getTradeKeywords().getKeyword()) {
				if(keyword.getKeywordName().equals(keywordName)) {
					return keyword.getKeywordValue();
				}
			}
		}
		return null;
		
	}
	
	public Double getKeyWordValueAsDouble(String keywordName) {
		String keywordValue = getKeyWordValue(keywordName);
		if(Util.isEmpty(keywordValue))
			return 0.0d;
		return Double.parseDouble(keywordValue);
	}
	
	public BigDecimal getKeyWordValueAsBigDecimal(String keywordName) {
		String keywordValue = getKeyWordValue(keywordName);
		if(Util.isEmpty(keywordValue))
			return BigDecimal.ZERO;
		return new BigDecimal(keywordValue);
	}
	
	public void removeKeyword(String keywordName) {
		if(calypsoTrade.getTradeKeywords()!=null) {
			Iterator<Keyword> it = calypsoTrade.getTradeKeywords().getKeyword().iterator();
			while(it.hasNext()) {
				Keyword kw = it.next();
				if(kw.getKeywordName().equals(keywordName))
					it.remove();
			}
		}
		
	}
	
	public boolean isEventActionCancelReissue() {
		String eventAction = getKeyWordValue(KW_EVENT_ACTION);
		if(!Util.isEmpty(eventAction) && eventAction.equals("CancelReevent")) {
			return true;
		}
		return false;
	}
	
	public boolean isEventActionCancel() {
		String eventAction = getKeyWordValue(KW_EVENT_ACTION);
		if(!Util.isEmpty(eventAction) && eventAction.equals("Cancel")) {
			return true;
		}
		return false;
	}
	
	public void setKeyWordValue(String keywordName, String keywordValue) {
		removeKeyword(keywordName);
		if(calypsoTrade.getTradeKeywords()==null) {
			TradeKeywords keywords = new TradeKeywords();
			calypsoTrade.setTradeKeywords(keywords);
		}
		Keyword newKw = new Keyword();
		newKw.setKeywordName(keywordName);
		newKw.setKeywordValue(keywordValue);
		calypsoTrade.getTradeKeywords().getKeyword().add(newKw);
	}
	
	
	public boolean isCancelMaturityExtended() {
		
		String mxLastEvent = getKeyWordValue(KW_MX_LAST_EVENT);
		String eventAction = getKeyWordValue(KW_EVENT_ACTION);
		
		if(mxLastEvent!=null 
				&& eventAction!=null 
				&& mxLastEvent.equals(KW_MX_LAST_EVENT_MATURITY_EXTENSION) 
				&& eventAction.equals(KW_EVENT_ACTION_CANCEL))  {
			isMaturityExtended = false;
			return true;
			
		}
		
		return false;
		
	}
	
	public boolean isMaturityExtended() {
		if(isMaturityExtended!=null)
			return isMaturityExtended;
		
		if(existingTrade!=null) {
			String maturityExtended = existingTrade.getKeywordValue(KW_MATURITY_EXTENDED);
			if(Util.isTrue(maturityExtended)) {
				isMaturityExtended = true;
				return true;
			}	
		}
		
		String mxLastEvent = getKeyWordValue(KW_MX_LAST_EVENT);
		String eventAction = getKeyWordValue(KW_EVENT_ACTION);

		if(mxLastEvent!=null 
				&& eventAction!=null 
				&& mxLastEvent.equals(KW_MX_LAST_EVENT_MATURITY_EXTENSION) 
				&&  ((eventAction.equals(KW_EVENT_ACTION_CANCELREEVENT)) 
						|| eventAction.equals(KW_EVENT_ACTION_NEW))) {
			isMaturityExtended = true;
			return true;
		}
		isMaturityExtended = false;
		return false;
	}
	
	/**
	 * Map fields from Murex to Calypso.
	 * mapping of Date Roll
	 * + mapping of rateIndex
	 * @param doc
	 */
	protected abstract void mapProduct(CalypsoTrade trade, Vector<BOException> errors) ;
	
}

