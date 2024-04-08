package calypsox.tk.util.swiftparser;

import calypsox.tk.util.swiftparser.util.SwiftMessageProcessorUtil;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;

import java.util.Vector;

/**
 * MT548MessageProcessor
 *
 * @author Ruben Garcia
 */
public class MT548MessageProcessor extends com.calypso.tk.util.swiftparser.MT548MessageProcessor{

    @Override
    protected void beforeSave(BOMessage message, Object indexedObject, Trade indexedTrade, BOMessage indexedMessage, BOTransfer indexedTransfer, SwiftMessage swiftMessage, boolean indexed, boolean matched, Vector errors, DSConnection ds, Object dbCon) {
        super.beforeSave(message, indexedObject, indexedTrade, indexedMessage, indexedTransfer, swiftMessage, indexed, matched, errors, ds, dbCon);
        SwiftMessageProcessorUtil.updateBOMessageOriginalID(message, swiftMessage);
    }

}
