package calypsox.tk.util;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.bo.BOPostingUtil;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.BalancePosition;
import com.calypso.tk.bo.accounting.keyword.KeywordUtil;
import com.calypso.tk.core.Action;
import com.calypso.tk.core.Book;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InstantiateUtil;
import com.calypso.tk.util.PostingArray;
import com.calypso.tk.util.ScheduledTaskREPORT;
import com.calypso.tk.util.TradeArray;

import calypsox.util.SantCalypsoUtilities;
import calypsox.util.collateral.CollateralUtilities;


public class ScheduledTaskBRS_MATURITY extends ScheduledTaskREPORT{


	public static final String REPORT_TYPE = "REPORT TYPE";
	public static final String REPORT_TEMPLATE_NAME_FMM = "REPORT TEMPLATE NAME FFM";
	public static final String REPORT_TEMPLATE_NAME_FCM = "REPORT TEMPLATE NAME FCM";
	long[] tradeLongIdList;

	@Override
	public String getTaskInformation() {
		return "Create mature Posting for balance FFM and FCM";
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Vector<String> getAttributeDomain(final String attr, final Hashtable currentAttr) {
		return super.getAttributeDomain(attr, currentAttr);
	}

	
    @SuppressWarnings("unchecked")
	@Override
    public Vector<String> getDomainAttributes() {
        Vector<String> domainAttributes = super.getDomainAttributes();
        domainAttributes.add(REPORT_TEMPLATE_NAME_FMM);
        domainAttributes.add(REPORT_TEMPLATE_NAME_FCM);
        return domainAttributes;
    }
	
	
	@SuppressWarnings({ "rawtypes" })
	@Override
	public boolean isValidInput(final Vector messages) {
		super.isValidInput(messages);
		return messages.isEmpty();
	}
	
	
	@Override
	public boolean process(DSConnection dsCon, PSConnection ps) {

		String reportType = getAttribute(REPORT_TYPE);
		String templateNameFFM = getAttribute(REPORT_TEMPLATE_NAME_FMM);
		String templateNameFCM = getAttribute(REPORT_TEMPLATE_NAME_FCM);
		PostingArray postingList = new PostingArray();
		
		try {
 			DefaultReportOutput outputFFM = null;
    		DefaultReportOutput outputFCM = null;
    		try {
    			outputFFM = (DefaultReportOutput) generateReportOutput(dsCon, reportType, templateNameFFM, new JDatetime());
    			outputFCM = (DefaultReportOutput) generateReportOutput(dsCon, reportType, templateNameFCM, new JDatetime());
    		} catch (RemoteException e) {
    			Log.error(LOG_CATEGORY, e);
    		}
    	     
    		
    		// FFM_Balances
    		if(outputFFM != null) {
	    		ReportRow[] rowsFFM = outputFFM.getRows();
	    		Map<String, Double> endListFFM = new HashMap<String, Double>();
	    		for (int i = 0; i < rowsFFM.length; i++) {
	    			BalancePosition balance = (BalancePosition) rowsFFM[i].getProperties().get("BalancePosition");
	    			long tradeId = balance.getTradeLongId();
	    			int accountId = balance.getAccountId();
	    			Account account = dsCon.getRemoteAccounting().getAccount(accountId);
	    			String accountName = account.getName();
	    			String posicion = getAccountProperty(account, "Posicion");
	    			String currency = balance.getCurrency();
	    			Double movement = balance.getChange(); 
	    			String key = tradeId + "-" + accountId + "-" + posicion + "-" + accountName + "-" + currency ;
	    			if(null == endListFFM.get(key)){
	    				endListFFM.put(key, movement);
	    			}
	    			else {
	    				Double sum = movement + endListFFM.get(key);
	    				endListFFM.put(key, sum);
	    			}
	    		}
	    		for (Map.Entry<String, Double> entry : endListFFM.entrySet()) {
	    			String key = entry.getKey();
	    			Double value = entry.getValue();
	    		    //System.out.println(key + " - " + value);
	    		    BOPosting posting = createPostings(dsCon,key,value);
	    		    if(posting != null) {
	    		    	postingList.add(posting);
	    		    }
	    		}
    		}
    		
    		// FCM_Balances
    		if(outputFCM != null) {
	    		ReportRow[] rowsFCM = outputFCM.getRows();
	    		Map<String, Double> endListFCM = new HashMap<String, Double>();
	    		for (int i = 0; i < rowsFCM.length; i++) {
	    			BalancePosition balance = (BalancePosition) rowsFCM[i].getProperties().get("BalancePosition");
	    			long tradeId = balance.getTradeLongId();
	    			int accountId = balance.getAccountId();
	    			Account account = dsCon.getRemoteAccounting().getAccount(accountId);
	    			String accountName = account.getName();
	    			String posicion = getAccountProperty(account, "Posicion");
	    			String currency = balance.getCurrency();
	    			Double movement = balance.getChange(); 
	    			String key = tradeId + "-" + accountId + "-" + posicion + "-" + accountName + "-" + currency ;
	    			if(null == endListFCM.get(key)){
	    				endListFCM.put(key, movement);
	    			}
	    			else {
	    				Double sum = movement + endListFCM.get(key);
	    				endListFCM.put(key, sum);
	    			}
	    		}
	    		for (Map.Entry<String, Double> entry : endListFCM.entrySet()) {
	    			String key = entry.getKey();
	    			Double value = entry.getValue();
	    		    //System.out.println(key + " - " + value);
	    		    BOPosting posting = createPostings(dsCon,key,value);
	    		    if(posting != null) {
	    		    	postingList.add(posting);
	    		    }
	    		}
    		}
    		
    		// Save postings
    		if(postingList != null && postingList.size()>0 ) {
    			dsCon.getRemoteBO().savePostings(postingList);
    		}
    		
    		// Remove the keyword "MatureCash"
    		String filter = this.getTradeFilter();
    		TradeFilter tradeFilter = dsCon.getRemoteReferenceData().getTradeFilter(filter);
    		TradeArray tradeArray = SantCalypsoUtilities.getInstance().getTradesWithTradeFilter(this.tradeLongIdList);
            if (!Util.isEmpty(tradeArray)) {
                for (int i = 0; i < tradeArray.size(); i++) {
                	Trade trade = (Trade) tradeArray.get(i);
        			trade.removeKeyword("MatureCash");
        			trade.setAction(Action.AMEND);
        			if(CollateralUtilities.isTradeActionApplicable(trade,Action.AMEND)){
	        			try {
							dsCon.getRemoteTrade().save(trade);
						} catch (RemoteException ex) {
							Log.error(this, ex);
						}
        			}
                }
            }
    		return true;
		} catch (CalypsoServiceException e) {
			return false;
		}
	}


    private String getAccountProperty(Account account, String propName) {
    	Vector <String> properties = account.getAccountProperties();
        String propValue = "";
        if (Util.isEmpty(properties)) {
            return propValue;
        }
        for (int i = 0; i < properties.size(); i++) {
            String prop = properties.get(i);
            if (prop.equals(propName)) {
            	if ((i + 1) < properties.size()) {
                    propValue = properties.get(i + 1);
                    return propValue;
                }
            }
        }
        return propValue;
    }


//	@SuppressWarnings("unchecked")
//    private Account getAccountByAttrMaturitySource(DSConnection dsCon, String maturitySource) {
//    	String propName = "MaturitySource";
//		try {
//			final Vector <Account> accounts = dsCon.getRemoteAccounting().getAccounts(true);
//			if(accounts!=null && accounts.size()>0) {
//				for (final Account account : accounts) {
//					account.getParentAccountId();
//					Vector <String> properties = account.getAccountProperties();
//			        if (Util.isEmpty(properties)) {
//			            continue;
//			        }
//			        for (int i = 0; i < properties.size(); i++) {
//			            String prop = properties.get(i);
//			            if (prop.equals(propName) && maturitySource.equals(properties.get(i+1))) {
//			            	return account;
//			            }
//			        }
//				}
//			}
//		} catch (final RemoteException e) {
//			final StringBuffer message = new StringBuffer();
//			message.append("Couldn't load the accounts ");
//		}
//    	return null;
//    }


	@SuppressWarnings({ "unchecked", "rawtypes" })
	private BOPosting createPostings(DSConnection dsCon, String key, Double value) {		
		BOPosting posting = new BOPosting();
		String[] parts = key.split("-");
		long tradeId = Long.parseLong(parts[0]);
		int accountId = Integer.parseInt(parts[1]);
		String position = parts[2];
		String accountName = parts[3];
		String accountAutoName = accountName.substring(4);
		String currency = parts[4];
    	Account account = BOCache.getAccount(dsCon, accountId);
    	String accountCurrency = account.getName().substring(0,3);
		Trade trade = null;
		try {
			trade = dsCon.getRemoteTrade().getTrade(tradeId);
		} catch (CalypsoServiceException e) {
			Log.error(this, e);
		}
		if (trade==null) {
			return null;
		}
		Book book = trade.getBook();
		JDatetime valuationDate = new JDatetime();
		JDate valDate = valuationDate.getJDate();
		
		posting.setId(0L);
		posting.setTradeId(tradeId);
		posting.setLinkedId(0);
		posting.initProcessFlags();
    	posting.setStatus("NEW");
    	posting.setPostingType("NEW");
    	posting.setOriginalEventType("MATURED_TRADE");
		//posting.setEffectiveDate(valDate);
		posting.setEffectiveDate(super.getValuationDatetime().getJDate(TimeZone.getDefault()));
		posting.setBookId(book.getId());
    	posting.setBookingDate(valDate);
    	posting.setCreationDate(valuationDate);
    	posting.setAmount(Math.abs(value));    	
    	posting.setCurrency(accountCurrency);
    	posting.setEventType("MATURE");
    	posting.setDescription("NONE");
    	posting.addAttribute("ORIGINAL_CCY" , accountCurrency);
    	if(!accountCurrency.equalsIgnoreCase(currency)) {
    		posting.addAttribute("FxTranslationOnly", "true");
    		return null;
    	}
    	
		Account newAccount = null;
		try {
			//dsCon.getRemoteAccounting().getAccountByAttribute("MaturitySource", position);
			Vector<Account> accountList = dsCon.getRemoteAccounting().getAccountByAttribute("MaturitySource", accountAutoName);
			if(null != accountList && accountList.size()>0) {
				newAccount = accountList.get(0);
			}
			else {
				Log.info(this, "There is not any account with the attribute 'MaturitySource' set to " + position);
				return null;
			}
		} catch (CalypsoServiceException e1) {
			Log.error(this, e1);
		}
		
        Vector errors = new Vector<String>();
        Vector<String> attributeValues = new Vector<String>();
        String accountResult = KeywordUtil.fillAccount(posting, newAccount, trade,(BOTransfer)null, (BOPosting)null, dsCon, errors, attributeValues);
        Account autoAccount = BOPostingUtil.getAccount(accountResult, book.getProcessingOrgBasedId(), posting.getCurrency());
        if (autoAccount == null) {
            autoAccount = newAccount.generateAccount(posting, trade, (BOTransfer)null, dsCon, new Vector<String>());
            Vector<Account> accsToSave = new Vector<>();
            accsToSave.add(autoAccount);
            try {
    			dsCon.getRemoteAccounting().saveAccounts(accsToSave);
    		} catch (CalypsoServiceException e) {
    			Log.error(this, e);
    		}    
        }
        int autoAccountId = autoAccount.getId();
        if(autoAccountId==0) {
        	Account acc = BOCache.getAccount(dsCon, accountResult);
        	autoAccountId = acc.getId();
        }
   
        if(value<0) {
            posting.setDebitAccountId(autoAccount.getId());
    		posting.setCreditAccountId(accountId);	
    	}
    	else {
    		posting.setDebitAccountId(accountId);
    		posting.setCreditAccountId(autoAccount.getId());
    	}
        posting.setCurrency(currency);
		return posting;
	}


	protected ReportOutput generateReportOutput(DSConnection dsCon, String type, String templateName, JDatetime valDatetime) throws RemoteException {
		PricingEnv env = dsCon.getRemoteMarketData().getPricingEnv("DirtyPrice", valDatetime);
		Report reportToFormat = createReport(dsCon, type, templateName, env);
		if (reportToFormat == null) {
			Log.info(this, "Invalid report type: " + type + " or no trades to process.");
			return null;
		} else if (reportToFormat.getReportTemplate() == null) {
			Log.error(this, "Invalid report template: " + type);
			return null;
		} else {
			Vector<String> holidays = new Vector<String>();
			holidays.add("SYSTEM");
			reportToFormat.getReportTemplate().setHolidays(holidays);
			if (TimeZone.getDefault() != null) {
				reportToFormat.getReportTemplate().setTimeZone(TimeZone.getDefault());
			}
			Vector<String> errorMsgs = new Vector<String>();
			return reportToFormat.load(errorMsgs);
		}
	}


	@SuppressWarnings("deprecation")
	protected Report createReport(DSConnection dsCon, String type, String templateName, PricingEnv env) throws RemoteException {

		String filter = this.getTradeFilter();
		TradeFilter tradeFilter = dsCon.getRemoteReferenceData().getTradeFilter(filter);
		this.tradeLongIdList = DSConnection.getDefault().getRemoteTrade().getTradeIds(tradeFilter, null, false);
		Vector<Long> tradeIdList = new Vector<Long>();
		
		if(this.tradeLongIdList==null) {
			Log.info(this, "There is no trades to process.");
			return null;
		}
			
		for (Long tradeId : this.tradeLongIdList) {
			tradeIdList.add(tradeId);
		}
		
		Report report;
		try {
			String template = "tk.report." + type + "Report";
			report = (Report) InstantiateUtil.getInstance(template, true);
			report.setPricingEnv(env);
			report.setValuationDatetime(new JDatetime());
		} catch (Exception arg7) {
			Log.error(this, arg7);
			report = null;
		}
		if (report != null && !Util.isEmpty(templateName)) {
			ReportTemplate template1 = dsCon.getRemoteReferenceData().getReportTemplate(ReportTemplate.getReportName(type), templateName);
			if (template1 == null) {
				Log.error(this, "Template " + templateName + " Not Found for " + type + " Report");
			} else {
				report.setReportTemplate(template1);
				template1.setValDate(new JDatetime().getJDate());
				template1.callBeforeLoad();
			}
		}
		report.getReportTemplate().put("TradeIds", tradeIdList);
		//report.getReportTemplate().put("TradeIds", tradeLongIdList);
		return report;
	}


}