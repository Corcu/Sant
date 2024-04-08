package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.TimeZone;
import java.util.Vector;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.PerformanceSwap;
import com.calypso.tk.product.PerformanceSwapLeg;
import com.calypso.tk.product.SwapLeg;
import com.calypso.tk.refdata.CurrencyDefault;
import com.calypso.tk.refdata.CurrencyPair;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReportStyle;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import calypsox.util.collateral.CollateralUtilities;

/**
 * The Class TradeFXAuditReportStyle.
 */
public class BrsMifidReportStyle extends TradeReportStyle {

	
	/** The Constant TRADE_ID. */
	public static final String TRADE_ID = "TRADE_ID";

	/** The Constant FO_SOURCE. */
	public static final String FO_SOURCE = "FO_SOURCE";
	
	/** The Constant EXTERNAL_REFERENCE. */
	public static final String EXTERNAL_REFERENCE = "EXTERNAL_REFERENCE";
	
	/** The Constant ORIGINAL_CONTRACT. */
	public static final String ORIGINAL_CONTRACT = "ORIGINAL_CONTRACT";
	
	/** The Constant PROCESS_DATE. */
	public static final String PROCESS_DATE = "PROCESS_DATE";
	
	/** The Constant PRODUCT_TYPE. */
	public static final String PRODUCT_TYPE = "PRODUCT_TYPE";
	
	/** The Constant TRADE_KEYWORD_ORIGPRODUCTTYPE. */
	public static final String TRADE_KEYWORD_ORIGPRODUCTTYPE = "TRADE_KEYWORD_ORIGPRODUCTTYPE";
	
	/** The Constant COUNTERPARTY_SHORT_NAME. */
	public static final String COUNTERPARTY_SHORT_NAME = "COUNTERPARTY_SHORT_NAME";
	
	/** The Constant COUNTERPARTY_FULL_NAME. */
	public static final String COUNTERPARTY_FULL_NAME = "COUNTERPARTY_FULL_NAME";
	
	/** The Constant TRADE_DATE. */
	public static final String TRADE_DATE = "TRADE_DATE";
	
	/** The Constant TRADE_SETTLE_DATE. */
	public static final String TRADE_SETTLE_DATE = "TRADE_SETTLE_DATE";
	
	/** The Constant PURCHASE_CURRENCY. */
	public static final String PURCHASE_CURRENCY = "PURCHASE_CURRENCY";
	
	/** The Constant SALE_CURRENCY. */
	public static final String SALE_CURRENCY = "SALE_CURRENCY";
	
	/** The Constant SALE_AMOUNT. */
	public static final String SALE_AMOUNT = "SALE_AMOUNT";
	
	/** The Constant PURC_AMOUNT. */
	public static final String PURC_AMOUNT = "PURC_AMOUNT";
	
	/** The Constant PURC_NPV_L. */
	public static final String PURC_NPV_L = "PURC_NPV_L";
	
	/** The Constant SALE_NPV_L. */
	public static final String SALE_NPV_L = "SALE_NPV_L";
	
	/** The Constant PARTENON_TRADE_ID. */
	public static final String PARTENON_TRADE_ID = "PARTENON_TRADE_ID";
	
	/** The Constant PARTENON_TRADE_ID_NEAR. */
	public static final String PARTENON_TRADE_ID_NEAR = "PARTENON_TRADE_ID_NEAR";
	
	/** The Constant MTM. */
	public static final String MTM = "MTM";
	
	/** The Constant IS_SPOT. */
	public static final String IS_SPOT = "IS_SPOT";
	
	/** The Constant PRODUCTO_CONCRET. */
	public static final String PRODUCTO_CONCRETO = "PRODUCTO_CONCRETO";


  /**
   * This method returns row value for the column.
   *
   * @param row
   *            the object for which the value of the column will be
   *            determined.
   * @param columnName
   *            the id of a column present in the report style.
   * @param errors
   *            the errors
   * @return the value of the column for the reportLine. null if no value can
   *         be determined.
   */
  @SuppressWarnings({ "unchecked" })
  @Override
  public Object getColumnValue(final ReportRow row, final String columnName,
      @SuppressWarnings("rawtypes") final Vector errors) {
    
	if (row == null) {
      return null;
    }
    final Trade trade = (Trade) row.getProperty(ReportRow.TRADE);
    if (trade == null) {
      return null;
    }
    
    final JDatetime valDatetime = (JDatetime) row.getProperty(ReportRow.VALUATION_DATETIME);
    final JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
    final PricingEnv pricingEnv = (PricingEnv) row.getProperty(ReportRow.PRICING_ENV);    
    boolean isPrimaryLegBuy = isPrimaryLegBuy(trade);
    PerformanceSwap perfSwap = (PerformanceSwap) trade.getProduct();
	PerformanceSwapLeg primaryLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
	SwapLeg secondaryLeg = (SwapLeg) perfSwap.getSecondaryLeg();
	  
	final PLMark plMarkValue = getPLMarkValue(pricingEnv, trade, valDate);	
	
    if (columnName.equals("TRADE_ID")) {
    	return trade.getLongId();
    }
    else if (columnName.equals("FO_SOURCE")) {
        return "MUREX";
    }
    else if (columnName.equals("EXTERNAL_REFERENCE")) {
    	return trade.getExternalReference();
    }
    else if (columnName.equals("ORIGINAL_CONTRACT")) {
    	return trade.getKeywordValue("MurexRootContract");
    }
    else if (columnName.equals("PROCESS_DATE")) {
    	return valDate;
    }
    else if (columnName.equals("PRODUCT_TYPE")) {
    	return trade.getProductType();
    }
    else if (columnName.equals("TRADE_KEYWORD_ORIGPRODUCTTYPE")) {
    	return trade.getProductType();
    }
    else if (columnName.equals("COUNTERPARTY_SHORT_NAME")) {
     return trade.getCounterParty().getCode();
    }
    else if (columnName.equals("COUNTERPARTY_FULL_NAME")) {
    	return trade.getCounterParty().getName();
    }
    else if (columnName.equals("TRADE_DATE")) {
    	return trade.getTradeDate().getJDate();
    }
    else if (columnName.equals("TRADE_SETTLE_DATE")) {
    	return trade.getSettleDate();
    }
    else if (columnName.equals("PURCHASE_CURRENCY")) {
    	if(isPrimaryLegBuy) {
    		return primaryLeg.getCurrency();
    	}
    	else {
    		return secondaryLeg.getCurrency();
    	}
    }
    else if (columnName.equals("SALE_CURRENCY")) {
    	if(isPrimaryLegBuy) {
    		return secondaryLeg.getCurrency();
    	}
    	else {
    		return primaryLeg.getCurrency();
    	}
    }
    else if (columnName.equals("SALE_AMOUNT")) {
    	if(isPrimaryLegBuy) {
    		return secondaryLeg.getPrincipal();
    	}
    	else {
    		return primaryLeg.getNotional();
    	}
    }	
    else if (columnName.equals("PURC_AMOUNT")) {
    	if(isPrimaryLegBuy) {
    		return primaryLeg.getNotional();
    	}
    	else {
    		return secondaryLeg.getPrincipal();
    	}
    }
    else if (columnName.equals("PURC_NPV_L")) {
    	Double amount = getPLMark(plMarkValue,"NPV_LEG1");
    	if(null!=amount) {
    		return amount;
    	}
    	else {
    		return 0.0;
    	}
    }	
    else if (columnName.equals("SALE_NPV_L")) {
    	Double amount = getPLMark(plMarkValue,"NPV_LEG2");
    	if(null!=amount) {
    		return amount;
    	}
    	else {
    		return 0.0;
    	}
    }	
    else if (columnName.equals("PARTENON_TRADE_ID")) {
    	return trade.getKeywordValue("PartenonAccountingID");
    }
    else if (columnName.equals("PARTENON_TRADE_ID_NEAR")) {
    	return "";
    }	
    else if (columnName.equals("MTM")) {
    	Double amount = getPLMark(plMarkValue,"NPV");
    	if(null!=amount) {
    		return amount;
    	}
    	else {
    		return 0.0;
    	}
    }	
    else if (columnName.equals("IS_SPOT")) {
    	return isSpot(trade, perfSwap, primaryLeg, secondaryLeg);
    }
    else if (columnName.equals("PRODUCTO_CONCRETO")) {
    	return "Bond Return Swap";
    }
    else {
    	return super.getColumnValue(row, columnName, errors);
    }
  } 

    
  private boolean isPrimaryLegBuy(Trade trade) {  
	    
	  PerformanceSwap perfSwap = (PerformanceSwap) trade.getProduct();
	  if(perfSwap ==null) {
		  return false;
	  }
	  
	  PerformanceSwapLeg primaryLeg = (PerformanceSwapLeg) perfSwap.getPrimaryLeg();
	  if(primaryLeg ==null) {
		  return false;
	  }
	
	  if(primaryLeg.getBuySell(trade)==1){
		  return true;
	  }
	  else {
		  return false;
	  }
  }
  
  
  private String isSpot(Trade trade, PerformanceSwap perfSwap, PerformanceSwapLeg primaryLeg, SwapLeg secondaryLeg) {    
	  JDate tradeDate = trade.getTradeDate().getJDate(TimeZone.getDefault());
	  JDate maturityDate = trade.getMaturityDate();
	  CurrencyPair ccyPair = perfSwap.getCurrencyPair(); 
	  final Vector<String> holidays = new Vector<String>();
	  final CurrencyDefault ccy1 = LocalCache.getCurrencyDefault(primaryLeg.getCurrency());
      final CurrencyDefault ccy2 = LocalCache.getCurrencyDefault(secondaryLeg.getCurrency());
      holidays.addAll(ccy1.getDefaultHolidays());
      holidays.addAll(ccy2.getDefaultHolidays());
      if(tradeDate.gte(maturityDate.addBusinessDays(0 - getSpotDays(ccyPair), holidays))) {
    	  return "Y";
      }
      else {
    	  return "N";
      }
  }
  
  
  public int getSpotDays(final CurrencyPair pair) {
	    if (pair != null) {
	      final int spotDays = pair.getSpotDays();
	      if (spotDays < 0) {
	        final int primarySpotDays = pair.getPrimary()
	            .getDefaultSpotDays();
	        final int secundarySpotDays = pair.getQuoting()
	            .getDefaultSpotDays();
	        if (primarySpotDays > secundarySpotDays) {
	          return primarySpotDays;
	        } else {
	          return secundarySpotDays;
	        }
	      } else {
	        return spotDays;
	      }
	    } else {
	      return 2;
	    }
  }
  
 
  private PLMark getPLMarkValue(PricingEnv pricingEnv, Trade trade, JDate date) {
      PLMark plMark = null;
      if(null!=date && null!=pricingEnv){
          date = date.addBusinessDays(-1,Util.string2Vector("SYSTEM"));
      }
      try {
          plMark = CollateralUtilities.retrievePLMark(trade, DSConnection.getDefault(),
                  pricingEnv.getName(), date);
          return plMark;
      } catch (RemoteException e) {
          Log.error(this, e);
          return null;

      }
  }
  
  
  private Double getPLMark(PLMark plMark, String type){
      return null!=plMark && null!=plMark.getPLMarkValueByName(type) ? plMark.getPLMarkValueByName(type).getMarkValue() : 0.0D;
  }


}