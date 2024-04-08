package calypsox.tk.bo;

import calypsox.tk.confirmation.RepoBSBConfirmationMsgBuilder;
import calypsox.tk.confirmation.RepoConfirmationMsgBuilder;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatter;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.util.CollateralBasedUtil;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * @author aalonsop
 */
public class CALYPSOCONFIRMRepoMessageFormatter extends MessageFormatter {

    public String parseCALYPSOCONFIRM_XML(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                          BOTransfer transfer, DSConnection dsConn) {
        if(CollateralBasedUtil.isBSB(trade)){
            return new RepoBSBConfirmationMsgBuilder(message, transfer, trade).build().toString();
        }
        return new RepoConfirmationMsgBuilder(message, transfer, trade).build().toString();
    }
}
