package calypsox.camel.routes;

import org.apache.camel.builder.RouteBuilder;

public class CreRouteKafkaBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:micKafkaMessage")
                .to("{{jms.type}}:{{output.queue.name}}")
                .transform(body().append("\n"))
                .toF("file:/calypso_interfaces/mic/?fileName=mic_sent_messagesKafka_${date:now:yyyyMMdd}.log&fileExist=Append");

        //Mensaje de vuelta de MIC
        /*
        from("{{jms.type}}:{{input.queue.name}}")
                .process(new CreMicReturnProcessor())
                .transform(body().append("\n"))
                .toF("file:/calypso_interfaces/mic/?fileName=mic_response_messagesKafa_${date:now:yyyyMMdd}.log&fileExist=Append");
         */

    }
}
