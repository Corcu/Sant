package calypsox.camel;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import net.bytebuddy.dynamic.TypeResolutionStrategy;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.artemis.jms.client.ActiveMQQueueConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Processor;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConfiguration;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.PropertiesComponent;

import javax.jms.*;
import java.util.List;
import java.util.Properties;

/**
 * @author acd
 */
public class CamelConnectionManagement {

    private static final String SEND_ROUTE = "direct:sendMessage";
    //Connection
    private CamelContext context = new DefaultCamelContext();

    private RouteBuilder routeBuilder;

    private Processor processor;

    private Properties properties;

    public CamelConnectionManagement() {
    }

    /**
     * @param routeBuilder
     */
    public void setRouteBuilder(RouteBuilder routeBuilder) {
        this.routeBuilder = routeBuilder;
    }

    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    public CamelConnectionManagement usingRouterBuilder(RouteBuilder routeBuilder) {
        this.routeBuilder = routeBuilder;

        return this;
    }

    public CamelConnectionManagement initCamelContext(Properties paramProperties) {
        if (null != paramProperties) {
            final Properties properties = paramProperties;

            final ConnectionFactory connectionFactory = CamelConnectionFactory.getFactory().getConnectionFactory(properties);
            context.addComponent(properties.getProperty("jms.type"), JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

            try {
                if (null != this.routeBuilder) {
                    context.addRoutes(this.routeBuilder);

                } else {
                    this.routeBuilder = getRouteBuilder(properties);
                    context.addRoutes(this.routeBuilder);
                }

                context.start();
                final ServiceStatus status = context.getStatus();

            } catch (Exception e) {
                Log.error(this, "Cannot init Camel connection. " + e);
            }
        }
        return this;
    }

    public CamelConnectionManagement initCamelContext(Properties paramProperties, RouteBuilder routes) {
        if (null != paramProperties) {
            properties = paramProperties;

            final ConnectionFactory connectionFactory = CamelConnectionFactory.getFactory().getConnectionFactory(properties);
            PropertiesComponent propertiesComponent = context.getPropertiesComponent();
            propertiesComponent.setInitialProperties(paramProperties);
            context.addComponent(properties.getProperty("jms.type"), JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

            try {
                context.addRoutes(routes);
            } catch (Exception e) {
                Log.error(this, "Error setting Routes. " + e.getCause().getMessage());
            }
        }
        return this;
    }

    public CamelConnectionManagement initMultiCamelContexts(Properties paramProperties, RouteBuilder routes, List<String> prefixList) {
        if (null != paramProperties) {
            properties = paramProperties;
            List<ConnectionFactory> connectionFactoryList = CamelConnectionFactory.getFactory().getConnectionFactoryList(properties, prefixList);
            PropertiesComponent propertiesComponent = context.getPropertiesComponent();
            propertiesComponent.setInitialProperties(paramProperties);
            for (int i = 0; i < connectionFactoryList.size(); i++) {
                String prefix = prefixList.get(i);
                String componentName = "";
                if (!Util.isEmpty(prefix)) {
                    componentName = prefix + paramProperties.getProperty(prefix + ".jms.type");
                } else {
                    componentName = prefix + paramProperties.getProperty("jms.type");
                }
                context.addComponent(componentName, JmsComponent.jmsComponentAutoAcknowledge(connectionFactoryList.get(i)));
            }

            try {
                context.addRoutes(routes);
            } catch (Exception e) {
                Log.error(this, "Error setting Routes. " + e.getCause().getMessage());
            }
        }
        return this;
    }

    public CamelConnectionManagement initCamelKafkaContext(Properties paramProperties, RouteBuilder routes) {
        if (null != paramProperties) {
            properties = paramProperties;
            PropertiesComponent propertiesComponent = context.getPropertiesComponent();
            propertiesComponent.setInitialProperties(paramProperties);
            KafkaComponent kafkaComponent = new KafkaComponent();

            KafkaConfiguration configuration = new KafkaConfiguration();
            configuration.setSecurityProtocol(properties.getProperty("kafka.security.protocol"));
            configuration.setSslKeyPassword(properties.getProperty("kafka.ssl.key.password"));
            configuration.setSslKeystoreLocation(properties.getProperty("kafka.ssl.keystore.location"));
            configuration.setSslKeystorePassword(properties.getProperty("kafka.ssl.keystore.password"));
            configuration.setSslTruststoreLocation(properties.getProperty("kafka.ssl.truststore.location"));
            configuration.setSslTruststorePassword(properties.getProperty("kafka.ssl.truststore.password"));
            configuration.setBrokers(properties.getProperty("jms.url"));
            kafkaComponent.setConfiguration(configuration);

            context.addComponent(properties.getProperty("jms.type"), kafkaComponent);
            try {
                context.addRoutes(routes);
            } catch (Exception e) {
                Log.error(this, "Error setting Routes. " + e.getCause().getMessage());
            }
        }
        return this;
    }

    public CamelConnectionManagement initCamelMQContext(Properties paramProperties, RouteBuilder routes) {
        if (null != paramProperties) {
            properties = paramProperties;
            MQConnectionFactory mqConnectionFactory = new MQConnectionFactory();
            try {
                mqConnectionFactory.setHostName(properties.getProperty("jms.url"));
                mqConnectionFactory.setPort(1414);
                mqConnectionFactory.setChannel(properties.getProperty("jms.channel"));
                mqConnectionFactory.setQueueManager(properties.getProperty("jms.queuemanager"));
                mqConnectionFactory.setTransportType(1); // 1: TCP, 2: Bindings
                mqConnectionFactory.setStringProperty(WMQConstants.USERID, properties.getProperty("jms.user"));
                mqConnectionFactory.setStringProperty(WMQConstants.PASSWORD, properties.getProperty("jms.password"));
                Connection connection = mqConnectionFactory.createConnection();
                JmsComponent jmsComponent = JmsComponent.jmsComponent(mqConnectionFactory);
                context.addRoutes(routes);

                context.addComponent("mq", jmsComponent);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
        return this;
    }

    public CamelConnectionManagement start() {
        try {
            if (null != this.context) {
                this.context.start();
            }
        } catch (Exception e) {
            Log.error(this, "Error starting Camel Context. " + e.getCause().getMessage());
        }
        return this;
    }

    /**
     * Send message by default using the route "direct:sendMessage".
     * Need configure this route on your RouteBuilder.
     *
     * @param message
     */
    public void sendMessage(String message) throws Exception {
        if (this.context != null) {
            context.createProducerTemplate().sendBody(SEND_ROUTE, message);
        }
    }

    /**
     * Send message by a uri defined on RouteBuilder.
     *
     * @param uri
     * @param message
     */
    public void sendMessage(String uri, String message) {
        if (this.context != null) {
            context.createProducerTemplate().sendBody(uri, message);
        }
    }

    /**
     * Stop Camel Context
     */
    public void stopConnection() {
        try {
            this.context.stop();
        } catch (Exception e) {
            Log.error(this, "Cannot stop Camel connection. " + e);
        }
    }

    private RouteBuilder getRouteBuilder(Properties properties) {
        if (null != this.routeBuilder) {
            return this.routeBuilder;
        }
        return CamelRoutesBuilderFactory.getFactory().getRouteBuilder(properties);
    }

    public CamelContext getContext() {
        return this.context;
    }

}
