package calypsox.camel.processor;

import com.calypso.tk.core.Log;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class CamelTestProcessor implements Processor {

    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);

        Log.system("RESPONSE MESSAGE", body);
        System.out.println(body);
    }
}
