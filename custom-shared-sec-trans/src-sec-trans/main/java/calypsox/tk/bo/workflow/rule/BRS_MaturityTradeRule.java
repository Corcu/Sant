package calypsox.tk.bo.workflow.rule;

import com.calypso.tk.bo.*;
import com.calypso.tk.bo.accounting.keyword.KeywordUtil;
import com.calypso.tk.bo.workflow.WfTradeRule;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.report.*;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.InstantiateUtil;
import com.calypso.tk.util.PostingArray;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

public class BRS_MaturityTradeRule implements WfTradeRule {
	
	public static final String REPORT_TEMPLATE_NAME = "BalanceReport";
	public static final String REPORT_TYPE = "Balance";
	public static final String LOG_CATEGORY = "BRS_MaturityTradeRule";
	TradeFilter tradeFilter = null;

	
    @Override
    public boolean check(TaskWorkflowConfig paramTaskWorkflowConfig, Trade trade, Trade oldTrade, Vector paramVector1,
                         DSConnection paramDSConnection, Vector paramVector2, Task paramTask, Object paramObject,
                         Vector paramVector3) {
        return true;
    }

    
    @Override
    public String getDescription() {
        return "Generate Mature Posting related to CST Posting";
    }


	@Override
	public boolean update(TaskWorkflowConfig wc, Trade trade, Trade oldTrade, Vector messages,
			DSConnection dsCon, Vector excps, Task task, Object dbCon, Vector events) {
 		long tradeId = oldTrade.getLongId();
		try {
 			DefaultReportOutput outputFFM = null;
    		DefaultReportOutput outputFCM = null;
    		try {
    			outputFFM = (DefaultReportOutput) generateReportOutput(dsCon, oldTrade.getLongId(), "Balance", "FFM_Balances", new JDatetime());
    			outputFCM = (DefaultReportOutput) generateReportOutput(dsCon, oldTrade.getLongId(), "Balance", "FCM_Balances", new JDatetime());
    		} catch (RemoteException e) {
    			Log.error(LOG_CATEGORY, e);
    		}
    	     
    		// FFM_Balances
    		ReportRow[] rowsFFM = outputFFM.getRows();
    		Map<String, Double> endListFFM = new HashMap<String, Double>();
    		for (int i = 0; i < rowsFFM.length; i++) {
    			BalancePosition balance = (BalancePosition) rowsFFM[i].getProperties().get("BalancePosition");
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
    		PostingArray postingList = new PostingArray();
    		for (Map.Entry<String, Double> entry : endListFFM.entrySet()) {
    			String key = entry.getKey();
    			Double value = entry.getValue();
    		    System.out.println(key + " - " + value);
    		    BOPosting posting = createPostings(dsCon,key,value);
    		    postingList.add(posting);
    		}
    		
    		// FCM_Balances
    		ReportRow[] rowsFCM = outputFCM.getRows();
    		Map<String, Double> endListFCM = new HashMap<String, Double>();
    		for (int i = 0; i < rowsFCM.length; i++) {
    			BalancePosition balance = (BalancePosition) rowsFCM[i].getProperties().get("BalancePosition");
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
    		    System.out.println(key + " - " + value);
    		    BOPosting posting = createPostings(dsCon,key,value);
    		    postingList.add(posting);
    		}
    		
    		// Save postings
    		dsCon.getRemoteBO().savePostings(postingList);
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


	@SuppressWarnings("unchecked")
    private Account getAccountByAttrMaturitySource(DSConnection dsCon, String maturitySource) {
    	String propName = "MaturitySource";
		try {
			final Vector <Account> accounts = dsCon.getRemoteAccounting().getAccounts(true);
			if(accounts!=null && accounts.size()>0) {
				for (final Account account : accounts) {
					account.getParentAccountId();
					Vector <String> properties = account.getAccountProperties();
			        if (Util.isEmpty(properties)) {
			            continue;
			        }
			        for (int i = 0; i < properties.size(); i++) {
			            String prop = properties.get(i);
			            if (prop.equals(propName) && maturitySource.equals(properties.get(i+1))) {
			            	return account;
			            }
			        }
				}
			}
		} catch (final RemoteException e) {
			final StringBuffer message = new StringBuffer();
			message.append("Couldn't load the accounts ");
		}
    	return null;
    }


	private BOPosting createPostings(DSConnection dsCon, String key, Double value) {		
		BOPosting posting = new BOPosting();
		String[] parts = key.split("-");
		long tradeId = Long.parseLong(parts[0]);
		int accountId = Integer.parseInt(parts[1]);
		String position = parts[2];
		String accountName = parts[3];
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
    	posting.setEffectiveDate(valDate);
    	posting.setBookingDate(valDate);
    	posting.setCreationDate(valuationDate);
    	posting.setAmount(Math.abs(value));    	
    	posting.setCurrency(accountCurrency);
    	posting.setEventType("MATURE");
    	posting.setDescription("NONE");
    	
		Account newAccount = getAccountByAttrMaturitySource(dsCon, position);    		
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


	protected ReportOutput generateReportOutput(DSConnection dsCon, long tradeId, String type, String templateName, JDatetime valDatetime) throws RemoteException {
		PricingEnv env = dsCon.getRemoteMarketData().getPricingEnv("DirtyPrice", valDatetime);
		Report reportToFormat = createReport(dsCon, tradeId, type, templateName, env);
		if (reportToFormat == null) {
			Log.error(this, "Invalid report type: " + type);
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
	protected Report createReport(DSConnection dsCon, long tradeId, String type, String templateName, PricingEnv env) throws RemoteException {
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
		Vector<Long> tradeIdList = new Vector<Long>();			
		tradeIdList.add(tradeId);
		report.getReportTemplate().put("TradeIds", tradeIdList);
		return report;
	}


	
	
	
}