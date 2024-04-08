/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.refdata;

import static calypsox.util.TradeInterfaceUtils.TRD_IMP_FIELD_NUM_FRONT_ID;

import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.HedgeRelationship;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.StaticDataFilterInterface;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

/**
 * Part of the development: Trades Exclusions, based on DDR CalypsoColl - TradeInterface_Gaps_v1.2. It is required to
 * check if the Front Office ID ends with the word "SWAP".
 * 
 * @author Guillermo Solano
 * @version 1.0; 24/07/2013
 * 
 */

@SuppressWarnings("deprecation")
public class SantTradeFORefEndsStaticDataFilter implements StaticDataFilterInterface {

	// Cte to define de SD filter name and Domain
	public final static String FO_REFERENCE_ENDS_WITH = "FORefEndsWithWord";

	// cte domain of words we actually want to filter
	private final static String ENDS_WITH_SWAP = "SWAP";
	private final static String NOT_FOUND = "124%%4sdk2";

	/**
	 * Inserts the SDFilter name
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void getDomainValues(DSConnection con, @SuppressWarnings("rawtypes") Vector v) {

		v.add(FO_REFERENCE_ENDS_WITH);
	}

	/**
	 * To be added under other nodes apart from the CUSTOM one
	 */
	@Override
	public boolean fillTreeList(DSConnection con, TreeList tl) {

		Vector<String> nodes = new Vector<String>();
		nodes.add("SantTrade");
		tl.add(nodes, FO_REFERENCE_ENDS_WITH);
		return false;
	}

	// USER CRITERIA OPTIONS:

	/**
	 * Option type of the filter. This is the criteria section in SDF window.
	 * 
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Vector getTypeDomain(String attributeName) {

		Vector<String> v = new Vector<String>(1);

		if (attributeName.equals(FO_REFERENCE_ENDS_WITH)) {
			v.addElement(StaticDataFilterElement.S_IN);
			v.addElement(StaticDataFilterElement.S_LIKE);
		}

		return v;
	}

	/**
	 * these are the filter Value(s) of the in SDF window, when the user chooses a criteria that admits the domain.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Vector getDomain(DSConnection con, String attributeName) {

		Vector<String> domain = new Vector<String>();
		domain.add(ENDS_WITH_SWAP);

		return domain;
	}

	/**
	 * Main method of the SDFilter. Trade is mandatory so from the BO reference will be taken and check if finishes with
	 * the filter element.
	 * 
	 */
	@Override
	public Object getValue(Trade trade, LegalEntity le, String role, Product product, BOTransfer transfer,
			BOMessage message, TradeTransferRule rule, ReportRow reportRow, Task task, Account glAccount,
			CashFlow cashflow, HedgeRelationship relationship, String filterElement, StaticDataFilterElement element) {

		if ((element == null) || (element.getValues().isEmpty())) {
			return NOT_FOUND; // is not filled the SD Filter Value to compare to
		}

		if (trade == null) { // must not happen, but we can try to recover trade from report
			if (reportRow == null) {
				return NOT_FOUND;
			}

			trade = (Trade) reportRow.getProperty(ReportRow.TRADE);

			if (trade == null) {
				return NOT_FOUND;
			}
		}

		final String foRef = trade.getKeywordValue(TRD_IMP_FIELD_NUM_FRONT_ID).trim();
		final String compare = ((String) element.getValues().get(0)).trim();

		if (Util.isEmpty(foRef) || Util.isEmpty(compare)) {
			return NOT_FOUND;
		}

		// satisfies the condition
		if (foRef.endsWith(compare)) {
			return compare;
		}

		return NOT_FOUND;
	}

	/**
	 * If requires Trade (yes)
	 */
	@Override
	public boolean isTradeNeeded(String arg0) {
		return true;
	}
}
