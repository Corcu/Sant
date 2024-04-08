package calypsox.tk.service.scheduledtask;

import com.calypso.tk.service.CalypsoMonitorableServer;

public abstract interface RemoteScheduledTaskSQL extends CalypsoMonitorableServer {

	public static final String SERVER_NAME = "RemoteScheduledTaskSQL";

	void updatePosition(String dateType, String positionType, String positionClass, String mccId, String productId);

}
