package calypsox.tk.util.swiftparser;

import com.calypso.analytics.Util;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;

import static calypsox.tk.util.swiftparser.util.SwiftMessageProcessorUtil.toDateStr;

public class MT540MessageProcessor extends com.calypso.tk.util.swiftparser.MT540MessageProcessor {

    public static final String PROCESSING_STATUS = "Processing_Status";

    @Override
    protected void setGenericAttributes(Trade trade, BOMessage message, SwiftMessage swiftMessage, BOTransfer indexedTransfer, BOMessage indexedMessage, DSConnection ds) throws MessageParseException {
        super.setGenericAttributes(trade, message, swiftMessage, indexedTransfer, indexedMessage, ds);
        if (message != null && swiftMessage != null) {
            SwiftFieldMessage field = swiftMessage.getSwiftField(swiftMessage.getFields(), ":22F:", "SETR", null);
            if (field != null) {
                String processingStatus = Util.isEmpty(field.getValue()) ? null : field.getValue().replace(":SETR/", "");
                message.setAttribute(PROCESSING_STATUS, processingStatus);

                field =swiftMessage.getSwiftField(swiftMessage.getFields(), ":98A:", "SETT", null);
                if (field!=null) {
                    message.setAttribute("SettleDate",  toDateStr(field.getValue()));
                }
                field =swiftMessage.getSwiftField(swiftMessage.getFields(), ":98A:", "TRAD", null);
                if (field!=null) {
                    message.setAttribute("TradeDate",  toDateStr(field.getValue()) );
                }
            }
        }
    }
}
