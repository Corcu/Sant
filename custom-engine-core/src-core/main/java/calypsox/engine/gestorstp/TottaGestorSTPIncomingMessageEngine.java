/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.engine.gestorstp;

import com.calypso.tk.service.DSConnection;

/**
 * This class implements GestorSTPIncomingMessageEngine for processing MT568,
 * MT569, MT900/MT910 for Chile.
 *
 * @author Guillermo Solano
 * @version 1.0
 * @date 03/07/2017
 */
public class TottaGestorSTPIncomingMessageEngine extends GestorSTPIncomingMessageEngine {

    public static String ENGINE_NAME_TOTTA = "SANT_TOTTA_GestorSTPIncomingMessageEngine";

    /**
     * @param dsCon
     * @param hostName
     * @param port
     */
    public TottaGestorSTPIncomingMessageEngine(DSConnection dsCon, String hostName, int port) {
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
