package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.AccountingBook;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.AccountingRule;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CreArray;
import com.calypso.tk.util.TradeArray;

import calypsox.tk.bo.cremapping.util.BOCreConstantes;
import calypsox.tk.core.SantanderUtil;
import calypsox.util.collateral.CollateralUtilities;

public class EquityContaCarteraReport extends EquityMisPlusCarteraReport {
	private static final String CRES_WHERE_CLAUSE = "bo_cre.BO_CRE_TYPE = 'REALIZED_PL' "
			+ "and bo_cre.CRE_TYPE in ('NEW', 'REVERSAL') "
			+ "and bo_cre.PRODUCT_ID IN (";
	private static final String PRODUCTS_WHERE_CLAUSE = "product_desc.product_id in (";
	private static final String ACC_RULE_GESTION = "RV_Conta_Gestion";
	private static final String ACC_RULE_REAL = "RV_Conta_Real";
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("rawtypes")
    @Override
    public ReportOutput load(Vector errorMsgs) {
        StandardReportOutput output = new StandardReportOutput(this);
        DefaultReportOutput coreOutput = (DefaultReportOutput)super.load(errorMsgs);
        
        if (coreOutput == null) {
			return null;
        }
        
        AccountingRule accRuleGestion = null;
        AccountingRule accRuleReal = null;
		try {
			accRuleGestion = DSConnection.getDefault().getRemoteAccounting().getAccountingRule(ACC_RULE_GESTION);
			accRuleReal= DSConnection.getDefault().getRemoteAccounting().getAccountingRule(ACC_RULE_REAL);
		} catch (CalypsoServiceException e2) {
			Log.error(this, "Could not find Acc Rule Gestion or Real - Aborting");
			return null;
		}
        
        ReportTemplate reportTemplate = getReportTemplate();
        String aggregationType = reportTemplate.get("AGGREGATION");
//        String tradeFilterS = reportTemplate.get("TRADE_FILTER");
        String reportTemplateName = reportTemplate.getTemplateName();
        boolean isGestionTemplate = false;
        if (reportTemplateName.contains("Gestion")) {
        	isGestionTemplate = true;
		}
        TradeFilter tradeFilter = null;
//        if (!Util.isEmpty(tradeFilterS)) {
//	        try {
//				tradeFilter = DSConnection.getDefault().getRemoteReferenceData().getTradeFilter(tradeFilterS);
//			} catch (CalypsoServiceException e1) {
//				Log.error(this, "Could not retrieve Report Template Trade Filter");
//			}
//        }
        
		ReportRow[] rows = coreOutput.getRows();
		try {
			Set<Integer> productIds = new HashSet<Integer>();
			
			for (int i = 0; i < rows.length; i++) {
				ReportRow row = rows[i];
				HashMap<String, Object> defaultProperties = (HashMap<String, Object>)row.getProperty(ReportRow.DEFAULT);
				Integer productId = (Integer)defaultProperties.get("Product Id");

				productIds.add(productId);
			}
			
			// get ALL products
			HashMap<Integer, Product> productsMap = getProducts(productIds);
			
			// get ALL Cres for ALL products
			HashMap<Integer, ArrayList<BOCre>> cresPerProduct = getCresForProduct(productIds, tradeFilter);
			
			Set<JDate> allEffectiveDates = new HashSet<JDate>();
			Set<String> allCurrencies = new HashSet<String>();
			Set<Integer> allBooks = new HashSet<Integer>();
			for (Integer productId : productIds) {
				ArrayList<BOCre> cres = cresPerProduct.get(productId);
				if (Util.isEmpty(cres)) {
					continue;
				}
				
				for (int i = 0; i < cres.size(); i++) {
					BOCre cre = cres.get(i);
					String currency = cre.getCurrency(0);
					
					allBooks.add(cre.getBookId());
					
					if (currency.equals(SantanderUtil.EUR)) {
						continue;
					}
					
					allEffectiveDates.add(cre.getEffectiveDate());
					allCurrencies.add(currency);
				}
			}
			
			// get ALL Quotes
			HashMap<String, QuoteValue> quotesMap = getQuoteValues(allEffectiveDates, allCurrencies);
			
			// get ALL Books
			HashMap<String, Book> booksMap = getBooks(allBooks);
			
			Log.info(this, "CALC|ProductID|CRE ID|ACC Rule ID|Cre Type|Cre Effective Date|Cre Positive/Negative|Cre Amount|Cre Amount for calculation|Quote for EUR conversion|Cre Amount converted for calculation");
			for (int i = 0; i < rows.length; i++) {
				ReportRow row = rows[i];
				HashMap<String, Object> defaultProperties = (HashMap<String, Object>)row.getProperty("Default");

				Integer rowProductId = (Integer)defaultProperties.get("Product Id");
				
				String rowBookS = (String)defaultProperties.get("Book");
				Book rowBook = getBook(reportTemplate.getTemplateName(), defaultProperties, booksMap, productsMap);
				
				row.setProperty(EquityContaCarteraReportStyle.STRATEGY, getStrategy(rowBook));
				
				ArrayList<BOCre> cres = cresPerProduct.get(rowProductId);
				if (Util.isEmpty(cres)) {
					StringBuilder strBld = new StringBuilder();
					addRowInfo(strBld, row, rowProductId, rowBook, rowBookS);
					strBld.append(" - No CRE found.");
					Log.info(this, strBld.toString());
					continue;
				}
				
				int accRuleIdToUse = accRuleReal.getId();
				if (isGestionTemplate) {
					for (BOCre cre : cres) {
						if (cre.getAccountingRuleId() == accRuleGestion.getId()) {
							accRuleIdToUse = accRuleGestion.getId();
						}
					}
				}
				
				double cres_realized_pos = 0.0;
				double cres_realized_neg = 0.0;
				double cres_realized_pos_eur = 0.0;
				double cres_realized_neg_eur = 0.0;
				for (BOCre cre : cres) {
					// Only sum same Year CREs
					if (cre.getEffectiveDate().getYear() != getValDate().getYear()) {
						StringBuilder strBld = new StringBuilder();
						addRowInfo(strBld, row, rowProductId, rowBook, rowBookS);
						strBld.append(" - Ignoring Cre of another Year: ");
						addCreInfo(strBld, cre);
						Log.info(this, strBld.toString());
						continue;
					}
					
					// Only sum CREs of given ACC Rule ID
					if (cre.getAccountingRuleId() != accRuleIdToUse) {
						StringBuilder strBld = new StringBuilder();
						addRowInfo(strBld, row, rowProductId, rowBook, rowBookS);
						strBld.append(" - Ignoring Cre of another Accounting Rule: ");
						addCreInfo(strBld, cre);
						Log.info(this, strBld.toString());
						continue;
					}
					
					// Only consider Same Book
					if (aggregationType != null && aggregationType.equals("BookName")) {
						boolean skip = false;
						
						if (rowBook == null) {
							Log.error(this, "Error: Aggregation is BookName and row Book has not been found");
							skip = true;
						}
						else if (rowBook.getId() != cre.getBookId()) {
							skip = true;
						}
						
						if (skip) {
							StringBuilder strBld = new StringBuilder();
							addRowInfo(strBld, row, rowProductId, rowBook, rowBookS);
							strBld.append(" - Ignoring Cre of another Book: ");
							addCreInfo(strBld, cre);
							Log.info(this, strBld.toString());
							continue;
						}
					}

					// Only consider Same Legal Entity
					if (aggregationType != null && aggregationType.equals("LegalEntity")) {
						boolean skip = false;
						Book creBook = DSConnection.getDefault().getRemoteReferenceData().getBook(cre.getBookId());

						if (rowBook == null) {
							Log.error(this, "Error: Aggregation is BookName and row Book has not been found");
							skip = true;
						}
						else if (rowBook.getLegalEntity().getId() != creBook.getLegalEntity().getId()) {
							skip = true;
						}

						if (skip) {
							StringBuilder strBld = new StringBuilder();
							addRowInfo(strBld, row, rowProductId, rowBook, rowBookS);
							strBld.append(" - Ignoring Cre of another Book Legal Entity: ");
							addCreInfo(strBld, cre);
							Log.info(this, strBld.toString());
							continue;
						}
					}

					// Remove CRE from other ACC Rules
					if (cre.getAccountingRuleId() != accRuleGestion.getId() && cre.getAccountingRuleId() != accRuleReal.getId()) {
						StringBuilder strBld = new StringBuilder();
						addRowInfo(strBld, row, rowProductId, rowBook, rowBookS);
						strBld.append(" - Ignoring Cre of another Accounting Rule: ");
						addCreInfo(strBld, cre);
						Log.info(this, strBld.toString());
						continue;
					}

					StringBuilder strBld = new StringBuilder();
					addRowInfo(strBld, row, rowProductId, rowBook, rowBookS);
					strBld.append(" - Using Cre for calculation : ");
					addCreInfo(strBld, cre);
					Log.info(this, strBld.toString());
					
					double creAmount = cre.getAmount(0);
					String creCurrency = cre.getCurrency(0);
					
					double quote = 1.0d;
					if (!creCurrency.equals(SantanderUtil.EUR)) {
						String quoteKey = creCurrency + "-" + cre.getEffectiveDate();
						QuoteValue quoteV = quotesMap.get(quoteKey);

						if (quoteV != null) {
							quote = quoteV.getClose();
						}
						else {
							Log.error(this, "Error: no Quote found for " + SantanderUtil.FX_EUR + quoteKey);
						}
					}
					
					if (cre.getEventType().equals(BOCreConstantes.REALIZED_PL)) {
						logCalc(cre, quote);
						int sign = 1;
						if (cre.getCreType().equals(BOCre.REVERSAL)) {
							sign = -1;
						}
						if (creAmount > 0.0d) {
							cres_realized_pos += sign * creAmount;
							cres_realized_pos_eur  += (sign * creAmount) / quote;
						}
						else {
							cres_realized_neg += sign * creAmount;
							cres_realized_neg_eur  += (sign * creAmount) / quote;
						}
					}
				}
				
				row.setProperty(EquityContaCarteraReportStyle.B_VEN_DIV, cres_realized_pos);
				row.setProperty(EquityContaCarteraReportStyle.P_VEN_DIV, cres_realized_neg);
				row.setProperty(EquityContaCarteraReportStyle.B_VENTAS, cres_realized_pos_eur);
				row.setProperty(EquityContaCarteraReportStyle.P_VENTAS, cres_realized_neg_eur);
			}
		}
		catch (CalypsoServiceException e) {
			Log.error(this, e.toString());
		}

        output.setRows(coreOutput.getRows());
        return output;
    }
    
    private void logCalc(BOCre cre, double quote) {
    	double creAmount = cre.getAmount(0);
    	int sign = 1;
		if (cre.getCreType().equals(BOCre.REVERSAL)) {
			sign = -1;
		}
    	
    	StringBuilder strBld = new StringBuilder();
    	strBld.append("CALC|");
    	strBld.append(cre.getProductId());
    	strBld.append("|");
    	strBld.append(cre.getId());
    	strBld.append("|");
    	strBld.append(cre.getAccountingRuleId());
    	strBld.append("|");
    	strBld.append(cre.getCreType());
    	strBld.append("|");
    	strBld.append(cre.getEffectiveDate());
    	strBld.append("|");
    	if (creAmount > 0.0d) {
    		strBld.append("POS");
    	}
    	else {
    		strBld.append("NEG");
    	}
    	strBld.append("|");
    	strBld.append(creAmount);
    	strBld.append("|");
    	strBld.append(sign * creAmount);
    	strBld.append("|");
    	strBld.append(quote);
    	strBld.append("|");
    	strBld.append((sign * creAmount) / quote);
    	Log.info(this, strBld.toString());
    }

	private Book getBook(String templateName, HashMap<String, Object> defaultProperties, HashMap<String, Book> booksMap, HashMap<Integer, Product> allProducts) throws CalypsoServiceException {
		String rowBookS = null;
		
		
//		if (!Util.isEmpty(templateName) && templateName.contains("Gestion")){
//			rowBookS = (String)defaultProperties.get("Book");
//		}
//		else if (!Util.isEmpty(templateName) && templateName.contains("Real")){
			rowBookS = (String)defaultProperties.get("Book");

		//           if (Util.isEmpty(rowBookS)) {
		//           	rowBookS = "ESSR_REPOSMM";
//            }
//        }
		
		if (Util.isEmpty(rowBookS)) {
			return null;
		}
		
		Book rowBook = booksMap.get(rowBookS);
		if (rowBook == null) {
			Log.info(this, "Book is not present in BookMap, getting it from Calypso: " + rowBookS);
			rowBook = DSConnection.getDefault().getRemoteReferenceData().getBook(rowBookS);
			booksMap.put(rowBookS, rowBook);
		}
		
		return rowBook;
    }
    
    private String getStrategy(Book book) {
    	String strategy = "NEG";
    	if (book != null) {
    		AccountingBook accBook = book.getAccountingBook();
    		if (accBook != null) {
    			String accBookName = accBook.getName();
    			if (accBookName.equals("Negociacion")) {
    				strategy = "NEG";
    			}
    			else if (accBookName.equals("Inversion crediticia")) {
    				strategy = "COS";
    			}
				else if (accBookName.equals("Otros a valor razonable")) {
					strategy = "OVR";
				}
    		}
    	}
		
		return strategy;
    }
    
    private void addRowInfo(StringBuilder strBld, ReportRow row, Integer rowProductId, Book rowBook, String rowBookS) {
    	strBld.append(" Row Product ID: ");
		strBld.append(rowProductId);
		strBld.append(", Book ID: ");
		if (rowBook != null) {
			strBld.append(rowBook.getId());
		}
		else {
			strBld.append("[No BOOK related to CREs, Book Name in ROW: ");
			strBld.append(rowBookS);
			strBld.append("]");
		}
	}

	private void addCreInfo(StringBuilder strBld, BOCre cre) {
		if (cre == null) {
			strBld.append("  No CRE.");
			return;
		}
		strBld.append("\n");
		strBld.append("  Cre ID: ");
    	strBld.append(cre.getId());
    	strBld.append("\n");
    	strBld.append("  Cre Type: ");
    	strBld.append(cre.getCreType());
    	strBld.append("\n");
		strBld.append("  Event Type: ");
		strBld.append(cre.getEventType());
		strBld.append("\n");
		strBld.append("  Amount 0: ");
		strBld.append(cre.getAmount(0));
		strBld.append("\n");
		strBld.append("  Currency 0: ");
		strBld.append(cre.getCurrency(0));
		strBld.append("\n");
		strBld.append("  Effective Date: ");
		strBld.append(cre.getEffectiveDate());
		strBld.append("\n");
		strBld.append("  Book ID: ");
		strBld.append(cre.getBookId());
		strBld.append("\n");
		strBld.append("  Accounting Rule ID: ");
		strBld.append(cre.getAccountingRuleId());
		strBld.append("\n");
    }
    
	private HashMap<String, Book> getBooks(Set<Integer> allBooks) throws CalypsoServiceException {
		HashMap<String, Book> booksMap = new HashMap<String, Book>();
		
		// We do not want to query the whole DB
		if (Util.isEmpty(allBooks)) {
			return booksMap;
		}
		
		ArrayList<CalypsoBindVariable> bindVariables = new ArrayList<CalypsoBindVariable>();
		String whereClause = getBooksWhereClause(allBooks, bindVariables);
		
		Vector<Book> books = getDSConnection().getRemoteReferenceData().getBooks(null, whereClause, bindVariables);
		
		for (Book book : books) {
			booksMap.put(book.getName(), book);
		}
		
		return booksMap;
	}


	private String getBooksWhereClause(Set<Integer> allBooks, ArrayList<CalypsoBindVariable> bindVariables) {
		StringBuilder strBld = new StringBuilder(" BOOK_ID in (");
		
		for (Integer bookId : allBooks) {
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.INTEGER, bookId));
			strBld.append("?,");
		}
		
		String bookIdList = strBld.toString();
		if (bookIdList.length() == 0) {
			return null;
		}
		
		String whereClause = bookIdList.substring(0, bookIdList.length()-1) + ")";
		
		Log.info(this, "Books where clause : " + whereClause);
		
		return whereClause;
	}


	private HashMap<String, QuoteValue>  getQuoteValues(Set<JDate> allEffectiveDates, Set<String> allCurrencies) throws CalypsoServiceException {
		HashMap<String, QuoteValue> quotesMap = new HashMap<String, QuoteValue>();
		
		// We do not want to query the whole DB
		if (Util.isEmpty(allEffectiveDates)) {
			return quotesMap;
		}
		
		ArrayList<CalypsoBindVariable> bindVariables = new ArrayList<CalypsoBindVariable>();
		String whereClause = getQuotesWhereClause(allEffectiveDates, allCurrencies, bindVariables);
		
		Vector<QuoteValue> quotes = getDSConnection().getRemoteMarketData().getQuoteValues(whereClause);
		
		for (QuoteValue quote : quotes) {
			quotesMap.put(quote.getName().substring(quote.getName().lastIndexOf(".") + 1) + "-" + quote.getDate(), quote);
		}
		
		return quotesMap;
	}

	private String getQuotesWhereClause(Set<JDate> allEffectiveDates, Set<String> allCurrencies,
			ArrayList<CalypsoBindVariable> bindVariables) {
		
		StringBuilder strBld = new StringBuilder(" QUOTE_SET_NAME = 'OFFICIAL' ");

		if (!Util.isEmpty(allEffectiveDates)) {
			strBld.append(" AND QUOTE_DATE in (");
			
			Iterator<JDate> it = allEffectiveDates.iterator();
			while (it.hasNext()) {
				strBld.append(Util.date2SQLString(it.next()));
				if (it.hasNext()) {
					strBld.append(", ");
				}
			}
			strBld.append(")");
		}
		
		if (!Util.isEmpty(allCurrencies)) {
			strBld.append(" AND QUOTE_NAME in (");
			
			Iterator<String> it = allCurrencies.iterator();
			while (it.hasNext()) {
				String ccy = it.next();
				strBld.append("'");
				strBld.append(SantanderUtil.FX_EUR);
				strBld.append(ccy);
				strBld.append("' ");
				if (it.hasNext()) {
					strBld.append(", ");
				}
			}
			strBld.append(" ) ");
		}
		
		String whereClause = strBld.toString();

		Log.info(this, "Quotes where clause : " + whereClause);
		
		return whereClause;
	}

	
	protected HashMap<Integer, Product> getProducts(Set<Integer> productIds) throws CalypsoServiceException {
		HashMap<Integer, Product> productsMap = new HashMap<Integer, Product>();
		
		// We do not want to query the whole DB
		if (Util.isEmpty(productIds)) {
			return productsMap;
		}
		
		List<List<Integer>> productsSplitted = CollateralUtilities.splitCollection(productIds, 999);
		for (List<Integer> subProductsIds : productsSplitted) {
			ArrayList<CalypsoBindVariable> bindVariables = new ArrayList<CalypsoBindVariable>();
			String whereClause = getProductsWhereClause(subProductsIds, bindVariables);

			Vector<Product> products = getDSConnection().getRemoteProduct().getAllProducts(null, whereClause, bindVariables);
			for (Product product : products) {
				productsMap.put(product.getId(), product);
			}
		}
		
		return productsMap;
	}
	
	protected String getProductsWhereClause(List<Integer> productIds, List<CalypsoBindVariable> bindVariables) {
		StringBuilder strBld = new StringBuilder();

		for (Integer productId : productIds) {
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.INTEGER, productId));
			strBld.append("?,");
		}
		
		String productIdList = strBld.toString();
		if (productIdList.length() == 0) {
			return null;
		}
		String whereClause = PRODUCTS_WHERE_CLAUSE + productIdList.substring(0, productIdList.length()-1) + ")";
		
		Log.info(this, "Products where clause : " + whereClause);
		
		return whereClause;
	}

	protected HashMap<Integer, ArrayList<BOCre>> getCresForProduct (Set<Integer> productIds, TradeFilter tradeFilter) throws CalypsoServiceException {
		HashMap<Integer, ArrayList<BOCre>> cresPerProduct = new HashMap<Integer, ArrayList<BOCre>>();
		
		List<List<Integer>> productsSplitted = CollateralUtilities.splitCollection(productIds, 999);
		CreArray allCres = new CreArray();
		for (List<Integer> subProductsIds : productsSplitted) {
			ArrayList<CalypsoBindVariable> bindVariables = new ArrayList<CalypsoBindVariable>();
			String whereClause = getCresWhereClause(subProductsIds, bindVariables);

			CreArray cres = getDSConnection().getRemoteBO().getBOCres(null, whereClause, bindVariables);
			allCres.add(new Vector(Arrays.asList(cres.getCres())));
		}
		HashMap<Long, Trade> cresTrades = getCreTrades(allCres);
		
		for (BOCre cre : allCres) {
			Trade creTrade = cresTrades.get(cre.getTradeLongId());
			if (creTrade == null) {
				StringBuilder strBld = new StringBuilder();
				strBld.append("Cre ");
				strBld.append(cre.getId());
				strBld.append(" (");
				strBld.append(cre.getEventType());
				strBld.append(", ");
				strBld.append(cre.getAmount(0));
				strBld.append(", ");
				strBld.append(cre.getCurrency(0));
				strBld.append(")");
				strBld.append(" ignored : related Trade could not be loaded.");
				strBld.append(cre.getTradeLongId());
				Log.info(this, strBld.toString());
				continue;
			}
//			if (!tradeFilter.accept(creTrade)) {
//				StringBuilder strBld = new StringBuilder();
//				strBld.append("Cre ");
//				strBld.append(cre.getId());
//				strBld.append(" (");
//				strBld.append(cre.getEventType());
//				strBld.append(", ");
//				strBld.append(cre.getAmount(0));
//				strBld.append(", ");
//				strBld.append(cre.getCurrency(0));
//				strBld.append(")");
//				strBld.append(" on Trade ");
//				strBld.append(creTrade.getLongId());
//				strBld.append(" (");
//				strBld.append(creTrade.getStatus());
//				strBld.append(", ");
//				strBld.append(creTrade.getProductType());
//				strBld.append(")");
//				strBld.append(" has not been accepted by filter ");
//				strBld.append(tradeFilter.getName());
//				Log.info(this, strBld.toString());
//				continue;
//			}
			
			ArrayList<BOCre> cresPerOneProduct = cresPerProduct.get(cre.getProductId());
			if (cresPerOneProduct == null) {
				cresPerOneProduct = new ArrayList<BOCre>();
				cresPerProduct.put(cre.getProductId(), cresPerOneProduct);
			}
			cresPerOneProduct.add(cre);
		}
		
		return cresPerProduct;
	}
	
	
	private HashMap<Long, Trade> getCreTrades(CreArray cres) throws CalypsoServiceException {
		Set<Long> allTradeIds = new HashSet<Long>();
		for (BOCre cre : cres) {
			allTradeIds.add(cre.getTradeLongId());
		}
		long[] allTradeIdsArray = new long[allTradeIds.size()];
		int count = 0;
        for (long tradeId : allTradeIds) {
        	allTradeIdsArray[count] = tradeId;
        	count++;
        }
		TradeArray allTrades = getDSConnection().getRemoteTrade().getTrades(allTradeIdsArray);
		HashMap<Long, Trade> cresTrades = new HashMap<Long, Trade>();
		for (Trade trade : allTrades.getTrades()) {
			cresTrades.put(trade.getLongId(), trade);
		}
		
		return cresTrades;
	}


	protected String getCresWhereClause(List<Integer> productIds, List<CalypsoBindVariable> bindVariables) {
		StringBuilder strBld = new StringBuilder();

		for (Integer productId : productIds) {
			bindVariables.add(new CalypsoBindVariable(CalypsoBindVariable.INTEGER, productId));
			strBld.append("?,");
		}
		
		String productIdList = strBld.toString();
		if (productIdList.length() == 0) {
			return null;
		}
		StringBuilder whereClause = new StringBuilder();

		whereClause.append(CRES_WHERE_CLAUSE);
		whereClause.append(productIdList.substring(0, productIdList.length()-1));
		whereClause.append(")");
		whereClause.append(" AND (TRUNC(bo_cre.effective_date) >= ");
		whereClause.append(com.calypso.tk.core.Util.date2SQLString(getFirstDayOfYear()));
        whereClause.append(" AND TRUNC(bo_cre.effective_date) <= ");
        whereClause.append(com.calypso.tk.core.Util.date2SQLString(getValDate()));
		whereClause.append(")");
		
		Log.info(this, "CREs where clause : " + whereClause.toString());
		
		return whereClause.toString();
	}

	private JDate getFirstDayOfYear(){
		JDate date = getValDate();
		int days360 = date.getDayOfMonth();
		days360 += (date.getMonth()-1)*30 - (days360==31? 2: 1);
		return date.substractTenor(days360);
	}
}