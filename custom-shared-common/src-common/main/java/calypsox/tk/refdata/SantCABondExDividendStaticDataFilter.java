package calypsox.tk.refdata;

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
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.CA;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.StaticDataFilterInterface;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

public class SantCABondExDividendStaticDataFilter implements StaticDataFilterInterface {

	/** TreeList Parent reference for custom PARENT_CUSTOM_REFERENCE. */
	private static final String PARENT_CUSTOM_REFERENCE = "Custom";

	/** CA_BOND_ED. */
	private static final String CA_BOND_ED = "CA Bond Ex-Dividend";

	@Override
	public boolean fillTreeList(DSConnection con, TreeList tl) {
		Vector<String> nodes = new Vector<>();
		nodes.add(PARENT_CUSTOM_REFERENCE);
		tl.add(nodes, CA_BOND_ED);
		return false;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Vector getDomain(DSConnection con, String attributeName) {
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void getDomainValues(DSConnection con, Vector v) {
		v.add(CA_BOND_ED);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Vector getTypeDomain(String attributeName) {
		Vector<String> v = new Vector<String>();
		if (attributeName.equals(CA_BOND_ED)) {
			v.addElement(StaticDataFilterElement.S_FLOAT_RANGE);
		}
		return v;
	}

	@Override
	public Object getValue(Trade trade, LegalEntity le, String role, Product product, BOTransfer transfer,
			BOMessage message, TradeTransferRule rule, ReportRow reportRow, Task task, Account glAccount,
			CashFlow cashflow, HedgeRelationship relationship, String filterElement, StaticDataFilterElement element) {
		if (trade != null) {
			Product prod = trade.getProduct();
			if (prod instanceof CA) {
				Product p=((CA) prod).getSecurity();
				if( p instanceof Bond) {
					return (double) ((Bond)p).getExdividendDays();
				}
			}
		}
		return 0d;
	}

	@Override
	public boolean isTradeNeeded(String attributeName) {
		return true;
	}

}
