package calypsox.camel;

import com.calypso.engine.Engine;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.PropertiesComponent;

import javax.jms.ConnectionFactory;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * @author aalonsop
 */
public class CamelContextBean {

    private CamelContext camelContext;
    private Properties camelProperties;

    private static final String DEF_SEND_ROUTE_NAME = "direct:sendMessage";

    private CamelContextBean() {
        //HIDDEN
    }
    public CamelContextBean(Properties camelProperties) {
        this.camelProperties=camelProperties;
    }

    public void stop() throws Exception {
        this.camelContext.stop();
    }

    public void start() throws Exception {
        this.camelContext.start();
    }

    public boolean isOnline() {
        return Optional.ofNullable(this.camelContext).map(context -> ServiceStatus.Started.equals(context.getStatus()))
                .orElse(false);
    }

    public void sendMsgThroughDefaultRoute(String message) {
        this.camelContext.createProducerTemplate().sendBody(DEF_SEND_ROUTE_NAME, message);
    }

    public static String getDefaultSendRouteName() {
        return DEF_SEND_ROUTE_NAME;
    }

    /**
     * For CamelBasedSenderEngine and CamelBasedDocumentSender
     *
     * @param camelProperties
     * @param routes
     */
    public void buildCamelContext(RouteBuilder routeBuilder) throws Exception {
        this.camelContext = new DefaultCamelContext();
        //Set to true if trace logging needed
        this.camelContext.setTracing(false);
        if (!Util.isEmpty(this.camelProperties)) {
            final ConnectionFactory connectionFactory = CamelConnectionFactory.getFactory().getConnectionFactory(this.camelProperties);
            PropertiesComponent propertiesComponent = this.camelContext.getPropertiesComponent();
            propertiesComponent.setInitialProperties(this.camelProperties);
            this.camelContext.addComponent(camelProperties.getProperty("jms.type"), JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
            this.camelContext.addRoutes(routeBuilder);
        }else{
            Log.error(Engine.class.getName(),"Properties not found for engine so "+routeBuilder.getClass().getSimpleName()+" couldn't be initialized");
        }
    }

    /**
     * For CamelBasedSenderEngine and CamelBasedDocumentSender
     *
     * @param camelProperties
     * @param routes
     */
    public void buildCamelContextMultipleEms(RouteBuilder routeBuilder, List<String> prefixList) throws Exception {
        this.camelContext = new DefaultCamelContext();
        if (!Util.isEmpty(this.camelProperties)) {
            List<ConnectionFactory> connectionFactoryList = CamelConnectionFactory.getFactory().getConnectionFactoryList(this.camelProperties,prefixList);
            PropertiesComponent propertiesComponent = this.camelContext.getPropertiesComponent();
            propertiesComponent.setInitialProperties(this.camelProperties);
            for(int i=0;i<connectionFactoryList.size();i++) {
                String prefix=prefixList.get(i);
                this.camelContext.addComponent(prefix+camelProperties.getProperty("jms.type"), JmsComponent.jmsComponentAutoAcknowledge(connectionFactoryList.get(i)));
            }
            this.camelContext.addRoutes(routeBuilder);
        }else{
            Log.error(Engine.class.getName(),"Properties not found for engine so "+routeBuilder.getClass().getSimpleName()+" couldn't be initialized");
        }
    }
}
