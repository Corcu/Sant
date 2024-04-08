package calypsox.tk.camel.routes;

import calypsox.tk.camel.processors.CPartenonProcessor;
import org.apache.camel.builder.RouteBuilder;

/**
 * @author acd
 * Routes for send and receibe Partenon ID from MIC
 */
public class CPartenonRoutesBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:partenonContractMessage")
                .to("{{jms.type}}:{{output.queue.name}}");

        //Mensaje de vuelta de MIC
        from("{{jms.type}}:{{input.queue.name}}").process(new CPartenonProcessor());
                //.transform(body().append("\n"))
                //.toF("file:/calypso_interfaces/mic/?fileName=partenon_response_messages_${date:now:yyyyMMdd}.log&fileExist=Append");
    }

}
