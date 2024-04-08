/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import calypsox.tk.bo.document.GatewayEUROCLEARDocumentSender;

// CAL_INT_017

/**
 * This class defines the name of the adapter used to send messages to
 * GestorSTP.
 *
 * @author Carlos Cejudo <c.cejudo.bermejo@accenture.com>
 */
public class SantanderJMSQueueGestorSTPIEAdapterConfig extends
        SantanderJMSQueueIEAdapterConfig {

    /**
     * Instantiates a new santander routing queue gestor stpie adapter config.
     */
    public SantanderJMSQueueGestorSTPIEAdapterConfig() {
        super(GatewayEUROCLEARDocumentSender.ADAPTER_TYPE);
    }
}
