package calypsox.camel;

import calypsox.camel.route.JMSCamelRouteBuilder;
import com.calypso.engine.Engine;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.ConnectException;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.impl.DefaultCamelContext;

import javax.jms.Session;

/**
 * @author aalonsop
 */
public class JMSGatewayEngine extends Engine {

    /**
     *
     */
    private static CamelContext context = new DefaultCamelContext();

    /**
     * @param dsCon
     * @param hostName
     * @param port
     */
    public JMSGatewayEngine(DSConnection dsCon, String hostName, int port) {
        super(dsCon, hostName, port);
    }

    /**
     * @return
     * @throws ConnectException
     */
    @Override
    public boolean start() throws ConnectException {
        fillCamelContext();
        return this.start(true);
    }

    /**
     * @throws ConnectException
     */
    private void fillCamelContext() throws ConnectException {
        try {
            context.addComponent("activemq", getJmsComponent());
            context.addRoutes(new JMSCamelRouteBuilder());
            context.start();
        } catch (Exception exc) {
            Log.error(this, "Exception while trying to connect to JMS connection factory", exc.getCause());
            throw new ConnectException(exc.getMessage(), exc.getCause());
        }
    }

    /**
     * @param event
     * @return
     */
    @Override
    public boolean process(PSEvent event) {
        return false;
    }

    public JmsComponent getJmsComponent() {
        // Create JmsComponent with connectionFactory
        return JmsComponent.jmsComponentAutoAcknowledge(getConnectionFactory());
    }

    public JmsConfiguration getJmsConfiguration() {
        JmsConfiguration jmsConfiguration = new JmsConfiguration();
        // Once all the messages are sent or received, the client send
        // acknowledgement to ActiveMQ
        jmsConfiguration.setAcknowledgementMode(Session.AUTO_ACKNOWLEDGE);
        jmsConfiguration.setTransacted(false);
        // It will start at 3 parallel consumers
        jmsConfiguration.setConcurrentConsumers(3);
        jmsConfiguration.setConnectionFactory(getPooledConnectionFactory());
        return jmsConfiguration;
    }

    public PooledConnectionFactory getPooledConnectionFactory() {
        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
        // A maximum of 10 connections can be opened on high volume of messages
        pooledConnectionFactory.setMaxConnections(10);
        pooledConnectionFactory.setConnectionFactory(getConnectionFactory());
        return pooledConnectionFactory;
    }


    public ActiveMQConnectionFactory getConnectionFactory() {
        return new ActiveMQConnectionFactory("tcp://127.0.0.1:61616");
    }
}
