package calypsox.tk.report;

import calypsox.tk.pledge.util.TripartyPledgeProrateCalculator;
import calypsox.util.collateral.CollateralUtilities;
import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteSet;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Equity;
import com.calypso.tk.product.Pledge;
import com.calypso.tk.product.Repo;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.TradeReport;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static calypsox.tk.report.RepoMISReportTemplate.PLEDGE_NOMINAL;
import static calypsox.tk.report.RepoMISReportTemplate.PLEDGE_PRINCIPAL;

public class RepoMISReport extends TradeReport {

	//public static final String TRANSFERS = "TRANSFERS";
	
	
	public static final String YESTERDAY_DIRTY_PRICE_STR = "YesterdayDirtyPrice";
	public static final String DIRTY_PRICE_STR = "DirtyPrice";
	public static final String CLEAN_PRICE_STR = "CleanPrice";
	public static final String OFFICIAL = "OFFICIAL";
	public static final String HOLIDAYS = "Holidays";
	
	public static final String CASH_COLLATERAL = "CASH_COLLATERAL";
	
	public static final String IDISSUEFINAN_ISSUE_FINANCIAL = "ISSUE_FINANCIAL";
	public static final String IDISSUEFINAN_ISSUE = "ISSUE";
	public static final String IDISSUEFINAN_FINANCIAL = "FINANCIAL";
	private static final String MUREX_ROOT_CONTRACT = "MurexRootContract";
	PriceCache priceCache = new PriceCache();
	
	private static final String WHERE_CLAUSE = "bo_transfer.transfer_status<>'CANCELLED' "
			+ "and trade.trade_id = bo_transfer.trade_id "
			+ "and bo_transfer.transfer_type IN ( 'SECURITY','COLLATERAL')"
			+ "and bo_transfer.trade_id IN (";

	ConcurrentHashMap<Long, ReportRow> listOfPledge = new ConcurrentHashMap<>();
	ConcurrentHashMap<Long, ReportRow> listOfRepos = new ConcurrentHashMap<>();
	TripartyPledgeProrateCalculator calculator = null;
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

		public void clear(){
			priceCache.clear();
		}
	}
	
	HashMap<String, PricingEnv> pEnvs = new HashMap<String, PricingEnv>();
	
	@SuppressWarnings("rawtypes")
	public ReportOutput load(Vector errorMsgs) {
		init();
		if(calculateDirtyPrice())
			pEnvs.put(DIRTY_PRICE_STR, AppUtil.loadPE(DIRTY_PRICE_STR, getValuationDatetime()));
		
		if(calculateCleanPrice())
			pEnvs.put(CLEAN_PRICE_STR, AppUtil.loadPE(CLEAN_PRICE_STR, getValuationDatetime()));
		
		//
		DefaultReportOutput output = (DefaultReportOutput)super.load(errorMsgs);
		if(output==null)
			return null;
		ReportRow[] rows = output.getRows();
		HashMap<Long, ArrayList<BOTransfer>> transfersPerTrades = new HashMap<Long, ArrayList<BOTransfer>>();

		try {
			ArrayList<Trade> trades = new ArrayList<Trade>();
			for(int i=0; i<rows.length; i++) {
				Trade trade = rows[i].getProperty(ReportRow.TRADE);
				rows[i].setProperty(RepoMISReportStyle.IDISSUEFINAN, "");
				if(trade.getProduct() instanceof Repo){
					listOfRepos.put(trade.getLongId(),rows[i]);
					trades.add(trade);
				}else if(trade.getProduct() instanceof Pledge){
					trades.add(trade);
					listOfPledge.put(trade.getLongId(), rows[i]);
				}
			}

			int size = 200;
			for (int start = 0; start < trades.size(); start += size) {
				int end = Math.min(start + size, trades.size());
				final List<Trade> tradeSubList = trades.subList(start, end);
				final HashMap<Long, ArrayList<BOTransfer>> transferForTrades = getTransferForTrades(tradeSubList);
				transfersPerTrades.putAll(transferForTrades);
			}

		} catch (Exception e) {
			Log.error("RepoMISReport", e);
		}

		processRepos(output,transfersPerTrades,errorMsgs);
		processPledges(output,transfersPerTrades);
		
		return output;
	}

	/**
	 * Process Pledges and calculate prorate for MTM, PRINCIPAL and ACCRUAL, getting cres from father triparty repo
	 */
	private void processPledges(DefaultReportOutput output, HashMap<Long, ArrayList<BOTransfer>> transfersPerTrades){
		listOfPledge.values().forEach( row -> {
			row.setProperty(RepoMISReportStyle.IDISSUEFINAN, "");
			Trade trade = (Trade) row.getProperty(ReportRow.TRADE);
			String internalReference = trade.getInternalReference();
			if(!Util.isEmpty(internalReference)){

				Trade fatherTripartyRepo = getTripartyRepo(internalReference);
				if(null!=fatherTripartyRepo){
					row.setProperty(RepoInformesReport.FATHER_TRIPARTY_REPO,fatherTripartyRepo);
					row.setProperty(RepoMISReportStyle.IDISSUEFINAN,IDISSUEFINAN_ISSUE_FINANCIAL);
					String murexRootContract = fatherTripartyRepo.getKeywordValue(MUREX_ROOT_CONTRACT);
					String murexOrigin = fatherTripartyRepo.getKeywordValue("Mx Origin");
					String murexTradeId = fatherTripartyRepo.getKeywordValue("MurexTradeID");
					String mxrateIndex = fatherTripartyRepo.getKeywordValue("MX_REFINDEX");

					final ArrayList<BOTransfer> boTransfers = transfersPerTrades.get(trade.getLongId());
					row.setProperty(ReportRow.TRANSFER, getLastXfer(boTransfers));
					trade.addKeyword(MUREX_ROOT_CONTRACT,murexRootContract);
					trade.addKeyword("MurexTradeID",murexTradeId);
					trade.addKeyword("Mx Origin",murexOrigin);
					trade.addKeyword("MX_REFINDEX",mxrateIndex);

					calculator.calculate(trade,fatherTripartyRepo,row);

					JDatetime valDateTime = ReportRow.getValuationDateTime(row);
					JDate valDate = valDateTime.getJDate(TimeZone.getDefault());
					generatePrices(row,trade.getProduct(),valDate);

					String fatherRepoCCy = fatherTripartyRepo.getProduct() instanceof Repo ? ((Repo)fatherTripartyRepo.getProduct()).getCurrency() : "";
					String pledgeCCy = trade.getProduct() instanceof Pledge ? ((Pledge)trade.getProduct()).getCurrency() : "";
					row.setProperty(PLEDGE_NOMINAL,trade.computeNominal());
					Pledge pledge = null!=trade.getProduct() && trade.getProduct() instanceof Pledge ? (Pledge)trade.getProduct() : null;
					if(null!=pledge){
						row.setProperty(PLEDGE_PRINCIPAL,pledge.computeNominal(trade,valDate,pledge.getSecurityId()));
					}

					if(!pledgeCCy.equalsIgnoreCase(fatherRepoCCy)){ //MultiCCy duplicate line for pledge
						ReportRow newRow = row.clone();
						row.setProperty(RepoMISReportStyle.IDISSUEFINAN,IDISSUEFINAN_FINANCIAL);
						pledgeMultiCcyRow(row,newRow,pledgeCCy,fatherRepoCCy,valDate);
						newRow.setProperty("Default", row.getProperty("Default") + "-2");
						newRow.setProperty(RepoMISReportStyle.IDISSUEFINAN, IDISSUEFINAN_ISSUE);
						output.addReportRow(null, newRow);
					}
				}
			}
		});
	}

	private void pledgeMultiCcyRow(ReportRow row, ReportRow newRow, String pledgeCCy, String fatherRepoCCy, JDate valDate){
		try {
			double nominal = row.getProperty(PLEDGE_NOMINAL);
			double principal = row.getProperty(PLEDGE_PRINCIPAL);
			final PricingEnv pricingenv = DSConnection.getDefault().getRemoteMarketData().getPricingEnv("OFFICIAL", valDate.getJDatetime(TimeZone.getDefault()));
			principal = CollateralUtilities.convertCurrency(pledgeCCy, principal, fatherRepoCCy, valDate, pricingenv);
			nominal = CollateralUtilities.convertCurrency(pledgeCCy, nominal, fatherRepoCCy, valDate, pricingenv);
			row.setProperty(PLEDGE_NOMINAL,nominal);
			row.setProperty(PLEDGE_PRINCIPAL,principal);

			Amount principalAmpount = newRow.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_PRINCIPAL);
			Amount marketValueMan = newRow.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_MARKETVALUEMAN);
			Amount accrual = newRow.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_ACCRUAL);
			Amount mtm = newRow.getProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_MTM);

			double proratePrincipal = CollateralUtilities.convertCurrency(fatherRepoCCy, principalAmpount.get(), pledgeCCy, valDate, pricingenv);
			double prorateMarketValueMan = CollateralUtilities.convertCurrency(fatherRepoCCy, marketValueMan.get(), pledgeCCy, valDate, pricingenv);
			double prorateAccrual = CollateralUtilities.convertCurrency(fatherRepoCCy, accrual.get(), pledgeCCy, valDate, pricingenv);
			double prorateMTM = CollateralUtilities.convertCurrency(fatherRepoCCy, mtm.get(), pledgeCCy, valDate, pricingenv);

			newRow.setProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_PRINCIPAL,new Amount(proratePrincipal));
			newRow.setProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_MARKETVALUEMAN,new Amount(prorateMarketValueMan));
			newRow.setProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_ACCRUAL,new Amount(prorateAccrual));
			newRow.setProperty(RepoTripartyPledgeReportTemplate.PLEDGE_PRORATE_MTM,new Amount(prorateMTM));

		} catch (Exception e) {
			Log.error(this,"Error converting ccy: " + e);
		}

	}

	private BOTransfer getLastXfer(ArrayList<BOTransfer> boTransfers){
		BOTransfer lastBoXfer = null;
		if(!Util.isEmpty(boTransfers)){
			for (BOTransfer xfer : boTransfers){
				if(!xfer.getIsReturnB()){
					if(lastBoXfer!=null && (lastBoXfer.getLongId() < xfer.getLongId())){
						lastBoXfer = xfer;
					}else {
						lastBoXfer = xfer;
					}
				}
			}
		}
		return lastBoXfer;
	}



	/**
	 *
	 * Process only Repo && Repo Triparty load transfer and pricers
	 * @param output
	 * @param transfersPerTrades
	 * @param errorMsgs
	 */
	private void processRepos(DefaultReportOutput output,HashMap<Long, ArrayList<BOTransfer>> transfersPerTrades,Vector errorMsgs){

		listOfRepos.values().forEach( row -> {
			Trade trade = row.getProperty(ReportRow.TRADE);
			row.setProperty(RepoMISReportStyle.IDISSUEFINAN, "");
			PricingEnv pricingEnv = ReportRow.getPricingEnv(row);
			JDatetime valDateTime = ReportRow.getValuationDateTime(row);
			JDate valDate = valDateTime.getJDate(pricingEnv.getTimeZone());

			Product product = trade.getProduct();

			generatePrices(row,product,valDate);

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

			if(trade.getProduct() instanceof Repo){
				Repo repo = (Repo)trade.getProduct();
				String secCurrency = null!=repo.getSecurity() ? repo.getSecurity().getCurrency() : "";
				String cashCurrency = null!=repo.getCash() ? repo.getCash().getCurrency() : "";
				if (secCurrency.equals(cashCurrency)) {
					row.setProperty(RepoMISReportStyle.IDISSUEFINAN, IDISSUEFINAN_ISSUE_FINANCIAL);
				}
				else {
					row.setProperty(RepoMISReportStyle.IDISSUEFINAN, IDISSUEFINAN_FINANCIAL);

					ReportRow newRow = row.clone();
					newRow.setProperty("Default", row.getProperty("Default") + "-2");
					row.setProperty(RepoMISReportStyle.IDISSUEFINAN, IDISSUEFINAN_ISSUE);
					output.addReportRow(null, newRow);
				}
			}
		});
	}



	private void generatePrices(ReportRow row, Product product,JDate valDate){
		Vector errorMsgs = new Vector();
		PricingEnv dirtyPricePE = null;
		PricingEnv cleanPricePE = null;
		Product security = null;
		if(product instanceof Repo) {
			 security = ((Repo) product).getSecurity();
			 dirtyPricePE = getDirtyPricePricingEnv(security);
			 cleanPricePE = getCleanPricePricingEnv(security);
		}else if(product instanceof Pledge){
			security = ((Pledge) product).getSecurity();
			dirtyPricePE = getDirtyPricePricingEnv(security);
			cleanPricePE = getCleanPricePricingEnv(security);
		}

		if(dirtyPricePE!=null && security!=null) {

			if(calculateDirtyPrice()) {
				Double dirtyPrice = priceCache.getPrice(security, DIRTY_PRICE_STR);

				if(dirtyPrice==null) {
					QuoteSet quoteSet = dirtyPricePE.getQuoteSet();
					dirtyPrice = getQuotePrice(security, quoteSet, valDate, dirtyPricePE, getHolidays(), errorMsgs);
					priceCache.addPrice(security, DIRTY_PRICE_STR, dirtyPrice);
				}
				row.setProperty(DIRTY_PRICE_STR, dirtyPrice);
			}

			if(calculateYesterdayDirtyPrice()) {

				Double yesterdayDirtyPrice = priceCache.getPrice(security, YESTERDAY_DIRTY_PRICE_STR);

				if(yesterdayDirtyPrice==null) {
					QuoteSet quoteSet = dirtyPricePE.getQuoteSet();
					JDate valDateMinus1 = valDate.addBusinessDays(-1, getHolidays());
					yesterdayDirtyPrice = getQuotePrice(security, quoteSet, valDateMinus1, dirtyPricePE, getHolidays(), errorMsgs);
					priceCache.addPrice(security, YESTERDAY_DIRTY_PRICE_STR, yesterdayDirtyPrice);
				}
				row.setProperty(YESTERDAY_DIRTY_PRICE_STR, yesterdayDirtyPrice);
			}
		}
		if(cleanPricePE!=null && calculateCleanPrice() && security!=null) {

			Double cleanPrice = priceCache.getPrice(security, CLEAN_PRICE_STR);

			if(cleanPrice==null) {
				QuoteSet quoteSet = cleanPricePE.getQuoteSet();
				cleanPrice = getQuotePrice(security, quoteSet, valDate, cleanPricePE, getHolidays(), errorMsgs);
				priceCache.addPrice(security, CLEAN_PRICE_STR, cleanPrice);
			}
			row.setProperty(CLEAN_PRICE_STR, cleanPrice);
		}
	}



	/**
	 * Get Repo-Triparty from internal reference of Pledge
	 * @param interalRef
	 * @return
	 */
	private Trade getTripartyRepo(String interalRef){
		try{
			final long fatherTripartyRepoID = Long.parseLong(interalRef);
			if(listOfRepos.containsKey(fatherTripartyRepoID)){
				final ReportRow row = listOfRepos.get(fatherTripartyRepoID);
				return  (Trade) row.getProperty(ReportRow.TRADE);
			}else {
				try {
					return DSConnection.getDefault().getRemoteTrade().getTrade(fatherTripartyRepoID);
				} catch (CalypsoServiceException e) {
					Log.error(this,"Error loading Trade: " + fatherTripartyRepoID + " " + e);
				}
			}
		}catch (Exception e){
			Log.error(this,"Error parsing internalReference " + interalRef + " " + e);
		}
		return null;
	}
	
	protected HashMap<Long, ArrayList<BOTransfer>> getTransferForTrades (List<Trade> trades) {
		HashMap<Long, ArrayList<BOTransfer>> transfersPerTrades = new HashMap<Long, ArrayList<BOTransfer>>();
		try {
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
		}catch (CalypsoServiceException e){
			Log.error(this,"Error loading xfers for trades: " + e);
			if(!Util.isEmpty(trades)){
				Log.error(this,"Error trades: " + trades.toString());
			}
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

            JDate quoteDate = valDate.addBusinessDays(-1, holidays);
            QuoteValue productQuote = quoteSet.getProductQuote(product, quoteDate, getPriceType(pEnv.getName()));

            if ((productQuote != null) && (!Double.isNaN(productQuote.getClose())))
                return productQuote.getClose();

            final String error = "Quote not available for Product ISIN: " + product.getSecCode("ISIN");
            errors.add(error);
            Log.error(this, error);
            return null;
    }
    
    
    public boolean calculateEquityPrice() {
        //if(getReportTemplate().getVisibleColumns().contains(RepoMISReportStyle.DIRTYPRICE)){
        	return true;
        //}
        //return false;
    }
    
    public boolean calculateDirtyPrice() {
        //if(getReportTemplate().getVisibleColumns().contains(RepoMISReportStyle.DIRTYPRICE)){
        	return true;
        //}
        //return false;
    }
    
    public boolean calculateYesterdayDirtyPrice() {
        if(getReportTemplate().getVisibleColumns().contains(RepoMISReportStyle.YESTERDAYPRICE)){
        	return true;
        }
        return false;
    }
    
    public boolean calculateCleanPrice() {
        //if(getReportTemplate().getVisibleColumns().contains(RepoMISReportStyle.CLEANPRICE)){
        	return true;
        //}
        //return false;
    }
    
    
	public static String getCleanPricePricingEnvName(Product product) {
		
		String pricePE = "";
		
		if(product instanceof Bond) {
			pricePE = CLEAN_PRICE_STR;
		}
		else if (product instanceof Equity) {
			pricePE = OFFICIAL;
		}
		
		return pricePE;
		
	}
    
	public PricingEnv getCleanPricePricingEnv(Product product) {
		
		return pEnvs.get(getCleanPricePricingEnvName(product));
		
	}
	
	public PricingEnv getDirtyPricePricingEnv(Product product) {
		
		return pEnvs.get(getDirtyPricePricingEnvName(product));
		
	}
	
	
	public static String getPriceType(String penvName) {
		if(penvName.equals(OFFICIAL))
			return "Price";
		else
			return penvName;
	}
	
	public static String getDirtyPricePricingEnvName(Product product) {
		
		String pricePE = "";
		
		if(product instanceof Bond) {
			pricePE = DIRTY_PRICE_STR;
		}
		else if (product instanceof Equity) {
			pricePE = OFFICIAL;
		}
		
		return pricePE;
		
	}

	private void init(){
		listOfRepos.clear();
		listOfPledge.clear();
		calculator = new TripartyPledgeProrateCalculator();
		priceCache.clear();
	}
	
};