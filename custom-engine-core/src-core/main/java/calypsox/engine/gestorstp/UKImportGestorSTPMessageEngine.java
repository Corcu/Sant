package calypsox.engine.gestorstp;

import com.calypso.tk.service.DSConnection;

/**
 * 
 * @author xIS16412
 *
 */
public class UKImportGestorSTPMessageEngine extends ImportGestorSTPMessageEngine {


	public static final String ENGINE_NAME_UK = "SANT_UK_ImportGestorSTPMessageEngine";

	/**
	 * @param dsCon
	 * @param hostName
	 * @param port
	 */
	public UKImportGestorSTPMessageEngine(DSConnection dsCon, String hostName, int port) {
		super(dsCon, hostName, port);
	}

	/**
	 * Name of the engine that offers this service
	 */
	@Override
	public String getEngineName() {
		return ENGINE_NAME_UK;
	}

}
