package calypsox.tk.refdata;
import java.util.Iterator;
import java.util.Vector;

import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.CashFlow;
import com.calypso.tk.core.HedgeRelationship;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Status;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.StaticDataFilterInterface;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;


@SuppressWarnings("deprecation")
public class IsAnyXferSettledStaticDataFilter implements StaticDataFilterInterface {


    public static final String IS_ANY_XFER_SETTLED = "Is Any Xfer Settled";


	@Override
	public boolean fillTreeList(DSConnection con, TreeList tl) {
        Vector<String> nodes = new Vector<>();
        nodes.add("Custom");
        tl.add(nodes, IS_ANY_XFER_SETTLED);
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
        v.add(IS_ANY_XFER_SETTLED);
	}


	@SuppressWarnings("rawtypes")
	@Override
	public Vector getTypeDomain(String attributeName) {
        Vector<String> v = new Vector<String>();
        if (attributeName.equals(IS_ANY_XFER_SETTLED)) {
            v.addElement(StaticDataFilterElement.S_IS);
        }
        return v;
	}


	@Override
	public Object getValue(Trade trade, LegalEntity le, String role, Product product, BOTransfer transfer,
			BOMessage message, TradeTransferRule rule, ReportRow reportRow, Task task, Account glAccount,
			CashFlow cashflow, HedgeRelationship relationship, String filterElement, StaticDataFilterElement element) {

		TransferArray transferList = null;
		try {
			transferList = DSConnection.getDefault().getRemoteBO().getBOTransfers(trade.getLongId());
			if (!transferList.isEmpty()) {
				Iterator<BOTransfer> iterator = transferList.iterator();
				while (iterator.hasNext()) {
					final BOTransfer xfer = iterator.next();
					if(Status.S_SETTLED.equals(xfer.getStatus())) {
						return Boolean.TRUE;
					}
				}
			}
		} catch (CalypsoServiceException e) {
			Log.error(this, "Could not get transfers from trade " + trade.getLongId());
		}
		return Boolean.FALSE;
	}


	@Override
	public boolean isTradeNeeded(String attributeName) {
		return true;
	}


}
