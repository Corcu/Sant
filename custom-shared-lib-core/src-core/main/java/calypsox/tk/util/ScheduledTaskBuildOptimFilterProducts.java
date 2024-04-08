/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 * 
 */
package calypsox.tk.util;

import static com.santander.collateral.constants.LoggingConstants.LOG_CATEGORY_SCHEDULED_TASK;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import calypsox.util.SantReportingUtil;

import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSConnection;
import com.calypso.tk.refdata.OptimizationConfiguration;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ScheduledTask;

public class ScheduledTaskBuildOptimFilterProducts extends ScheduledTask {

	private static final long serialVersionUID = 1L;
	private static final String OPTIM_CONFIG_LIST = "Optim Config List";

	@Override
	public Vector<String> getDomainAttributes() {
		final Vector<String> attr = new Vector<String>();
		attr.add(OPTIM_CONFIG_LIST);
		return attr;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Vector<String> getAttributeDomain(final String attribute, final Hashtable hashtable) {
		Vector<String> vector = new Vector<String>();
		if (OPTIM_CONFIG_LIST.equals(attribute)) {

			try {
				List<String> loaded = ServiceRegistry.getDefault().getCollateralDataServer()
						.loadAllOptimizationConfigurationNames();
				if (!Util.isEmpty(loaded)) {
					vector.addAll(loaded);
				}
			} catch (Exception e) {
				Log.error(this, e);
			}
		}
		return vector;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean isValidInput(final Vector messages) {
		final String seperator = getAttribute(OPTIM_CONFIG_LIST);
		if (Util.isEmpty(seperator)) {
			messages.addElement(" Optimization configs cannot be empty");
			return false;
		}
		return true;
	}

	@Override
	public boolean process(final DSConnection conn, final PSConnection connPS) {
		boolean result = false;

		try {
			String optimConfigName = getAttribute(OPTIM_CONFIG_LIST);
			OptimizationConfiguration optimizationConfiguration = ServiceRegistry.getDefault()
					.getCollateralDataServer().loadOptimizationConfiguration(optimConfigName);
			SantReportingUtil.getSantReportingService(getDSConnection()).buildAndCacheFilterProdIds(
					optimizationConfiguration, null, null);

			result = true;
		} catch (Exception exc) {
			Log.error(LOG_CATEGORY_SCHEDULED_TASK, exc);
		}
		return result;
	}

	@Override
	public String getTaskInformation() {
		return "Builds Static Data Filters, and all the product ids that are accepted by those SDF. "
				+ "Then save them into a tabel so those results can be used ny OptimumReport.";
	}

}
