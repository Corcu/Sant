package calypsox.tk.report;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.calypso.tk.core.*;
import com.calypso.tk.refdata.CurrencyDefault;
import org.apache.commons.beanutils.BeanUtils;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Inventory;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.report.ReportTemplateName;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.service.collateral.CacheCollateralClient;
import calypsox.tk.collateral.util.SantMarginCallUtil;
import calypsox.util.collateral.CollateralManagerUtil;

public class CumbreAccountingReport extends Report {

	/** Serial version Id */
	private static final long serialVersionUID = 1L;

	private static final String HOLIDAYS = "Holidays";
	private static final String SYSTEM = "SYSTEM";
	private static final String SECURTITY_POSITION = "BOSecurityPosition";
	private static final String CASH_POSITION = "BOCashPosition";
	private static final String SEC = "SEC";
	private static final String CASH = "CASH";
	private static JDate processDate = null;
	private boolean lodeActivated = false;
	DefaultReportOutput reportOutputToday = null;
	DefaultReportOutput reportOutputYesterday = null;
	DefaultReportOutput reportOutputNoExist = null;
	boolean done = false;
	
	

	@SuppressWarnings("unchecked")
	@Override
	public ReportOutput load(Vector paramVector) {

		// Check the activation of Lode Report modifications 
		DSConnection dsCon = DSConnection.getDefault();
		final String lodeUpdateActivated = LocalCache.getDomainValueComment(dsCon, "domainName", "LodeUpdateActivated");
		if(!Util.isEmpty(lodeUpdateActivated) && "YES".equalsIgnoreCase(lodeUpdateActivated)) {
			lodeActivated= true;
		}
		
		final DefaultReportOutput output = new StandardReportOutput(this);
		final ReportTemplate reportTemplate = getReportTemplate();
		PricingEnv env = getPriceEnv(output, reportTemplate);
		processDate=getValDate();

		final ArrayList<ReportRow> reportRows = getCumbreBalanceReportRows(env, reportTemplate);

		output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
		return output;
	}

	@SuppressWarnings("unchecked")
	private ArrayList<ReportRow> getCumbreBalanceReportRows(final PricingEnv env, final ReportTemplate reportTemplate) {

		final ArrayList<ReportRow> reportRows = new ArrayList<>();
		JDate valDate = reportTemplate.getValDate();
		@SuppressWarnings("rawtypes")

		// Valuation dates    (domm: only today; lode: today and yesterday)
		class ThDate {								// date info for a thread
			protected JDate 	valDate;			// the valDate for the report
			protected boolean	isYesterday;		// the date is yesterday
			
			protected ThDate(JDate date, boolean isYesterday) { 
				this.valDate = date;
				this.isYesterday = isYesterday;
			}
		}
		ArrayList<ThDate> thDates = new ArrayList<>();
		thDates.add( new ThDate(valDate, false) );									// add today
		
		// Add yesterday if lode    (yesterday = previous laborable day)
		String rtName = reportTemplate.getTemplateName();
		boolean isLode = (rtName != null && rtName.contains("LODE"));				// ex: Cumbre Movements Report LODE
		if (isLode)
		{
			Vector holidays = getHolidays(reportTemplate);
			Vector cityHol = null;
			cityHol = Util.isEmpty(holidays)
					? Util.string2Vector(SYSTEM)
					: holidays;
			JDate valDateYesterday = valDate.addBusinessDays(-1, cityHol);
			//JDate valDateYesterday = valDate.addBusinessDays(0, cityHol);
			thDates.add( new ThDate(valDateYesterday, true) );
		}

		// Launch report threads
		ArrayList<ExecuteCumbreReport> threads = new ArrayList<>();
		for (ThDate aThDate: thDates)
		{
			// Prepare threads to load reports
			ExecuteCumbreReport secThread = new ExecuteCumbreReport(SECURTITY_POSITION, aThDate.valDate, aThDate.isYesterday, env);
			ExecuteCumbreReport cashThread = new ExecuteCumbreReport(CASH_POSITION, aThDate.valDate, aThDate.isYesterday, env);

			// Launch loading of reports
			startReport(secThread, reportTemplate, SEC);
			try {
				secThread.join();
			} catch (InterruptedException e) {
				Log.error(this, e);
			}
			startReport(cashThread, reportTemplate, CASH);
			
			// Remember threads
			threads.add(secThread);
			threads.add(cashThread);
		}


		// Wait for threads and collect results
		
		for(ExecuteCumbreReport thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				Log.error(this, e);
			}
		}
		
		List<CumbreAccountingReportBean> beansToday = new ArrayList<>();
		List<CumbreAccountingReportBean> beansYesterday = new ArrayList<>();
		
		for(ExecuteCumbreReport thread : threads) {
			if (thread.isYesterday)
				beansYesterday.addAll(thread.getBeans());
			else
				beansToday.addAll(thread.getBeans());
		}
		
		// Show today rows, then yesterday rows
		List<CumbreAccountingReportBean> beans = new ArrayList<>();
		beans.addAll(beansToday);
		beans.addAll(beansYesterday);
		
		// create rows
		if (!Util.isEmpty(beans)) {
			for (CumbreAccountingReportBean bean : beans) {
				bean.setIsLode(isLode);								// to be used at CumbreAccountingReportStyle.getColumnValue()
				final ReportRow repRow = new ReportRow(bean);
				reportRows.add(repRow);
			}
		}

		return reportRows;
	}

	private Vector getHolidays(final ReportTemplate reportTemplate) {
		Vector holidays = new Vector<>();
		Vector holidaysAttr = reportTemplate.getAttributes().get(HOLIDAYS);
		if (Util.isEmpty(holidaysAttr)) {
			holidays.add(SYSTEM);
		} else {
			holidays = holidaysAttr;
		}
		return holidays;
	}

	private PricingEnv getPriceEnv(DefaultReportOutput output, ReportTemplate reportTemplate) {
		PricingEnv env = null;
		if (output.getPricingEnv() != null) {
			env = output.getPricingEnv();
		} else if (reportTemplate.getAttributes().get("PricingEnvName") != null) {
			String pename = reportTemplate.getAttributes().get("PricingEnvName").toString();
			try {
				env = DSConnection.getDefault().getRemoteMarketData().getPricingEnv(pename);
			} catch (CalypsoServiceException e) {
				Log.error(KGR_Collateral_MarginCallReport.class, "Cannot get pricingEnv for: " + pename + " " + e);

			}
		} else {
			Log.warn(IMAccountingReport.class, "Cannot get pricingEnv for null PE");
		}
		return env;

	}

	public List<CumbreAccountingReportBean> createBeans(DefaultReportOutput reportoutput, JDate valDate, boolean isYesterday, PricingEnv pricingEnv) {

		List<CumbreAccountingReportBean> beans = new ArrayList<>();
		for (ReportRow row : reportoutput.getRows()) {
			Inventory inventory = getInventory(row);
			beans.addAll(buildAccounting(row, valDate, isYesterday, pricingEnv, false));
		}

		return beans;
	}
	
	
	public List<CumbreAccountingReportBean> createBeans(DefaultReportOutput reportoutput, JDate valDate, boolean isYesterday, PricingEnv pricingEnv, boolean forceZeroBalance) {

		List<CumbreAccountingReportBean> beans = new ArrayList<>();
		for (ReportRow row : reportoutput.getRows()) {
			Inventory inventory = getInventory(row);
			beans.addAll(buildAccounting(row, valDate, isYesterday, pricingEnv, forceZeroBalance));
		}
		return beans;
	}
	
	
	public List<CumbreAccountingReportBean> createBeansNoExist(DefaultReportOutput reportoutput, JDate valDate, boolean isYesterday, PricingEnv pricingEnv, boolean forceZeroBalance, String isinNoExist) {
		List<CumbreAccountingReportBean> beans = new ArrayList<>();
		for (ReportRow row : reportoutput.getRows()) {
			Inventory inventory = getInventory(row);
			String isin = inventory.getProduct().getSecCode("ISIN");
			if (isinNoExist.equalsIgnoreCase(isin)) {
				beans.addAll(buildAccounting(row, valDate, isYesterday, pricingEnv, forceZeroBalance));
			}
		}
		return beans;
	}
	

	public List<CumbreAccountingReportBean> buildAccounting(ReportRow row, JDate valDate, boolean isYesterday, PricingEnv pricingEnv, boolean forceZeroBalance) {

		List<CumbreAccountingReportBean> beans = new ArrayList<>();

		Inventory inventory = getInventory(row);
		// to avoid sonar warning on method complexity
		if (inventory == null) { 
			return beans;
		}
			
		MarginCallConfig contract = BOCache.getMarginCallConfig(DSConnection.getDefault(), inventory.getMarginCallConfigId());
		if (contract != null) {
			CumbreAccountingReportBean bean1 = new CumbreAccountingReportBean();		// bean for debt
			double balance = forceZeroBalance?0.0D:CumbreReportLogic.getBalanceAmount(inventory, valDate, pricingEnv, row);
			boolean isPositive = (balance >= 0);

			//Funcionalidad que redondea el balance por si la currency no lleva decimales segun ISO 4217
			Vector<String> roundMethod = LocalCache.getDomainValues(DSConnection.getDefault(), "CumbreRoundingMethod");

			Double var = CurrencyDefault.valueOf(inventory.getSettleCurrency()).getRounding();
			if(!roundMethod.isEmpty()){
				if(roundMethod.contains("NEAREST")){
					balance = RoundingMethod.roundNearest(balance, var.intValue());
				}else if(roundMethod.contains("UP")){
					balance = RoundingMethod.roundUp(balance, var.intValue());
				}else if(roundMethod.contains("DOWN")){
					balance = RoundingMethod.roundDown(balance, var.intValue());
				}
			}
			///////////////////////////////////////////////////////////////////////////////////////////////////

			// Nuevo desarrollo para no recalcular las posiciones en D-1
			//String movementAmount = CumbreReportLogic.getConvertedAmount(inventory, balance, valDate, pricingEnv, 16, false, 2).replace(".", "");
			String movementAmount = CumbreReportLogic.formatDoubleToString(16, balance, false, var.intValue()).replace(".", "");
			if(lodeActivated){
				// Si es ayer --> Recupera y devuelve 'amountValue' para mostrar
				if(isYesterday) {
					movementAmount = getPositionValue(contract, inventory, valDate, movementAmount);
				}
				// Si es hoy --> Almacena y devuelve 'amountValue' para mostrar
				else {
					movementAmount = setPositionValue(contract, inventory, valDate, movementAmount, forceZeroBalance);
				}
			}

			bean1.setProcessDate(CumbreReportLogic.getBalanceProcessDate(valDate));
			bean1.setBalanceBranch((CumbreReportLogic.getBalanceBranchNumber()));
			bean1.setCptyShortName(CumbreReportLogic.getCptyShortName(contract, true));
			bean1.setAccountNumber(formatAccountLength(new StringBuilder(CumbreReportLogic.getAccountNumber(true))));	
			bean1.setMovementType( (isPositive ^ isYesterday) ? "C" : "D" );				// positive=C, negative=D. Inverse if yesterday.
			bean1.setBalanceReference(CumbreReportLogic.getBalanceReference(contract, inventory, 20)); 
			bean1.setBalanceCcy(CumbreReportLogic.getBalanceCcy(inventory)); 
			bean1.setBalanceAmount( CumbreReportLogic.formatDoubleToString(31, balance, true, 2).replace(".", "") );			// ask for 31 characters because the "replace" will remove 1 (the decimal separator)
			bean1.setAmount( CumbreReportLogic.getConvertedAmount(inventory, balance, valDate, pricingEnv, 31, true, 2).replace(".", "") );						// ask for 31 characters because the "replace" will remove 1 (the decimal separator) 
			bean1.setValueDate(CumbreReportLogic.getValueDate(processDate));
			
			if(lodeActivated) {
				bean1.setMovementAmount(movementAmount);
				bean1.setMovementValue(CumbreReportLogic.getConvertedAmount(inventory, new Double(movementAmount), valDate, pricingEnv, 16, false, 2).replace(".", ""));
			}
			else {
				bean1.setMovementAmount( CumbreReportLogic.formatDoubleToString(16, balance, false, 2).replace(".", "") );			// ask for 16 characters because the "replace" will remove 1 (the decimal separator)
				bean1.setMovementValue( CumbreReportLogic.getConvertedAmount(inventory, balance, valDate, pricingEnv, 16, false, 2).replace(".", "") );						// ask for 16 characters because the "replace" will remove 1 (the decimal separator)				
			}
			
			bean1.setMovementRef(CumbreReportLogic.getBalanceReference(contract, inventory, 35));
			bean1.setMovementProcessDate(CumbreReportLogic.getMovementProcessDate(processDate));
			bean1.setMovementBranch(CumbreReportLogic.getMovementBranchNumber());

			// create direction 2
			CumbreAccountingReportBean bean2 = null;
			try {
				bean2 = (CumbreAccountingReportBean) BeanUtils.cloneBean(bean1);
			} catch (IllegalAccessException | InstantiationException | InvocationTargetException
					| NoSuchMethodException e) {
				Log.error("Cannot create the second direction of the position", e);
			}
			if (bean2 != null) {
				bean2.setAccountNumber(formatAccountLength(new StringBuilder(CumbreReportLogic.getAccountNumber(false))));
				bean2.setBalanceAmount( bean2.getBalanceAmount().replace(isPositive ? "+" : "-", isPositive ? "-" : "+") );
				bean2.setAmount( bean2.getAmount().replace(isPositive ? "+" : "-", isPositive ? "-" : "+") );
				bean2.setMovementType( (isPositive ^ isYesterday) ? "D" : "C" );			// positive=D, negative=C. Inverse if yesterday.
			}
			
			// add directions
			beans.add(bean1);
			if (bean2 != null)
				beans.add(bean2);
			
		}
			
		return beans;
	}
	
	private String formatAccountLength(StringBuilder acc) {
		String account = acc.toString();
		int length = acc.length();
		if(length<7){
			//account=(acc.append(CumbreReportLogic.appendChar("0", 7 - length))).toString();	
			int max = 7 - length;
			int i=0;
			while(i<max) {
				account=(acc.insert(0, "0")).toString();
				i++;
			}
		}
		return account;
	}

	private Inventory getInventory(ReportRow row) {
		return row.getProperty(ReportRow.DEFAULT);
	}

	private void startReport(ExecuteCumbreReport thread, ReportTemplate reportTemplate, String templateName) {
		if (null != reportTemplate.getAttributes().get(templateName)) {
			thread.setTemplateName(reportTemplate.getAttributes().get(templateName).toString());
			thread.start();
		}
	}

	private class ExecuteCumbreReport extends Thread {
		private String templatename;
		private String report;
		private JDate valDate;
		private boolean isYesterday;
		private PricingEnv pricingEnv;
		protected List<CumbreAccountingReportBean> beans;

		public ExecuteCumbreReport(String report, JDate valDate, boolean isYesterday, PricingEnv pricingEnv) {
			this.report = report;
			this.valDate = valDate;
			this.isYesterday = isYesterday;
			this.beans = new ArrayList<>();
			this.pricingEnv = pricingEnv;
		}

		public void setTemplateName(String temp) {
			this.templatename = temp;
		}

		public List<CumbreAccountingReportBean> getBeans() {
			return this.beans;
		}

		@Override
		public void run() {
			Vector<String> errorMsgs = new Vector<>();
			try {
				DefaultReportOutput defaultReportOutput = null;
				DefaultReportOutput previousDateDefaultReportOutput = null;
				ReportTemplateName templateName = new ReportTemplateName(templatename);
				ReportTemplate template = BOCache.getReportTemplate(DSConnection.getDefault(), report, templateName);
				if (template != null) {
					if (SECURTITY_POSITION.equals(report)) {
						BOSecurityPositionReport securityreport = new BOSecurityPositionReport();
						securityreport.setReportTemplate(template);
						securityreport.setPricingEnv(getPricingEnv());
						securityreport.setValuationDatetime(valDate.getJDatetime());
						securityreport.setStartDate(valDate);
						securityreport.setEndDate(valDate);
						defaultReportOutput = (DefaultReportOutput) securityreport.load(errorMsgs);
						
						if(!this.isYesterday) {
							checkStoredPositions(securityreport, valDate);
							reportOutputToday = defaultReportOutput;
						}
						else if(this.isYesterday) {
							reportOutputYesterday = defaultReportOutput;
						}
						
						// V16 Migration Get the previous position from D-2 to D-1 (To align with V14 behavior)
						JDate startDatePreviousDate = getPositionPreviousDate(valDate);           
						securityreport.setValuationDatetime(startDatePreviousDate.getJDatetime());
						securityreport.setStartDate(startDatePreviousDate);
						securityreport.setEndDate(valDate.addDays(-1));
						previousDateDefaultReportOutput = (DefaultReportOutput) securityreport.load(errorMsgs);
					} else if (CASH_POSITION.equals(report)) {
						BOCashPositionReport cashreport = new BOCashPositionReport();
						cashreport.setReportTemplate(template);
						cashreport.setPricingEnv(getPricingEnv());
						cashreport.setValuationDatetime(valDate.getJDatetime());
						cashreport.setStartDate(valDate);
						cashreport.setEndDate(valDate);
						defaultReportOutput = (DefaultReportOutput) cashreport.load(errorMsgs);
						
						// V16 Migration Get the previous position from D-1 to D-2 (To align with V14 behavior)
			            JDate startDatePreviousDate = getPositionPreviousDate(valDate);
			            cashreport.setValuationDatetime(startDatePreviousDate.getJDatetime());
			            cashreport.setStartDate(startDatePreviousDate);
			            cashreport.setEndDate(valDate.addDays(-1));
						previousDateDefaultReportOutput = (DefaultReportOutput) cashreport.load(errorMsgs);
						
					}
					if (null != defaultReportOutput) {
							beans.addAll(createBeans(defaultReportOutput, valDate, this.isYesterday, pricingEnv));
							List<CumbreAccountingReportBean> previousDayBeans = createBeans(previousDateDefaultReportOutput, valDate, this.isYesterday, pricingEnv,true);
							
							// V16 Migration add the previous position in D-2 to 0 if the movement ref is not in the bean list (To align with V14 behavior)
							List<CumbreAccountingReportBean> previousDayBeansToAdd = new ArrayList<CumbreAccountingReportBean>();
							for(CumbreAccountingReportBean bean : previousDayBeans) {
								if(!containsMovementRef(beans,bean)) {
									previousDayBeansToAdd.add(bean);
								}
							}
							beans.addAll(previousDayBeansToAdd);
					}
				}
				if(this.isYesterday && SECURTITY_POSITION.equals(report)) {
					for (ReportRow rowOnD : reportOutputToday.getRows()) {
						boolean find = false;
						Inventory inventoryD = getInventory(rowOnD);
						String isinD = inventoryD.getProduct().getSecCode("ISIN"); 
						for (ReportRow rowOnY : reportOutputYesterday.getRows()) {
							Inventory inventoryY = getInventory(rowOnY);
							String isinY = inventoryY.getProduct().getSecCode("ISIN");
							if(isinD.equalsIgnoreCase(isinY)){
								find = true;
								break;
							}
						}
						if(!find){
							List<CumbreAccountingReportBean> noExistBeans = createBeansNoExist(reportOutputToday, valDate, this.isYesterday, pricingEnv,true,isinD);
							for(CumbreAccountingReportBean noExistBean : noExistBeans) {
								if(!containsMovementRef(beans,noExistBean)) {
									beans.add(noExistBean);
								}
							}
						}
					}
				}
			} catch (Exception e) {
				Log.error(this, "Cannot load: " + report + " Error: " + e + " Errors: " + errorMsgs);
			}
			if (!Util.isEmpty(errorMsgs)) {
				Log.info(this, errorMsgs.toString());
			}
		}
	}
	
	/**
	 * check if movement ref is in a bean list
	 * @param beans
	 * @param beanToMatch
	 * @return
	 */
	public static boolean containsMovementRef(List<CumbreAccountingReportBean> beans, CumbreAccountingReportBean beanToMatch) {
		for(CumbreAccountingReportBean bean:beans)	{
			if(bean.getMovementRef().equals(beanToMatch.getMovementRef()) && bean.getMovementType().equals(beanToMatch.getMovementType())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * V16 Migration : Get the previous Position Date in D-2 to align to V14 behavior
	 * @param valDate
	 * @return
	 */
	public JDate getPositionPreviousDate(JDate valDate) {
		Vector<String> holidays = Util.string2Vector("SYSTEM");
        // first -1 business day (to align with custom getValuationDate in BOSecurityPositionReport)
         JDate startDatePreviousDate = valDate.addBusinessDays(-1, holidays);
        // -1 day (to align with getPositionStartDate in v14)
        return startDatePreviousDate.addDays(-1);
	}

	
	
	
	/**
	 * 
	 * @param contract
	 * @param inventory
	 * @param valDate
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	private String getPositionValue(MarginCallConfig contracts, Inventory inventory, JDate valDate, String amountValue) {
		String result = "";
		List<Integer> mceIds = new ArrayList<>();
    	mceIds.add(inventory.getMarginCallConfigId());
    	List<MarginCallEntryDTO> mceList = null;
    	MarginCallEntry mce = null;
		try {
			mceList = ServiceRegistry.getDefault().getCollateralServer().loadEntries(mceIds, valDate, ServiceRegistry.getDefaultContext().getId());
			if(Util.isEmpty(mceList)) {
				// Return the default value
	            return amountValue;
			}
			final CollateralConfig collateralConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mceList.get(0).getCollateralConfigId());
			mce = SantMarginCallUtil.getMarginCallEntry(mceList.get(0), collateralConfig, false);
		} catch (CollateralServiceException e) {
            Log.error(this, "Couldn't get Margin Call Entries." + e.getMessage());
		}
    	if(mce==null){
    		// Return the default value
    		return amountValue;
		}
    	
		HashMap<String, Object> mceAttrs = (HashMap<String, Object>) mce.getAttributes();
		// Si no hay ningun attributo en el mce
		if(Util.isEmpty(mceAttrs)) {
			// Return the default value
			return amountValue;
		}
		HashMap<String, Object> positionMap = (HashMap<String, Object>) mceAttrs.get("MCE_POSITIONS");
		if(Util.isEmpty(positionMap)){
			// Return the default value
			return amountValue;
		}
		result = (String) positionMap.get(inventory.getProduct().getSecCode("ISIN"));
		if(Util.isEmpty(result)){
			result = "000000000000000";
		}
		return result;
	}   
	
	
	/**
	 * 
	 * @param contract
	 * @param inventory
	 * @param valDate
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	private String setPositionValue(MarginCallConfig contract, Inventory inventory, JDate valDate, String amountValue, boolean forceZeroBalance) {
		String amount = amountValue;
		if(!forceZeroBalance) {
			final String defaultValue="000000000000000";
			List<Integer> mceIds = new ArrayList<>();
	    	mceIds.add(inventory.getMarginCallConfigId());
	    	List<MarginCallEntryDTO> mceList = null;
	    	MarginCallEntry mce = null; 	
	    	
			try {
				mceList = ServiceRegistry.getDefault().getCollateralServer().loadEntries(mceIds, valDate, ServiceRegistry.getDefaultContext().getId());
				if(Util.isEmpty(mceList)) {
					Log.info(this, "There is no MarginCallEntryDTO for Margin Call " + inventory.getMarginCallConfigId());
					return defaultValue;
				}
				CollateralConfig collateralConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mceList.get(0).getCollateralConfigId());		
				mce = SantMarginCallUtil.getMarginCallEntry(mceList.get(0), collateralConfig, false);
			} catch (CollateralServiceException e) {
	            Log.error(this, "Couldn't get Margin Call Entries.");
			} 
	    	if(mce==null) {
	    		Log.info(this, "There is no MarginCallEntry");
				return defaultValue;
			}
	
	    	HashMap<String, Object> mceAttrs = (HashMap<String, Object>) mce.getAttributes();
			// Si no hay ningun attributo en el mce
			if(Util.isEmpty(mceAttrs)) {
				mceAttrs = new HashMap<String, Object>();
			}
			HashMap<String, Object> positionMap = (HashMap<String, Object>) mceAttrs.get("MCE_POSITIONS");
			String isin = inventory.getProduct().getSecCode("ISIN");
			List<String> errors = new ArrayList<String>();
	
	    	// Si existe el atributo mapa 'PositionMap'
			if(Util.isEmpty(positionMap)){
				positionMap = new HashMap<String, Object>();
			}
			positionMap.put(isin,amount);
			mceAttrs.put("MCE_POSITIONS", positionMap);
			mce.setAttributes(mceAttrs);
			List<MarginCallEntry> mceEndList = new ArrayList<MarginCallEntry>();
			mceEndList.add(mce);
			CollateralManagerUtil.saveEntries(mceEndList, Action.UPDATE.toString(), errors);
		}
		return amount;
		
	}
		
	@SuppressWarnings({ "unchecked", "unused" })
	private void checkStoredPositions(BOSecurityPositionReport report, JDate valDate) {
    	List<MarginCallConfig> ccs = null;
		try {
			String pOrgs = "BFOM,5HSF,BCHB";		
			ccs = DSConnection.getDefault().getRemoteReferenceData().getAllMarginCallConfig();
            for (MarginCallConfig cc : ccs) {
            	List<String> errors = new ArrayList<String>();
            	String po = cc.getProcessingOrg().getCode();
            	if (pOrgs.contains(po)) {
            		cleanStoredPositions(cc, valDate);
            	}
            }
		} catch (CalypsoServiceException e2) {
			Log.error(this, "Could not get Margin Call Config.");
		}
			
	}


	public void cleanStoredPositions(final MarginCallConfig marginCall, JDate valDate) {

		List<Integer> mceIds = new ArrayList<>();
    	mceIds.add(marginCall.getId());
    	List<MarginCallEntryDTO> mceList = null;
    	MarginCallEntry mce = null; 	
    	
		try {
			mceList = ServiceRegistry.getDefault().getCollateralServer().loadEntries(mceIds, valDate, ServiceRegistry.getDefaultContext().getId());
			if(Util.isEmpty(mceList)) {
				return;
			}
			CollateralConfig collateralConfig = CacheCollateralClient.getCollateralConfig(DSConnection.getDefault(), mceList.get(0).getCollateralConfigId());		
			mce = SantMarginCallUtil.getMarginCallEntry(mceList.get(0), collateralConfig, false);
		} catch (CollateralServiceException e) {
            Log.error(this, "Couldn't get Margin Call Entries.");
		} 
    	
		if(mce==null) {
			return;
		}
		
    	HashMap<String, Object> mceAttrs = (HashMap<String, Object>) mce.getAttributes();
		if(Util.isEmpty(mceAttrs)) {
			return;
		}
		List<String> errors = new ArrayList<String>();
		HashMap<String, Object> positionMap = (HashMap<String, Object>) mceAttrs.get("MCE_POSITIONS");		
		if(!Util.isEmpty(positionMap)){
			mceAttrs.put("MCE_POSITIONS", null);
			mce.setAttributes(mceAttrs);
			List<MarginCallEntry> mceEndList = new ArrayList<MarginCallEntry>();
			mceEndList.add(mce);
			CollateralManagerUtil.saveEntries(mceEndList, Action.UPDATE.toString(), errors);
		}
		return;
	}
	

}
