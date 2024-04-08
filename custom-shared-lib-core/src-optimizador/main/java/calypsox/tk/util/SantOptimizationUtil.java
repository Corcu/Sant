package calypsox.tk.util;

import calypsox.tk.optimization.service.RemoteSantOptimizationService;

import com.calypso.tk.service.DSConnection;

public class SantOptimizationUtil {

	public static RemoteSantOptimizationService getSantOptimizationService(final DSConnection ds) {
		return (RemoteSantOptimizationService) ds.getRMIService("baseSantOptimizationService",
				RemoteSantOptimizationService.class);
	}

}
