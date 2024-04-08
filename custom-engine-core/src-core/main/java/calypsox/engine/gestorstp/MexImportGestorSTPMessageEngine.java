package calypsox.engine.gestorstp;

import com.calypso.tk.service.DSConnection;

/**
 * ACK/NACK to the MT527 message
 * @author xIS16412
 *
 */
public class MexImportGestorSTPMessageEngine extends ImportGestorSTPMessageEngine {


	public static final String ENGINE_NAME_MEX = "SANT_MEX_ImportGestorSTPMessageEngine";

	/**
	 * @param dsCon
	 * @param hostName
	 * @param port
	 */
	public MexImportGestorSTPMessageEngine(DSConnection dsCon, String hostName, int port) {
		super(dsCon, hostName, port);
	}

	/**
	 * Name of the engine that offers this service
	 */
	@Override
	public String getEngineName() {
		return ENGINE_NAME_MEX;
	}

}
