package calypsox.tk.swift.formatter;

import calypsox.tk.swift.formatter.common.CustomSwiftTagHandler;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

public class MT541BILATSWIFTFormatter extends MT541SWIFTFormatter{

    /**
     * Get the Mx BANKCODESP from trade keyword
     *
     * @param message       the BOMessage
     * @param trade         the Trade
     * @param sender        the Message Sender
     * @param rec           the Message Receiver
     * @param transferRules the transfer rules vector
     * @param transfer      the BOTransfer
     * @param dsCon         the Data Server connection
     * @return the COMM Mx BANKCODESP
     */
    public String parseMX_REFERENCE(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        return new CustomSwiftTagHandler().customizeBankCodeOfSpain(trade);
    }

    /**
     * Return true if Mx BANKCODESP is not empty
     *
     * @param message       the BOMessage
     * @param trade         the Trade
     * @param sender        the Message Sender
     * @param rec           the Message Receiver
     * @param transferRules the transfer rules vector
     * @param transfer      the BOTransfer
     * @param con           the Data Server connection
     * @return true if Mx BANKCODESP not empty
     */
    public boolean isMxReference(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection con) throws MessageFormatException {
        return new CustomSwiftTagHandler().isMxReference(trade);
    }

}
