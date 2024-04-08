package calypsox.tk.refdata;

import static calypsox.util.TradeInterfaceUtils.TRADE_KWD_BO_REFERENCE;

import java.util.Vector;

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
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.CollateralStaticDataFilter;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;

/**
 * Custom Static DataFilter for margin call
 * 
 * @author aela
 * 
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SantTKBoRefStaticDataFilter extends CollateralStaticDataFilter {

	protected static final String KEYWORD_BO_REF = "Sant Keyword Bo_Reference";

	@Override
	public void getDomainValues(DSConnection arg0, Vector vect) {
		vect.addElement(KEYWORD_BO_REF);
	}

	@Override
	public boolean fillTreeList(DSConnection ds, TreeList treeList) {
		Vector nodes = new Vector();
		nodes.addElement("SantTradeKeyword");
		treeList.add(nodes, KEYWORD_BO_REF);
		return false;
	}

	@Override
	public Vector getTypeDomain(String name) {
		Vector<String> vect = new Vector<String>();
		if (name.equals(KEYWORD_BO_REF)) {
			vect.addElement(StaticDataFilterElement.S_INT_ENUM);
			vect.addElement(StaticDataFilterElement.S_INT_RANGE);
			vect.addElement(StaticDataFilterElement.S_NOT_IN_INT_ENUM);
		}

		return vect;
	}

	@Override
	public Vector getDomain(DSConnection dsCon, String domain) {
		Vector vect = new Vector();
		if (KEYWORD_BO_REF.equals(domain)) {
			// vect = LocalCache.getDomainValues(dsCon, "legalAgreementType");
		}

		return vect;
	}

	@Override
	public Object getValue(Trade trade, LegalEntity le, String role, Product product, BOTransfer transfer,
			BOMessage message, TradeTransferRule rule, ReportRow reportRow, Task task, Account glAccount,
			CashFlow cashflow, HedgeRelationship relationship, String filterElement, StaticDataFilterElement element) {

		if (filterElement.equals(KEYWORD_BO_REF)) {
			if ((trade != null) && !Util.isEmpty(trade.getKeywordValue(TRADE_KWD_BO_REFERENCE))) {
				return trade.getKeywordAsInt(TRADE_KWD_BO_REFERENCE);
			}

		}

		return null;
	}

	@Override
	public boolean isTradeNeeded(String arg0) {
		return false;
	}

}
