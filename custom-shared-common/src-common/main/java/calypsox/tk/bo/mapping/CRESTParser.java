package calypsox.tk.bo.mapping;

import calypsox.tk.bo.swift.CRESTMessage;
import com.calypso.tk.bo.ExternalMessage;
import com.calypso.tk.bo.ExternalMessageParser;
import com.calypso.tk.bo.swift.SwiftMessage;
import com.calypso.tk.util.MessageParseException;

@SuppressWarnings("unused")
public class CRESTParser extends SwiftMessage implements ExternalMessageParser {

    @Override
    public ExternalMessage readExternal(String msgString, String gateway) throws MessageParseException {
        CRESTMessage parsedMessage = new CRESTMessage();

        if ("CREST".equalsIgnoreCase(gateway) || "SWIFT".equalsIgnoreCase(gateway)) {
            try {
                parsedMessage.parseCRESTMessage(msgString);
                return parsedMessage;
            } catch (Exception var5) {
                throw new MessageParseException("Parsing Failed", var5);
            }
        }

        return parsedMessage;
    }

    @Override
    public void writeExternal(ExternalMessage msg, String gateway) {
        throw new UnsupportedOperationException("writeExternal not implemented in " + this.getClass().getName());
    }


}