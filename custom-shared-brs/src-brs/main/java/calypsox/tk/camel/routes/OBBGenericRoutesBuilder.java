package calypsox.tk.camel.routes;

import calypsox.tk.camel.processors.CPartenonProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * @author acd
 * Routes for obb generic messages
 */
public class OBBGenericRoutesBuilder extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:obbGenericMessages")
                .to("{{jms.type}}:{{output.queue.name}}")
                .transform(body().append("\n"))
                .toF("file:/calypso_interfaces/mic/?fileName=obbgeneric_sent_messages_${date:now:yyyyMMdd}.log&fileExist=Append");

        //Mensaje de vuelta de MIC
        from("{{jms.type}}:{{input.queue.name}}")
                .process(new CPartenonProcessor())
                .transform(body().append("\n"))
                .toF("file:/calypso_interfaces/mic/?fileName=obbgeneric_response_messages_${date:now:yyyyMMdd}.log&fileExist=Append");
    }
}
