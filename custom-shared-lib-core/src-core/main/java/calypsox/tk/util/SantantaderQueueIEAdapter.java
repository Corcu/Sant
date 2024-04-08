/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.util;

import javax.jms.Session;

/**
 * SantQueueIEAdapter abstract class defines the methods that should be
 * overridden for every IEAdapter in the CML importer.
 *
 * @author Adrian Anton
 */
public abstract class SantantaderQueueIEAdapter extends SantanderIEAdapter {

    /**
     * Gets the session used in the IEAdapter. Only for JMS Adapters.
     *
     * @return Session used in the IEAdapter.
     */
    public abstract Session getSession();

}
