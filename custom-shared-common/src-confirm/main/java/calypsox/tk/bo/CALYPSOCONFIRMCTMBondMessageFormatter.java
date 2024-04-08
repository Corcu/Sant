package calypsox.tk.bo;

import calypsox.tk.confirmation.BondConfirmationCTMMsgBuilder;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

public class CALYPSOCONFIRMCTMBondMessageFormatter extends CALYPSOCONFIRMBondMessageFormatter {


    @Override
    public String parseCALYPSOCONFIRM_XML(BOMessage message, Trade trade, LEContact po, LEContact cp, Vector paramVector,
                                          BOTransfer transfer, DSConnection dsConn) {
        return new BondConfirmationCTMMsgBuilder(message, transfer, trade).build().toString();
    }
}
