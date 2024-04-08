package calypsox.tk.bo.document;

import calypsox.camel.document.CamelBasedDocumentSender;
import calypsox.tk.camel.route.confirmation.CalypsoConfirmRouteBuilder;
import org.apache.camel.builder.RouteBuilder;

/**
 * @author aalonsop
 */
public class GatewayCALYPSOCONFIRMDocumentSender extends CamelBasedDocumentSender {

    @Override
    public RouteBuilder getCamelRouteBuilder() {
        return new CalypsoConfirmRouteBuilder();
    }
}
