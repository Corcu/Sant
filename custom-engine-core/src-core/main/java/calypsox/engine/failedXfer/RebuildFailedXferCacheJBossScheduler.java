package calypsox.engine.failedXfer;

import java.rmi.RemoteException;
import java.util.List;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;

import calypsox.tk.bo.CustomFallidasfallidasReportClientCacheAdapter;
import calypsox.tk.bo.ReporRowCacheAdapterInterface;
import calypsox.tk.event.PSEventResetCustomBOCache;

@Singleton
@Startup
public class RebuildFailedXferCacheJBossScheduler {
	private static final String CALYPSO_ENGINE_MANAGER_CONFIG = "calypso.engine.manager.config";
	private static final String DVENGINESERVERWS = "DVEngineServerWS";

	@Schedule(dayOfWeek = "*", hour = "06", minute = "00", second = "00", persistent = true)
	public void rebuildCacheReport() {
		DSConnection dsConn = getDSConnection();
		if (dsConn != null) {
			Log.info(this, "Executing schedule to rebuild report caches");
			List<DomainValues.DomainValuesRow> engineServerNameDV = null;
			String engineServerName = System.getProperty(CALYPSO_ENGINE_MANAGER_CONFIG);
			try {
				engineServerNameDV = dsConn.getRemoteReferenceData().getDomainValuesRows(DVENGINESERVERWS);
				for (DomainValues.DomainValuesRow row : engineServerNameDV) {
					rebuildCacheFromDV(row, engineServerName, dsConn);
				}
			} catch (Exception e) {
				Log.error(this, "Error loading domain value to get Engine server configuration.", e);
			}
		}
	}

	public void rebuildCacheFromDV(DomainValues.DomainValuesRow row, String engineServerName, DSConnection dsConn) {
		String reportType = null;
		String reportTemplate = null;
		String value = row.getValue();
		String comment = row.getComment();
		if (!Util.isEmpty(engineServerName) && !Util.isEmpty(comment)
				&& comment.trim().equalsIgnoreCase(engineServerName.trim()) && !Util.isEmpty(value)) {
			String[] dvArr = value.split("_");
			if (dvArr.length == 2) {
				reportType = dvArr[0];
				reportTemplate = dvArr[1];
				if(getCacheAdapter(reportType, reportTemplate)!=null) {
					PSEventResetCustomBOCache request = new PSEventResetCustomBOCache(reportType, reportTemplate,true); 
					try {
						dsConn.getRemoteTrade().saveAndPublish(request);
					} catch (CalypsoServiceException e) {
						Log.error(this, "Error saving event to reset custom BOCaches.", e);
					}
				}
			}
		}
	}

	public DSConnection getDSConnection() {
		DSConnection dsConn = null;
		int retryNumber = 0;
		try {
			dsConn = DSConnection.getDefault();
			if (dsConn != null) {
				while (retryNumber < 25 && dsConn.getRemoteAccess().getServerCurrentDatetime() == null) {
					retryNumber++;
					dsConn.reconnect();
					Thread.sleep(1000);
				}
			}
		} catch (RemoteException | ConnectException e1) {
			Log.error(CustomFallidasfallidasReportClientCacheAdapter.class,
					"Error getting DSConnection. BOCache will not be updated.", e1);
			dsConn = null;
		} catch (InterruptedException e) {
			Log.error(CustomFallidasfallidasReportClientCacheAdapter.class,
					"Error getting DSConnection. Killed Thread...", e);
			Thread.currentThread().interrupt();
		}
		return dsConn;
	}
	public ReporRowCacheAdapterInterface getCacheAdapter(String reportType, String reportTemplate) {
		ReporRowCacheAdapterInterface customReportRowFallidasClientCacheAdapter = null;

		try {
			customReportRowFallidasClientCacheAdapter = (ReporRowCacheAdapterInterface) Class
					.forName("calypsox.tk.bo." + reportType + reportTemplate + "ClientCacheAdapter").newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			Log.error(this, "Error instantiating cache class.");
		}
		return customReportRowFallidasClientCacheAdapter;
	}
}
