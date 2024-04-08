package com.calypso.apps.trading;

import com.calypso.apps.util.AppUtil;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.refdata.Account;
import com.calypso.tk.refdata.SettleDeliveryInstruction;
import com.calypso.tk.service.DSConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class TradeTransferAgentWindowWrapper extends TradeTransferAgentWindow {

    public TradeTransferAgentWindowWrapper(boolean initDomain) {
        super(initDomain);
    }
    protected JComboBox<SettleDeliveryInstruction> getFromSDIChoice() {
        return fromSDIChoice;
    }

    protected JComboBox<SettleDeliveryInstruction> getToSDIChoice() {
        return toSDIChoice;
    }
    protected JTextField getAccountFromText() {
        return fromAccountText;
    }

    protected JTextField getAccountToText() {
        return toAccountText;
    }
}
