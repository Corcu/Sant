package calypsox.engine.im;

import com.calypso.tk.service.DSConnection;

import calypsox.engine.im.export.SantInitialMarginExportEngine;
import calypsox.engine.im.importim.SantInitialMarginImportIMEngine;
import calypsox.engine.im.simulator.SantInitialMarginSimulatorEngine;

public class SantInitialMarginEngineFactory implements SantInitialMarginEngineConstants {

	public static SantInitialMarginBaseEngine getInitialMarginEngine(String type, DSConnection ds,
			String host, int port) {
		if (SantInitialMarginEngineConstants.EXPORTQEF_IM_ENGINE_NAME.equals(type)) {
			return new SantInitialMarginExportEngine(ds, host, port);
		} else if (SantInitialMarginEngineConstants.IMPORTIM_IM_ENGINE_NAME.equals(type)) {
			return new SantInitialMarginImportIMEngine(ds, host, port);
		} else if (SantInitialMarginEngineConstants.SIMULATOR_IM_ENGINE_NAME.equals(type)) {
			return new SantInitialMarginSimulatorEngine(ds, host, port);
		}

		return null;
	}
}
