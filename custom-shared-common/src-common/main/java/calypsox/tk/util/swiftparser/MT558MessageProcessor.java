package calypsox.tk.util.swiftparser;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.swift.SwiftFieldMessage;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.MessageParseException;

import java.util.Vector;

/**
 * MT558MessageProcessor
 *
 * @author Ruben Garcia
 */
public class MT558MessageProcessor extends com.calypso.tk.util.swiftparser.MT558MessageProcessor {

    public static final String CINS_VALUE = "Cins_Value";

    public static final String EXQR = "Execution Requested Date";

    @Override
    protected void setMessageAttributes(Trade trade, BOMessage message, SwiftMessage swiftMessage, BOMessage indexedMessage,
                                        Vector errors, DSConnection ds) throws MessageParseException {
        super.setMessageAttributes(trade, message, swiftMessage, indexedMessage, errors, ds);
        if (swiftMessage != null && message != null) {
            SwiftFieldMessage field = swiftMessage.getSwiftField(":70E:", "CINS//", null);
            if (field != null) {
                Object value = field.parse(swiftMessage.getType());
                if (value instanceof String) {
                    message.setAttribute(CINS_VALUE, (String) value);
                }
            }
            field = swiftMessage.getSwiftField(":98A:", "EXRQ//", null);
            if (field != null) {
                Object value = field.parse(swiftMessage.getType());
                if (value instanceof JDate) {
                    message.setAttribute(EXQR, Util.idateToString((JDate) value));
                }
            }
        }
    }
}
