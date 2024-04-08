package calypsox.tk.report;

import java.util.ArrayList;
import java.util.List;

import org.jfree.util.Log;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.CalypsoBindVariable;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.report.BOPositionReport;
import com.calypso.tk.report.transfer.TransferSelector;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;

import calypsox.util.binding.CustomBindVariablesUtil;

public class WSTransferReportSelector implements TransferSelector {
	private String where = null;
	private String from = null;
	private String orderBy = null;
	private boolean isArchived = false;
	private List<CalypsoBindVariable> bindVariables = null;

	@Override
	public void checkIsValidSelector() {
		if (where == null) {
			throw new IllegalArgumentException("builtQuery must be set.");
		}
	}

	public WSTransferReportSelector(String from, String where, String orderBy, boolean isArchived,
			List<CalypsoBindVariable> bindVariables) {
		super();
		this.from = from;
		this.where = where;
		this.orderBy = orderBy;
		this.isArchived = isArchived;
		this.bindVariables = bindVariables;
	}

	@Override
	public TransferArray getTransfers() {
		TransferArray transfers = null;
		try {
			from = Util.isEmpty(from) ? "" : from;

			addAttrJoinsToWhere();
			addProductCodeJoinsToWhere();
			addPOInfoAndAttrJoinsToWhere();
			addCptyInfoAndAttrJoinsToWhere();
			addAgentInfoAndAttrJoinsToWhere();
			addTradeKeywordJoinsToWhere();
			addTradeJoinsToWhere();
			addBookJoinsToWhere();
			addGLAccountJoinsToWhere();
			addProductCAJoinsToWhere();
			addProductMarginCallJoinsToWhere();
			addMarginCallContractJoinsToWhere();
			addPayerInfoAndAttrJoinsToWhere();
			addReceiverInfoAndAttrJoinsToWhere();
			addBalanceCallJoinsToWhere();
			orderBy = orderBy.replaceFirst(" ORDER BY ", "");
			transfers = DSConnection.getDefault().getRemoteBO().getBOTransfers(from, where, orderBy, 0, bindVariables);
		} catch (CalypsoServiceException e) {
			Log.error("Error getting Transfers for WS.", e);
		}
		return transfers;
	}

	private void addAttrJoinsToWhere() {
		String tableAttr = this.isArchived ? "xfer_attr_hist" : "xfer_attributes";
		String tableXfer = this.isArchived ? "bo_transfer_hist" : "bo_transfer";
		if (orderBy != null && orderBy.contains("xfer_attributes.")) {
			String join = "";
			List<CalypsoBindVariable> bindVariablesCopy = null;
			List<String> names = getAttrNames("xfer_attributes");
			for (String name : names) {
				join += join.length() > 0 ? " and " : "";
				join += " " + tableXfer + ".transfer_id=" + name.toLowerCase() + ".transfer_id (+) and "
						+ name.toLowerCase() + ".attr_name(+)=?";

				if (bindVariablesCopy == null) {
					bindVariablesCopy = CustomBindVariablesUtil.createNewBindVariable(name);
				} else {
					CustomBindVariablesUtil.addNewBindVariableToList(name, bindVariablesCopy);
				}
				from += (Util.isEmpty(from) ? "" : ",") + tableAttr + " " + name.toLowerCase();
				orderBy = orderBy.replaceFirst("xfer_attributes." + name, "xfer_attributes.attr_value");
				orderBy = orderBy.replaceFirst(tableAttr, name.toLowerCase());
			}
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
			bindVariablesCopy.addAll(bindVariables);
			bindVariables = bindVariablesCopy;
		}
	}

	private void addProductCodeJoinsToWhere() {
		String tableXfer = this.isArchived ? "bo_transfer_hist" : "bo_transfer";
		if (orderBy != null && orderBy.contains("product_sec_code.")) {
			String join = "";
			List<CalypsoBindVariable> bindVariablesCopy = null;
			List<String> names = getAttrNames("product_sec_code");
			for (String name : names) {
				join += join.length() > 0 ? " and " : "";
				join += " " + tableXfer + ".product_id=" + name.toLowerCase() + ".product_id (+) and "
						+ name.toLowerCase() + ".sec_code(+)=?";

				if (bindVariablesCopy == null) {
					bindVariablesCopy = CustomBindVariablesUtil.createNewBindVariable(name);
				} else {
					CustomBindVariablesUtil.addNewBindVariableToList(name, bindVariablesCopy);
				}
				from += (Util.isEmpty(from) ? "" : ",") + "product_sec_code" + " " + name.toLowerCase();
				orderBy = orderBy.replaceFirst("product_sec_code."+name, "product_sec_code.code_value");
				orderBy = orderBy.replaceFirst("product_sec_code", name.toLowerCase());
			}
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
			bindVariablesCopy.addAll(bindVariables);
			bindVariables = bindVariablesCopy;
		}
	}

	private void addPOInfoAndAttrJoinsToWhere() {
		String tableXfer = this.isArchived ? "bo_transfer_hist" : "bo_transfer";
		if (orderBy != null && orderBy.contains("processingOrg.")) {
			String join = " " + tableXfer + ".int_le_id=processingOrg.legal_entity_id (+) ";
			from += (Util.isEmpty(from) ? "" : ",") + "legal_entity processingOrg";
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
		}
		if (orderBy != null && orderBy.contains("POAttribute.")) {
			String join = "";
			List<CalypsoBindVariable> bindVariablesCopy = null;
			List<String> names = getAttrNames("POAttribute");
			for (String name : names) {
				join += join.length() > 0 ? " and " : "";
				join += " " + tableXfer + ".int_le_id=" + name.toLowerCase() + "PO.legal_entity_id (+) and "
						+ name.toLowerCase() + "PO.attribute_type(+)=?";

				if (bindVariablesCopy == null) {
					bindVariablesCopy = CustomBindVariablesUtil.createNewBindVariable(name);
				} else {
					CustomBindVariablesUtil.addNewBindVariableToList(name, bindVariablesCopy);
				}
				from += (Util.isEmpty(from) ? "" : ",") + "le_attribute " + name.toLowerCase() + "PO";
				orderBy = orderBy.replaceFirst("POAttribute." + name, "POAttribute.attribute_value");
				orderBy = orderBy.replaceFirst("POAttribute", name.toLowerCase() + "PO");
			}
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
			bindVariablesCopy.addAll(bindVariables);
			bindVariables = bindVariablesCopy;
		}
	}

	private void addCptyInfoAndAttrJoinsToWhere() {
		String tableXfer = this.isArchived ? "bo_transfer_hist" : "bo_transfer";
		if (orderBy != null && orderBy.contains("counterparty.")) {
			String join = " " + tableXfer + ".ext_le_id=counterparty.legal_entity_id (+) ";
			from += (Util.isEmpty(from) ? "" : ",") + "legal_entity counterparty";
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
		}
		if (orderBy != null && orderBy.contains("CPTYAttribute.")) {
			String join = "";
			List<CalypsoBindVariable> bindVariablesCopy = null;
			List<String> names = getAttrNames("CPTYAttribute");
			for (String name : names) {
				join += join.length() > 0 ? " and " : "";
				join += " " + tableXfer + ".ext_le_id=" + name.toLowerCase() + "CPTY.legal_entity_id (+) and "
						+ name.toLowerCase() + "CPTY.attribute_type(+)=?";

				if (bindVariablesCopy == null) {
					bindVariablesCopy = CustomBindVariablesUtil.createNewBindVariable(name);
				} else {
					CustomBindVariablesUtil.addNewBindVariableToList(name, bindVariablesCopy);
				}
				from += (Util.isEmpty(from) ? "" : ",") + "le_attribute " + name.toLowerCase() + "CPTY";
				orderBy = orderBy.replaceFirst("CPTYAttribute." + name, "CPTYAttribute.attribute_value");
				orderBy = orderBy.replaceFirst("CPTYAttribute", name.toLowerCase() + "CPTY");
			}
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
			bindVariablesCopy.addAll(bindVariables);
			bindVariables = bindVariablesCopy;
		}
	}

	private void addAgentInfoAndAttrJoinsToWhere() {
		String tableXfer = this.isArchived ? "bo_transfer_hist" : "bo_transfer";
		if (orderBy != null && orderBy.contains("agent.")) {
			String join = " " + tableXfer + ".ext_agent_le_id=agent.legal_entity_id (+) ";
			from += (Util.isEmpty(from) ? "" : ",") + "legal_entity agent";
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
		}
		if (orderBy != null && orderBy.contains("agentAttribute.")) {
			String join = "";
			List<CalypsoBindVariable> bindVariablesCopy = null;
			List<String> names = getAttrNames("agentAttribute");
			for (String name : names) {
				join += join.length() > 0 ? " and " : "";
				join += " " + tableXfer + ".ext_agent_le_id=" + name.toLowerCase() + "Agent.legal_entity_id (+) and "
						+ name.toLowerCase() + "Agent.attribute_type(+)=?";

				if (bindVariablesCopy == null) {
					bindVariablesCopy = CustomBindVariablesUtil.createNewBindVariable(name);
				} else {
					CustomBindVariablesUtil.addNewBindVariableToList(name, bindVariablesCopy);
				}
				from += (Util.isEmpty(from) ? "" : ",") + "le_attribute " + name.toLowerCase() + "Agent";
				orderBy = orderBy.replaceFirst("agentAttribute." + name, "agentAttribute.attribute_value");
				orderBy = orderBy.replaceFirst("agentAttribute", name.toLowerCase() + "Agent");
			}
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
			bindVariablesCopy.addAll(bindVariables);
			bindVariables = bindVariablesCopy;
		}
	}

	private void addTradeKeywordJoinsToWhere() {
		String tableAttr = this.isArchived ? "trade_keyword_hist" : "trade_keyword";
		String tableXfer = this.isArchived ? "bo_transfer_hist" : "bo_transfer";
		if (orderBy != null && orderBy.contains("trade_keyword.")) {
			String join = "";
			List<CalypsoBindVariable> bindVariablesCopy = null;
			List<String> names = getAttrNames("trade_keyword");
			for (String name : names) {
				join += join.length() > 0 ? " and " : "";
				join += " " + tableXfer + ".trade_id=" + name.toLowerCase() + ".trade_id (+) and " + name.toLowerCase()
						+ ".keyword_name(+)=?";

				if (bindVariablesCopy == null) {
					bindVariablesCopy = CustomBindVariablesUtil.createNewBindVariable(name);
				} else {
					CustomBindVariablesUtil.addNewBindVariableToList(name, bindVariablesCopy);
				}
				from += (Util.isEmpty(from) ? "" : ",") + tableAttr + " " + name.toLowerCase();
				orderBy = orderBy.replaceFirst("trade_keyword." + name, "trade_keyword.keyword_value");
				orderBy = orderBy.replaceFirst(tableAttr, name.toLowerCase());
			}
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
			bindVariablesCopy.addAll(bindVariables);
			bindVariables = bindVariablesCopy;
		}
	}

	private void addTradeJoinsToWhere() {
		String tableAttr = this.isArchived ? "trade_hist" : "trade";
		String tableXfer = this.isArchived ? "bo_transfer_hist" : "bo_transfer";
		if (orderBy != null
				&& (orderBy.contains("trade.") || orderBy.contains("book.") || orderBy.contains("bookAttribute."))) {
			String join = " " + tableXfer + ".trade_id=" + tableAttr + ".trade_id (+)";
			from += (Util.isEmpty(from) ? "" : ",") + tableAttr;
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
		}
	}

	private void addBookJoinsToWhere() {
		String tableXfer = this.isArchived ? "bo_transfer_hist" : "bo_transfer";
		if (orderBy != null && orderBy.contains("book.")) {
			String join = " trade.book_id=book.book_id (+)";
			from += (Util.isEmpty(from) ? "book" : ",book");
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
		}
		if (orderBy != null && orderBy.contains("book_attr_value.")) {
			String join = "";
			List<CalypsoBindVariable> bindVariablesCopy = null;
			List<String> names = getAttrNames("book_attr_value");
			for (String name : names) {
				join += join.length() > 0 ? " and " : "";
				join += " " + tableXfer + ".book_id=" + name.toLowerCase() + "BookAttr.book_id (+) and "
						+ name.toLowerCase() + "BookAttr.attribute_name(+)=?";

				if (bindVariablesCopy == null) {
					bindVariablesCopy = CustomBindVariablesUtil.createNewBindVariable(name);
				} else {
					CustomBindVariablesUtil.addNewBindVariableToList(name, bindVariablesCopy);
				}
				from += (Util.isEmpty(from) ? "" : ",") + "book_attr_value " + name.toLowerCase() + "BookAttr";
				orderBy = orderBy.replaceFirst("book_attr_value." + name, "book_attr_value.attribute_value");
				orderBy = orderBy.replaceFirst("book_attr_value", name.toLowerCase() + "BookAttr");
			}
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
			bindVariablesCopy.addAll(bindVariables);
			bindVariables = bindVariablesCopy;
		}
	}

	private void addGLAccountJoinsToWhere() {
		String tableXfer = this.isArchived ? "bo_transfer_hist" : "bo_transfer";
		if (orderBy != null && orderBy.contains("acc_account.")) {
			String join = " " + tableXfer + ".gl_account_id=acc_account.acc_account_id (+)";
			from += (Util.isEmpty(from) ? "" : ",") + "acc_account";
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
		}
	}

	private void addProductCAJoinsToWhere() {
		String tableXfer = this.isArchived ? "bo_transfer_hist" : "bo_transfer";
		if (orderBy != null && orderBy.contains("product_ca.")) {
			String join = " " + tableXfer + ".product_id=product_ca.product_id (+)";
			from += (Util.isEmpty(from) ? "" : ",") + "product_ca";
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
		}
	}

	private void addProductMarginCallJoinsToWhere() {
		String tableXfer = this.isArchived ? "bo_transfer_hist" : "bo_transfer";
		if (orderBy != null && (orderBy.contains("product_simplexfer.") || orderBy.contains("mrgcall_config."))) {
			String join = " " + tableXfer + ".trade_id=trade.trade_id AND " + "trade.product_id=product_simplexfer.product_id (+)";
			from += (Util.isEmpty(from) ? "" : ",") + "product_simplexfer";
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
		}
	}

	private void addMarginCallContractJoinsToWhere() {

		if (orderBy != null && orderBy.contains("mrgcall_config.")) {
			String join = " product_simplexfer.linked_id=mrgcall_config.mrg_call_def (+)";
			from += (Util.isEmpty(from) ? "" : ",") + "mrgcall_config";
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
		}
	}

	private void addBalanceCallJoinsToWhere() {
		String tableXfer = this.isArchived ? "bo_transfer_hist" : "bo_transfer";
		if (orderBy != null && orderBy.contains("inv_sec_balance.isinbalance")) {
			StringBuilder whereSB = new StringBuilder();
			whereSB.append("select inv_sec_balance.security_id,sum(total_security) isinbalance from inv_sec_balance ");
			whereSB.append("where inv_sec_balance.config_id=?");
			List<CalypsoBindVariable> bindVariablesCopy = CustomBindVariablesUtil.createNewBindVariable(0);
			whereSB.append(" and inv_sec_balance.INTERNAL_EXTERNAL=?");
			CustomBindVariablesUtil.addNewBindVariableToList(BOPositionReport.INTERNAL.toUpperCase(),
					bindVariablesCopy);
			whereSB.append(" and inv_sec_balance.POSITION_TYPE=?");
			CustomBindVariablesUtil.addNewBindVariableToList(BOPositionReport.ACTUAL.toUpperCase(), bindVariablesCopy);
			whereSB.append(" and inv_sec_balance.DATE_TYPE=?");
			CustomBindVariablesUtil.addNewBindVariableToList(BOPositionReport.SETTLE.toUpperCase(), bindVariablesCopy);
			whereSB.append(" group by inv_sec_balance.security_id ");
			String join = " " + tableXfer + ".product_id=inv_sec_balance.security_id (+) ";
			from += (Util.isEmpty(from) ? "(" : ",(") + whereSB.toString() + ") inv_sec_balance";
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
			bindVariablesCopy.addAll(bindVariables);
			bindVariables = bindVariablesCopy;
		}
	}

	private void addPayerInfoAndAttrJoinsToWhere() {
		String tableXfer = this.isArchived ? "bo_transfer_hist" : "bo_transfer";
		if (orderBy != null && orderBy.contains("payer.")) {
			String join = " (CASE WHEN " + tableXfer + ".payreceive_type = 'PAY' THEN " + tableXfer + ".int_le_id ELSE "
					+ tableXfer + ".ext_le_id END)=payer.legal_entity_id (+) ";
			from += (Util.isEmpty(from) ? "" : ",") + "legal_entity payer";
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
		}
		if (orderBy != null && orderBy.contains("payerAttribute.")) {
			String join = "";
			List<CalypsoBindVariable> bindVariablesCopy = null;
			List<String> names = getAttrNames("payerAttribute");
			for (String name : names) {
				join += join.length() > 0 ? " and " : "";
				join += " (CASE WHEN " + tableXfer + ".payreceive_type = 'PAY' THEN " + tableXfer + ".int_le_id ELSE "+ tableXfer + ".ext_le_id END)="
						+ name.toLowerCase() + "Payer.legal_entity_id (+) and " + name.toLowerCase()
						+ "Payer.attribute_type(+)=?";

				if (bindVariablesCopy == null) {
					bindVariablesCopy = CustomBindVariablesUtil.createNewBindVariable(name);
				} else {
					CustomBindVariablesUtil.addNewBindVariableToList(name, bindVariablesCopy);
				}
				from += (Util.isEmpty(from) ? "" : ",") + "le_attribute " + name.toLowerCase() + "Payer";
				orderBy = orderBy.replaceFirst("payerAttribute." + name, "payerAttribute.attribute_value");
				orderBy = orderBy.replaceFirst("payerAttribute", name.toLowerCase() + "Payer");
			}
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
			bindVariablesCopy.addAll(bindVariables);
			bindVariables = bindVariablesCopy;
		}
	}

	private void addReceiverInfoAndAttrJoinsToWhere() {
		String tableXfer = this.isArchived ? "bo_transfer_hist" : "bo_transfer";
		if (orderBy != null && orderBy.contains("receiver.")) {
			String join = " (CASE WHEN " + tableXfer + ".payreceive_type <> 'PAY' THEN " + tableXfer
					+ ".int_le_id ELSE " + tableXfer + ".ext_le_id END)=receiver.legal_entity_id (+) ";
			from += (Util.isEmpty(from) ? "" : ",") + "legal_entity receiver";
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
		}
		if (orderBy != null && orderBy.contains("receiverAttribute.")) {
			String join = "";
			List<CalypsoBindVariable> bindVariablesCopy = null;
			List<String> names = getAttrNames("receiverAttribute");
			for (String name : names) {
				join += join.length() > 0 ? " and " : "";
				join += " (CASE WHEN " + tableXfer + ".payreceive_type <> 'PAY' THEN " + tableXfer + ".int_le_id ELSE "
						+ tableXfer + ".ext_le_id END)=" + name.toLowerCase() + "Receiver.legal_entity_id (+) and "
						+ name.toLowerCase()
						+ "Receiver.attribute_type(+)=?";

				if (bindVariablesCopy == null) {
					bindVariablesCopy = CustomBindVariablesUtil.createNewBindVariable(name);
				} else {
					CustomBindVariablesUtil.addNewBindVariableToList(name, bindVariablesCopy);
				}
				from += (Util.isEmpty(from) ? "" : ",") + "le_attribute " + name.toLowerCase() + "Receiver";
				orderBy = orderBy.replaceFirst("receiverAttribute." + name, "receiverAttribute.attribute_value");
				orderBy = orderBy.replaceFirst("receiverAttribute", name.toLowerCase() + "Receiver");
			}
			where = join + ((Util.isEmpty(join) || Util.isEmpty(where)) ? "" : " AND ") + where;
			bindVariablesCopy.addAll(bindVariables);
			bindVariables = bindVariablesCopy;
		}
	}

	private List<String> getAttrNames(String table) {
		List<String> lstAttrs = new ArrayList<String>();
		int idx = orderBy.indexOf(table + ".");
		while (idx >= 0) {
			String strName = "";
			int idxSpace = orderBy.indexOf(" ", idx + (table + ".").length());
			int idxComma = orderBy.indexOf(",", idx + (table + ".").length());
			int end = Math.min(idxSpace, idxComma);
			if (end < 0) {
				end = Math.max(idxSpace, idxComma);
			}
			if (end < 0) {
				strName = orderBy.substring(idx + (table + ".").length());
			} else {
				strName = orderBy.substring(idx + (table + ".").length(), end);
			}
			lstAttrs.add(strName);
			idx = orderBy.indexOf(table + ".", idx + 1);
		}
		return lstAttrs;
	}
}
