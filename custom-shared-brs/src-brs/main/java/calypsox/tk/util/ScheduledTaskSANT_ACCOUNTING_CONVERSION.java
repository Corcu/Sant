package calypsox.tk.util;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.bo.BOPostingUtil;
import com.calypso.tk.bo.BalancePosition;
import com.calypso.tk.bo.BalanceUtil;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.accounting.accountingcurrencyposition.AccountingConversionUtil;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.RoundingMethod;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.marketdata.QuoteValue;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CurrencyPair;
import com.calypso.tk.refdata.FXReset;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.ReportTemplateName;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CurrencyUtil;
import com.calypso.tk.util.InstantiateUtil;
import com.calypso.tk.util.PostingArray;
import com.calypso.tk.util.ScheduledTaskACCOUNTING_CONVERSION;
import com.calypso.tk.util.TaskArray;

import calypsox.tk.bo.accounting.accountingcurrencyposition.SantAccountingConversionUtil;

/**
 * adaptation of core ST accounting convertion to perform FX translation per trade
 * this ST is based on a Balance Report
 * @author CedricAllain
 *
 */
public class ScheduledTaskSANT_ACCOUNTING_CONVERSION extends ScheduledTaskACCOUNTING_CONVERSION {

	public static final String REPORT_TEMPLATE_NAME = "BalanceReport";
	public static final String REPORT_TYPE = "Balance";
	
	public static final String FX_TRANSLATION_BV = "FX Translation Back-Value";
	public static final String FX_TRANSLATION_BV_DAYS = "FX Translation Back-Value Days";

	public HashMap<Account,String> functionCcyCache = new HashMap<Account,String>();
	
	SantAccountingConversionUtil santAccountingConversionUtil = new SantAccountingConversionUtil();
	
	protected boolean useBusinessDate = false;

	DefaultReportOutput output=null;
	
	List<BalancePosition> allBalances= null;
	HashMap<String, BalancePosition> balancePositions = new HashMap<String, BalancePosition>();
	HashMap<Long,Trade> trades = new HashMap<Long,Trade>();
	
	
	public JDatetime getPreviousDatetime() {
		return getModValuationDatetime().add(-1, 0, 0, 0, 0);
	}
	
	/**
	 * get all the balances from the balance report generated
	 * @return
	 */
	public List<BalancePosition> getAllBalances() {
		if(allBalances==null) {
			String templateName = this.getAttribute(REPORT_TEMPLATE_NAME);
			JDatetime valDatetime = this.getPreviousDatetime();
			try {
				output = (DefaultReportOutput) generateReportOutput(REPORT_TYPE, templateName, valDatetime);
			} catch (RemoteException e) {
				Log.error(LOG_CATEGORY, e);
			}
			allBalances = new ArrayList<BalancePosition>();
			ReportRow[] rows = output.getRows();
			for (int i = 0; i < rows.length; i++) {
				BalancePosition newBalance = (BalancePosition) rows[i].getProperties().get(ReportRow.BALANCE);
				Trade trade = (Trade) rows[i].getProperties().get(ReportRow.TRADE);
				trades.put(trade.getLongId(), trade);
				allBalances.add(newBalance);
			}
		}
		
		return allBalances;
	}

	
	public String balancePositionKey(BalancePosition balance) {
		return balance.getAccountId() + "#" + balance.getCurrency() + "#" + balance.getTradeLongId();
	}
	
	/**
	 * update the balance up to toDate
	 * @param toDate
	 */
	public void updateBalances(JDate toDate) {
		
		ArrayList<BalancePosition> usedBalances = new ArrayList<BalancePosition>();
		for(BalancePosition newBalance : getAllBalances()) {
			if(newBalance.getPositionDate().before(toDate)) {
				String key = balancePositionKey(newBalance);
				BalancePosition oldBalance = balancePositions.get(key);
				if (oldBalance != null)
					oldBalance.add(newBalance);
				else
					balancePositions.put(key,newBalance);
				
				usedBalances.add(newBalance);
			}
		}
		
		getAllBalances().removeAll(usedBalances);
	}

	@Override
	public boolean handleAccountingConversion(DSConnection ds, PSConnection ps, TaskArray tasks) {

		JDatetime valDatetime = this.getValuationDatetime();
		PricingEnv env = null;
		try {
			env = ds.getRemoteMarketData().getPricingEnv(this._pricingEnv, valDatetime);
		} catch (Exception arg18) {
			Log.error(this, arg18);
			return false;
		}
		JDate valDate = JDate.valueOf(valDatetime, this._timeZone);
		JDate backDatedValueDate = getFXRevaluationBVDate(valDate);
		if(backDatedValueDate==null)
			backDatedValueDate=JDate.valueOf(valDate);
		while(!backDatedValueDate.after(valDate)) {
			this.handleFXEffectPerTrade(backDatedValueDate, tasks, env);
			backDatedValueDate = backDatedValueDate.addBusinessDays(1, this.getHolidays());
		}

		return true;

	}

	@Override
	public Vector<String> getDomainAttributes() {
		Vector<String> v = new Vector<String>();
		v.addElement(FX_TRANSLATION_BV);
		v.addElement(FX_TRANSLATION_BV_DAYS);
		v.addElement(REPORT_TEMPLATE_NAME);
		return v;
	}

	/**
	 * generate report output
	 * @param type
	 * @param templateName
	 * @param valDatetime
	 * @return
	 * @throws RemoteException
	 */
	protected ReportOutput generateReportOutput(String type, String templateName, JDatetime valDatetime)
			throws RemoteException {
		PricingEnv env = getDSConnection().getRemoteMarketData().getPricingEnv(this._pricingEnv, valDatetime);
		Report reportToFormat = this.createReport(type, templateName, env);
		if (reportToFormat == null) {
			Log.error(this, "Invalid report type: " + type);
			return null;
		} else if (reportToFormat.getReportTemplate() == null) {
			Log.error(this, "Invalid report template: " + type);
			return null;
		} else {
			Vector<String> holidays = this.getHolidays();
			if (!Util.isEmpty(holidays)) {
				reportToFormat.getReportTemplate().setHolidays(holidays);
			}

			if (this.getTimeZone() != null) {
				reportToFormat.getReportTemplate().setTimeZone(this.getTimeZone());
			}

			Vector<String> errorMsgs = new Vector<String>();
			return reportToFormat.load(errorMsgs);
		}
	}

	
	/**
	 * create the report
	 * @param type
	 * @param templateName
	 * @param env
	 * @return
	 * @throws RemoteException
	 */
	protected Report createReport(String type, String templateName, PricingEnv env) throws RemoteException {
		Report report;
		try {
			String template = "tk.report." + type + "Report";
			report = (Report) InstantiateUtil.getInstance(template, true);
			report.setPricingEnv(env);
			report.setFilterSet(this._tradeFilter);
			report.setValuationDatetime(this.getValuationDatetime());
		} catch (Exception arg7) {
			Log.error(this, arg7);
			report = null;
		}

		if (report != null && !Util.isEmpty(templateName)) {
			ReportTemplate template1 = DSConnection.getDefault().getRemoteReferenceData()
					.getReportTemplate(ReportTemplate.getReportName(type), templateName);
			if (template1 == null) {
				Log.error(this, "Template " + templateName + " Not Found for " + type + " Report");
			} else {
				report.setReportTemplate(template1);
				template1.setValDate(this.getValuationDatetime().getJDate(this._timeZone));
				template1.callBeforeLoad();
			}
		}

		return report;
	}

	@SuppressWarnings("rawtypes")
	public Vector<String> getAttributeDomain(String attr, Hashtable currentAttr) {
		Vector<String> v = new Vector<String>();

		if (attr.equals(REPORT_TEMPLATE_NAME)) {
			if (currentAttr == null) {
				return v;
			}

			String type = REPORT_TYPE;
			type = ReportTemplate.getReportName(type);
			Vector<ReportTemplateName> names = BOCache.getReportTemplateNames(DSConnection.getDefault(), type, (String) null);

			for (int i = 0; i < names.size(); ++i) {
				ReportTemplateName r = (ReportTemplateName) names.elementAt(i);
				v.add(r.getTemplateName());
			}

			return v;
		}

		return v;
	}

	/**
	 * handle fx effect per trade based on balance position report and valDate
	 * @param valDate
	 * @param tasks
	 * @param env
	 * @return
	 */
	protected boolean handleFXEffectPerTrade(JDate valDate, TaskArray tasks, PricingEnv env) {

		boolean success = true;
		JDate nextDate = valDate;
		JDate previousDate = nextDate.addBusinessDays(-1, this.getHolidays());
		System.out.println(">>>>>>>>> " + nextDate);
		updateBalances(nextDate);
		IsGainForFxProcessVisitor isGainForFxProcessVisitor = new IsGainForFxProcessVisitor();
		PostingArray postingsToSave = new PostingArray();

		try {

			for(BalancePosition balance : balancePositions.values()) {
				Account account = getAccount(balance);

				String functionCcy = getFunctionCcy(account);

				if (Util.isEmpty(functionCcy)) {
					Log.error(LOG_CATEGORY, "Missing FunctionCurrency for Account " + account);
					success = false;
					continue;
				}
				if (Util.isEmpty(account.getAccountProperty("FXTranslation(+)DEBIT"))) {
					Log.error(LOG_CATEGORY, "Missing FXTranslation(+)DEBIT for Account " + account);
					success = false;
					continue;
				}
				if (Util.isEmpty(account.getAccountProperty("FXTranslation(+)CREDIT"))) {
					Log.error(LOG_CATEGORY, "Missing FXTranslation(+)CREDIT for Account " + account);
					success = false;
					continue;
				}
				if (Util.isEmpty(account.getAccountProperty("FXTranslation(-)DEBIT"))) {
					Log.error(LOG_CATEGORY, "Missing FXTranslation(-)DEBIT for Account " + account);
					success = false;
					continue;
				}
				if (Util.isEmpty(account.getAccountProperty("FXTranslation(-)CREDIT"))) {
					Log.error(LOG_CATEGORY, "Missing FXTranslation(-)CREDIT for Account " + account);
					success = false;

					continue;
				}
				
				if(balance.getCurrency().equals(functionCcy))
					continue;

				CurrencyPair cp = CurrencyUtil.getCurrencyPairPosRef(balance.getCurrency(), functionCcy);

				QuoteValue qv1 = null;
				QuoteValue qv0 = null;
				String publishedFXQuoteName = getAttribute("Use Published FX Quote Name");
				if (!Util.isEmpty(publishedFXQuoteName)) {
					FXReset fxReset = DSConnection.getDefault().getRemoteReferenceData()
							.getFXReset(publishedFXQuoteName, balance.getCurrency(), functionCcy);
					if (null != fxReset) {
						qv1 = env.getFXQuote(fxReset, nextDate);
						qv0 = env.getFXQuote(fxReset, previousDate);
					}
				} else {
					qv1 = env.getFXQuote(cp.getPrimaryCode(), cp.getQuotingCode(), nextDate);
					qv0 = env.getFXQuote(cp.getPrimaryCode(), cp.getQuotingCode(), previousDate);
				}
				if (Log.isCategoryLogged("ACCOUNTING_CONVERSION")) {
					Log.debug("ACCOUNTING_CONVERSION", "Quote for Ccy " + balance.getCurrency() + "/" + functionCcy
							+ " on Date " + nextDate + qv1 + " Date : " + previousDate + " " + qv0);
				}

				if (qv1 == null || qv0 == null) {
					String error = "Rate missing " + balance.getCurrency() + "/" + functionCcy + " on " + nextDate + "/"
							+ previousDate;
					Log.error(this, error);
					addSantException(error, tasks);
					success = false;
				} else {

					BigDecimal fxDiff = BigDecimal.valueOf(qv1.getClose()).subtract(BigDecimal.valueOf(qv0.getClose()));

					double otherAmount = balance.getTotal();
					double convertedAmount1 = CurrencyUtil.convertAmount(cp, balance.getTotal(), functionCcy,
							qv1.getClose());
					double convertedAmount0 = CurrencyUtil.convertAmount(cp, balance.getTotal(), functionCcy,
							qv0.getClose());
					double postAmount = CurrencyUtil
							.roundAmount(Math.abs(convertedAmount1) - Math.abs(convertedAmount0), functionCcy);
					boolean fxGain = (postAmount > 0.0D);
					if (Log.isCategoryLogged("FXTRANSLATION")) {
						StringBuffer sb = new StringBuffer();
						sb.append("Balance: ").append(balance.toString()).append("\n");
						sb.append(qv0.getDate()).append(" Quote: ").append(qv0.getClose())
						.append(" Converted amount0: ").append(convertedAmount0).append("\n");
						sb.append(qv1.getDate()).append(" Quote: ").append(qv1.getClose())
						.append(" Converted amount1: ").append(convertedAmount1).append("\n");
						sb.append("Posting amount: ").append(" ABS(Converted amount1) - ABS(Converted amount0) = ")
						.append(postAmount).append("\n");
						sb.append("Is FX Gain: ").append(Boolean.valueOf(fxGain).toString()).append("\n");
						Log.debug("FXTRANSLATION", sb.toString());
					}
					postAmount = Math.abs(postAmount);

					String accountPlusDebit = "FXTranslation(+)DEBIT";
					String accountPlusCredit = "FXTranslation(+)CREDIT";
					String accountMinusDebit = "FXTranslation(-)DEBIT";
					String accountMinusCredit = "FXTranslation(-)CREDIT";
				
					BOPosting posting = santAccountingConversionUtil.createPostingForFXProcess(account, nextDate, balance, functionCcy, fxGain,
							account.getProcessingOrgId(), "FX_TRANSLATION", postAmount, otherAmount, accountPlusDebit,
							accountPlusCredit, accountMinusDebit, accountMinusCredit, isGainForFxProcessVisitor,
							getType(), null);
					
					if (posting == null) {
						success = false;
					} else {
						Trade trade = trades.get(posting.getTradeLongId());
						if(trade!=null)
							posting.setBookId(trade.getBookId());
						if (useBusinessDate && account.getProcessingOrgId() > 0) {

							LegalEntityAttribute lea = BOCache.getLegalEntityAttribute(DSConnection.getDefault(),
									account.getProcessingOrgId(), account.getProcessingOrgId(), "ProcessingOrg",
									"ACC_BUSINESS_DATE");

							if (lea != null) {
								JDate businessDate = Util.istringToJDate(lea.getAttributeValue());
								if (businessDate != null)
									posting.setBookingDate(businessDate);
							}
						}
						boolean existing = SantAccountingConversionUtil.cancelPreviousPostings(posting, nextDate, postingsToSave, isByBookingDate(),
								"FX_TRANSLATION");
						if (!existing && fxDiff != BigDecimal.ZERO)
							postingsToSave.add(posting);
					}
				}
			}

			if (postingsToSave.size() > 0) {

				for (int k = 0; k < postingsToSave.size(); k++) {
					BOPosting post = (BOPosting) postingsToSave.get(k);
					if (RoundingMethod.roundNearest(post.getAmount(), 2) == 0.0D) {
						postingsToSave.remove(k);
						k--;

					} else if (!isByBookingDate() && post.getId() == 0L && !useBusinessDate) {
						post.setBookingDate(valDate);
					}
				}

				if (!getBooleanAttribute("Check Market Data Only")) {
					getReadWriteDS(DSConnection.getDefault()).getRemoteBO().savePostings(0L, getClass().getName(),
							postingsToSave, new TaskArray(), true, null);
				}
			}
		} catch (Exception e) {
			Log.error(LOG_CATEGORY, e);
			success = false;
		}

		return success;

	}

	protected void addSantException(String message, TaskArray tasks) {
		Task task = new Task();
		task.setComment(this.toString() + " " + message);
		tasks.add(task);
	}

	class IsGainForFxProcessVisitor implements AccountingConversionUtil.IsGainForFxProcessVisitorI {
		public boolean isGainForFxProcess(Account account, JDate valDate, BalancePosition bal, String functionCcy,
				boolean fxGain, int poId, String eventType) {
		    Boolean isCreditAmountForFxProcess = Boolean.valueOf(BalanceUtil.isCreditAmount(bal.getTotal()));
		    return AccountingConversionUtil.isGainForFxProcess(account, valDate, bal, functionCcy, fxGain, poId, eventType, isCreditAmountForFxProcess);
		}
	}


	private JDatetime getModValuationDatetime() {
		JDatetime valDatetime = null;
		int oldValue = this.getValuationTime();
		if (oldValue == 0) {
			JDatetime currentDatetime = this.getDatetime();
			int hours = currentDatetime.getField(11, this.getTimeZone());
			int min = currentDatetime.getField(12, this.getTimeZone());
			this.setValuationTime(100 * hours + min);
			valDatetime = this.getValuationDatetime();
			this.setValuationTime(oldValue);
		} else {
			valDatetime = this.getValuationDatetime();
		}

		return valDatetime;
	}

	private Account getAccount(BalancePosition balancePosition) {
		int accId = balancePosition.getAccountId();
		if (accId == 0) {
			return null;
		} else {
			Account acc = BOCache.getAccount(this.getDSConnection(), accId);
			return acc == null ? null : acc;
		}
	}
	 
		private JDate getFXRevaluationBVDate(JDate valDate) {
			boolean bv = Util.isTrue(this.getAttribute(FX_TRANSLATION_BV), true);
			JDate bvDate = null;
			if (bv) {
				String attribute = this.getAttribute(FX_TRANSLATION_BV_DAYS);
				int bvDays = (int) Util.stringToNumber(attribute);
				if (bvDays == 0) {
					return valDate;
				}

				if (bvDays > 0) {
					bvDate = valDate.addDays(-bvDays);
				}
			}

			return bvDate;
		}
		
		/**
		 * get function ccy using cache
		 * @param account
		 * @return
		 */
		public String getFunctionCcy(Account account) {
			String functionCcy = functionCcyCache.get(account);
			if(functionCcy==null) {
				functionCcy = BOPostingUtil.getFunctionCcy(account);
				functionCcyCache.put(account, functionCcy);
			}
			return functionCcy;
		}
	
}
