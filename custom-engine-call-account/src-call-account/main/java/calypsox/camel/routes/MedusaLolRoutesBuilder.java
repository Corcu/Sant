package calypsox.camel.routes;

import calypsox.camel.processor.MedusaResProccesor;
import org.apache.camel.builder.RouteBuilder;

public class MedusaLolRoutesBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:medusaMessages").to("{{jms.type}}:topic:{{output.queue.name}}")
                .transform(body().append("\n"))
                .toF("file:/calypso_interfaces/medusa/?fileName=medusaMessagesSent_${date:now:yyyyMMdd}.log&fileExist=Append");

        from("medusajndi:{{medusa.input.queue.name}}").process(new MedusaResProccesor())
                .transform(body().append("\n"))
                .toF("file:/calypso_interfaces/medusa/?fileName=medusaMessagesReceived_${date:now:yyyyMMdd}.log&fileExist=Append");

    }
}