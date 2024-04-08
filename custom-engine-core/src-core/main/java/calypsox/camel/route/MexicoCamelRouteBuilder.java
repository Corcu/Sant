package calypsox.camel.route;

import calypsox.camel.processor.MexicoDataUploaderProcessor;
import org.apache.camel.builder.RouteBuilder;

public class MexicoCamelRouteBuilder extends RouteBuilder {

    public static final String SEND_ROUTE_NAME = "direct:mexAckMessage";

    @Override
    public void configure() throws Exception {
        from(SEND_ROUTE_NAME).to("{{jms.type}}:{{output.queue.name}}");
                //.transform(body().append("\n"))
                //.toF("file:/calypso_interfaces/medusa/?fileName=medusaMessagesSent_${date:now:yyyyMMdd}.log&fileExist=Append");

        //Mensaje recibido de Mex
        from("{{jms.type}}:{{input.queue.name}}")
                .process(new MexicoDataUploaderProcessor());
                //.transform(body().append("\n"))
                //.toF("file:/calypso_interfaces/mic/?fileName=mic_response_messages_${date:now:yyyyMMdd}.log&fileExist=Append");
    }
}
