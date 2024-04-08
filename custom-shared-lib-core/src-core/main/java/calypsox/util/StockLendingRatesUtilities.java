package calypsox.util;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;

/**
 * Class with all necessary stuff to manage ELBE & KGR contracts files
 * 
 * @author David Porras Mart?nez
 */
public class StockLendingRatesUtilities {

	public static Map<Integer, ?> getSecuritiesFromList(List<Integer> securitiesList) {
		Set<Integer> securitiesSet = new HashSet<Integer>(securitiesList);
		try {
			return DSConnection.getDefault().getRemoteProduct().getProducts(securitiesSet);
		} catch (RemoteException e) {
			Log.error(StockLendingRatesUtilities.class, "Cannot get products from DB.", e);
		}
		return null;
	}
}
