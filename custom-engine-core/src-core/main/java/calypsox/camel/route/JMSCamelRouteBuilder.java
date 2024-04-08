package calypsox.camel.route;

import calypsox.camel.publisher.JMSMessagePublisher;
import org.apache.camel.builder.RouteBuilder;

/**
 * @author aalonsop
 */
public class JMSCamelRouteBuilder extends RouteBuilder {

    /**
     *
     */
    @Override
    public void configure() {
        from("activemq:queue:CAMEL.IN").autoStartup(true).routeId("test")
                .bean(new JMSMessagePublisher());
    }

}

