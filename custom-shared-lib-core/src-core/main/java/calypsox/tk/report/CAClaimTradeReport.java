package calypsox.tk.report;

import java.util.List;
import java.util.Vector;

import org.jfree.util.Log;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Util;
import com.calypso.tk.mo.TradeFilter;
import com.calypso.tk.report.ReportOutput;
import com.calypso.tk.report.ReportTemplate;

import calypsox.tk.report.restservices.WebServiceReport;
import calypsox.util.binding.CustomBindVariablesUtil;

public class CAClaimTradeReport extends com.calypso.tk.report.TradeReport implements WebServiceReport {
	private static final String CPTY_NAME = "CptyName";
	static final long serialVersionUID = -2417009574002215610L;
	private static String injectionQuery = null;
	private static final String STRING_SEP = ",";

	@SuppressWarnings("rawtypes")
	@Override
	public ReportOutput load(Vector errorMsgs) {
//		injectionQuery = " ORDER BY product_sec_code.ISIN ASC OFFSET 0 ROWS FETCH NEXT 2000 ROWS ONLY ";
		setCptyIdsByName();
		if (!this._countOnly && !Util.isEmpty(injectionQuery)) {
			String filterName = this._reportTemplate.<String>get("TRADE_FILTER");
			if (Util.isEmpty(filterName)) {
				filterName = this._reportTemplate.<String>get("TRADE_FILTER2");
			} else {
				this._reportTemplate.put("TRADE_FILTER2", filterName);
				this._reportTemplate.remove("TRADE_FILTER");
				this._tradeFilterSetName = null;
			}
		}
		ReportOutput out = super.load(errorMsgs);
		injectionQuery = null;
		String filterName = this._reportTemplate.<String>get("TRADE_FILTER2");
		if (!Util.isEmpty(filterName)) {
			this._reportTemplate.put("TRADE_FILTER", filterName);
		}
		return out;
	}

	@Override
	public ReportOutput loadFromWS(String query, Vector<String> errorMsgs) {
		injectionQuery = query;
		return load(errorMsgs);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String buildQuery(boolean buildQueryForRepoOrSecLending, List<CalypsoBindVariable> bindVariables2) {
		List<CalypsoBindVariable> bindVariables = bindVariables2;
		if (!this._countOnly && !Util.isEmpty(injectionQuery)) {
			String filterName = this._reportTemplate.<String>get("TRADE_FILTER");
			if (Util.isEmpty(filterName)) {
				filterName = this._reportTemplate.<String>get("TRADE_FILTER2");
			} else {
				this._reportTemplate.put("TRADE_FILTER2", filterName);
				this._reportTemplate.remove("TRADE_FILTER");
				this._tradeFilterSetName = null;
			}
			String tfActualWhere = "";
			if (!Util.isEmpty(filterName) && !"ALL".equals(filterName)) {
				TradeFilter tfActual = BOCache.getTradeFilter(getDSConnection(), filterName);
				if (tfActual == null) {
					Log.error("Could not load Trade Filter: " + filterName);
				}
				JDatetime undoDatetime = getUndoDatetime();
				try {
					tfActual = (TradeFilter) tfActual.clone();
				} catch (CloneNotSupportedException e) {
					Log.error(e);
				}
				tfActual.setValDate(this._valuationDateTime);
				List<CalypsoBindVariable> bindVar = null;
				try {

					Object[] tfActualObj = getDSConnection().getRemoteReferenceData().generateWhereClause(tfActual,
							undoDatetime, bindVariables);
					tfActualWhere = (String) tfActualObj[0];
					bindVar = (List<CalypsoBindVariable>) tfActualObj[1];
				} catch (CalypsoServiceException e) {
					Log.error(e);
				}

				if (!Util.isEmpty(bindVar)) {
					bindVariables.addAll(bindVar);
				}
			}
			String where = "";
			if (injectionQuery.contains("product_ca.") || injectionQuery.contains("product_sec_code.")
					|| injectionQuery.contains("underlying_product_desc.")) {
				where += " trade.product_id=product_ca.product_id ";
			}
			String sec_cod = "";
			if (injectionQuery.contains("product_sec_code.")) {
				if (!Util.isEmpty(where)) {
					where += " AND ";
				}
				where += " product_ca.underlying_id=product_sec_code.product_id AND product_sec_code.sec_code=? ";
				int ini = injectionQuery.indexOf("product_sec_code.");
				int end = injectionQuery.indexOf(" ", ini);
				if (end <= 0) {
					end = injectionQuery.indexOf(",", ini);
				}
				String str = injectionQuery.substring(ini, end);
				String str2 = str.replaceAll("product_sec_code.", "");
				if (Util.isEmpty(bindVariables)) {
					bindVariables = CustomBindVariablesUtil.createNewBindVariable(str2);
				} else {
				CustomBindVariablesUtil.addNewBindVariableToList(str2, bindVariables);
				}
				sec_cod = injectionQuery.replaceAll(str, "product_sec_code.code_value");
			} else {
				sec_cod = injectionQuery;
			}
			if (sec_cod.contains("trade_keyword.")) {
				int ini = sec_cod.indexOf("trade_keyword.");
				while (ini >= 0) {
					int end = sec_cod.indexOf(" ", ini);
					if (end <= 0) {
						end = sec_cod.indexOf(",", ini);
					}
					String str = sec_cod.substring(ini, end);
					String str2 = str.replaceAll("trade_keyword.", "");
					if (!Util.isEmpty(where)) {
						where += " AND ";
					}
					where += " trade.trade_id=" + str2 + ".trade_id (+) AND " + str2 + ".keyword_name(+)=? ";
					if (Util.isEmpty(bindVariables)) {
						bindVariables = CustomBindVariablesUtil.createNewBindVariable(str2);
					} else {
					CustomBindVariablesUtil.addNewBindVariableToList(str2, bindVariables);
					}
					sec_cod = sec_cod.replaceAll(str, str2 + ".keyword_value");
					ini = sec_cod.indexOf("trade_keyword.",
							ini - str.length() + (str2 + ".keyword_value").length());
				}
			}

			if (injectionQuery.contains("counterparty.")) {
				if (!Util.isEmpty(where)) {
					where += " AND ";
				}
				where += " trade.cpty_id=counterparty.legal_entity_id ";
			}
			if (injectionQuery.contains("book.") || injectionQuery.contains("processingOrg.")
					|| injectionQuery.contains("POAttribute.")) {
				if (!Util.isEmpty(where)) {
					where += " AND ";
				}
				where += " trade.book_id=book.book_id ";
			}
			if (injectionQuery.contains("processingOrg.") || where.contains("legal_entity.")) {
				if (!Util.isEmpty(where)) {
					where += " AND ";
				}
				where += " book.legal_entity_id=processingOrg.legal_entity_id ";
			}
			if (injectionQuery.contains("POAttribute.")) {
				if (!Util.isEmpty(where)) {
					where += " AND ";
				}
				where += " book.legal_entity_id=POAttribute.legal_entity_id(+) and POAttribute.attribute_type(+)=? ";
				int ini = injectionQuery.indexOf("POAttribute.");
				int end = injectionQuery.indexOf(" ", ini);
				if (end <= 0) {
					end = injectionQuery.indexOf(",", ini);
				}
				String str = injectionQuery.substring(ini, end);
				String str2 = str.replaceAll("POAttribute.", "");
				if (Util.isEmpty(bindVariables)) {
					bindVariables = CustomBindVariablesUtil.createNewBindVariable(str2);
				} else {
					CustomBindVariablesUtil.addNewBindVariableToList(str2, bindVariables);
				}
				sec_cod = sec_cod.replaceAll(str, "POAttribute.attribute_value");

			}
			if (injectionQuery.contains("underlying_product_desc.")) {
				if (!Util.isEmpty(where)) {
					where += " AND ";
				}
				where += " product_ca.underlying_id=underlying_product_desc.product_id ";
			}
			if (!Util.isEmpty(tfActualWhere)) {
				if (!Util.isEmpty(where)) {
					where += " AND ";
				}
				where += tfActualWhere;
			}
			bindVariables2.addAll(bindVariables);
			String strQuery = super.buildQuery(buildQueryForRepoOrSecLending, bindVariables2);
			if (!Util.isEmpty(strQuery) && !Util.isEmpty(where)) {
				strQuery = " AND " + strQuery;
			}
			strQuery.replaceAll("legal_entity.", "processingOrg.");
			return where + strQuery + sec_cod;
		}
		return super.buildQuery(buildQueryForRepoOrSecLending, bindVariables2);
	}

	@Override
	protected String buildFrom(String where) {
		if (!this._countOnly && !Util.isEmpty(injectionQuery)) {

			ReportTemplate h = this._reportTemplate;
			String from = "";
			String strProTyp = (String) h.get("ProductType");
			if (!Util.isEmpty(strProTyp)) {
				from = "product_desc";
			}
			if (where.contains("product_ca.")) {
				if (!Util.isEmpty(from)) {
					from = from.concat(",product_ca");
				} else {
					from = from.concat("product_ca");
				}
			}
			if (injectionQuery.contains("product_sec_code.")) {
				if (!Util.isEmpty(from)) {
					from = from.concat(",product_sec_code");
				} else {
					from = from.concat("product_sec_code");
				}
			}
			String result = super.buildFrom(where);
			if (injectionQuery.contains("counterparty.")) {
				if (!Util.isEmpty(from)) {
					from = from.concat(",legal_entity counterparty");
				} else {
					from = from.concat("legal_entity counterparty");
				}
			}
			result.replaceAll(",legal_entity", "");
			if ((injectionQuery.contains("book.") || injectionQuery.contains("processingOrg.")
					|| injectionQuery.contains("POAttribute.")) && !result.contains("book")) {
				if (!Util.isEmpty(from)) {
					from = from.concat(",book");
				} else {
					from = from.concat("book");
				}
			}
			if (injectionQuery.contains("processingOrg.")) {
				if (!Util.isEmpty(from)) {
					from = from.concat(",legal_entity processingOrg");
				} else {
					from = from.concat("legal_entity processingOrg");
				}
			}
			if (injectionQuery.contains("underlying_product_desc.")) {
				if (!Util.isEmpty(from)) {
					from = from.concat(",product_desc underlying_product_desc");
				} else {
					from = from.concat("product_desc underlying_product_desc");
				}
			}
			if (injectionQuery.contains("trade_keyword.")) {
				int ini = injectionQuery.indexOf("trade_keyword.");
				while (ini >= 0) {
					int end = injectionQuery.indexOf(" ", ini);
					if (end <= 0) {
						end = injectionQuery.indexOf(",", ini);
					}
					String str = injectionQuery.substring(ini, end);
					String str2 = str.replaceAll("trade_keyword.", "");
					if (!Util.isEmpty(from)) {
						from = from.concat(",trade_keyword " + str2);
					} else {
						from = from.concat("trade_keyword " + str2);
					}
					ini = injectionQuery.indexOf("trade_keyword.", ini + 1);
				}
			}
			if (injectionQuery.contains("POAttribute.")) {
				if (!Util.isEmpty(from)) {
					from = from.concat(",le_attribute POAttribute");
				} else {
					from = from.concat("le_attribute POAttribute");
				}
			}
			String filterName = this._reportTemplate.<String>get("TRADE_FILTER");
			if (Util.isEmpty(filterName)) {
				filterName = this._reportTemplate.<String>get("TRADE_FILTER2");
			} else {
				this._reportTemplate.put("TRADE_FILTER2", filterName);
				this._reportTemplate.remove("TRADE_FILTER");
				this._tradeFilterSetName = null;
			}
			if (!Util.isEmpty(filterName) && !"ALL".equals(filterName)) {
				TradeFilter tfActual = BOCache.getTradeFilter(getDSConnection(), filterName);
				if (tfActual == null) {
					Log.error("Could not load Trade Filter: " + filterName);
				}
				try {
					tfActual = (TradeFilter) tfActual.clone();
				} catch (CloneNotSupportedException e) {
					Log.error(e);
				}
				tfActual.setValDate(this._valuationDateTime);
				try {
					String tfActualFrom = getDSConnection().getRemoteReferenceData().generateFromClause(tfActual);
					if (!Util.isEmpty(tfActualFrom)) {
						from += from.concat(",").concat(tfActualFrom);
					}
				} catch (CalypsoServiceException e) {
					Log.error(e);
				}
			}

			if (result != null && !result.isEmpty()) {
				return result.concat(",").concat(from);
			} else {
				return from;
			}

		}
		return super.buildFrom(where);
	}

	private void setCptyIdsByName() {
		String cptyNames = this.getReportTemplate().get(CPTY_NAME);
		List<String> cptyList = Util.stringToList(cptyNames);
		StringBuilder lstIdAcc = new StringBuilder();
		for (String s : cptyList) {
			LegalEntity le = BOCache.getLegalEntity(getDSConnection(), s);
			if (lstIdAcc.length() > 0) {
				lstIdAcc.append(STRING_SEP);
			}
			if (le != null) {
				lstIdAcc.append(le.getId());
			} else if (Util.isNumber(s)) {
				lstIdAcc.append(s);
			}
		}
		if (lstIdAcc.length() > 0) {
			this.getReportTemplate().put(CPTY_NAME, lstIdAcc.toString());
		} else if (!Util.isEmpty(cptyNames)) {
			this.getReportTemplate().put(CPTY_NAME, "-1");
		}
	}
}
