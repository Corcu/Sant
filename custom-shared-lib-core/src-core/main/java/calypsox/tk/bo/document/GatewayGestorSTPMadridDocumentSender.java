/*
 *
 * Copyright (c) 2011 Banco Santander
 * Author: Jimmy Ventura (jimmy.ventura@siag-management.com)
 * All rights reserved.
 *
 */
package calypsox.tk.bo.document;

/**
 * Document sender to Gestor STP via JMS
 */
public class GatewayGestorSTPMadridDocumentSender extends GatewayAbstractGestorSTPDocumentSender {
    /**
     * Adapter type
     */
    public static final String ADAPTER_TYPE = "gstp.out";
    private static final String CONFIG_FILE_NAME = "gstp.connection.properties";
    private static final String CONFIG_NAME_GESTOR_STP = "SantanderJMSQueueGestorSTP";

    /**
     * Default Constructor
     */
    public GatewayGestorSTPMadridDocumentSender() {
        super(ADAPTER_TYPE);
    }

    @Override
    public String getConfigFileName() {
        return CONFIG_FILE_NAME;
    }

    @Override
    public String getConfigName() {
        return CONFIG_NAME_GESTOR_STP;
    }

}
