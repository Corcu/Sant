package calypsox.tk.bo;

import calypsox.tk.confirmation.BRSConfirmMsgBuilder;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * @author aalonsop
 */
public class CALYPSOCONFIRMPerformanceSwapMessageFormatter extends MessageFormatter {

    public String parseCALYPSOCONFIRM_XML(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                          BOTransfer transfer, DSConnection dsConn) {
        return new BRSConfirmMsgBuilder(message, transfer, trade).build().toString();
    }
}
