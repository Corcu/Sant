package calypsox.tk.util.swiftparser;

import calypsox.tk.util.swiftparser.util.SwiftMessageProcessorUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;

import java.util.Vector;

/**
 * MT546MessageProcessor
 *
 * @author Ruben Garcia
 */
public class MT546MessageProcessor extends com.calypso.tk.util.swiftparser.MT546MessageProcessor{

    @Override
    protected void beforeSave(BOMessage message, Object indexedObject, Trade indexedTrade, BOMessage indexedMessage, BOTransfer indexedTransfer, SwiftMessage swiftMessage, boolean indexed, boolean matched, Vector errors, DSConnection ds, Object dbCon) {
        super.beforeSave(message, indexedObject, indexedTrade, indexedMessage, indexedTransfer, swiftMessage, indexed, matched, errors, ds, dbCon);
        SwiftMessageProcessorUtil.updateBOMessageOriginalID(message, swiftMessage);
    }

    @Override
    protected void setGenericAttributes(Trade trade, BOMessage message, SwiftMessage swiftMessage, BOTransfer indexedTransfer, BOMessage indexedMessage, DSConnection ds) throws MessageParseException {
        super.setGenericAttributes(trade, message, swiftMessage, indexedTransfer, indexedMessage, ds);
        super.setDateAttribute(message, swiftMessage, "ESET", "ESET_SettleDate");
    }
}
