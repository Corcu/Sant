/* Actualizado por David Porras Martinez 22-11-11 */

package calypsox.tk.report;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.MarginCallReport;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

import calypsox.ErrorCodeEnum;
import calypsox.tk.util.ControlMErrorLogger;
import calypsox.util.CheckRowsNumberReport;
import calypsox.util.SantReportingUtil;
import calypsox.util.binding.CustomBindVariablesUtil;
import calypsox.util.collateral.CollateralManagerUtil;
import calypsox.util.collateral.CollateralUtilities;

public class ELBEAgreementsExtractionReport extends MarginCallReport implements CheckRowsNumberReport {

	private static final long serialVersionUID = -7848716938915379876L;
	public static final String PO = "PO";
	public static final String LEGAL_ENTITY_ROLE_PROCESSING_ORG = "ProcessingOrg";
	private static final String AGREEMENT_STATUS_CLOSED = "CLOSED";
	private static ArrayList<Integer> blackList = new ArrayList<>();
	int cont = 1;

	@Override
	public void setAllowPricingEnv(boolean allowPricingEnv) {
		super.setAllowPricingEnv(true);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public ReportOutput load(final Vector errorMsgsP) {
		final DefaultReportOutput output = new StandardReportOutput(this);
		final ArrayList<ReportRow> reportRows = new ArrayList<>();
		Collection<LegalEntity> legalEntities;
		List<CollateralConfig> marginCalls;
		JDate jdate = null;
		final PricingEnv pricingEnv = loadPricingEnv();
		final ReportTemplate reportTemp = getReportTemplate();
		// We retrieve the different columns specified for the current report,
		// and the attribute 'Processing Org' to filter the data retrieved.

		try {
			// get date
			jdate = reportTemp.getValDate();
			// get legal entities // GSM 24/07/15. SBNA Multi-PO filter
			legalEntities = CollateralUtilities.filterLEPoByTemplate(reportTemp);
			if (!Util.isEmpty(legalEntities)) {
				Set<Long> lstContractsOK=getContractIDsWithTrades(jdate);
				for (LegalEntity legalEntity : legalEntities) {
					if (legalEntity != null) {
						// get contracts
						marginCalls = loadContracts(legalEntity.getId());
						if (!Util.isEmpty(marginCalls)) {
							checkContractAddReportRows(reportTemp, reportRows, marginCalls, jdate, pricingEnv,lstContractsOK,
									errorMsgsP);
						}
					}
				}
			}
			// set report rows on output
			output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
			// Generate a task exception if the number of rows is out of an umbral defined
			HashMap<String, String> value = SantReportingUtil.getSchedTaskNameOrReportTemplate(this);
			checkAndGenerateTaskReport(output, value);
			blackList.clear();
			return output;
		} catch (final RemoteException e) {
			Log.error(this, "Generic error loading data in ELBEAgreementsExtractionReport - " + e);
			ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");
		} catch (final NumberFormatException e) {
			Log.error(this, "Number format error loading data in ELBEAgreementsExtractionReport - " + e);
			ControlMErrorLogger.addError(ErrorCodeEnum.OutputCVSFileCanNotBeWritten, "Not document generated");
		}
		blackList.clear();
		return null;
	}

	private void checkContractAddReportRows(final ReportTemplate reportTemp, final ArrayList<ReportRow> reportRows,
			final List<CollateralConfig> marginCalls, final JDate jdate, final PricingEnv pricingEnv,final Set<Long> lstContractsOK,
			final List<String> errorMsgsP) {
		boolean emptyTrade;
		for (CollateralConfig marginCall : marginCalls) {
			emptyTrade = true;
			if (marginCall != null && checkContract(marginCall)) {
				if (lstContractsOK.contains(Long.valueOf((marginCall.getId())))) {
					emptyTrade = false;
				}
				final List<ELBEAgreementsExtractionItem> marginCallReportRows = ELBEAgreementsExtractionLogic
						.getReportRows(marginCall, pricingEnv, getActualDate(), jdate, reportTemp.getHolidays(),
								errorMsgsP, emptyTrade);
				for (int j = 0; j < marginCallReportRows.size(); j++) {
					ELBEAgreementsExtractionItem item = marginCallReportRows.get(j);
					ReportRow reportRow = new ReportRow(item);
					reportRow.setProperty("MarginCallConfig", item.getMarginCallConfig());
					reportRows.add(reportRow);
					Log.debug(this, "Adding row to the report." + cont++);
				}
			}
		}

	}

	/**
	 * The method getActualDate
	 * 
	 * @return String
	 */

	private String getActualDate() {

		final Date date = new Date();
		final SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
		return sdf.format(date);

	}

	/**
	 * Method checkCOntract
	 * 
	 * @param marginCall
	 * @return boolean
	 */
	private boolean checkContract(CollateralConfig marginCall) {

		// check status
		if (AGREEMENT_STATUS_CLOSED.equals(marginCall.getAgreementStatus())) {
			return false;
		}
		// check black list
		if (blackList.contains(marginCall.getId())) {
			return false;
		}
		// Exclude CSA Facade
		if (KGR_Collateral_MarginCallReport.CSA_FACADE.equals(marginCall.getContractType())) {
			return false;
		}

		// add
		blackList.add(marginCall.getId());

		return true;

	}

	@SuppressWarnings({ "rawtypes" })
	public Set<Long> getContractIDsWithTrades(final JDate jdate) throws RemoteException {
	StringBuilder query = new StringBuilder();
		HashSet<Long> results = new HashSet<>();
		query.append("select product_simplexfer.linked_id "); 
		query.append(" from "); 
		query.append(" product_desc, product_simplexfer, trade "); 
		query.append(" where ");
		query.append(" trade.product_id = product_simplexfer.product_id ");
		query.append(" and trade.product_id = product_desc.product_id ");
		query.append(" and product_desc.product_type = ? ");
		List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil.createNewBindVariable("MarginCall");
		query.append(" and TRUNC(trade.TRADE_DATE_TIME)= ? ");
		CustomBindVariablesUtil.addNewBindVariableToList(jdate, bindVariables);
		query.append(" GROUP BY product_simplexfer.linked_id ");
		
		try {
			List retValues=null;
			retValues =  DSConnection.getDefault().getRemoteAccess().executeSelectSQL(query.toString(), bindVariables);
			if(!Util.isEmpty(retValues)) {
				for (int i=0;i<retValues.size();i++) {
					Object retValu=retValues.get(i);
					if(retValu instanceof List && i>1) {
						Long val=(Long)((List) retValu).get(0);
						results.add(val);
					}
				}
				return results;			
			}
		} catch (RemoteException e) {
			Log.error(this, "Cannot execute query: " + query.toString());
			throw e;
		} catch (NumberFormatException e) {
			Log.error(this, "Cannot cast to number");
			throw e;
		}
		return results;
	}
	/**
	 * Method loadPricingEnv
	 * 
	 * @return
	 */
	private PricingEnv loadPricingEnv() {
		PricingEnv prcingEnv = getPricingEnv();
		if (null == prcingEnv) {
			try {
				// Load default OFFICIAL
				prcingEnv = DSConnection.getDefault().getRemoteMarketData().getPricingEnv("OFFICIAL");
			} catch (CalypsoServiceException e) {
				Log.error(this, "Cannot get PricingEnv OFFICIAL. " + e);
			}
		}
		return prcingEnv;
	}

	/**
	 * Method loadContracts
	 * 
	 * @param ownerId
	 * @return
	 * @throws RemoteException
	 */
	private List<CollateralConfig> loadContracts(int ownerId) throws RemoteException {

		MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();

		List<Integer> list = new ArrayList<>();
		list.add(ownerId);

		mcFilter.setProcessingOrgIds(list);

		return CollateralManagerUtil.loadCollateralConfigs(mcFilter);
	}

}
