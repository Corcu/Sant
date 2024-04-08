package calypsox.camel;

import calypsox.tk.camel.AbstractCamelRouteBuilder;
import com.calypso.engine.Engine;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import org.apache.camel.ServiceStatus;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.engine.AbstractCamelContext;
import org.apache.camel.spi.PropertiesComponent;

import javax.jms.ConnectionFactory;
import java.util.Optional;
import java.util.Properties;

/**
 * @author aalonsop
 */
public class CamelContextComponentBean {

    private Properties camelProperties;

    private CamelContextComponentBean() {
        //HIDDEN
    }

    public CamelContextComponentBean(Properties camelProperties) {
        this.camelProperties = camelProperties;
    }

    public void stop() throws Exception {
        CamelContextSingleton.INSTANCE.getCamelContext().stop();
    }

    public void start() throws Exception {
        CamelContextSingleton.INSTANCE.getCamelContext().start();
    }

    public boolean isOnline() {
        return Optional.ofNullable(CamelContextSingleton.INSTANCE.getCamelContext())
                .map(context -> ((AbstractCamelContext) context).getRouteStatus(""))
                .map(ServiceStatus::isStarted)
                .orElse(false);
    }

    /**
     * For CamelBasedSenderEngine and CamelBasedDocumentSender
     *
     * @param camelProperties
     * @param routes
     */
    public void buildCamelContext(AbstractCamelRouteBuilder routeBuilder) throws Exception {
        //Set to true if trace logging needed
        CamelContextSingleton.INSTANCE.getCamelContext().setTracing(false);
        if (!Util.isEmpty(this.camelProperties)) {
            final ConnectionFactory connectionFactory = CamelConnectionFactory.getFactory().getConnectionFactory(this.camelProperties);
            PropertiesComponent propertiesComponent = CamelContextSingleton.INSTANCE.getCamelContext().getPropertiesComponent();
            propertiesComponent.setInitialProperties(this.camelProperties);
            CamelContextSingleton.INSTANCE.getCamelContext()
                    .addComponent(routeBuilder.getCamelComponentName() + "." + camelProperties.getProperty("jms.type"), JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
            CamelContextSingleton.INSTANCE.getCamelContext().addRoutes(routeBuilder);
        } else {
            Log.error(Engine.class.getName(), "Properties not found for engine so " + routeBuilder.getClass().getSimpleName() + " couldn't be initialized");
        }
    }

}
