package calypsox.camel.routes;

import calypsox.camel.processor.MedusaResProccesor;
import org.apache.camel.builder.RouteBuilder;

/**
 * @author acd
 */
public class MedusaRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:medusaMessages").to("{{jms.type}}:{{output.queue.name}}")
                .transform(body().append("\n"))
                .toF("file:/calypso_interfaces/medusa/?fileName=medusaMessagesSent_${date:now:yyyyMMdd}.log&fileExist=Append");

        from("{{jms.type}}:{{input.queue.name}}").process(new MedusaResProccesor())
                .transform(body().append("\n"))
                .toF("file:/calypso_interfaces/medusa/?fileName=medusaMessagesReceived_${date:now:yyyyMMdd}.log&fileExist=Append");;
    }
}
