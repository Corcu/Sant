package calypsox.tk.report;

import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.core.sql.ioSQL;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.util.TransferArray;

import java.util.*;

public class PdvMISReport extends TradeReport {

	//public static final String TRANSFERS = "TRANSFERS";
	
	
	public static final String YESTERDAY_DIRTY_PRICE_STR = "YesterdayDirtyPrice";
	public static final String DIRTY_PRICE_STR = "DirtyPrice";
	public static final String CLEAN_PRICE_STR = "CleanPrice";
	public static final String OFFICIAL = "OFFICIAL";
	public static final String HOLIDAYS = "Holidays";
	
	public static final String CASH_COLLATERAL = "CASH_COLLATERAL";
	
	private static final String WHERE_CLAUSE = "bo_transfer.transfer_status<>'CANCELLED' "
			+ "and trade.trade_id = bo_transfer.trade_id "
			+ "and bo_transfer.trade_id IN (";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public class PriceCache {
		
		HashMap<Product,HashMap<String,Double>> priceCache = new HashMap<Product,HashMap<String,Double>>();
		
		public void addPrice(Product sec, String type, Double price) {
			HashMap<String,Double>  priceHash = priceCache.get(sec);
			if(priceHash==null) {
				priceHash = new HashMap<String,Double>();
			}
			priceHash.put(type, price);
			priceCache.put(sec, priceHash);
		}
		
		
		public Double getPrice(Product sec, String type) {
			HashMap<String,Double>  priceHash = priceCache.get(sec);
			if(priceHash!=null) {
				return priceHash.get(type);
			}
			return null;
		}
		
	}
	
	HashMap<String, PricingEnv> pEnvs = new HashMap<String, PricingEnv>();
	
	@SuppressWarnings("rawtypes")
	public ReportOutput load(Vector errorMsgs) {
		

		if(calculateDirtyPrice())
			pEnvs.put(DIRTY_PRICE_STR, AppUtil.loadPE(DIRTY_PRICE_STR, getValuationDatetime()));
		
		if(calculateCleanPrice())
			pEnvs.put(CLEAN_PRICE_STR, AppUtil.loadPE(CLEAN_PRICE_STR, getValuationDatetime()));
		
		if(calculateEquityPrice())
			pEnvs.put(OFFICIAL, AppUtil.loadPE(OFFICIAL, getValuationDatetime()));
		
		//
		DefaultReportOutput output = (DefaultReportOutput)super.load(errorMsgs);
		if(output==null)
			return null;
		ReportRow[] rows = output.getRows();
		try {
		
			ArrayList<Trade> trades = new ArrayList<Trade>();
			
			HashMap<Long, ArrayList<BOTransfer>> transfersPerTrades = new HashMap<Long, ArrayList<BOTransfer>>();
		
			for(int i=0; i<rows.length; i++) {
				Trade trade = rows[i].getProperty(ReportRow.TRADE);
				
			/*	Vector<TradeTransferRule> tr = BOProductHandler.buildTransferRules(trade, errorMsgs, DSConnection.getDefault(), trade.isArchived());
				
				
				trade.setTransferRules(tr);*/
				trades.add(trade);
				if(trades.size()>=ioSQL.MAX_ITEMS_IN_LIST) {
					HashMap<Long, ArrayList<BOTransfer>> transfers = getTransferForTrades(trades);
					transfersPerTrades.putAll(transfers);
					trades = new ArrayList<Trade>();
				}
			}
			
			if(trades.size()>0) {
				HashMap<Long, ArrayList<BOTransfer>> transfers = getTransferForTrades(trades);
				transfersPerTrades.putAll(transfers);
			}
		
			
			PriceCache priceCache = new PriceCache();
			
			for(int i=0; i<rows.length; i++) {
				ReportRow row = rows[i];
				Trade trade = row.getProperty(ReportRow.TRADE);
				PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
				JDatetime valDateTime = ReportRow.getValuationDateTime(row);
				JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());
				
				Product product = trade.getProduct();
				
				if(product instanceof SecLending) {
					
					PricingEnv dirtyPricePE = getDirtyPricePricingEnv((SecLending)product);
					PricingEnv cleanPricePE = getCleanPricePricingEnv((SecLending)product);
					
					if(dirtyPricePE!=null) {
									
						if(calculateDirtyPrice()) {
							Double dirtyPrice = priceCache.getPrice(((SecLending)product).getSecurity(), DIRTY_PRICE_STR);
							
							if(dirtyPrice==null) {
								QuoteSet quoteSet = dirtyPricePE.getQuoteSet();
								dirtyPrice = getQuotePrice(((SecLending)product).getSecurity(), quoteSet, valDate, dirtyPricePE, getHolidays(), errorMsgs);
								priceCache.addPrice(((SecLending)product).getSecurity(), DIRTY_PRICE_STR, dirtyPrice);
							}
							
							row.setProperty(DIRTY_PRICE_STR, dirtyPrice);
						}
						
						if(calculateYesterdayDirtyPrice()) {
						
							Double yesterdayDirtyPrice = priceCache.getPrice(((SecLending)product).getSecurity(), YESTERDAY_DIRTY_PRICE_STR);
							
							if(yesterdayDirtyPrice==null) {
								QuoteSet quoteSet = dirtyPricePE.getQuoteSet();
								JDate valDateMinus1 = valDate.addBusinessDays(0, getHolidays());
								yesterdayDirtyPrice = getQuotePrice(((SecLending)product).getSecurity(), quoteSet, valDateMinus1, dirtyPricePE, getHolidays(), errorMsgs);
								priceCache.addPrice(((SecLending)product).getSecurity(), YESTERDAY_DIRTY_PRICE_STR, yesterdayDirtyPrice);
							}
							row.setProperty(YESTERDAY_DIRTY_PRICE_STR, yesterdayDirtyPrice);
						}
						
						
					}
					if(cleanPricePE!=null && calculateCleanPrice()) {
						
						Double cleanPrice = priceCache.getPrice(((SecLending)product).getSecurity(), CLEAN_PRICE_STR);
						
						if(cleanPrice==null) {
							QuoteSet quoteSet = cleanPricePE.getQuoteSet();
							cleanPrice = getQuotePrice(((SecLending)product).getSecurity(), quoteSet, valDate, cleanPricePE, getHolidays(), errorMsgs);
							priceCache.addPrice(((SecLending)product).getSecurity(), CLEAN_PRICE_STR, cleanPrice);
						}
						row.setProperty(CLEAN_PRICE_STR, cleanPrice);
					}
				
				}
				
				ArrayList<BOTransfer> transfers = transfersPerTrades.get(trade.getLongId());
				if(transfers!=null) {
					ArrayList<BOTransfer> secTransfers = getTransfersOfType(transfers,"SECURITY");
					ArrayList<BOTransfer> colTransfers = getTransfersOfType(transfers,"COLLATERAL");
					
					
					if(secTransfers!=null && secTransfers.size()>0) {
						secTransfers.sort(new Comparator<BOTransfer> () {
							@Override
							public int compare(BOTransfer o1, BOTransfer o2) {
								if(o1.getLongId()>o2.getLongId())
									return 1;
								if(o1.getLongId()<o2.getLongId())
									return -1;
								return 0;
							}
						});
						row.setProperty(ReportRow.TRANSFER, secTransfers.get(0));
						row.setProperty(CASH_COLLATERAL, getTransfersSum(colTransfers,valDate));
						
					}
				}
			}
			
		} catch (CalypsoServiceException e) {
			Log.error("PdvMISReport", e);
		}
		
		return output;
	}
	
	protected HashMap<Long, ArrayList<BOTransfer>> getTransferForTrades (List<Trade> trades) throws CalypsoServiceException {
		HashMap<Long, ArrayList<BOTransfer>> transfersPerTrades = new HashMap<Long, ArrayList<BOTransfer>>();
		
		ArrayList<CalypsoBindVariable> bindVariables = new ArrayList<CalypsoBindVariable>();
		String whereClause = getWhereClause(trades,bindVariables);
		
		TransferArray transfers = getDSConnection().getRemoteBO().getTransfers("trade", whereClause, bindVariables);
		for(BOTransfer transfer : transfers) {
			ArrayList<BOTransfer> transferForTrade = transfersPerTrades.get(transfer.getTradeLongId());
			if(transferForTrade==null) {
				transferForTrade = new ArrayList<BOTransfer>();
				transfersPerTrades.put(transfer.getTradeLongId(), transferForTrade);
			}
			transferForTrade.add(transfer);
			
		}
		
		return transfersPerTrades;
		
	}
	
	protected Double getTransfersSum(ArrayList<BOTransfer> transfers, JDate asofDate) {
		
		Double cashAmount = 0.0d;
		
		for(BOTransfer transfer : transfers) {
			if(transfer.getValueDate().lte(asofDate)) {
				cashAmount+=transfer.getSettlementAmount();
			}
		}
		
		return cashAmount;
		
	}
	
	
	protected ArrayList<BOTransfer> getTransfersOfType(ArrayList<BOTransfer> transfers, String type) {
		ArrayList<BOTransfer> result = new ArrayList<BOTransfer>();
		for(BOTransfer transfer : transfers) {
			if(transfer.getTransferType().equals(type)) {
				result.add(transfer);
			}
		}
		return result;
		
	}

	
	protected String getWhereClause(List<Trade> trades, List<CalypsoBindVariable> bindVariables) {
		StringBuilder strBld = new StringBuilder();

		for(Trade trade : trades) {
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.LONG,trade.getLongId()));
			strBld.append("?,");
		}
		String tradeIdList = strBld.toString();
		if(tradeIdList.length()==0) {
			return null;
		}
		String whereClause = WHERE_CLAUSE + tradeIdList.substring(0, tradeIdList.length()-1) + ")";
		return whereClause;
	}
	
    protected Vector getHolidays() {
        Vector holidays = new Vector<>();
        if (getReportTemplate().getHolidays() != null) {
            holidays = getReportTemplate().getHolidays();
        } else {
            holidays.add("SYSTEM");
        }
        return holidays;
    }
    
	
    /**
     * @return close quotePrice for the product
     */
    private Double getQuotePrice(final Product product, final QuoteSet quoteSet, JDate valDate, PricingEnv pEnv,Collection<String> holidays, Vector<String> errors) {

            JDate quoteDate = valDate.addBusinessDays(0, holidays);
            QuoteValue productQuote = quoteSet.getProductQuote(product, quoteDate, getPriceType(pEnv.getName()));

            if ((productQuote != null) && (!Double.isNaN(productQuote.getClose())))
                return productQuote.getClose();

            final String error = "Quote not available for Product ISIN: " + product.getSecCode("ISIN");
            errors.add(error);
            Log.error(this, error);
            return null;
    }
    
    
    public boolean calculateEquityPrice() {
        //if(getReportTemplate().getVisibleColumns().contains(PdvMISReportStyle.DIRTYPRICE)){
        	return true;
        //}
        //return false;
    }
    
    public boolean calculateDirtyPrice() {
        //if(getReportTemplate().getVisibleColumns().contains(PdvMISReportStyle.DIRTYPRICE)){
        	return true;
        //}
        //return false;
    }
    
    public boolean calculateYesterdayDirtyPrice() {
        if(getReportTemplate().getVisibleColumns().contains(PdvMISReportStyle.YESTERDAYPRICE)){
        	return true;
        }
        return false;
    }
    
    public boolean calculateCleanPrice() {
        //if(getReportTemplate().getVisibleColumns().contains(PdvMISReportStyle.CLEANPRICE)){
        	return true;
        //}
        //return false;
    }
    
    
	public static String getCleanPricePricingEnvName(SecLending product) {
		
		String pricePE = "";
		
		if(((SecLending)product).getSecurity() instanceof Bond) {
			pricePE = CLEAN_PRICE_STR;
		}
		else if (((SecLending)product).getSecurity() instanceof Equity) {
			pricePE = OFFICIAL;
		}
		
		return pricePE;
		
	}
    
	public PricingEnv getCleanPricePricingEnv(SecLending product) {
		
		return pEnvs.get(getCleanPricePricingEnvName(product));
		
	}
	
	public PricingEnv getDirtyPricePricingEnv(SecLending product) {
		
		return pEnvs.get(getDirtyPricePricingEnvName(product));
		
	}
	
	
	public static String getPriceType(String penvName) {
		if(penvName.equals(OFFICIAL))
			return "Price";
		else
			return penvName;
	}
	
	public static String getDirtyPricePricingEnvName(SecLending product) {
		
		String pricePE = "";
		
		if(((SecLending)product).getSecurity() instanceof Bond) {
			pricePE = DIRTY_PRICE_STR;
		}
		else if (((SecLending)product).getSecurity() instanceof Equity) {
			pricePE = OFFICIAL;
		}
		
		return pricePE;
		
	}
	
};