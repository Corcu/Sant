package calypsox.tk.report;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;


import calypsox.ErrorCodeEnum;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.util.ControlMErrorLogger;
import calypsox.util.collateral.CollateralUtilities;

import com.calypso.infra.util.Util;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.marketdata.FeedAddress;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.AccountInterestConfig;
import com.calypso.tk.refdata.AccountInterestConfigRange;
import com.calypso.tk.refdata.AccountInterests;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.CollateralConfigCurrency;
import com.calypso.tk.refdata.RateIndex;
import com.calypso.tk.report.AccountReport;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

/**
 * Extracts information from linked call accounts to Colleral contracts for providing the optimizer.
 * 
 * @author Olivier & Guillermo Solano
 * @version 3.0. Accounts validators & Asset control Mapping
 *
 */
public class Opt_ReferenceRateReport extends AccountReport {

	private static final long serialVersionUID = 123L;
	private static final String ASSET_CONTROL = "ASSET_CONTROL";
	private static final String MC_CONTRACT = "MARGIN_CALL_CONTRACT";
	private static final String ACTIVE_ACCOUNT = "Active";
	private Map<String, String> feedMap = new HashMap<String, String>();
	//GSM 04/06/14: check the RefRate is active
	private JDate today = JDate.getNow();

	/**
	 * Main method of the report
	 */
	@Override
	public ReportOutput load(@SuppressWarnings("rawtypes") Vector errorMsgs) {
		
		//fix MultiPO 31/08/2015 - AccountReport uses attribute "ProcessingOrg" as IDs, opposite to rest of reports
		ReportTemplate reportTemplate = super.getReportTemplate();
		String PoList = "";
		if (reportTemplate.get(SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES) != null){
			PoList = (String) reportTemplate.get(SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES);
			reportTemplate.put(SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES,reportTemplate.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS));
		}
		
		StandardReportOutput output = new StandardReportOutput(this);
		DefaultReportOutput defOutput = ((DefaultReportOutput) super.load(errorMsgs));
		ReportRow[] rows = defOutput.getRows();
		
		//roll fix
		if (rows.length > 0){
			reportTemplate.put(SantGenericTradeReportTemplate.PROCESSING_ORG_NAMES,PoList);
		}
		
		
		
		//BAU 05/05/15: check accountActiveStatus DV has this value
		if (!Account.isActive(ACTIVE_ACCOUNT)){
			Log.error(this, "DV accountActiveStatus not set. Cannot filter non-active accounts");
		}

		//GSM: feed the map with the relation between References Rates in Calypso and Asset Control
		this.feedMap = buildFeedMapping(DSConnection.getDefault());

		List<ReportRow> rowsList = new ArrayList<ReportRow>();
		for (int i = 0; i < rows.length; i++) {
			addRows(rowsList, rows[i]);
		}

		output.setRows(rowsList.toArray(new ReportRow[0]));
		return output;
	}

	/**
	 * Generation of the rows with the different Account Interest Configs and their rate indexes
	 * @param rowsList
	 * @param reportRow
	 */
	private void addRows(List<ReportRow> rowsList, ReportRow reportRow) {

		final Account account = (Account) reportRow.getProperty(ReportRow.DEFAULT);

		final CollateralConfig contract = getMarginCallConfig(account);
		
		if (contract == null)
			return;
		
		// GSM 20/07/15. SBNA Multi-PO filter
		if (CollateralUtilities.filterPoByTemplate(super.getReportTemplate(), contract)) {
			return;
		}
		
		//BAU 05/05/15: GSM, check account is closed
		if ((!Util.isEmpty(account.getAccountStatus()) && !account.getAccountStatus().equals(ACTIVE_ACCOUNT)) 
				|| (account.getActiveTo() != null && account.getActiveTo().before(today)))
			return;  
		
		
		
		//GSM: 01/10/14. Check account ccy is one of the MCC elegible currencies
		final List<CollateralConfigCurrency> eligibleCCYs = contract.getEligibleCurrencies();
		
		boolean containsCCY = false;
		for (CollateralConfigCurrency colConfigCCY:  eligibleCCYs){
			if (colConfigCCY.getCurrency().equals(account.getCurrency())){
				containsCCY = true;
				break;
			}	
		}
		
		if (!containsCCY) //ccy not found, discard
			return;

		for (AccountInterests accInterest : account.getAccountInterests()) {

			AccountInterestConfig config = null;
			try {
				config = DSConnection.getDefault().getRemoteAccounting()
						.getAccountInterestConfig(accInterest.getConfigId());
				
			} catch (RemoteException e) {
				Log.error(this, e); //sonar
			}
			
			
			if (config == null ) {
				continue;
			}
			
			if ((accInterest.getActiveFrom() != null && accInterest.getActiveFrom().after(today)) 
					|| (accInterest.getActiveTo() != null && accInterest.getActiveTo().before(today))){
				continue;
			}
			

			for (Object object : config.getRanges()) {

				final AccountInterestConfigRange range = (AccountInterestConfigRange) object;

				final RateIndex rateIndex = range.getRateIndex();
			
				//GSM: fix if index is FIXED. 22/05/2014.
				String assetName = getAssetControlName(rateIndex);
				
				if (Util.isEmpty(assetName))
				{
					//GSM: put name if fixed
					if (range.isFixed()){
						assetName = "FIXED"; // + range.getFixedRate();						
					}
					else {
						continue;
					}
				}
					//continue;
				
				//build the row
				ReportRow row = (ReportRow) reportRow.clone();
				row.setProperty(ReportRow.MARGIN_CALL_CONFIG, contract);
				row.setProperty("Range", range);
				row.setProperty("AssetName", assetName);
				row.setProperty("Account", account);
				row.setProperty("AccountConfig", config);
				rowsList.add(row);
			}
		}

	}

	/**
	 * @param rateIndex
	 * @return the mapped name of asset control
	 */
	private String getAssetControlName(final RateIndex rateIndex) {
		
		if (rateIndex == null || rateIndex.getQuoteName() == null)
			return "";
		
		final String quoteName = rateIndex.getQuoteName();
		final String assetName = this.feedMap.get(quoteName);
		
		if ((assetName != null) && !assetName.isEmpty()) {
			return assetName;
		}
		Log.warn(Opt_ReferenceRateReport.class, "QuoteName " + quoteName + " is NOT mapped with A.C."+
		" \n Check Mapping between A.C. and Calypso. QuoteName is returned instead");
		
		return quoteName;
	}

	/**
	 * 
	 * @param conn
	 * @return a map with the mapped quotesNames and their AC name
	 */
	private HashMap<String, String> buildFeedMapping(final DSConnection conn) {

		final HashMap<String, String> feedHash = new HashMap<String, String>();

		try {
			@SuppressWarnings("unchecked")
			final Vector<FeedAddress> feeds = conn.getRemoteMarketData().getAllFeedAddress(ASSET_CONTROL);
			// only the addresses for TLM export
			if ((null != feeds) && (feeds.size() > 0)) {
				for (int i = 0; i < feeds.size(); i++) {
					feedHash.put(feeds.get(i).getQuoteName(), feeds.get(i).getFeedAddress());
				}
			}
		} catch (final RemoteException e) {
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, e);
			ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");// CONTROL-M
			// ERROR
		}
		return feedHash;
	}

	/**
	 * Retrieve MC contract for the account
	 * @param account
	 * @return
	 */
	protected CollateralConfig getMarginCallConfig(final Account account) {

		CollateralConfig contract = null;

		if (account != null) {
			CollateralServiceRegistry service = ServiceRegistry.getDefault();
			try {
				contract = service.getCollateralDataServer().getMarginCallConfig(
						Integer.parseInt(account.getAccountProperty(MC_CONTRACT)));
			} catch (NumberFormatException nfe) {
				Log.warn(Opt_ReferenceRateReport.class, nfe.getMessage());
				Log.warn(this, nfe); //sonar
			} catch (RemoteException re) {
				Log.warn(Opt_ReferenceRateReport.class, re.getMessage());
				Log.warn(this, re); //sonar
			}
		}

		return contract;
	}

}
