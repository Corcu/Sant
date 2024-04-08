package calypsox.camel;

import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.ibm.mq.jms.MQConnectionFactory;
import com.tibco.tibjms.TibjmsConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.*;

/**
 * @author acd
 */
public class CamelConnectionFactory {

    private static final String JMS = "jms";
    private static final String TIBCO = "tibco";
    private static final String JNDI = "jndi";
    private static final String MQ = "mq";

    private static final String JMS_TYPE = "jms.type";
    private static final String JMS_URL = "jms.url";
    private static final String JMS_USER = "jms.user";
    private static final String JMS_PASS = "jms.password";
    private static final String JMS_SSL_PASS = "jms.ssl.password";
    private static final String JMS_CONNECTION_FACTORY = "jms.queue.connectionFactory";
    private static final String JMS_MODETYPECLASS = "jms.modetypeclass";

    private static CamelConnectionFactory instance;

    public static synchronized CamelConnectionFactory getFactory() {
        if (instance == null) {
            instance = new CamelConnectionFactory();
        }
        return instance;
    }

    /**
     * In order to connect to N EMSs per properties file, a prefix is needed to create required camel components
     * @param properties
     * @param prefixList
     * @return
     */
    public List<ConnectionFactory> getConnectionFactoryList(Properties properties, List<String> prefixList) {
        List<ConnectionFactory> factoryList = new ArrayList<>();
        for (String prefix : prefixList) {
            if (!prefix.isEmpty()) {
                prefix = prefix + ".";
            }
            factoryList.add(getConnectionFactory(properties,prefix));
        }
        return factoryList;
    }

    /**
     * No properties prefix is used, in case of a unique EMS connection per properties file
     * @param properties
     * @return
     */
    public ConnectionFactory getConnectionFactory(Properties properties) {
        return getConnectionFactory(properties,"");
    }

    private ConnectionFactory getConnectionFactory(Properties properties,String prefix) {
        ConnectionFactory connectionFactory = null;
        if (properties != null) {
            String queue = properties.getProperty(prefix + JMS_TYPE);
            String url = properties.getProperty(prefix + JMS_URL);
            String user = properties.getProperty(prefix + JMS_USER);
            String password = properties.getProperty(prefix + JMS_PASS);
            String factory = properties.getProperty(prefix + JMS_CONNECTION_FACTORY);
            String modetypeclass = properties.getProperty(prefix + JMS_MODETYPECLASS);
            String sslPassword = properties.getProperty(prefix + JMS_SSL_PASS);

            connectionFactory = getConnectionFactory(queue, url, user, password, factory, modetypeclass, sslPassword);
        }
        return connectionFactory;
    }

    private ConnectionFactory getConnectionFactory(String queue, String url, String user, String password, String factory, String modetypeclass, String sslPass) {
        ConnectionFactory connectionFactory = null;
        if (validConfig(queue, url)) {
            if (JMS.equalsIgnoreCase(queue)) {
                connectionFactory = new ActiveMQConnectionFactory(url);
                if (validLogin(user, password)) {
                    ((ActiveMQConnectionFactory) connectionFactory).setUserName(user);
                    ((ActiveMQConnectionFactory) connectionFactory).setPassword(password);
                }
            } else if (TIBCO.equalsIgnoreCase(queue)) {
                connectionFactory = new TibjmsConnectionFactory(url);
                if (validLogin(user, password)) {
                    ((TibjmsConnectionFactory) connectionFactory).setUserName(user);
                    ((TibjmsConnectionFactory) connectionFactory).setUserPassword(password);
                    setTibJmsSslProperties((TibjmsConnectionFactory) connectionFactory, sslPass);
                }
            } else if (JNDI.equalsIgnoreCase(queue)) {
                final Hashtable<String, String> props = new Hashtable<>();
                if (validFactory(factory,modetypeclass)) {
                    props.put(Context.INITIAL_CONTEXT_FACTORY, modetypeclass);
                    props.put(Context.PROVIDER_URL, url);
                }
                try {
                    Context ctx = new InitialContext(props);
                    connectionFactory = (ConnectionFactory) ctx.lookup(factory);
                } catch (NamingException e) {
                    Log.error(this, "Error: " + e);
                }
            } else if (MQ.equalsIgnoreCase(queue)) {
                final Hashtable<String, String> props = new Hashtable<>();
                if (validFactory(factory,modetypeclass)) {
                    props.put(Context.INITIAL_CONTEXT_FACTORY, modetypeclass);
                    props.put(Context.PROVIDER_URL, url);
                }
                try {
                    Context ctx = new InitialContext(props);
                    connectionFactory = (MQConnectionFactory) ctx.lookup(factory);
                } catch (NamingException e) {
                    Log.error(this, "Error: " + e);
                }
            }
        }
        return connectionFactory;
    }

    private boolean validConfig(String queue,String url){ return !Util.isEmpty(queue) && !Util.isEmpty(url); }

    private boolean validLogin(String user, String password){ return !Util.isEmpty(user) && !Util.isEmpty(password); }

    private boolean validFactory(String factory, String modetypeclass) {
        return !Util.isEmpty(factory) && !Util.isEmpty(modetypeclass);
    }

    private void setTibJmsSslProperties(TibjmsConnectionFactory connectionFactory, String sslPass) {
        Optional.ofNullable(sslPass).filter(pass->!pass.isEmpty())
                .ifPresent(pass -> {
                    connectionFactory.setSSLPassword(pass);
                    connectionFactory.setSSLEnableVerifyHost(false);
                    connectionFactory.setSSLEnableVerifyHostName(false);
                });
    }

}
