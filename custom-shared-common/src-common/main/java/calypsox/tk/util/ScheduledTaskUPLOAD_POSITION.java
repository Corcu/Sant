package calypsox.tk.util;

import java.util.Hashtable;
import java.util.Vector;

import com.calypso.tk.entitlement.DataEntitlementCheckProxy;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.marketdata.MarketDataEntitlementController;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.DataServer;
import com.calypso.tk.util.BOPositionConstants;
import com.calypso.tk.util.ScheduledTask;

import calypsox.tk.service.scheduledtask.RemoteScheduledTaskSQL;

/**
 * @author jailson.viana
 */
public class ScheduledTaskUPLOAD_POSITION extends ScheduledTask {

	public static final String POSITION_TYPE = "Position Type";
	public static final String DATE_TYPE = "Date Type";
	public static final String CLASS = "Class";
	public static final String MCC_ID = "MCC ID";
	public static final String PRODUCT_ID = "Product Id";
	
	@Override
	public boolean process(DSConnection ds, PSConnection ps) {
		boolean res = true;
		
		String dateType = getAttribute(DATE_TYPE);
		String positionType = getAttribute(POSITION_TYPE);
		String positionClass = getAttribute(CLASS);
		String mccId = getAttribute(MCC_ID);
		String isin = getAttribute(PRODUCT_ID);
		
		RemoteScheduledTaskSQL service = (RemoteScheduledTaskSQL) getDSConnection().getRMIService("RemoteScheduledTaskSQL",
				RemoteScheduledTaskSQL.class);
		
		if ((!DataServer._isDataServer)
				&& (MarketDataEntitlementController.getDefault().needToPerformEntitlementCheck())) {
			service = DataEntitlementCheckProxy.newInstance(service);
		}
		
		service.updatePosition(dateType, positionType, positionClass, mccId, isin);

		return res;
	}

	@Override
	public Vector getDomainAttributes() {
		final Vector<String> result = super.getDomainAttributes();
		result.add(POSITION_TYPE);
		result.add(DATE_TYPE);
		result.add(CLASS);
		result.add(MCC_ID);
		result.add(PRODUCT_ID);

		return result;
	}

	@Override
	public Vector<String> getAttributeDomain(String attr, Hashtable<String, String> currentAttr) {

		Vector<String> v = new Vector<String>();

		if (attr.equals(DATE_TYPE)) {
			return BOPositionConstants.getPositionDateDomain();

		} else if (attr.equals(POSITION_TYPE)) {
			v.addElement(" ");
			v.addAll(BOPositionConstants.getPositionTypeDomain());
			return v;

		} else if (attr.equals(CLASS)) {
			return BOPositionConstants.getPositionClassDomain();
		}
		return v;
	}

	@Override
	public String getTaskInformation() {
		return "";
	}

}
