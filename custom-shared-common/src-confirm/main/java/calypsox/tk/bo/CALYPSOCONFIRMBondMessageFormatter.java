package calypsox.tk.bo;

import calypsox.tk.confirmation.BondFwdConfirmationMsgBuilder;
import calypsox.tk.confirmation.BondSpotConfirmationMsgBuilder;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;
import java.util.Vector;

/**
 * @author dmenendd
 */
public class CALYPSOCONFIRMBondMessageFormatter extends MessageFormatter implements BondForwardFilterAdapter {


    public String parseCALYPSOCONFIRM_XML(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                          BOTransfer transfer, DSConnection dsConn) {
        if(isBondForward(trade)) {
            return new BondFwdConfirmationMsgBuilder(message, transfer, trade).build().toString();
        }else{
            return new BondSpotConfirmationMsgBuilder(message,transfer,trade).build().toString();
        }
    }
}
