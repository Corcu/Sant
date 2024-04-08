/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.refdata;

import java.util.Vector;

import calypsox.tk.product.BondCustomData;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.HedgeRelationship;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.StaticDataFilterInterface;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

@SuppressWarnings("deprecation")
public class SantCentralBankHaircutsStaticDataFilter implements StaticDataFilterInterface {

	private static final String SANT_BONDCUSTOMDISCOUNTHAIRCUT_FED = "Sant_BondCustomDiscountHaircut_FED";
	private static final String SANT_BONDCUSTOMDISCOUNTHAIRCUT_BOE = "Sant_BondCustomDiscountHaircut_BOE";
	private static final String SANT_BONDCUSTOMDISCOUNTHAIRCUT_SWISS = "Sant_BondCustomDiscountHaircut_Swiss";
	private static final String SANT_BONDCUSTOMDISCOUNTHAIRCUT_ECB = "Sant_BondCustomDiscountHaircut_ECB";
	private static final String SANT_BONDCUSTOMDISCOUNTHAIRCUT_EUREX = "Sant_BondCustomDiscountHaircut_EUREX";

	@Override
	public boolean fillTreeList(DSConnection con, TreeList tl) {
		Vector<String> nodes = new Vector<String>();
		nodes.add("SantCentralBankHaircuts");

		tl.add(nodes, SANT_BONDCUSTOMDISCOUNTHAIRCUT_FED);
		tl.add(nodes, SANT_BONDCUSTOMDISCOUNTHAIRCUT_BOE);
		tl.add(nodes, SANT_BONDCUSTOMDISCOUNTHAIRCUT_SWISS);
		tl.add(nodes, SANT_BONDCUSTOMDISCOUNTHAIRCUT_ECB);
		tl.add(nodes, SANT_BONDCUSTOMDISCOUNTHAIRCUT_EUREX);

		return false;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void getDomainValues(DSConnection con, Vector v) {
		v.add(SANT_BONDCUSTOMDISCOUNTHAIRCUT_FED);
		v.add(SANT_BONDCUSTOMDISCOUNTHAIRCUT_BOE);
		v.add(SANT_BONDCUSTOMDISCOUNTHAIRCUT_SWISS);
		v.add(SANT_BONDCUSTOMDISCOUNTHAIRCUT_ECB);
		v.add(SANT_BONDCUSTOMDISCOUNTHAIRCUT_EUREX);

	}

	@SuppressWarnings("rawtypes")
	@Override
	public Vector getTypeDomain(String attributeName) {
		Vector<String> v = new Vector<String>();
		v.addElement(StaticDataFilterElement.S_FLOAT_RANGE);

		return v;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Vector getDomain(DSConnection con, String attributeName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getValue(Trade trade, LegalEntity le, String role, Product product, BOTransfer transfer,
			BOMessage message, TradeTransferRule rule, ReportRow reportRow, Task task, Account glAccount,
			CashFlow cashflow, HedgeRelationship relationship, String filterElement, StaticDataFilterElement element) {

		if ((product == null) || !(product instanceof Bond)) {
			return null;
		}
		// Vector<String> filtElementValues = element.getValues();
		// if (Util.isEmpty(filtElementValues)) {
		// return null;
		// }

		Bond bond = (Bond) product;
		if (bond.getCustomData() == null) {
			return null;
		}

		BondCustomData customData = (BondCustomData) bond.getCustomData();

		if (filterElement.equals(SANT_BONDCUSTOMDISCOUNTHAIRCUT_FED)) {
			return customData.getHaircut_fed();
		} else if (filterElement.equals(SANT_BONDCUSTOMDISCOUNTHAIRCUT_BOE)) {
			return customData.getHaircut_boe();
		} else if (filterElement.equals(SANT_BONDCUSTOMDISCOUNTHAIRCUT_SWISS)) {
			return customData.getHaircut_swiss();
		} else if (filterElement.equals(SANT_BONDCUSTOMDISCOUNTHAIRCUT_ECB)) {
			return customData.getHaircut_ecb();
		} else if (filterElement.equals(SANT_BONDCUSTOMDISCOUNTHAIRCUT_EUREX)) {
			return customData.getHaircut_eurex();
		}

		return null;
	}

	@Override
	public boolean isTradeNeeded(String attributeName) {
		return false;
	}

}
