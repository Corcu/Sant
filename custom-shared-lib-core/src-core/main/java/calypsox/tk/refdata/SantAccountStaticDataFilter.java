/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.refdata;

import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.HedgeRelationship;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.AccessUtil;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.StaticDataFilterInterface;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

public class SantAccountStaticDataFilter implements StaticDataFilterInterface {

	public static final String ACCOUNT_PO = "Account PO";

	@Override
	public boolean fillTreeList(DSConnection con, TreeList tl) {
		Vector<String> nodes = new Vector<String>();
		nodes.addElement("SantAccount");
		tl.add(nodes, ACCOUNT_PO);
		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void getDomainValues(DSConnection con, Vector v) {
		v.add(ACCOUNT_PO);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Vector getTypeDomain(String attributeName) {
		Vector<String> v = new Vector<String>();
		if (attributeName.equals(ACCOUNT_PO)) {
			v.addElement(StaticDataFilterElement.S_IN);
		}
		return v;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Vector getDomain(DSConnection con, String attributeName) {
		Vector vect = new Vector();
		if (ACCOUNT_PO.equals(attributeName)) {
			vect = AccessUtil.getAccessiblePONames(false, false);
		}
		return vect;
	}

	@Override
	public Object getValue(Trade trade, LegalEntity le, String role, Product product, BOTransfer transfer,
			BOMessage message, TradeTransferRule rule, ReportRow reportRow, Task task, Account glAccount,
			CashFlow cashflow, HedgeRelationship relationship, String filterElement, StaticDataFilterElement element) {

		if (glAccount != null) {
			LegalEntity po = BOCache.getLegalEntity(DSConnection.getDefault(), glAccount.getProcessingOrgId());
			if (po != null) {
				return po.getCode();
			}
		}
		return null;
	}

	@Override
	public boolean isTradeNeeded(String attributeName) {
		return true;
	}

}
