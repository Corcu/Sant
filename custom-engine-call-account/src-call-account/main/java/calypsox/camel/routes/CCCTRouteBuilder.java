package calypsox.camel.routes;

import calypsox.camel.processor.CreCCCTReturnProcessor;
import calypsox.camel.processor.CreMicReturnProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import javax.jms.JMSException;

public class CCCTRouteBuilder extends RouteBuilder {

    private String inputqueue = "";
    private String outputqueue = "";

    public CCCTRouteBuilder(String input, String output) {
        this.inputqueue = input;
        this.outputqueue = output;
    }

    @Override
    public void configure() throws Exception {
       // removed and use workaround in engine to send message without JMS header
        // from("direct:ccctMessages")
       //         .to("mq:" + this.outputqueue)
       //         .transform(body().append("\n"))
       //         .toF("file:/calypso_interfaces/mic/?fileName=ccct_sent_messages_${date:now:yyyyMMdd}.log&fileExist=Append");

        from("mq:" + this.inputqueue)
                .process(new CreCCCTReturnProcessor())
                .transform(body().append("\n"))
                .toF("file:/calypso_interfaces/mic/?fileName=ccct_response_messages_${date:now:yyyyMMdd}.log&fileExist=Append");
    }
}
