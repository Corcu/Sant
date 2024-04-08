package calypsox.tk.bo.document;

import com.calypso.tk.core.Util;

/**
 * @author aalonsop
 */
public abstract class GatewayAbstractGestorSTPDocumentSender extends GatewayMQDocumentSender {

    /**
     * CR LF
     */
    private static final String GESTOR_STP_EOL = "\r\n";
    /**
     * LF
     */
    private static final String UNIX_EOL = "\n";

    /**
     * concrete classes use this constructor to indicate the adapterType
     *
     * @param adapterType adapter type
     */
    protected GatewayAbstractGestorSTPDocumentSender(String adapterType) {
        super(adapterType);
    }

    /**
     * @param originalMessage
     * @return SwiftMessage with CRLF EOL format. As GestorSTP accepts.
     */
    @Override
    protected String validateAndConverEOLFormat(String originalMessage) {
        String fixedEOLMessage = originalMessage;
        if (isUnixEOLString(originalMessage)) {
            fixedEOLMessage = originalMessage.replace(UNIX_EOL, GESTOR_STP_EOL);
        }
        return fixedEOLMessage;
    }

    /**
     * @param inputString
     * @return true if String EOL is \n instead of \r\n
     */
    private boolean isUnixEOLString(String inputString) {
        return !Util.isEmpty(inputString) && !inputString.contains(GESTOR_STP_EOL) && inputString.contains(UNIX_EOL);
    }
}
