package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import calypsox.tk.core.CollateralStaticAttributes;
import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;
import calypsox.util.collateral.CollateralManagerUtil;

import com.calypso.apps.util.AppUtil;
import com.calypso.tk.collateral.dto.MarginCallEntryDTO;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;

public class SantAgreementParametersReport extends SantReport {

	private static final long serialVersionUID = 1L;

	@Override
	protected boolean checkProcessEndDate() {
		return false;
	}

	@Override
	public ReportOutput loadReport(Vector<String> errorMsgs) {

		if (getReportTemplate() == null) {
			return null;
		}

		Collection<CollateralConfig> contracts = null;
		HashMap<Integer, MarginCallEntryDTO> mcEntries = null;
		Map<Integer, Integer> tradeCountMap = null;
		try {
			contracts = loadContracts(this._reportTemplate, errorMsgs);
			mcEntries = getMCEntries(contracts);
			tradeCountMap = getTradeCountMap(mcEntries);
			
		} catch (Exception e) {
			Log.error(SantAgreementParametersReport.class, e.getMessage());
			Log.error(SantAgreementParametersReport.class, e);
		}

		final DefaultReportOutput output = new DefaultReportOutput(this);
		final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();

		if (contracts != null){
			String peName = getPricingEnv().getName();
			JDate valDate = CollateralUtilities.getMCValDate(getProcessStartDate());
			PricingEnv pricingEnv = AppUtil.loadPE(peName, new JDatetime(valDate, TimeZone.getDefault()));

			Iterator<CollateralConfig> iterator = contracts.iterator();
			while (iterator.hasNext()) {
				CollateralConfig agreement = iterator.next();
				final ReportRow row = new ReportRow(agreement);
				row.setProperty("MarginCallConfig", agreement);
				
				if (!Util.isEmpty(mcEntries)) {
					MarginCallEntryDTO entryDTO = mcEntries.get(agreement.getId());
					if (entryDTO != null) {
						row.setProperty(ReportRow.MARGIN_CALL_ENTRY, entryDTO);
						row.setProperty("TRADE_COUNT", tradeCountMap.get(entryDTO.getId()));

						if ((entryDTO.getPreviousSecurityMargin() != 0.0) && (entryDTO.getPreviousCashMargin() != 0.0)) {
							// we need two rows if both security and cash margins are not zero
							row.setProperty(SantAgreementParametersReportStyle.EFFECTIVE_ASSET_TYPE, "BOTH");
							// try {
							// ReportRow secRow = (ReportRow) row.clone();
							// secRow.setProperty(SantAgreementParametersReportStyle.EFFECTIVE_ASSET_TYPE, "SECURITY");
							// secRow.setProperty("ProcessDate", getProcessStartDate());
							// reportRows.add(secRow);
							// } catch (Exception e) {
							// Log.error(SantAgreementParametersReport.class, e);
							// }
						} else {
							if (entryDTO.getPreviousSecurityMargin() != 0.0) {
								row.setProperty(SantAgreementParametersReportStyle.EFFECTIVE_ASSET_TYPE, "SECURITY");
							} else {
								row.setProperty(SantAgreementParametersReportStyle.EFFECTIVE_ASSET_TYPE, "CASH");
							}
						}
					}
				}
				row.setProperty("ProcessDate", getProcessStartDate());
				row.setProperty("ValDate", valDate);
				row.setProperty("PricingEnv", pricingEnv);
				reportRows.add(row);
			}
		}
		output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
		return output;
	}

	private Map<Integer, Integer> getTradeCountMap(HashMap<Integer, MarginCallEntryDTO> mcEntries)
			throws RemoteException {
		ArrayList<Integer> mcEntryIdsList = new ArrayList<Integer>();

		Iterator<MarginCallEntryDTO> iterator = mcEntries.values().iterator();
		while (iterator.hasNext()) {
			mcEntryIdsList.add(iterator.next().getId());
		}
		return SantReportingUtil.getSantReportingService(getDSConnection()).getTradeCountForEntries(mcEntryIdsList);

	}

	private HashMap<Integer, MarginCallEntryDTO> getMCEntries(Collection<CollateralConfig> list) throws RemoteException {
		HashMap<Integer, MarginCallEntryDTO> map = new HashMap<Integer, MarginCallEntryDTO>();
		if (!Util.isEmpty(list)) {

			ArrayList<Integer> idsList = new ArrayList<Integer>();
			Iterator<CollateralConfig> iterator = list.iterator();
			while (iterator.hasNext()) {
				CollateralConfig agreement = iterator.next();
				idsList.add(agreement.getId());
			}

			List<MarginCallEntryDTO> entries = CollateralManagerUtil.loadMarginCallEntriesDTO(idsList, getProcessStartDate());

			for (int i = 0; i < entries.size(); i++) {
				MarginCallEntryDTO entry = entries.get(i);
				map.put(entry.getCollateralConfigId(), entry);
			}
		}
		return map;
	}

	private Collection<CollateralConfig> loadContracts(ReportTemplate template, Vector<String> errors) throws Exception {
		Map<Integer, CollateralConfig> marginCallConfigMap = new HashMap<Integer, CollateralConfig>();

		List<CollateralConfig> finalList = new ArrayList<CollateralConfig>();

		String agrIds = (String) template.get(SantGenericTradeReportTemplate.AGREEMENT_ID);
		String agrPoIdsList = (String) template.get(SantGenericTradeReportTemplate.PROCESSING_ORG_IDS);
		String cptyIdsList = (String) template.get(SantGenericTradeReportTemplate.COUNTERPARTY);
		String agrType = (String) template.get(SantGenericTradeReportTemplate.AGREEMENT_TYPE);
		// String agrStatus = (String) template.get(SantGenericTradeReportTemplate.AGREEMENT_STATUS);
		String agrDirection = (String) template.get(SantGenericTradeReportTemplate.AGREEMENT_DIRECTION);
		String poEligibleSecInd = (String) template.get(SantGenericTradeReportTemplate.PO_ELIGIBLE_SEC_IND);
		String baseCurrency = (String) template.get(SantGenericTradeReportTemplate.BASE_CURRENCY);

		String economicSector = (String) template.get(SantGenericTradeReportTemplate.ECONOMIC_SECTOR);
		String headCloneIndicator = (String) template.get(SantGenericTradeReportTemplate.HEAD_CLONE_INDICATOR);

		ArrayList<Integer> agrIdsListFromDB = null;
		if (Util.isEmpty(agrPoIdsList) && Util.isEmpty(cptyIdsList)) {
			// Load all contracts
			StringBuilder sql = new StringBuilder();
			String extCriteria = addExtraCriteriaToSQL(sql, agrIds, agrType, agrDirection, baseCurrency,
					poEligibleSecInd);
			sql.append("select mrg_call_def from mrgcall_config mc ");
			if (!Util.isEmpty(extCriteria)) {
				sql.append(" where ").append(extCriteria);
			}

			agrIdsListFromDB = SantReportingUtil.getSantReportingService(getDSConnection()).getMarginCallConfigIds(
					sql.toString());

		} else if (!Util.isEmpty(agrPoIdsList) && !Util.isEmpty(cptyIdsList)) {
			// both the lists are present
			agrIdsListFromDB = getMarginCallConfigs(agrPoIdsList, cptyIdsList, agrIds, agrType, agrDirection,
					baseCurrency, poEligibleSecInd);

		} else if (!Util.isEmpty(agrPoIdsList) || !Util.isEmpty(cptyIdsList)) {
			String idsList = "";
			String leRole = "";
			if (!Util.isEmpty(agrPoIdsList)) {
				idsList = agrPoIdsList;
				leRole = "ProcessingOrg";
			} else {
				idsList = cptyIdsList;
				leRole = "CounterParty";
			}

			agrIdsListFromDB = getMarginCallConfigIdsWithLe(idsList, leRole, agrIds, agrType, agrDirection,
					baseCurrency, poEligibleSecInd);
		}

		if (!Util.isEmpty(agrIdsListFromDB)) {
			marginCallConfigMap = SantReportingUtil.getSantReportingService(getDSConnection())
					.getMarginCallConfigByIds(agrIdsListFromDB);
			// Now filter unwanted ones as per additional fields specified
			Set<Integer> keySet = marginCallConfigMap.keySet();
			Iterator<Integer> iterator = keySet.iterator();
			while (iterator.hasNext()) {
				Integer key = iterator.next();
				CollateralConfig marginCallConfig = marginCallConfigMap.get(key);
				if (!Util.isEmpty(economicSector)
						&& !economicSector.equals(marginCallConfig
								.getAdditionalField(CollateralStaticAttributes.MCC_ADD_FIELD_ECONOMIC_SECTOR))) {
					continue;
				}
				if (!Util.isEmpty(headCloneIndicator)
						&& !headCloneIndicator.equals(marginCallConfig
								.getAdditionalField(CollateralStaticAttributes.MCC_ADD_FIELD_HEAD_CLONE))) {
					continue;
				}

				finalList.add(marginCallConfig);
			}
		}

		return finalList;
	}

	/**
	 * Retreives MarginCallContracts for the combination of Po ids, cpty ids, contract ids passed in.
	 * 
	 * @param poIdsList
	 *            List of po ids seperated by comma
	 * @param cptyIdsList
	 *            List of cpty ids seperated by comma
	 * @param contractIdsList
	 *            List of contract ids seperated by comma
	 * 
	 * @param agrType
	 *            Type of Agreement
	 * @param direction
	 *            direction BILATERAL/UNILATERAL
	 * 
	 * @return
	 * @throws RemoteException
	 */
	public ArrayList<Integer> getMarginCallConfigs(String poIdsList, String cptyIdsList, String contractIdsList,
			String agrType, String direction, String baseCurrency, String eligibleSecInd) throws RemoteException {

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT mrg_call_def FROM mrgcall_config mc ");
		sql.append("WHERE (   process_org_id in ("
				+ poIdsList
				+ ") OR (mrg_call_def IN (SELECT mcc_id FROM MRGCALL_CONFIG_LE WHERE le_role='ProcessingOrg' AND le_id in ("
				+ poIdsList + ") ))    ) ");
		sql.append("AND (     legal_entity_id in ("
				+ cptyIdsList
				+ ") OR (mrg_call_def IN (SELECT mcc_id FROM MRGCALL_CONFIG_LE WHERE le_role='CounterParty' AND le_id in ("
				+ cptyIdsList + ")))   )");

		// Add the remaining contract criteria
		String extCriteria = addExtraCriteriaToSQL(sql, contractIdsList, agrType, direction, baseCurrency,
				eligibleSecInd);
		if (!Util.isEmpty(extCriteria)) {
			sql.append(" AND ").append(extCriteria);
		}

		return SantReportingUtil.getSantReportingService(getDSConnection()).getMarginCallConfigIds(sql.toString());
	}

	/**
	 * Gets list of contract ids for the given combination
	 * 
	 * @param leIdsList
	 * @param leRole
	 * @param contractIdsList
	 * @param agrType
	 * @param direction
	 * @return
	 * @throws Exception
	 */
	private ArrayList<Integer> getMarginCallConfigIdsWithLe(String leIdsList, String leRole, String contractIdsList,
			String agrType, String direction, String baseCurrency, String eligibleSecInd) throws Exception {

		StringBuilder sql = new StringBuilder();
		sql.append("select distinct mc.mrg_call_def from mrgcall_config mc, mrgcall_config_le mcle ");
		sql.append("where mc.mrg_call_def=mcle.mcc_id(+) ");

		if (leRole.equals("ProcessingOrg")) {
			sql.append("AND ( mc.process_org_id in (" + leIdsList + ")");
		} else {
			sql.append("AND ( mc.LEGAL_ENTITY_ID in (" + leIdsList + ")");
		}
		sql.append(" OR ");
		sql.append(" ( mcle.le_id in (" + leIdsList + ")").append(" and mcle.le_role='" + leRole + "') )");

		// Add the remaining contract criteria
		String extCriteria = addExtraCriteriaToSQL(sql, contractIdsList, agrType, direction, baseCurrency,
				eligibleSecInd);
		if (!Util.isEmpty(extCriteria)) {
			sql.append(" AND ").append(extCriteria);
		}

		return SantReportingUtil.getSantReportingService(getDSConnection()).getMarginCallConfigIds(sql.toString());

	}

	private String addExtraCriteriaToSQL(StringBuilder sql, String contractIdsList, String agrType, String direction,
			String baseCurrency, String eligibleSecInd) {
		StringBuilder temp = new StringBuilder();
		if (!Util.isEmpty(contractIdsList)) {
			temp.append(" mc.mrg_call_def in (" + contractIdsList + ") ");
		}

		if (!Util.isEmpty(agrType)) {
			if (temp.length() > 0) {
				temp.append(" AND ");
			}
			temp.append(" mc.contract_type='" + agrType + "'");
		}
		
		if (Util.isEmpty(agrType)){
			temp.append(" mc.contract_type <> 'CSA_FACADE' ");
		}

		if (!Util.isEmpty(direction)) {
			if (temp.length() > 0) {
				temp.append(" AND ");
			}
			temp.append(" mc.contract_direction='" + direction + "'");
		}

		if (!Util.isEmpty(baseCurrency)) {
			if (temp.length() > 0) {
				temp.append(" AND ");
			}
			temp.append(" mc.currency_code='" + baseCurrency + "'");
		}

		if (!Util.isEmpty(eligibleSecInd)) {
			if (temp.length() > 0) {
				temp.append(" AND ");
			}
			temp.append(" mc.po_coll_type='" + eligibleSecInd + "'");
		}
		if (temp.length() > 0) {
			temp.append(" AND ");
		}
		temp.append(" ( agreement_status='OPEN' OR (agreement_status='CLOSED' AND closing_date>"
				+ Util.date2SQLString(getProcessStartDate()) + " ) )");

		return temp.toString();
	}

}
