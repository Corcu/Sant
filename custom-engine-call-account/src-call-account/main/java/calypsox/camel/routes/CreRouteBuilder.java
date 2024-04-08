package calypsox.camel.routes;

import calypsox.camel.processor.CreMicReturnProcessor;
import org.apache.camel.builder.RouteBuilder;

public class CreRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:creMessages")
        .to("{{jms.type}}:{{output.queue.name}}")
        .transform(body().append("\n"))
        .toF("file:/calypso_interfaces/mic/?fileName=mic_sent_messages_${date:now:yyyyMMdd}.log&fileExist=Append");

        //Mensaje de vuelta de MIC
        from("{{jms.type}}:{{input.queue.name}}")
                .process(new CreMicReturnProcessor())
                .transform(body().append("\n"))
                .toF("file:/calypso_interfaces/mic/?fileName=mic_response_messages_${date:now:yyyyMMdd}.log&fileExist=Append");


    }
}
