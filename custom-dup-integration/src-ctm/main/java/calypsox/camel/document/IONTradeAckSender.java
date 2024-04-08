package calypsox.camel.document;

import calypsox.ctm.camel.CTMRouteBuilder;
import org.apache.camel.builder.RouteBuilder;

/**
 * @author aalonsop
 */
public class IONTradeAckSender extends CamelBasedDocumentSender {



    public void sendAck(String ionAckXml) {
        if(isOnline()) {
            sendMessage(ionAckXml);
        }
    }

    @Override
    public RouteBuilder getCamelRouteBuilder() {
        return new CTMRouteBuilder();
    }
}
