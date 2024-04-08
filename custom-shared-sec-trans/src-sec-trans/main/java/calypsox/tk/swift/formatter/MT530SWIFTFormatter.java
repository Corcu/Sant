package calypsox.tk.swift.formatter;

import calypsox.tk.swift.formatter.common.CustomSecuritySWIFTFormatter;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;


public class MT530SWIFTFormatter extends com.calypso.tk.swift.formatter.MT530SWIFTFormatter implements CustomSecuritySWIFTFormatter {
    @Override
    public String parseMESSAGE_ID(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        return customizeMessageIdentifier(super.parseMESSAGE_ID(message, trade, sender, rec, transferRules, transfer, dsCon));
    }

    @Override
    public String parseMESSAGE_LINKED_ID(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        return customizeMessageIdentifier(super.parseMESSAGE_LINKED_ID(message, trade, sender, rec, transferRules, transfer, dsCon));
    }
    
    @Override
    public String parsePREV_REF(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection dsCon) throws MessageFormatException {
    	return customizeMessageIdentifier(super.parsePREV_REF(message, trade, sender, rec, transferRules, transfer, format, dsCon));
    }
}
