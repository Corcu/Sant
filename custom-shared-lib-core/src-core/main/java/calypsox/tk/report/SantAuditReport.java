/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.service.CollateralServiceRegistry;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.AuditValue;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.core.sql.SQLQuery;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.refdata.User;
import com.calypso.tk.report.AuditReport;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.Report;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;

import calypsox.tk.report.audit.SantAuditPropertiesLoader;

public class SantAuditReport extends Report {

	private static final long serialVersionUID = 4213922413219511034L;

	private Map<Integer, CollateralConfig> finalContractsMap;

	// Here we can have n contracts for same cpty
	private Map<Integer, Integer> mccIdVsCptyId;

	private Set<Integer> uniqueCptyIds;

	private List<List<Integer>> mccIdsSubList;

	private List<List<Integer>> cptyIdsSubList;

	private final SantAuditPropertiesLoader auditPropsLoader;

	private JDate fromDate = null;

	private JDate toDate = null;

	public SantAuditReport() {
		this.auditPropsLoader = new SantAuditPropertiesLoader();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public ReportOutput load(Vector errorMsgs) {
		reInitDates();
		DefaultReportOutput output = new DefaultReportOutput(this);
		loadStaticData();
		buildSubLists();
		AuditReport auditReport = new AuditReport();

		List<ReportRow> rows = new ArrayList<ReportRow>();

		// Agreement
		DefaultReportOutput outputAudit = (DefaultReportOutput) auditReport.load(buildMarginCallConfigSQL(), errorMsgs);
		for (ReportRow row : outputAudit.getRows()) {
			AuditValue av = (AuditValue) row.getProperty(ReportRow.AUDIT);
			if (!this.auditPropsLoader.isAgreementPropertyAuditable(av)) {
				continue;
			}
			rows.addAll(buildReportRows(av, DataChangeType.AGREEMENT));
		}

		// Credit Rating
		outputAudit = (DefaultReportOutput) auditReport.load(buildCreditRatingSQL(), errorMsgs);
		for (ReportRow row : outputAudit.getRows()) {
			AuditValue av = (AuditValue) row.getProperty(ReportRow.AUDIT);
			if (!this.auditPropsLoader.isRiskPropertyAuditable(av)) {
				continue;
			}
			rows.addAll(buildReportRows(av, DataChangeType.RATING));
		}

		// Contact
		outputAudit = (DefaultReportOutput) auditReport.load(buildLEContactSQL(), errorMsgs);
		for (ReportRow row : outputAudit.getRows()) {
			AuditValue av = (AuditValue) row.getProperty(ReportRow.AUDIT);
			if (!this.auditPropsLoader.isContactPropertyAuditable(av)) {
				continue;
			}
			rows.addAll(buildReportRows(av, DataChangeType.CONTACT));
		}

		output.setRows(rows.toArray(new ReportRow[rows.size()]));

		return output;
	}

	private List<ReportRow> buildReportRows(AuditValue av, DataChangeType dataChangeType) {
		switch (dataChangeType) {
		case AGREEMENT:
			return buildAgreementsReportRows(av, dataChangeType);
		case RATING:
			return buildRatingsReportRows(av, dataChangeType);
		case CONTACT:
			return buildContactsReportRows(av, dataChangeType);
		}
		return new ArrayList<ReportRow>();
	}

	private List<ReportRow> buildAgreementsReportRows(AuditValue av, DataChangeType dataChangeType) {
		List<ReportRow> rows = new ArrayList<ReportRow>();
		CollateralConfig mcc = this.finalContractsMap.get(av.getEntityId());
		if (mcc != null) {
			rows.add(new ReportRow(new SantAuditValue(av, dataChangeType, mcc.getName()), "SantAudit"));
		}
		return rows;
	}

	private List<ReportRow> buildRatingsReportRows(AuditValue av, DataChangeType dataChangeType) {
		List<ReportRow> rows = new ArrayList<ReportRow>();
		List<Integer> mccIds = getKeyFromValue(av.getRelatedObjectId());
		CollateralConfig mcc = null;
		for (Integer mccId : mccIds) {
			mcc = this.finalContractsMap.get(mccId);
			if (mcc == null) {
				continue;
			}
			rows.add(new ReportRow(new SantAuditValue(av, dataChangeType, mcc.getName()), "SantAudit"));
		}
		return rows;
	}

	private List<ReportRow> buildContactsReportRows(AuditValue av, DataChangeType dataChangeType) {
		List<ReportRow> rows = new ArrayList<ReportRow>();
		LEContact contact = BOCache.getLegalEntityContact(getDSConnection(), av.getEntityId());
		if (contact == null) {
			return rows;
		}
		List<Integer> mccIds = getKeyFromValue(av.getRelatedObjectId());
		CollateralConfig mcc = null;
		for (Integer mccId : mccIds) {
			mcc = this.finalContractsMap.get(mccId);
			if (mcc == null) {
				continue;
			}
			if (mcc.getName().equals(contact.getStaticDataFilter())) {
				rows.add(new ReportRow(new SantAuditValue(av, dataChangeType, mcc.getName()), "SantAudit"));
			}
		}
		return rows;
	}

	private List<Integer> getKeyFromValue(int relatedObjectId) {
		List<Integer> mccIds = new ArrayList<Integer>();
		for (Integer key : SantAuditReport.this.mccIdVsCptyId.keySet()) {
			if (SantAuditReport.this.mccIdVsCptyId.get(key).equals(relatedObjectId)) {
				mccIds.add(key);
			}
		}
		return mccIds;
	}

	private void reInitDates() {
		this.fromDate = (JDate) getReportTemplate().get(SantAuditReportTemplate.FROM_DATE);
		this.toDate = (JDate) getReportTemplate().get(SantAuditReportTemplate.TO_DATE);

		if (this.toDate == null) {
			this.toDate = getValDate();
		}
		if ((this.fromDate == null) || this.fromDate.gte(this.toDate)) {
			Vector<String> holidays = Util.string2Vector("SYSTEM");
			this.fromDate = this.toDate.addBusinessDays(-1, holidays);
		}
	}

	private void loadStaticData() {
		ReportTemplate template = getReportTemplate();

		String contractPO = (String) template.get(SantAuditReportTemplate.PO);
		String contractCpty = (String) template.get(SantAuditReportTemplate.CPTY);

		LegalEntity po = BOCache.getLegalEntity(getDSConnection(), contractPO);
		LegalEntity le = BOCache.getLegalEntity(getDSConnection(), contractCpty);

		Collection<CollateralConfig> marginCallConfigs = null;

		int poId = 0;
		int leId = 0;

		if (po != null) {
			poId = po.getId();
		}
		if (le != null) {
			leId = le.getId();
		}
		try {
			final CollateralServiceRegistry srvReg = ServiceRegistry.getDefault();
			marginCallConfigs = srvReg.getCollateralDataServer().getAllMarginCallConfig(poId, leId);
			// GSM: Not working fine since new Core 1.5.6
			// @SuppressWarnings("deprecation")
			// marginCallConfigs = DSConnection.getDefault().getRemoteReferenceData().getAllMarginCallConfig(poId,
			// leId);

		} catch (Exception e) {
			Log.error(this, "Cannot retrieve Margin Call Contracts", e);
		}

		String contractType = (String) template.get(SantAuditReportTemplate.CONTRACT_TYPE);
		String baseCcy = (String) template.get(SantAuditReportTemplate.BASE_CCY);
		String headClone = (String) template.get(SantAuditReportTemplate.HEAD_CLONE);
		String hedgeFund = (String) template.get(SantAuditReportTemplate.HEDGE_FUND);
		String instrumentType = (String) template.get(SantAuditReportTemplate.INSTRUMENT_TYPE);
		String poCollatType = (String) template.get(SantAuditReportTemplate.PO_COLLAT_TYPE);
		String cptyCollatType = (String) template.get(SantAuditReportTemplate.CPTY_COLLAT_TYPE);

		this.mccIdVsCptyId = new HashMap<Integer, Integer>();
		this.uniqueCptyIds = new HashSet<Integer>();
		this.finalContractsMap = new HashMap<Integer, CollateralConfig>();

		for (CollateralConfig mcc : marginCallConfigs) {
			if (!contractType.equals("ALL") && !contractType.equals(mcc.getContractType())) {
				continue;
			}
			if (!baseCcy.equals("ALL") && !baseCcy.equals(mcc.getCurrency())) {
				continue;
			}
			String s = mcc.getAdditionalField("HEAD_CLONE");
			if (!headClone.equals("ALL") && !Util.isEmpty(s) && !headClone.equals(s)) {
				continue;
			}
			s = mcc.getAdditionalField("HEDGE_FUNDS_REPORT");
			if (!hedgeFund.equals("ALL") && !Util.isEmpty(s) && !hedgeFund.equals(s)) {
				continue;
			}
			if (!instrumentType.equals("ALL") && !mcc.getProductList().contains(instrumentType)) {
				continue;
			}
			if (!poCollatType.equals("ALL") && !mcc.getPoCollType().equals(poCollatType)) {
				continue;
			}
			if (!cptyCollatType.equals("ALL") && !mcc.getLeCollType().equals(cptyCollatType)) {
				continue;
			}

			this.finalContractsMap.put(mcc.getId(), mcc);
			this.mccIdVsCptyId.put(mcc.getId(), mcc.getLeId());
			this.uniqueCptyIds.add(mcc.getLeId());
		}

	}

	private void buildSubLists() {
		// Creation of the subList with empty default values
		this.mccIdsSubList = new ArrayList<List<Integer>>();
		this.cptyIdsSubList = new ArrayList<List<Integer>>();

		if (this.finalContractsMap.size() == 0) {
			return;
		}

		final int SQL_IN_ITEM_COUNT = 999;

		// ContractIds subList
		List<Integer> tempList = new ArrayList<Integer>(this.mccIdVsCptyId.keySet());

		int start = 0;
		for (int i = 0; i <= (tempList.size() / SQL_IN_ITEM_COUNT); i++) {
			int end = (i + 1) * SQL_IN_ITEM_COUNT;
			if (end > tempList.size()) {
				end = tempList.size();
			}
			this.mccIdsSubList.add(tempList.subList(start, end));
			start = end;
		}

		// CounterpartyIds subList
		tempList = new ArrayList<Integer>(this.uniqueCptyIds);

		start = 0;
		for (int i = 0; i <= (tempList.size() / SQL_IN_ITEM_COUNT); i++) {
			int end = (i + 1) * SQL_IN_ITEM_COUNT;
			if (end > tempList.size()) {
				end = tempList.size();
			}
			this.cptyIdsSubList.add(tempList.subList(start, end));
			start = end;
		}

	}

	private JDatetime getStartTime(JDate jdate) {
		return new JDatetime(jdate, 0, 0, 0, TimeZone.getDefault());
	}

	private JDatetime getEndTime(JDate jdate) {
		return new JDatetime(jdate, 23, 59, 59, TimeZone.getDefault());
	}

	private SQLQuery buildMarginCallConfigSQL() {
		SQLQuery query = new SQLQuery();
		//JRL 12/04/2016 Migration 14.4
		query.appendWhereClause("entity_class_name IN ('CollateralConfig', 'MarginCallConfig')");
		// query.appendWhereClause("TRUNC(modif_date) >= " + Util.date2SQLString(this.fromDate));
		// query.appendWhereClause("TRUNC(modif_date) <= " + Util.date2SQLString(this.toDate));
		query.appendWhereClause("modif_date>=" + Util.datetime2SQLString(getStartTime(this.fromDate)));
		query.appendWhereClause("modif_date<=" + Util.datetime2SQLString(getEndTime(this.toDate)));

		StringBuilder whereClause = new StringBuilder();

		for (int i = 0; i < this.mccIdsSubList.size(); i++) {
			if (i > 0) {
				whereClause.append(" OR ");
			}
			whereClause.append(" entity_id IN (");
			whereClause.append(Util.collectionToString(this.mccIdsSubList.get(i)));
			whereClause.append(")");
		}
		query.appendWhereClause(whereClause.toString());

		return query;
	}

	private SQLQuery buildCreditRatingSQL() {
		SQLQuery query = new SQLQuery();
		query.appendWhereClause("entity_class_name = 'CreditRating'");
		// query.appendWhereClause("TRUNC(modif_date) >= " + Util.date2SQLString(this.fromDate));
		// query.appendWhereClause("TRUNC(modif_date) <= " + Util.date2SQLString(this.toDate));
		query.appendWhereClause("modif_date>=" + Util.datetime2SQLString(getStartTime(this.fromDate)));
		query.appendWhereClause("modif_date<=" + Util.datetime2SQLString(getEndTime(this.toDate)));

		StringBuilder whereClause = new StringBuilder();
		for (int i = 0; i < this.cptyIdsSubList.size(); i++) {
			if (i > 0) {
				whereClause.append(" OR ");
			}
			whereClause.append(" related_id IN (");
			whereClause.append(Util.collectionToString(this.cptyIdsSubList.get(i)));
			whereClause.append(")");
		}
		query.appendWhereClause(whereClause.toString());

		return query;
	}

	private SQLQuery buildLEContactSQL() {
		SQLQuery query = new SQLQuery();
		query.appendWhereClause("entity_class_name = 'LEContact'");
		// query.appendWhereClause("TRUNC(modif_date) >= " + Util.date2SQLString(this.fromDate));
		// query.appendWhereClause("TRUNC(modif_date) <= " + Util.date2SQLString(this.toDate));
		query.appendWhereClause("modif_date>=" + Util.datetime2SQLString(getStartTime(this.fromDate)));
		query.appendWhereClause("modif_date<=" + Util.datetime2SQLString(getEndTime(this.toDate)));

		StringBuilder whereClause = new StringBuilder();
		for (int i = 0; i < this.cptyIdsSubList.size(); i++) {
			if (i > 0) {
				whereClause.append(" OR ");
			}
			whereClause.append(" related_id IN (");
			whereClause.append(Util.collectionToString(this.cptyIdsSubList.get(i)));
			whereClause.append(")");
		}
		query.appendWhereClause(whereClause.toString());

		return query;
	}

	@Override
	public boolean getAllowPricingEnv() {
		return false;
	}

	enum DataChangeType {
		AGREEMENT("Change In Agreement Data"), RATING("Change In Risk Data"), CONTACT("Change In Contact Data");

		private final String description;

		private DataChangeType(String description) {
			this.description = description;
		}

		public String getDescrption() {
			return this.description;
		}

	}

	class SantAuditValue {

		private final AuditValue auditValue;

		private final String agreementName;

		private final DataChangeType typeOfChange;

		public SantAuditValue(AuditValue auditValue, DataChangeType dataChangeType, String agreementName) {
			this.auditValue = auditValue;
			this.typeOfChange = dataChangeType;
			this.agreementName = agreementName;
		}

		public String getAgreementName() {
			return this.agreementName;
		}

		public String getTypeOfChange() {
			return this.typeOfChange.getDescrption();
		}

		public String getParameter() {
			switch (this.typeOfChange) {
			case RATING:
				return SantAuditReport.this.auditPropsLoader.getRiskProperty(this.auditValue);
			case AGREEMENT:
				return SantAuditReport.this.auditPropsLoader.getAgreementProperty(this.auditValue);
			case CONTACT:
				return SantAuditReport.this.auditPropsLoader.getContactProperty(this.auditValue);
			}
			return this.auditValue.getFieldName();
		}

		public Object getOldValue() {
			if (this.auditValue.getFieldName().equals("_leId") || this.auditValue.getFieldName().equals("_poId")) {
				try {
					int leId = Integer.parseInt(this.auditValue.getField().getOldValue());
					return BOCache.getLEFromCache(leId).getName();
				} catch (Exception e) {
					Log.error(this, e); //sonar
					return null;
				}
			}
			return this.auditValue.getDisplayValue(this.auditValue.getField().getOldValue());
		}

		public Object getNewValue() {
			if (this.auditValue.getFieldName().equals("_leId") || this.auditValue.getFieldName().equals("_poId")) {
				try {
					int leId = Integer.parseInt(this.auditValue.getField().getNewValue());
					return BOCache.getLEFromCache(leId).getName();
				} catch (Exception e) {
					Log.error(this, e); //sonar
					return null;
				}
			}
			return this.auditValue.getDisplayValue(this.auditValue.getField().getNewValue());
		}

		public JDatetime getModifiedDate() {
			return this.auditValue.getModifDate();
		}

		public String getUserName() {
			String userName = this.auditValue.getUserName();
			if (Util.isEmpty(userName)) {
				return null;
			}
			User user = AccessUtil.getUser(userName);
			if (user != null) {
				return userName + " - " + user.getFullName();
			}
			return userName;
		}

		@Override
		public String toString() {
			return getParameter();
		}
	}
}
