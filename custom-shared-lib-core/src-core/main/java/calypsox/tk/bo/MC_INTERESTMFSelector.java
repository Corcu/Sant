/*
 *
 * Copyright (c) ISBAN: Ingenieria de Software Bancario, S.L.
 * All rights reserved.
 *
 */
package calypsox.tk.bo;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.MFSelector;
import com.calypso.tk.bo.MessageFormatter;

public class MC_INTERESTMFSelector implements MFSelector {

    @Override
    public MessageFormatter getMessageFormatter(BOMessage message) {
        if (message.getMessageType().equals("MC_INTEREST")) {
            return new MC_INTERESTCustomerTransferMessageFormatter();
        }
        return null;
    }

}
