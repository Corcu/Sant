/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.refdata;

import java.util.Optional;
import java.util.Vector;

import com.calypso.tk.refdata.*;
import org.jfree.util.Log;

import com.calypso.apps.util.TreeList;
import com.calypso.infra.util.Util;
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
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

public class SantCustomStaticDataFilter implements StaticDataFilterInterface {

	private static final String COLLATERAL_GIVER = "Collateral Giver";
	private static final String COLLATERAL_TAKER = "Collateral Taker";
	public static final String HAS_TRIPARTY_KWD_SDI_IDENTIFIER = "Sant Has Triparty Kwd SDI Identifier";
	public static final String HAS_SAME_LEI="HasSameLEI";
	public static final String LEI = "LEI";

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void getDomainValues(DSConnection con, Vector v) {
		v.add(HAS_TRIPARTY_KWD_SDI_IDENTIFIER);
		v.add(HAS_SAME_LEI);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Vector getTypeDomain(String attributeName) {
		Vector<String> v = new Vector<String>();
		if (attributeName.equals(HAS_TRIPARTY_KWD_SDI_IDENTIFIER)) {
			v.addElement(StaticDataFilterElement.S_IS);
		}else if (attributeName.equals(HAS_SAME_LEI)) {
			v.addElement(StaticDataFilterElement.S_IS);
		}
		return v;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Vector getDomain(DSConnection con, String attributeName) {
		return null;
	}

	@Override
	public Object getValue(Trade trade, LegalEntity le, String role, Product product, BOTransfer transfer,
			BOMessage message, TradeTransferRule rule, ReportRow reportRow, Task task, Account glAccount,
			CashFlow cashflow, HedgeRelationship relationship, String filterElement, StaticDataFilterElement element) {

		if (trade == null) {
			if (reportRow == null) {
				return Boolean.FALSE;
			}
			if ((reportRow.getProperty(ReportRow.TRADE) != null)
					&& ((reportRow.getProperty(ReportRow.TRADE) instanceof Trade))) {
				trade = (Trade) reportRow.getProperty(ReportRow.TRADE);
				if (trade == null) {
					return Boolean.FALSE;
				}
			}
		}

		boolean res = false;
		if (HAS_TRIPARTY_KWD_SDI_IDENTIFIER.equals(filterElement)) {
			if (rule != null) {
				if (rule.getCounterPartySDId() != 0) {
					res = isBeneficiaryIdentifierTheSame(trade, rule.getCounterPartySDId(),
							trade.getQuantity() < 0 ? COLLATERAL_TAKER : COLLATERAL_GIVER);
				}

				if (rule.getProcessingOrgSDId() != 0) {
					res = isBeneficiaryIdentifierTheSame(trade, rule.getProcessingOrgSDId(),
							trade.getQuantity() < 0 ? COLLATERAL_GIVER : COLLATERAL_TAKER);
				}
			}
		}

		if (HAS_SAME_LEI.equals(filterElement)) {
			res=hasSameLEI(trade);
		}
		return res;
	}

	private boolean isBeneficiaryIdentifierTheSame(Trade trade, int sdiId, String collWay) {
		SettleDeliveryInstruction rsl = BOCache.getSettleDeliveryInstruction(DSConnection.getDefault(), sdiId);
		if ((rsl != null) && !Util.isEmpty(trade.getKeywordValue(collWay))
				&& !Util.isEmpty(rsl.getBeneficiaryIdentifier())) {
			Log.error(rsl.toString() + " -- " + rsl.getBeneficiaryIdentifier() + " -- "
					+ trade.getKeywordValue(collWay));
			return rsl.getBeneficiaryIdentifier().indexOf(trade.getKeywordValue(collWay).trim()) > -1;
		}
		return false;
	}

	private boolean hasSameLEI(Trade trade){
		LegalEntityAttribute poLeiAttr=
				BOCache.getLegalEntityAttribute
						(DSConnection.getDefault(),0,trade.getBook().getLegalEntity().getId(),LegalEntity.PROCESSINGORG,LEI);
		LegalEntityAttribute cptyLeiAttr=
				BOCache.getLegalEntityAttribute
						(DSConnection.getDefault(),0,trade.getCounterParty().getId(),LegalEntity.COUNTERPARTY,LEI);
		String poLei= Optional.ofNullable(poLeiAttr).map(LegalEntityAttribute::getAttributeValue).orElse("");
		String cptyLei=Optional.ofNullable(cptyLeiAttr).map(LegalEntityAttribute::getAttributeValue).orElse("");
		return com.calypso.tk.core.Util.isEqualStrings(poLei,cptyLei);
	}

	@Override
	public boolean isTradeNeeded(String attributeName) {
		return true;
	}

	/**
	 * @see com.calypso.tk.refdata.StaticDataFilterInterface#fillTreeList(com.calypso.tk.service.DSConnection,
	 *      com.calypso.apps.util.TreeList)
	 */
	@Override
	public boolean fillTreeList(DSConnection dsconnection, TreeList treeList) {
		Vector<String> nodes = new Vector<>();
		nodes.add("Custom");
		treeList.add(nodes, HAS_SAME_LEI);
		return false;
	}

}
