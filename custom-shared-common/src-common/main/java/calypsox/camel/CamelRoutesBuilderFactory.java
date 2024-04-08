package calypsox.camel;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

import java.util.Properties;

/**
 * @author acd
 */
public class CamelRoutesBuilderFactory {

    private static final String JMS_INPUT = "input.queue.name";
    private static final String JMS_OUTPUT = "output.queue.name";
    private static final String JMS_TYPE = "jms.type";
    private static final String SEND_MESSAGE_ROUTE = "direct:sendMessage";

    private String queueIN;
    private String queueOUT;
    public String type;

    private Processor processor;

    private static CamelRoutesBuilderFactory instance = new CamelRoutesBuilderFactory();

    public static final CamelRoutesBuilderFactory getFactory() {
        if (instance == null) {
            instance = new CamelRoutesBuilderFactory();
        }
        return instance;
    }

    public void setProcessor (Processor processor){
        this.processor = processor;
    }


    public RouteBuilder getRouteBuilder(Properties properties) {
        initQueues(properties);
        return getRoutes();
    }

    private void initQueues(Properties properties) {
        if (null != properties) {
            this.queueIN = properties.getProperty(JMS_INPUT);
            this.queueOUT = properties.getProperty(JMS_OUTPUT);
            this.type = properties.getProperty(JMS_TYPE);
        }
    }
    /**
     * Generate default CamelRouter (send message by direct:sendMessage)
     * @return
     */
    private RouteBuilder getRoutes(){
            String type = this.type;
            String in = this.queueIN;
            String out = this.queueOUT;
            Processor processor = getProcessor();
            return new RouteBuilder() {
                @Override
                public void configure() {
                    if(!Util.isEmpty(in)){
                        from(type+":"+in).autoStartup(true).process(processor);
                    }
                    if(!Util.isEmpty(out)){
                        from(SEND_MESSAGE_ROUTE).to(type+":"+out);
                    }
                }
            };
    }


    /**
     * Default Camel Porcessor just write the message on the log.
     * For use other a CustomProcessor call setProcessor before getRouteBuilder
     * @return @{@link Processor}
     */
    private Processor getProcessor(){
        if(null!=this.processor){
            return this.processor;
        }
        return new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                String body = exchange.getIn().getBody(String.class);
                Log.system("Camel Response Message: ", body);
            }
        };
    }



}
