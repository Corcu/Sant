package calypsox.tk.util.swiftparser;

import calypsox.tk.util.swiftparser.ccp.BilateralIncomingSwiftMatcher;
import calypsox.tk.util.swiftparser.util.SwiftMessageProcessorUtil;
import com.calypso.analytics.Util;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;
import com.calypso.tk.util.SwiftParserUtil;

import java.util.Vector;

import static calypsox.tk.util.swiftparser.util.SwiftMessageProcessorUtil.toDateStr;

/**
 * MT541MessageProcessor
 *
 * @author Ruben Garcia
 */
public class MT541MessageProcessor extends com.calypso.tk.util.swiftparser.MT541MessageProcessor {

    public static final String PROCESSING_STATUS = "Processing_Status";

    @Override
    protected void setGenericAttributes(Trade trade, BOMessage message, SwiftMessage swiftMessage, BOTransfer indexedTransfer, BOMessage indexedMessage, DSConnection ds) throws MessageParseException {
        super.setGenericAttributes(trade, message, swiftMessage, indexedTransfer, indexedMessage, ds);
        if(message != null && swiftMessage != null && BilateralIncomingSwiftMatcher.isECCNettingProposal(swiftMessage)){
            message.setAttribute(BilateralIncomingSwiftMatcher.IS_NETTING_ECC_PROPOSAL, "true");
        }
        if (message!=null  && swiftMessage != null) {
            SwiftFieldMessage field =swiftMessage.getSwiftField(swiftMessage.getFields(), ":22F:", "SETR", null);
            if (field!=null) {
                String processingStatus = Util.isEmpty( field.getValue()) ?null:field.getValue().replace(":SETR/", "");
                if (processingStatus!=null && processingStatus.startsWith("/"))
                    processingStatus = processingStatus.substring(1);
                message.setAttribute(PROCESSING_STATUS, processingStatus );
            }

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

    @Override
    protected void beforeSave(BOMessage message, Object indexedObject, Trade indexedTrade, BOMessage indexedMessage, BOTransfer indexedTransfer, SwiftMessage swiftMessage, boolean indexed, boolean matched, Vector errors, DSConnection ds, Object dbCon) {
        super.beforeSave(message, indexedObject, indexedTrade, indexedMessage, indexedTransfer, swiftMessage, indexed, matched, errors, ds, dbCon);
        SwiftMessageProcessorUtil.updateBOMessageOriginalID(message, swiftMessage);
    }
}
