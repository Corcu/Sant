package calypsox.tk.refdata;

import calypsox.util.product.NettedBOTransferUtil;
import com.calypso.apps.util.TreeList;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.Task;
import com.calypso.tk.bo.TradeTransferRule;
import com.calypso.tk.core.*;
import com.calypso.tk.product.Equity;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.StaticDataFilterElement;
import com.calypso.tk.refdata.StaticDataFilterInterface;
import com.calypso.tk.report.ReportRow;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.TransferArray;
import java.util.Optional;
import java.util.Vector;



public class IsProductCountryFromSpainStaticDataFilter implements StaticDataFilterInterface {


    public static final String IS_PRODUCT_COUNTRY_FROM_SPAIN = "Is Product Country From Spain";


	@Override
	public boolean fillTreeList(DSConnection con, TreeList tl) {
        Vector<String> nodes = new Vector<>();
        nodes.add("Custom");
        tl.add(nodes, IS_PRODUCT_COUNTRY_FROM_SPAIN);
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
        v.add(IS_PRODUCT_COUNTRY_FROM_SPAIN);
	}


	@SuppressWarnings("rawtypes")
	@Override
	public Vector getTypeDomain(String attributeName) {
        Vector<String> v = new Vector<String>();
        if (attributeName.equals(IS_PRODUCT_COUNTRY_FROM_SPAIN)) {
            v.addElement(StaticDataFilterElement.S_IS);
        }
        return v;
	}


	@Override
	public Object getValue(Trade trade, LegalEntity le, String role, Product product, BOTransfer transfer,
			BOMessage message, TradeTransferRule rule, ReportRow reportRow, Task task, Account glAccount,
			CashFlow cashflow, HedgeRelationship relationship, String filterElement, StaticDataFilterElement element) {

		Trade newTrade = null;
		Long newTradeId = transfer.getTradeLongId();

		if(newTradeId == 0){
			TransferArray underlyings = getNettedBOTransferUnderlyings(Optional.of(transfer));
			if(underlyings!=null && underlyings.size()>0){
				newTradeId = underlyings.get(0).getTradeLongId();
			}
		}

		try {
			newTrade = DSConnection.getDefault().getRemoteTrade().getTrade(newTradeId);
			if(newTrade == null){
				return Boolean.FALSE;
			}
			Equity equity = (Equity) newTrade.getProduct();
			String country = equity.getCountry();
			return "SPAIN".equalsIgnoreCase(country) ? Boolean.TRUE : Boolean.FALSE;
		} catch (CalypsoServiceException e) {
			Log.error(this, "Could not retrieve trade.");
		}
		return Boolean.FALSE;
	}


	@Override
	public boolean isTradeNeeded(String attributeName) {
		return true;
	}


	public static TransferArray getNettedBOTransferUnderlyings(Optional<BOTransfer> nettedBoTransfer) {
		TransferArray underlyings = nettedBoTransfer.map(BOTransfer::getUnderlyingTransfers).orElseGet(TransferArray::new);
		if (Util.isEmpty(underlyings)) {
			try {
				underlyings = DSConnection.getDefault().getRemoteBO().getNettedTransfers(nettedBoTransfer.map(BOTransfer::getLongId).orElse((long)0));
			} catch (CalypsoServiceException exc) {
				Log.error(NettedBOTransferUtil.class, exc.getCause());
			}
		}
		return underlyings;
	}


}
