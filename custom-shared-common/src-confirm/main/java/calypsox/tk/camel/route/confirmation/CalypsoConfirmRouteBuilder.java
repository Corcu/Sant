package calypsox.tk.camel.route.confirmation;

import calypsox.camel.CamelContextBean;
import calypsox.camel.log.CamelLogBean;
import calypsox.tk.camel.AbstractCamelRouteBuilder;
import calypsox.tk.camel.processor.confirmation.CalypsoConfirmationResponseProcessor;

/**
 * @author aalonsop
 */
public class CalypsoConfirmRouteBuilder  extends AbstractCamelRouteBuilder {

    @Override
    public void configure() throws Exception {
        String logFilePath = "/calypso_interfaces/brs/confirmation";
        from(CamelContextBean.getDefaultSendRouteName())
                .routeId(buildRouteName("SENT"))
                .choice()
                    .when(body().contains("Fixed Income Spot"))
                        .to("bondtibco:{{bond.output.queue.name}}")
                .otherwise()
                    .to("{{jms.type}}:{{output.queue.name}}")
                .end()
                .bean(CamelLogBean.getNewInstance(),"logSystem")
                .toF("file:" + logFilePath + "/?fileName=calypsoToCalConfirmationSent_${date:now:yyyyMMdd}.log&fileExist=Append");

        from("{{jms.type}}:{{input.queue.name}}")
                .routeId(buildRouteName("RECEIVE"))
                .bean(CamelLogBean.getNewInstance(),"logSystem")
                .toF("file:" + logFilePath + "/?fileName=calConfirmationToCalypsoReceived_${date:now:yyyyMMdd}.log&fileExist=Append")
                .process(new CalypsoConfirmationResponseProcessor());
    }
}
