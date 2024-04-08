package calypsox.tk.refdata;


import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.*;
import com.calypso.tk.core.*;
import com.calypso.tk.product.SecLending;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.StaticDataFilterInterface;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;
import java.util.Vector;


public class SantCASecLendingHasSdiIndicatorStaticDataFilter implements StaticDataFilterInterface {


    private static final String PARENT_CUSTOM_REFERENCE = "SantTransfer";
	private static final String CA_SECLENDING_SDI_INDICATOR = "CASecLendingHasSdiIndicator";
    private static final String VALUE_SECB = "SECB";
    private final static String NOT_FOUND = "124%%4sdk2";


	@Override
	public boolean fillTreeList(DSConnection con, TreeList tl) {
		Vector<String> nodes = new Vector<>();
		nodes.add(PARENT_CUSTOM_REFERENCE);
		tl.add(nodes, CA_SECLENDING_SDI_INDICATOR);
		return false;
	}


	@SuppressWarnings("rawtypes")
	@Override
	public Vector getDomain(DSConnection con, String attributeName) {
		Vector vect = new Vector();
		vect.add(VALUE_SECB);
		return vect;
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void getDomainValues(DSConnection con, Vector v) {
		v.add(CA_SECLENDING_SDI_INDICATOR);
	}


	@SuppressWarnings("rawtypes")
	@Override
	public Vector getTypeDomain(String attributeName) {
		Vector<String> v = new Vector<String>();
		if (attributeName.equals(CA_SECLENDING_SDI_INDICATOR)) {
			v.addElement(StaticDataFilterElement.S_IN);
			v.addElement(StaticDataFilterElement.S_NOT_IN);
		}
		return v;
	}


	@Override
	public Object getValue(Trade trade, LegalEntity le, String role, Product product, BOTransfer transfer,
			BOMessage message, TradeTransferRule rule, ReportRow reportRow, Task task, Account glAccount,
			CashFlow cashflow, HedgeRelationship relationship, String filterElement, StaticDataFilterElement element) {

	    Log.debug(Log.DEBUG,this.getClass().getSimpleName() + " >>>> Start of Cumstom SDI. TransferId:" + transfer.getLongId());

        if(transfer == null || trade == null) {
            Log.debug(Log.DEBUG,this.getClass().getSimpleName() + " >>>> Transfer: '" + transfer + "' Trade: '" + trade + "'");
            return NOT_FOUND;
        }

        if(!"Counterparty".equalsIgnoreCase(trade.getRole())) {
            Log.debug(Log.DEBUG, this.getClass().getSimpleName() + " >>>> Transfer: " + transfer.getLongId() + " - Role: '" + trade.getRole() + "'");
            return NOT_FOUND;
        }

        if(!"SecLending".equalsIgnoreCase(trade.getKeywordValue("CASourceProductType"))) {
            Log.debug(Log.DEBUG,this.getClass().getSimpleName() + " >>>> Transfer: " + transfer.getLongId() + " - CASourceProductType: '" + trade.getKeywordValue("CASourceProductType") + "'");
            return NOT_FOUND;
        }

        Log.debug(Log.DEBUG,this.getClass().getSimpleName() + " >>>> Transfer: " + transfer.getLongId() + " - Role: " + trade.getRole() + " - CASourceProductType: '" + trade.getKeywordValue("CASourceProductType") + "'");

        String caSource = trade.getKeywordValue("CASource");
        if(Util.isEmpty(caSource)) {
            Log.debug(Log.DEBUG, this.getClass().getSimpleName() + " >>>> Transfer: " + transfer.getLongId() + " - CaSource is empty");
            return NOT_FOUND;
        }

        Log.debug(Log.DEBUG,this.getClass().getSimpleName() + " >>>> Transfer: " + transfer.getLongId() + " - Role: " + trade.getRole() + " - CASourceProductType: '" + trade.getKeywordValue("CASourceProductType") + "' - CASource: " + caSource);

        Long secLendingId = Long.parseLong(caSource);
        try {
            final Trade secLendingTrade = DSConnection.getDefault().getRemoteTrade().getTrade(secLendingId);
            if (secLendingTrade != null && secLendingTrade.getLongId() > 0) {
                if (secLendingTrade.getProduct() instanceof SecLending) {
                    String direction = ((SecLending) secLendingTrade.getProduct()).getDirection();
                    TransferArray transferList = null;
                    StringBuilder whereClause = new StringBuilder();
                    whereClause.append("transfer_type = 'SECURITY' AND trade_id = ");
                    whereClause.append(secLendingId);
                    whereClause.append(" AND event_type = '");
                    if ("borrow".equalsIgnoreCase(direction)) {
                        whereClause.append("SEC_RECEIPT");
                    }
                    else if ("lend".equalsIgnoreCase(direction)) {
                        whereClause.append("SEC_DELIVERY");
                    }
                    whereClause.append("' AND transfer_status NOT IN ('CANCELED')");
                    Log.debug(Log.DEBUG,this.getClass().getSimpleName() + " >>>> WhereClause: " + whereClause);
                    transferList = DSConnection.getDefault().getRemoteBO().getTransfers(null, whereClause.toString(), null);
                    if (transferList != null && transferList.size() > 0) {
                        BOTransfer secLendingXfer = transferList.elementAt(0);
                        Log.debug(Log.DEBUG, this.getClass().getSimpleName() + " >>>> Transfer: " + transfer.getLongId() + " - Trade: " + trade.getLongId() +
                                " - Xfer Sec Lending: " + secLendingXfer.getLongId() + " - TradeSecLending: " + secLendingId +
                                " - Attr INDICATOR: " + secLendingXfer.getAttribute("INDICATOR"));
                        return secLendingXfer.getAttribute("INDICATOR");
                    }
               }
            }
        } catch (CalypsoServiceException e) {
            Log.debug(Log.DEBUG, this.getClass().getSimpleName() + " >>>> Transfer: " + transfer.getLongId() + " - Error loading xfers for trade  '" + secLendingId + "'");
            Log.error(this,"Error loading xfers for trade  '" + secLendingId + "'" + e);
        } catch (final Exception ex) {
            Log.error(this, ex);
        }
        Log.debug(Log.DEBUG,this.getClass().getSimpleName() + " >>>> End of Cumstom SDI. Transfer: " + transfer.getLongId());
        return NOT_FOUND;
    }


	@Override
	public boolean isTradeNeeded(String attributeName) {
		return true;
	}


}