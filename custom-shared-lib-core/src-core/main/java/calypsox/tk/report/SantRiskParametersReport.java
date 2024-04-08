package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.collateral.filter.MarginCallConfigFilter;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.collateral.CacheCollateralClient;

import calypsox.tk.report.generic.SantGenericTradeReportTemplate;
import calypsox.tk.report.riskparameters.SantRiskParameterWrapper;
import calypsox.tk.util.riskparameters.SantRiskParameter;
import calypsox.tk.util.riskparameters.SantRiskParameterBuilder;
import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;

public class SantRiskParametersReport extends SantReport {

	private static final long serialVersionUID = 790458666598235108L;

	private static final int SQL_IN_ITEM_COUNT = 1000;

	private JDate endDate;
	private JDate startDate;
	private boolean fetchAtCurrentDate = false;
	private final Vector<CollateralConfig> allContracts = new Vector<CollateralConfig>();

	private Vector<String> contractIdsSubSet;
	
	// CSA FACADE
	private final String CSA_FACADE = "CSA_FACADE";
	
	@Override
	public ReportOutput loadReport(final Vector<String> errorMsgs) {

		final DefaultReportOutput output = new DefaultReportOutput(this);
		computeDates(errorMsgs);

		// Converts the dates from processDate to ValDate
		computeValDates();

		if (!errorMsgs.isEmpty()) {
			return output;
		}
		// Agreement
		final String agreementIds = (String) getReportTemplate().get(SantGenericTradeReportTemplate.AGREEMENT_ID);
		
		// Owner of the contract
		// final String ownerIds = (String)
		// getReportTemplate().get(SantGenericTradeReportTemplate.OWNER_DEALS);
		// GSM 05/08/15. SBNA Multi-PO filter
		final String ownerIds = CollateralUtilities.filterPoIdsByTemplate(getReportTemplate());

		// Counterparty Owner
		String cpOwner = (String) getReportTemplate().get(SantRiskParametersReportStyle.COUNTERPARTY_OWNER);

		this.contractIdsSubSet = loadContracts(agreementIds, ownerIds, errorMsgs);

		FetchDataAtEndDate fetchAtEndDate = new FetchDataAtEndDate();
		FetchDataAtStartDate fetchAtStartDate = new FetchDataAtStartDate();

		while (fetchAtEndDate.isAlive() || fetchAtStartDate.isAlive()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Log.error(this, e);
				errorMsgs.add(e.getMessage());
				return output;
			}
		}
		//
		// // Fetch data at end date
		// List<SantRiskParameter> rpListAtEndDate = new
		// ArrayList<SantRiskParameter>();
		// if (this.fetchAtCurrentDate) {
		// rpListAtEndDate =
		// performRiskParameterAtCurrentDate(getDSConnection(), errorMsgs);
		// } else {
		// String sqlQuery = buildSQLQueryAtEndDate(this.contractIdsSubSet);
		// rpListAtEndDate = getRiskParametersFromDB(sqlQuery, errorMsgs);
		// }
		//
		// // Fetch data at start date
		// String sqlQuery = buildSQLQueryAtStartDate();
		// List<SantRiskParameter> rpListAtStartDate =
		// getRiskParametersFromDB(sqlQuery, errorMsgs);

		List<SantRiskParameter> rpListAtEndDate = fetchAtEndDate.getRpList();
		List<SantRiskParameter> rpListAtStartDate = fetchAtStartDate.getRpList();

		errorMsgs.addAll(fetchAtEndDate.getErrorMsgs());
		errorMsgs.addAll(fetchAtStartDate.getErrorMsgs());

		output.setRows(getReportRows(rpListAtEndDate, rpListAtStartDate, cpOwner));
		return output;
	}

	private List<SantRiskParameter> getRiskParametersFromDB(String sqlQuery, Vector<String> errorMsgs) {
		List<SantRiskParameter> rpList = new ArrayList<SantRiskParameter>();
		try {
			rpList.addAll(SantReportingUtil.getSantRiskParameterService(getDSConnection()).get(sqlQuery));

		} catch (RemoteException e) {
			String error = "Error Loading Risk Parameters " + e.getMessage();
			Log.error(this, error, e);
			errorMsgs.add(error);
		}
		return rpList;
	}

	private void computeDates(Vector<String> errorMsgs) {
		this.fetchAtCurrentDate = false;
		JDate now = JDate.getNow();
		this.endDate = getProcessEndDate();
		this.startDate = getProcessStartDate();

		if (this.endDate.after(now)) {
			String msg = String.format("ProcessEndDate %s has to be lower or equals than CurrentDate %s", this.endDate,
					now);
			errorMsgs.add(msg);
			return;
		}
		if (this.startDate.gte(this.endDate)) {
			String msg = String.format("ProcesStartDate %s has to be lower than ProcessEndDate %s", this.startDate,
					this.endDate);
			errorMsgs.add(msg);
			return;
		}

		if (this.endDate.equals(now)) {
			this.fetchAtCurrentDate = true;
		}

	}

	private void computeValDates() {
		this.endDate = CollateralUtilities.getMCValDate(this.endDate);
		this.startDate = CollateralUtilities.getMCValDate(this.startDate);
	}

	private ReportRow[] getReportRows(List<SantRiskParameter> rpListAtEndDate,
			List<SantRiskParameter> rpListAtStartDate, String cpOwner) {
		List<ReportRow> rows = new ArrayList<ReportRow>();

		Map<String, SantRiskParameterWrapper> map = new HashMap<String, SantRiskParameterWrapper>();

		for (SantRiskParameter rp : rpListAtEndDate) {
			if (Util.isEmpty(cpOwner)) {
				String key = "Owner" + rp.getContractId();
				buildMap("Owner", key, rp, map);
				key = "Counterparty" + rp.getContractId();
				buildMap("Counterparty", key, rp, map);
			} else if ("OWNER".equals(cpOwner)) {
				String key = "Owner" + rp.getContractId();
				buildMap("Owner", key, rp, map);
			} else {
				String key = "Counterparty" + rp.getContractId();
				buildMap("Counterparty", key, rp, map);
			}
		}

		for (SantRiskParameter rp : rpListAtStartDate) {
			if (Util.isEmpty(cpOwner)) {
				String key = "Owner" + rp.getContractId();
				buildMap("Owner", key, rp, map);
				key = "Counterparty" + rp.getContractId();
				buildMap("Counterparty", key, rp, map);
			} else if ("OWNER".equals(cpOwner)) {
				String key = "Owner" + rp.getContractId();
				buildMap("Owner", key, rp, map);
			} else {
				String key = "Counterparty" + rp.getContractId();
				buildMap("Counterparty", key, rp, map);
			}
		}

		for (SantRiskParameterWrapper wrapper : map.values()) {
			wrapper.build();
			rows.add(new ReportRow(wrapper, "SantRiskParameterWrapper"));
		}
		return rows.toArray(new ReportRow[rows.size()]);
	}

	private List<SantRiskParameter> performRiskParameterAtCurrentDate(DSConnection ds, Vector<String> errorMsgs) {
		List<SantRiskParameter> rpList = new ArrayList<SantRiskParameter>();
		SantRiskParameterBuilder builder = new SantRiskParameterBuilder();

		if (this.contractIdsSubSet.isEmpty()) {
			for (CollateralConfig contract : this.allContracts) {
				if (("CLOSE".equals(contract.getAgreementStatus())) && (CSA_FACADE.equals(contract.getContractType()))){
					continue;
				}
				
				SantRiskParameter rp = builder.build(contract, this.endDate, getPricingEnv());
				rpList.add(rp);
			}
			return rpList;
		}

		// Else
		for (String contractId : this.contractIdsSubSet) {
			CollateralConfig contract = CacheCollateralClient.getCollateralConfig(getDSConnection(),
					Integer.valueOf(contractId));

			if ("CLOSE".equals(contract.getAgreementStatus())) {
				continue;
			}
			SantRiskParameter rp = builder.build(contract, this.endDate, getPricingEnv());
			rpList.add(rp);
		}
		return rpList;

	}

	private void buildMap(String cpOwner, String key, SantRiskParameter rp, Map<String, SantRiskParameterWrapper> map) {
		SantRiskParameterWrapper wrapper = map.get(key);
		if (wrapper == null) {
			wrapper = new SantRiskParameterWrapper(this.endDate, this.startDate, cpOwner);
			wrapper.setCurrent(rp);
			map.put(key, wrapper);
		} else {
			if (wrapper.getPrevious() == null) {
				wrapper.setPrevious(rp);
			}
			// else do nothing
		}

	}

	private Vector<String> loadContracts(String agreementIds, String ownerIds, Vector<String> errorMsgs) {
		Vector<String> contractIds = new Vector<String>();

		if (!Util.isEmpty(agreementIds)) {
			contractIds = Util.string2Vector(agreementIds);
			return contractIds;
		}

		if (!Util.isEmpty(ownerIds)) {

			Vector<String> poIdsStr = Util.string2Vector(ownerIds);
			Vector<CollateralConfig> contracts = new Vector<CollateralConfig>();
			// AAP MIG14.4 In v14 didn't seem to filter the contracts
			List<Integer> poIdsInteger = new ArrayList<Integer>();
			try {
				for (String poId : poIdsStr)
					poIdsInteger.add(Integer.parseInt(poId));
				final CollateralServiceRegistry srvReg = ServiceRegistry.getDefault();
				MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();
				mcFilter.setProcessingOrgIds(poIdsInteger);
				contracts.addAll(srvReg.getCollateralDataServer().getMarginCallConfigs(mcFilter,
						srvReg.getCollateralDataServer().loadDefaultContext()));

			} catch (Exception e) {
				Log.error(this, "Cannot load CONTRACTS", e);
				errorMsgs.add("Cannot load CONTRACTS - " + e.getMessage());
			}
			// for (String poIdStr : poIdsStr) {
			//
			// try {
			//
			// final CollateralServiceRegistry srvReg =
			// ServiceRegistry.getDefault();
			// // AAP MIG14.4 In v14 didn't seem to filter the contracts
			// MarginCallConfigFilter mcFilter = new MarginCallConfigFilter();
			// mcFilter.setProcessingOrg(poIdStr);
			// contracts.addAll(srvReg.getCollateralDataServer().getMarginCallConfigs(mcFilter,
			// srvReg.getCollateralDataServer().loadDefaultContext()));
			// // contracts.addAll(
			// //
			// (srvReg.getCollateralDataServer().getAllMarginCallConfig(Integer.valueOf(poIdStr),
			// // 0)));
			//
			// } catch (Exception e) {
			// Log.error(this, "Cannot load CONTRACTS", e);
			// errorMsgs.add("Cannot load CONTRACTS - " + e.getMessage());
			// }
			// }
			for (CollateralConfig mcc : contracts) {
				contractIds.add(String.valueOf(mcc.getId()));
			}
			return contractIds;
		}
		// contract ids empty => load all contracts
		this.allContracts.clear();
		try {

			this.allContracts.addAll(ServiceRegistry.getDefault().getCollateralDataServer().getAllMarginCallConfig());
		} catch (Exception e) {
			Log.error(this, "Cannot load CONTRACTS", e);
			errorMsgs.add("Cannot load CONTRACTS - " + e.getMessage());
		}

		return contractIds;
	}

	class FetchDataAtStartDate {

		private final Vector<String> errorMsgs = new Vector<String>();
		private List<SantRiskParameter> rpList;
		private Thread innerThread;

		public FetchDataAtStartDate() {
			load();
		}

		public List<SantRiskParameter> getRpList() {
			return this.rpList;
		}

		public Vector<String> getErrorMsgs() {
			return this.errorMsgs;
		}

		public boolean isAlive() {
			return this.innerThread.isAlive();
		}

		private void load() {
			this.innerThread = new Thread(new Runnable() {

				@Override
				public void run() {
					loadData();
				}
			});
			this.innerThread.start();

		}

		private void loadData() {
			String sqlQuery = buildSQLQueryAtStartDate();
			this.rpList = getRiskParametersFromDB(sqlQuery, this.errorMsgs);
			if (!this.rpList.isEmpty()) {
				return;
			}
			sqlQuery = buildSQLQueryAtMaxDateLowerThanStartDate();
			this.rpList = getRiskParametersFromDB(sqlQuery, this.errorMsgs);
			if (!this.rpList.isEmpty()) {
				return;
			}
			sqlQuery = buildSQLQueryAtMinDate();
			this.rpList = getRiskParametersFromDB(sqlQuery, this.errorMsgs);
			if (!this.rpList.isEmpty()) {
				return;
			}
			this.rpList = performRiskParameterAtCurrentDate(getDSConnection(), this.errorMsgs);
		}

		private String buildSQLQueryAtStartDate() {

			StringBuilder builder = new StringBuilder(" select srp.* from san_risk_parameter srp ");
			builder.append(" where srp.val_date = ");
			builder.append(Util.date2SQLString(SantRiskParametersReport.this.startDate));

			if (!SantRiskParametersReport.this.contractIdsSubSet.isEmpty()) {
				int start = 0;
				for (int i = 0; i <= (SantRiskParametersReport.this.contractIdsSubSet.size()
						/ SQL_IN_ITEM_COUNT); i++) {
					int end = (i + 1) * SQL_IN_ITEM_COUNT;
					if (end > SantRiskParametersReport.this.contractIdsSubSet.size()) {
						end = SantRiskParametersReport.this.contractIdsSubSet.size();
					}
					final List<String> subList = SantRiskParametersReport.this.contractIdsSubSet.subList(start, end);
					start = end;

					if (i == 0) {
						builder.append(" and srp.contract_id in (").append(Util.collectionToString(subList))
								.append(")");
					} else {
						builder.append(" or srp.contract_id in (").append(Util.collectionToString(subList)).append(")");
					}
				}
			}
			builder.append(" order by srp.contract_id ASC, srp.val_date DESC ");

			return builder.toString();
		}

		private String buildSQLQueryAtMaxDateLowerThanStartDate() {

			StringBuilder builder = new StringBuilder(" select srp.* from san_risk_parameter srp ");
			builder.append(" where srp.val_date = ");
			builder.append(
					" (select MAX(temp.val_date) from san_risk_parameter temp where temp.contract_id = srp.contract_id and temp.val_date < ");
			builder.append(Util.date2SQLString(SantRiskParametersReport.this.startDate));
			builder.append(" ) ");

			if (!SantRiskParametersReport.this.contractIdsSubSet.isEmpty()) {
				int start = 0;
				for (int i = 0; i <= (SantRiskParametersReport.this.contractIdsSubSet.size()
						/ SQL_IN_ITEM_COUNT); i++) {
					int end = (i + 1) * SQL_IN_ITEM_COUNT;
					if (end > SantRiskParametersReport.this.contractIdsSubSet.size()) {
						end = SantRiskParametersReport.this.contractIdsSubSet.size();
					}
					final List<String> subList = SantRiskParametersReport.this.contractIdsSubSet.subList(start, end);
					start = end;

					if (i == 0) {
						builder.append(" and srp.contract_id in (").append(Util.collectionToString(subList))
								.append(")");
					} else {
						builder.append(" or srp.contract_id in (").append(Util.collectionToString(subList)).append(")");
					}
				}
			}
			builder.append(" order by srp.contract_id ASC, srp.val_date DESC ");

			return builder.toString();
		}

		private String buildSQLQueryAtMinDate() {

			StringBuilder builder = new StringBuilder(" select srp.* from san_risk_parameter srp ");
			builder.append(" where srp.val_date = ");
			builder.append(
					" (select MIN(temp.val_date) from san_risk_parameter temp where temp.contract_id = srp.contract_id ) ");

			if (!SantRiskParametersReport.this.contractIdsSubSet.isEmpty()) {
				int start = 0;
				for (int i = 0; i <= (SantRiskParametersReport.this.contractIdsSubSet.size()
						/ SQL_IN_ITEM_COUNT); i++) {
					int end = (i + 1) * SQL_IN_ITEM_COUNT;
					if (end > SantRiskParametersReport.this.contractIdsSubSet.size()) {
						end = SantRiskParametersReport.this.contractIdsSubSet.size();
					}
					final List<String> subList = SantRiskParametersReport.this.contractIdsSubSet.subList(start, end);
					start = end;

					if (i == 0) {
						builder.append(" and srp.contract_id in (").append(Util.collectionToString(subList))
								.append(")");
					} else {
						builder.append(" or srp.contract_id in (").append(Util.collectionToString(subList)).append(")");
					}
				}
			}
			builder.append(" order by srp.contract_id ASC, srp.val_date DESC ");

			return builder.toString();
		}
	}

	public class FetchDataAtEndDate {

		private final Vector<String> errorMsgs = new Vector<String>();
		private List<SantRiskParameter> rpList;
		private Thread innerThread;

		public FetchDataAtEndDate() {
			load();
		}

		public List<SantRiskParameter> getRpList() {
			return this.rpList;
		}

		public Vector<String> getErrorMsgs() {
			return this.errorMsgs;
		}

		public boolean isAlive() {
			return this.innerThread.isAlive();
		}

		private void load() {
			this.innerThread = new Thread(new Runnable() {

				@Override
				public void run() {
					loadData();
				}
			});
			this.innerThread.start();

		}

		private void loadData() {
			if (SantRiskParametersReport.this.fetchAtCurrentDate) {
				this.rpList = performRiskParameterAtCurrentDate(getDSConnection(), this.errorMsgs);
				return;
			}

			String sqlQuery = buildSQLQueryAtEndDate();
			this.rpList = getRiskParametersFromDB(sqlQuery, this.errorMsgs);
			if (!this.rpList.isEmpty()) {
				return;
			}
			sqlQuery = buildSQLQueryAtMaxDateLowerThanEndDate();
			this.rpList = getRiskParametersFromDB(sqlQuery, this.errorMsgs);
			if (!this.rpList.isEmpty()) {
				return;
			}
			sqlQuery = buildSQLQueryAtMinDateGreaterThanEndDate();
			this.rpList = getRiskParametersFromDB(sqlQuery, this.errorMsgs);
			if (!this.rpList.isEmpty()) {
				return;
			}
			this.rpList = performRiskParameterAtCurrentDate(getDSConnection(), this.errorMsgs);
		}

		private String buildSQLQueryAtEndDate() {

			StringBuilder builder = new StringBuilder(" select srp.* from san_risk_parameter srp ");
			builder.append(" where srp.val_date = ");
			builder.append(Util.date2SQLString(SantRiskParametersReport.this.endDate));

			if (!SantRiskParametersReport.this.contractIdsSubSet.isEmpty()) {
				int start = 0;
				for (int i = 0; i <= (SantRiskParametersReport.this.contractIdsSubSet.size()
						/ SQL_IN_ITEM_COUNT); i++) {
					int end = (i + 1) * SQL_IN_ITEM_COUNT;
					if (end > SantRiskParametersReport.this.contractIdsSubSet.size()) {
						end = SantRiskParametersReport.this.contractIdsSubSet.size();
					}
					final List<String> subList = SantRiskParametersReport.this.contractIdsSubSet.subList(start, end);
					start = end;

					if (i == 0) {
						builder.append(" and srp.contract_id in (").append(Util.collectionToString(subList))
								.append(")");
					} else {
						builder.append(" or srp.contract_id in (").append(Util.collectionToString(subList)).append(")");
					}
				}
			}
			builder.append(" order by srp.contract_id ASC, srp.val_date DESC ");

			return builder.toString();
		}

		private String buildSQLQueryAtMaxDateLowerThanEndDate() {

			StringBuilder builder = new StringBuilder(" select srp.* from san_risk_parameter srp ");
			builder.append(" where srp.val_date = ");
			builder.append(
					" (select MAX(temp.val_date) from san_risk_parameter temp where temp.contract_id = srp.contract_id and temp.val_date < ");
			builder.append(Util.date2SQLString(SantRiskParametersReport.this.endDate));
			builder.append(" ) ");

			if (!SantRiskParametersReport.this.contractIdsSubSet.isEmpty()) {
				int start = 0;
				for (int i = 0; i <= (SantRiskParametersReport.this.contractIdsSubSet.size()
						/ SQL_IN_ITEM_COUNT); i++) {
					int end = (i + 1) * SQL_IN_ITEM_COUNT;
					if (end > SantRiskParametersReport.this.contractIdsSubSet.size()) {
						end = SantRiskParametersReport.this.contractIdsSubSet.size();
					}
					final List<String> subList = SantRiskParametersReport.this.contractIdsSubSet.subList(start, end);
					start = end;

					if (i == 0) {
						builder.append(" and srp.contract_id in (").append(Util.collectionToString(subList))
								.append(")");
					} else {
						builder.append(" or srp.contract_id in (").append(Util.collectionToString(subList)).append(")");
					}
				}
			}
			builder.append(" order by srp.contract_id ASC, srp.val_date DESC ");

			return builder.toString();
		}

		private String buildSQLQueryAtMinDateGreaterThanEndDate() {

			StringBuilder builder = new StringBuilder(" select srp.* from san_risk_parameter srp ");
			builder.append(" where srp.val_date = ");
			builder.append(
					" (select MIN(temp.val_date) from san_risk_parameter temp where temp.contract_id = srp.contract_id and temp.val_date > ");
			builder.append(Util.date2SQLString(SantRiskParametersReport.this.endDate));
			builder.append(" ) ");

			if (!SantRiskParametersReport.this.contractIdsSubSet.isEmpty()) {
				int start = 0;
				for (int i = 0; i <= (SantRiskParametersReport.this.contractIdsSubSet.size()
						/ SQL_IN_ITEM_COUNT); i++) {
					int end = (i + 1) * SQL_IN_ITEM_COUNT;
					if (end > SantRiskParametersReport.this.contractIdsSubSet.size()) {
						end = SantRiskParametersReport.this.contractIdsSubSet.size();
					}
					final List<String> subList = SantRiskParametersReport.this.contractIdsSubSet.subList(start, end);
					start = end;

					if (i == 0) {
						builder.append(" and srp.contract_id in (").append(Util.collectionToString(subList))
								.append(")");
					} else {
						builder.append(" or srp.contract_id in (").append(Util.collectionToString(subList)).append(")");
					}
				}
			}
			builder.append(" order by srp.contract_id ASC, srp.val_date DESC ");

			return builder.toString();
		}
	}
}
