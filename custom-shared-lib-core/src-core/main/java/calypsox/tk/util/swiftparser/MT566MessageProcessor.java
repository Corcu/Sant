package calypsox.tk.util.swiftparser;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.Trade;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.MessageParseException;

import java.util.Vector;

public class MT566MessageProcessor extends com.calypso.tk.util.swiftparser.MT566MessageProcessor{

    public static final String positionConciMT566 = "positionConciliationFieldMT566";
    public static final String positionFromMT566 = "positionFromMT566";

    protected void setInternalAttributes(Trade trade, BOMessage message, SwiftMessage swiftMessage, BOMessage indexedMessage, BOTransfer indexedTransfer, Vector errors, DSConnection ds) throws MessageParseException {
        if (LocalCache.getDomainValues(ds, positionConciMT566).size()>0){
            String tag = LocalCache.getDomainValues(ds, positionConciMT566).get(0);
            String qualifier = LocalCache.getDomainValueComment(DSConnection.getDefault(), positionConciMT566, tag);
            SwiftFieldMessage s = swiftMessage.getSwiftField(tag, qualifier, null);
            if (s != null && s.getValue()!=null){
                String value = s.getValue().substring(12,s.getValue().length()-1);
                message.setAttribute(positionFromMT566, value);
            }
        }
        super.setInternalAttributes(trade, message, swiftMessage, indexedMessage, indexedTransfer, errors, ds);
    }
}
