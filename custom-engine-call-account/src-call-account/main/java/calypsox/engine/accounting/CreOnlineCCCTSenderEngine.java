package calypsox.engine.accounting;

import calypsox.camel.routes.CCCTRouteBuilder;
import com.calypso.engine.context.EngineContext;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventCre;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CreArray;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsDestination;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.directory.InitialDirContext;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Properties;

public class CreOnlineCCCTSenderEngine extends CreOnlineSenderEngine {

    protected static final String SEND_ROUTE = "direct:ccctMessages";
    protected static final String REVERSAL = "REVERSAL";
    protected static final String MATURITY = "MATURITY";
    private Connection connection;
    private Session session;
    private Destination destination;

    public CreOnlineCCCTSenderEngine(DSConnection dsCon, String hostName, int esPort) {
        super(dsCon, hostName, esPort);
    }

    @Override
    protected void init(EngineContext engineContext) {
        super.init(engineContext);
        initMQConnection();
    }

    @Override
    protected boolean processBOCre(BOCreWrapper creWrapper, PSEventCre event) {
        boolean sent = false;

        BOCre cre = creWrapper.cre;
        Trade trade = boCreUtils.getTrade(cre.getTradeLongId());
        doSleepifPredate(trade, cre);

        creWrapper.santBOCre = creMappingFactory.getCreTypeCCCT(cre, trade);

        if(isCreToAvoid(cre)) {
            Log.info(this, "Updating CANCEL MATURITY Cre to NOT SENT, time processing creID: " + cre.getId() + " eventType: " + cre.getEventType());
            updateCreToNotSent(creWrapper.cre, event);
            this.processCreEvent(event);
            return true;
        }

        if (blockCresByFilter(creWrapper.santBOCre)) {
            Log.info(this, "Sending cre blocked by Filter: " + creWrapper.santBOCre.getProductType() + FILTER + " - CreType: " + creWrapper.santBOCre.getCreType() + "CreId: " + cre.getId());
            this.processCreEvent(event);
            return true;
        }

        generateLog(trade, event, cre);
        this.processCreEvent(event);
        if (creWrapper.santBOCre != null) {
            //addAdditionalCreAtt(creWrapper.cre, trade, creWrapper.santBOCre);
            if (saveCre(creWrapper, trade)) {
                String line = creWrapper.santBOCre.getCCCTLine().toString();
                Log.system(this.getClass().getName(), "The string obtained from the CRE is " + line);
                sent = sendMQMessage(line);
                generateLog(line);
            }
        } else {
            Log.system(this.getClass().getName(), "No Cre Type found for: " + cre != null ? cre.getEventType() : "");
        }

        if (!sent && !"DELETED".equals(creWrapper.cre.getStatus())) {
            updateCreToSend(creWrapper.cre, event);
        }
        Log.info(this, "Time processing creID: " + cre.getId() + " eventType: " + cre.getEventType());
        return sent;
    }


    @Override
    protected void initCamelContext(Properties properties){
        String input = properties.getProperty("intput.queue.name");
        String output = properties.getProperty("output.queue.name");
        camelConnectionManagement.initCamelContext(properties,new CCCTRouteBuilder(input,output)).start();
    }

    protected boolean isCreToAvoid(BOCre cre){
        return cre.getCreType().equals(REVERSAL) && cre.getEventType().equals(MATURITY);
    }

    protected void updateCreToNotSent(BOCre cre, PSEvent event) {
        CreArray cres = new CreArray();
        if (null != cre) {
            try {
                BOCre boCre = this._ds.getRemoteBO().getBOCre(cre.getId());
                if (null != boCre) {
                    boCre.setSentStatus("NOT_SENT");
                    cres.add(boCre);
                }
                this._ds.getRemoteBO().saveCres(cres);
                Log.system(this.getClass().getName(), "The sending process failed, updating Sent Status of Cre with id: " + cre.getId());
            } catch (CalypsoServiceException e) {
                Log.error(this, "Error saving CRE: " + cre.getId() + " " + e);
            }
        }
    }

    private void initMQConnection(){
        String contextFactory = properties.getProperty("jms.modetypeclass");
        String initialContextUrl = properties.getProperty("jms.url");
        String destinationFromJndi = properties.getProperty("output.queue.name");
        String connectionFactoryFromJndi = properties.getProperty("jms.queue.connectionFactory");
        Hashtable<String, String> environment = new Hashtable<String, String>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
        environment.put(Context.PROVIDER_URL, initialContextUrl);
        try {
            Context context = new InitialDirContext(environment);
            JmsConnectionFactory cf = (JmsConnectionFactory) context.lookup(connectionFactoryFromJndi);
            destination = (JmsDestination) context.lookup(destinationFromJndi);
            connection = cf.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            connection.start();
        } catch (Exception e) {
            Log.error(this, e);
        }
    }

    //Workaround to send message without JMS header, only check the binding
    private boolean sendMQMessage(String content){
        MessageProducer producer = null;
        try {
            producer = session.createProducer(destination);
            TextMessage message = session.createTextMessage(content);
            producer.send(message);
            return true;
        } catch (Exception e) {
            Log.error(this, e);
            return false;
        }
    }

    @Override
    public void stop() {
        if( connection !=null){
            try {
                connection.stop();
                session.close();
            } catch (Exception exc) {
                Log.error(this.getClass().getSimpleName(), "Errors while stopping connection", exc.getCause());
            }
        }
        super.stop();
    }

    private void generateLog(String message) {
        ///calypso_interfaces/mic/?fileName=ccct_sent_messages_${date:now:yyyyMMdd}
        String fileName = "/calypso_interfaces/mic/ccct_sent_messages_";
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyyMMdd");
        String date = dateFormater.format(new Date());
        String extension = ".log";
        String fullFileName = fileName + date + extension;
        try{
            File file = new File(fullFileName);
            FileWriter w = new FileWriter(fullFileName, true);
            w.write(message+"\n");
            w.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
