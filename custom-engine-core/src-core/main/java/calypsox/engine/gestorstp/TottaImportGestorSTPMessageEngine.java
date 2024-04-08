package calypsox.engine.gestorstp;

import com.calypso.tk.service.DSConnection;

/**
 * ACK/NACK to the MT527 message for Chile
 *
 * @author xIS16412
 * @date 03/07/2017
 */
public class TottaImportGestorSTPMessageEngine extends ImportGestorSTPMessageEngine {


    public static final String ENGINE_NAME_TOTTA = "SANT_TOTTA_ImportGestorSTPMessageEngine";

    /**
     * @param dsCon
     * @param hostName
     * @param port
     */
    public TottaImportGestorSTPMessageEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }

    /**
     * Name of the engine that offers this service
     */
    @Override
    public String getEngineName() {
        return ENGINE_NAME_TOTTA;
    }

}
