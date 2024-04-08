package calypsox.tk.report;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import calypsox.util.collateral.CollateralUtilities;
import calypsox.util.SantReportingUtil;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.CollateralConfig;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

public class SantHaircutByIssuerReport extends SantReport {

	private static final long serialVersionUID = 123L;

	// cache for agreements
	private final HashMap<Integer, CollateralConfig> agreementsMap = new HashMap<Integer, CollateralConfig>();
	// cache for issuers
	private final HashMap<String, LegalEntity> issuersMap = new HashMap<String, LegalEntity>();

	@Override
	protected boolean checkProcessEndDate() {
		return false;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ReportOutput loadReport(final Vector errorMsgsP) {

		try {

			return getReportOutput();

		} catch (RemoteException e) {
			String error = "Error generating Haircut By Issuer Report\n";
			Log.error(this, error, e);
			errorMsgsP.add(error + e.getMessage());
		}
		return null;

	}

	private DefaultReportOutput getReportOutput() throws RemoteException {

		final DefaultReportOutput output = new StandardReportOutput(this);
		final ArrayList<ReportRow> reportRows = new ArrayList<ReportRow>();

		final ReportTemplate template = getReportTemplate();

		// get list of agreements
		String agreementIds = (String) template.get(SantHaircutByIssuerReportTemplate.AGREEMENT_ID);

		// get list of issuers
		final String issuerIds = (String) template.get(SantHaircutByIssuerReportTemplate.ISSUER_ID);

		// load haircut items
		List<SantHaircutByIssuerItem> haicutItems = new ArrayList<SantHaircutByIssuerItem>();
		haicutItems = loadSantHaircutByIssuerItems(agreementIds, issuerIds);

		for (SantHaircutByIssuerItem haircutItem : haicutItems) {
			CollateralConfig agreement = getContract(haircutItem.getAgreement());
			LegalEntity issuer = getIssuer(haircutItem.getIssuer());
			ReportRow row = new ReportRow(agreement, ReportRow.MARGIN_CALL_CONFIG);
			row.setProperty(ReportRow.LEGAL_ENTITY, issuer);
			row.setProperty(SantHaircutByIssuerItem.SANT_HAIRCUT_BY_ISSUER_ITEM, haircutItem);
			reportRows.add(row);
		}

		// set report rows on output
		output.setRows(reportRows.toArray(new ReportRow[reportRows.size()]));
		return output;

	}

	// --- METHODS --- //

	@Override
	protected JDate getValDate() {
		JDate valDate = getProcessStartDate();
		if (valDate == null) {
			return JDate.getNow();
		}
		return valDate;

	}

	/* get contract from contract id */
	private CollateralConfig getContract(int id) {

		CollateralConfig agreement = new CollateralConfig();

		if (this.agreementsMap.containsKey(id)) {
			agreement = this.agreementsMap.get(id);
		} else {
			try {
				agreement = ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfig(id);
				this.agreementsMap.put(id, agreement);
			} catch (RemoteException e) {
				Log.error(this, "Error getting contract " + id, e);
			}
		}

		return agreement;

	}

	/* get issuer le from issuer name */
	private LegalEntity getIssuer(String name) {

		LegalEntity issuer = new LegalEntity();

		if (this.issuersMap.containsKey(name)) {
			issuer = this.issuersMap.get(name);
		} else {
			issuer = BOCache.getLegalEntity(getDSConnection(), name);
			this.issuersMap.put(name, issuer);
		}

		return issuer;

	}

	/* load SantHaircutByIssuer items */
	private List<SantHaircutByIssuerItem> loadSantHaircutByIssuerItems(String agreementIds, String issuerIds) {

		String query = new String();
		List<SantHaircutByIssuerItem> haicutItems = new ArrayList<SantHaircutByIssuerItem>();

		// get query
		query = getQuery(agreementIds, issuerIds);

		// get items
		try {
			haicutItems = SantReportingUtil.getSantReportingService(DSConnection.getDefault())
					.getSantHaircutByIssuerItems(query);
		} catch (RemoteException e) {
			Log.error(this, "Error getting haircut items from DB", e);
		}

		return haicutItems;
	}

	/* get query for load agreements */
	private String getQuery(String agreementIds, String issuerIds) {

		String query = new String();

		// non filter
		if (Util.isEmpty(agreementIds) && Util.isEmpty(issuerIds)) {
			query = getNonFilterQuery();
		}
		// filter by agreement
		else if (!Util.isEmpty(agreementIds) && Util.isEmpty(issuerIds)) {
			query = getFilterByAgreementQuery(agreementIds);
		}
		// filter by issuer
		else if (Util.isEmpty(agreementIds) && !Util.isEmpty(issuerIds)) {
			query = getFilterByIssuerQuery(issuerIds);
		}
		// filter by both
		else {
			query = getFilterByBothQuery(agreementIds, issuerIds);
		}

		// 20/08/15. SBNA Multi-PO filter
		final Set<String> posIdsAllowed = new HashSet<String>(Util.string2Vector(CollateralUtilities
				.filterPoIdsByTemplate(getReportTemplate())));

		if (!Util.isEmpty(posIdsAllowed)) {
			query += " AND mrgcall_config.PROCESS_ORG_ID IN " + Util.collectionToSQLString(posIdsAllowed);
		}

		return query;

	}

	/* get query for get non-filtered info */
	private String getNonFilterQuery() {

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		select.append("select mrg_call_def, sd_filter_domain.domain_value, tenor, value ");
		from.append("from mrgcall_config, haircut, sd_filter, sd_filter_domain ");
		where.append("where mrgcall_config.haircut_name = haircut.name ");
		where.append("AND haircut.sec_sd_filter = sd_filter.sd_filter_name ");
		where.append("AND sd_filter.sd_filter_name = sd_filter_domain.sd_filter_name ");
		where.append("AND sd_filter_domain.element_name = 'Security Issuer' ");

		return select.toString() + from.toString() + where.toString();

	}

	/* get query for get info filtered by agreement */
	private String getFilterByAgreementQuery(String agreementIds) {

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		select.append("select mrg_call_def, sd_filter_domain.domain_value, tenor, value ");
		from.append("from mrgcall_config, haircut, sd_filter, sd_filter_domain ");
		where.append("where mrg_call_def IN ");
		where.append(Util.collectionToSQLString(Util.string2IntVector(agreementIds)));
		where.append(" AND mrgcall_config.haircut_name = haircut.name ");
		where.append("AND haircut.sec_sd_filter = sd_filter.sd_filter_name ");
		where.append("AND sd_filter.sd_filter_name = sd_filter_domain.sd_filter_name ");
		where.append("AND sd_filter_domain.element_name = 'Security Issuer' ");

		return select.toString() + from.toString() + where.toString();

	}

	/* get query for get info filtered by issuer */
	private String getFilterByIssuerQuery(String issuerIds) {

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		select.append("select mrg_call_def, sd_filter_domain.domain_value, tenor, value ");
		from.append("from mrgcall_config, haircut, sd_filter, sd_filter_domain ");
		where.append("where mrgcall_config.haircut_name = haircut.name ");
		where.append("AND haircut.sec_sd_filter = sd_filter.sd_filter_name ");
		where.append("AND sd_filter.sd_filter_name = sd_filter_domain.sd_filter_name ");
		where.append("AND sd_filter_domain.element_name = 'Security Issuer' ");
		where.append("AND sd_filter_domain.domain_value IN ");
		where.append(Util.collectionToSQLString(Util.string2Vector(issuerIds)));

		return select.toString() + from.toString() + where.toString();

	}

	/* get query for get info filtered by agreement and issuer */
	private String getFilterByBothQuery(String agreementIds, String issuerIds) {

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		select.append("select mrg_call_def, sd_filter_domain.domain_value, tenor, value ");
		from.append("from mrgcall_config, haircut, sd_filter, sd_filter_domain ");
		where.append("where mrg_call_def IN ");
		where.append(Util.collectionToSQLString(Util.string2IntVector(agreementIds)));
		where.append(" AND mrgcall_config.haircut_name = haircut.name ");
		where.append("AND haircut.sec_sd_filter = sd_filter.sd_filter_name ");
		where.append("AND sd_filter.sd_filter_name = sd_filter_domain.sd_filter_name ");
		where.append("AND sd_filter_domain.element_name = 'Security Issuer' ");
		where.append("AND sd_filter_domain.domain_value IN ");
		where.append(Util.collectionToSQLString(Util.string2Vector(issuerIds)));

		return select.toString() + from.toString() + where.toString();

	}

}
