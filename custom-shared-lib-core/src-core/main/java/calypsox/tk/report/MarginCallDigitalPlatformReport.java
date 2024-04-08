package calypsox.tk.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.collateral.MarginCallEntry;
import com.calypso.tk.collateral.sql.impl.CollateralSQLHelper;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.report.DefaultReportOutput;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportTemplate;
import com.calypso.tk.service.DSConnection;

import calypsox.tk.report.restservices.WebServiceReport;
import calypsox.util.CheckRowsNumberReport;
import calypsox.util.SantReportingUtil;
import calypsox.util.binding.CustomBindVariablesUtil;

public class MarginCallDigitalPlatformReport extends MarginCallEntryReport
		implements CheckRowsNumberReport, WebServiceReport {

	private static final long serialVersionUID = 6418918275421682056L;
	private static final String CPTY_NAME = "LEGAL_ENTITY_IDS";
	private static final String PO_NAME = "PROCESSING_ORG_IDS";
	private static final String BANK_NAME = "BANK_NAME";

	private static String injectionQuery = null;
	private static boolean _countOnly = false;

	@Override
	public ReportOutput loadFromWS(String query, Vector<String> errorMsgs) {
		injectionQuery = query;
		ReportOutput output = super.load(errorMsgs);
		injectionQuery = null;
		return output;
	}

	@Override
	protected String buildQuery(ReportTemplate template) {
		String where = "";
		setLEIdsByName(CPTY_NAME);
		setLEIdsByName(PO_NAME);
		setLEIdsByLongName(BANK_NAME, PO_NAME);
		int maxEntryperUser = CollateralSQLHelper.getMaxEntryPerUser();
		if (Util.isEmpty(injectionQuery) || maxEntryperUser <= 0 || _countOnly) {
			where = super.buildQuery(template);
			where = getAdditionalFilters(where, template).toString();
			if (!Util.isEmpty(injectionQuery) && !_countOnly) {
				where += injectionQuery;
			}
		}
		return where;
	}

	private StringBuilder getAdditionalFilters(String whereInp, ReportTemplate template) {
		StringBuilder where = new StringBuilder(whereInp);
		String s = (String) template.get("CURRENCY");
		if (!Util.isEmpty(s)) {
			List<String> list1 = new ArrayList<>();

			List<String> currencies = (List<String>) Util.stringToCollection(list1, s, ",", false);

			if (!Util.isEmpty(currencies)) {
				if (where.length() > 0)
					where.append(" AND ");

				where.append(" margin_call_entries.contract_currency ");
				if (currencies.size() == 1) {
					where.append('=');
					where.append('\'');
					where.append(currencies.get(0));
					where.append('\'');
				} else {
					where.append(" IN ");
					where.append(Util.collectionToSQLString(currencies));

				}
			}
		}
		s = (String) template.get("ACADIA");
		if (!Util.isEmpty(s) && !"ALL".equals(s)) {
			if (where.length() > 0) {
				where.append(" AND ");
			}
			if (!whereInp.contains("collateral_config.")) {
				where.append("(collateral_config");
				where.append(".mcc_id = ");
				where.append("margin_call_entries");
				where.append(".mcc_id or  ");
				where.append("collateral_config");
				where.append(".mcc_id =  ");
				where.append("margin_call_entries");
				where.append(".master_config_id) AND");
			}
			where.append("( collateral_config.wf_subtype ");
			if ("ACADIA".equals(s)) {
				where.append("= \'ACADIA\') ");
			} else {
				where.append("<> \'ACADIA\' OR collateral_config.wf_subtype is  null) ");
			}
		}
		s = (String) template.get("MC_ID");
		if (!Util.isEmpty(s)) {
			if (where.length() > 0)
				where.append(" AND ");
			where.append(" margin_call_entries.ID = ");
			where.append(s);
		}
		if (!_countOnly && !Util.isEmpty(injectionQuery)) {
			if (injectionQuery.contains("processingOrg.") || injectionQuery.contains("legalEntity.")
					|| injectionQuery.contains("mrgcall_config.")) {
				if (!whereInp.contains("mrgcall_config.")) {
					if (where.length() > 0) {
						where.append(" AND ");
					}
					where.append(" (");
					where.append("mrgcall_config");
					where.append(".mrg_call_def = ");
					where.append("margin_call_entries");
					where.append(".mcc_id  OR  ");
					where.append("mrgcall_config");
					where.append(".mrg_call_def =  ");
					where.append("margin_call_entries");
					where.append(".master_config_id)");
				}
			}
			if (injectionQuery.contains("processingOrg.")) {
				if (where.length() > 0) {
					where.append(" AND ");
				}
				where.append(" mrgcall_config.process_org_id=processingOrg.legal_entity_id ");
			}
			if (injectionQuery.contains("legalEntity.")) {
				if (where.length() > 0) {
					where.append(" AND ");
				}
			

				where.append(" mrgcall_config.legal_entity_id=legalEntity.legal_entity_id ");
			}
			
			if (injectionQuery.contains("collateral_config.") && !where.toString().contains("collateral_config.")) {
				if (where.length() > 0) {
					where.append(" AND ");
				}
			

				where.append(" margin_call_entries.mcc_id=collateral_config.mcc_id ");
			}
		}
		return where;
	}

	@Override
	public Map getPotentialSize() {
		_countOnly = true;
		Map res = super.getPotentialSize();
		_countOnly = false;
		return res;
	}

	protected List<String> getFrom(String where) {
		List<String> result = new ArrayList<>();
		if (!Util.isEmpty(where)) {
			if (where.contains("margin_call_entries."))
				result.add("margin_call_entries");
			if (where.contains("margin_call_detail_entries."))
				result.add("margin_call_detail_entries");
			if (where.contains("mrgcall_config."))
				result.add("mrgcall_config");
			if (where.contains("collateral_config."))
				result.add("collateral_config");
			if (where.contains("clearing_member_configuration."))
				result.add("clearing_member_configuration");
			if (where.contains("clearing_member_service."))
				result.add("clearing_member_service");
			if (where.contains("clearing_service."))
				result.add("clearing_service");
			if (where.contains("processingOrg.")) {
				result.add("legal_entity processingOrg");
			}
			if (where.contains("legalEntity.")) {
				result.add("legal_entity legalEntity");
			}
		}
		return result;
	}

	protected List<String> buildFrom(String where) {
		List<String> result = new ArrayList<>();
		int maxEntryperUser = CollateralSQLHelper.getMaxEntryPerUser();

		String whereSup = super.buildQuery(this.getReportTemplate());
		whereSup = getAdditionalFilters(whereSup, this.getReportTemplate()).toString();
		if (!_countOnly && !Util.isEmpty(injectionQuery) && maxEntryperUser > 0 && whereSup.length() > 0) {
			StringBuilder subQueryW = new StringBuilder();
			whereSup += injectionQuery;
			List<String> from = getFrom(whereSup);
			subQueryW.append(" (SELECT mceT.* from ");
			if (!Util.isEmpty(from)) {
				String str = Util.collectionToString(from);
				str = str.replaceAll("margin_call_entries", "margin_call_entries mceT");
				str = str.replaceAll("margin_call_detail_entries", "margin_call_detail_entries mcdeT");
				str = str.replaceAll("mrgcall_config", "mrgcall_config mccT");
				str = str.replaceAll("collateral_config", "collateral_config ccT");
				str = str.replaceAll("clearing_member_configuration", "clearing_member_configuration cmcT");
				str = str.replaceAll("clearing_member_service", "clearing_member_service cmsT");
				str = str.replaceAll("clearing_service", "clearing_service msT");
				subQueryW.append(str);
			} else {
				subQueryW.append(" margin_call_entries mceT");
			}
			subQueryW.append(" ");
			whereSup = whereSup.replaceAll("margin_call_entries[.]", "mceT.");
			whereSup = whereSup.replaceAll("margin_call_detail_entries[.]", "mcdeT.");
			whereSup = whereSup.replaceAll("mrgcall_config[.]", "mccT.");
			whereSup = whereSup.replaceAll("collateral_config[.]", "ccT.");
			whereSup = whereSup.replaceAll("clearing_member_configuration[.]", "cmcT.");
			whereSup = whereSup.replaceAll("clearing_member_service[.]", "cmsT.");
			whereSup = whereSup.replaceAll("clearing_service[.]", "msT.");

			whereSup = whereSup.replaceAll("margin_call_entries", "margin_call_entries mceT");
			whereSup = whereSup.replaceAll("margin_call_detail_entries", "margin_call_detail_entries mcdeT");
			whereSup = whereSup.replaceAll("mrgcall_config", "mrgcall_config mccT");
			whereSup = whereSup.replaceAll("collateral_config", "collateral_config ccT");
			whereSup = whereSup.replaceAll("clearing_member_configuration", "clearing_member_configuration cmcT");
			whereSup = whereSup.replaceAll("clearing_member_service", "clearing_member_service cmsT");
			whereSup = whereSup.replaceAll("clearing_service", "clearing_service msT");

			subQueryW.append("where ").append(whereSup).append(") margin_call_entries ");
			result.clear();
			result.add(subQueryW.toString());
		} else {
			result = getFrom(where);
		}
		return result;
	}

	@Override
	public ReportOutput load(Vector errorMsgs) {
		if (_countOnly) {
			StringBuilder result = new StringBuilder(64);
			String where = buildQuery(this._reportTemplate);
			List<String> from = buildFrom(where);
			result.append("select count(*) from ");
			if (Util.isEmpty(from)) {
				result.append(" margin_call_entries ");
			} else {
				result.append(Util.collectionToString(from));
			}
			if (!Util.isEmpty(where)) {
				result.append(" WHERE ");
				result.append(where);
			}
			int countVal = 0;
			Vector v = null;
			try {
				v = getDSConnection().getRemoteAccess().executeSelectSQL(result.toString(), null);
				countVal += ((Double) ((Vector) v.elementAt(2)).elementAt(0)).intValue();
			} catch (Exception e) {
				Log.error(this,e);
			}

			Boolean includeArchived = (Boolean) this._reportTemplate.get("ARCHIVED_ENTRIES");
			if (includeArchived) {
				result = new StringBuilder(64);
				result.append("select count(*) from ");
				if (Util.isEmpty(from)) {
					result.append(" mrgcall_entries_hist margin_call_entries ");
				} else {
					result.append(Util.collectionToString(from));
				}
				if (!Util.isEmpty(where)) {
					result.append(" WHERE ");
					result.append(where);
				}
				try {
					v = getDSConnection().getRemoteAccess().executeSelectSQL(result.toString(), null);
					countVal += ((Double) ((Vector) v.elementAt(2)).elementAt(0)).intValue();
				} catch (CalypsoServiceException e) {
					Log.error(this,e);
				}
			}

			addPotentialSize(MarginCallEntry.class.getName(), countVal);
			return new DefaultReportOutput(this);
		} else {
			DefaultReportOutput output = (DefaultReportOutput) super.load(errorMsgs);

			// Generate a task is the report size is out of a defined umbral
			HashMap<String, String> value = SantReportingUtil.getSchedTaskNameOrReportTemplate(this);
			if (!value.isEmpty() && value.keySet().iterator().next().equals("ScheduledTask: ")) {
				checkAndGenerateTaskReport(output, value);
			}

			return output;
		}
	}

	private void setLEIdsByName(String name) {
		Object obj = this.getReportTemplate().get(name);
		if (obj instanceof String) {
			String cptyNames = obj.toString();
			if (!Util.isEmpty(cptyNames)) {
				List<String> cptyList = Util.stringToList(cptyNames);
				Vector<Integer> lstId = new Vector<Integer>();
				for (String s : cptyList) {
					LegalEntity le = BOCache.getLegalEntity(getDSConnection(), s);
					if (le != null) {
						lstId.add(le.getId());
					} else if (Util.isNumber(s)) {
						lstId.add(Integer.valueOf(s));
					}
				}
				if(lstId.isEmpty()) {
					lstId.add(-1);
					this.getReportTemplate().put(name, lstId);
				} else {
					this.getReportTemplate().put(name, lstId);
				}
			}
		}
	}
	
	private void setLEIdsByLongName(String name, String filter) {
		Object obj = this.getReportTemplate().get(name);
		if (obj instanceof String) {
			String cptyName = obj.toString();
			if (!Util.isEmpty(cptyName)) {
				
				Vector<Integer> lstId = new Vector<Integer>();
				String where = "LONG_NAME = ? ";
				
				List<CalypsoBindVariable> bindVariables = CustomBindVariablesUtil.createNewBindVariable(cptyName);
				try {
					Vector<LegalEntity> le = DSConnection.getDefault().getRemoteReferenceData().getAllLE(where, bindVariables);
					for (LegalEntity entity: le) {
						lstId.add(entity.getEntityId());
					}
				} catch (CalypsoServiceException e) {
					Log.error(this, "Error getting Legal Entity by full name.", e);;
				}
				if(lstId.isEmpty()) {
					lstId.add(-1);
					this.getReportTemplate().put(filter, lstId);
				} else {
					this.getReportTemplate().put(filter, lstId);
				}
			}
		}
	}
}