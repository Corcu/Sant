/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import calypsox.ErrorCodeEnum;

import com.calypso.tk.core.AuditValue;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.event.PSEventEngineRequest;
import com.calypso.tk.service.DSConnection;

public class ScheduledTaskINVENTORY_CSVREPORT extends ScheduledTaskCSVREPORT {

	private static final String INVENTORY_ENGINE = "InventoryEngine";
	private static final String TRANSFER_ENGINE = "TransferEngine";

	// START OA 27/11/2013
	// Oracle recommendation : declare a serialVersionUID for each Serializable class in order to avoid
	// InvalidClassExceptions.
	// Please refer to Serializable javadoc for more details
	private static final long serialVersionUID = 354700458722L;

	// END OA OA 27/11/2013

	@Override
	public boolean process(DSConnection ds, PSConnection ps) {

		try {
			boolean hasStuckEvents = hasStuckEventsInEngines(true);
			if (hasStuckEvents && hasStuckEventsInEngines(false)) {
				ControlMErrorLogger.addError(ErrorCodeEnum.UndefinedException,
						"There are pending events in Inventory Engine.");
				return false;
			}
		} catch (RemoteException e) {
			ControlMErrorLogger.addError(ErrorCodeEnum.DataserverError,
					"Error checking pending events on InventoryEngine. " + e.getLocalizedMessage());
			Log.error(this, e); //sonar
			return false;
		}

		return super.process(ds, ps);
	}

	private boolean hasStuckEventsInEngines(boolean restartEngine) throws RemoteException {

		Hashtable<String, Integer> inventoryEngineEvcents = DSConnection.getDefault().getRemoteAccess()
				.getPendingProcessingCount(INVENTORY_ENGINE);
		Hashtable<String, Integer> transferEngineEvcents = DSConnection.getDefault().getRemoteAccess()
				.getPendingProcessingCount(TRANSFER_ENGINE);

		List<String> enginestoRestart = new ArrayList<String>();

		if (inventoryEngineEvcents.size() != 0) {
			enginestoRestart.add(INVENTORY_ENGINE);
		}
		if (transferEngineEvcents.size() != 0) {
			enginestoRestart.add(TRANSFER_ENGINE);
		}

		if (enginestoRestart.size() != 0) {
			if (restartEngine) {
				restartEngineAndWait(enginestoRestart);
			}
			return true;
		} else {
			return false;
		}
	}

	private void restartEngineAndWait(List<String> enginesToRestart) {
		for (String engineName : enginesToRestart) {
			PSEventEngineRequest ad = new PSEventEngineRequest();
			@SuppressWarnings("unused")
			Vector<AuditValue> avs = new Vector<AuditValue>();
			ad.setType(PSEventEngineRequest.REQUEST_RESTART);
			ad.setMessage(engineName);
			try {
				PSConnection.getCurrent().publish(ad);
			} catch (Exception e) {
				Log.error(ScheduledTaskINVENTORY_CSVREPORT.class, e);
			}
		}

		try {
			TimeUnit.MINUTES.sleep(4);
		} catch (InterruptedException e) {
			Log.error(ScheduledTaskINVENTORY_CSVREPORT.class, e);
		}

	}
}
