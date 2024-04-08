package calypsox.tk.swift.formatter;

import calypsox.tk.swift.formatter.common.CustomSecuritySWIFTFormatter;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.MessageFormatException;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.refdata.LEContact;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

public class MT548SWIFTFormatter extends com.calypso.tk.swift.formatter.MT548SWIFTFormatter implements CustomSecuritySWIFTFormatter {
    @Override
    public String parseMESSAGE_ID(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, DSConnection dsCon) {
        return customizeMessageIdentifier(super.parseMESSAGE_ID(message, trade, sender, rec, transferRules, transfer, dsCon));
    }

    @Override
    public String parseSTATUS(BOMessage message, Trade trade, LEContact sender, LEContact rec, Vector transferRules, BOTransfer transfer, String format, DSConnection dsCon) throws MessageFormatException {
        String xferProcessingStatus = transfer != null && !Util.isEmpty(transfer.getAttribute("ProcessingStatus")) ? transfer.getAttribute("ProcessingStatus") : null;
        if (!Util.isEmpty(xferProcessingStatus)) {
            String mappedValue = LocalCache.getDomainValueComment(dsCon, "XferAttributes.ProcessingStatus", xferProcessingStatus);
            if (!Util.isEmpty(mappedValue))
                return ":" + mappedValue;
        }
        return super.parseSTATUS(message, trade, sender, rec, transferRules, transfer, format, dsCon);
    }
}
