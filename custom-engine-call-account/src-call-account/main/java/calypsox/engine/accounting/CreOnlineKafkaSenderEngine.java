package calypsox.engine.accounting;

import calypsox.camel.routes.CreRouteKafkaBuilder;
import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEventCre;
import com.calypso.tk.service.DSConnection;

import java.util.Properties;

/**
 * @author acd
 */
public class CreOnlineKafkaSenderEngine extends CreOnlineSenderEngine {

    public CreOnlineKafkaSenderEngine(DSConnection dsCon, String hostName, int esPort) {
        super(dsCon, hostName, esPort);
    }

    @Override
    protected void initCamelContext(Properties properties){
        camelConnectionManagement.initCamelKafkaContext(properties,new CreRouteKafkaBuilder()).start();
    }


    @Override
    protected boolean processBOCre(BOCreWrapper creWrapper, PSEventCre event) {
        boolean sent = false;

        BOCre cre=creWrapper.cre;
        Trade trade = boCreUtils.getTrade(cre.getTradeLongId());
        doSleepifPredate(trade,cre);

        creWrapper.santBOCre = creMappingFactory.getCreTypeKafka(cre,trade);

        if(blockCresByFilter(creWrapper.santBOCre )){
            Log.info(this,"Sending cre blocked by Filter: "+ creWrapper.santBOCre .getProductType()+FILTER + " - CreType: " + creWrapper.santBOCre .getCreType()+ "CreId: " + cre.getId());
            this.processCreEvent(event);
            return true;
        }

        generateLog(trade,event,cre);
        this.processCreEvent(event);
        if(creWrapper.santBOCre !=null){
            addAdditionalCreAtt(creWrapper.cre,trade,creWrapper.santBOCre );
            if(saveCre(creWrapper,trade)) {
                sent = sendMessage(event,creWrapper.santBOCre.getCreLineJSon().toString());
            }
        }else{
            Log.system(this.getClass().getName(), "No Cre Type found for: " + cre!=null ? cre.getEventType() :"");
        }

        if(!sent&&!"DELETED".equals(creWrapper.cre.getStatus())){
            updateCreToSend(creWrapper.cre,event);
        }

        Log.info(this,"Time processing creID: " +cre.getId() + " eventType: " + cre.getEventType() );
        return sent;
    }

    /**
     * Send Message
     * @param messsage
     * @return
     */
    private boolean sendMessage(PSEventCre event,String messsage){
        if( camelConnectionManagement.getContext() !=null){
            try {
                if(!isTestingMode()){
                    Log.debug(this.getClass().getSimpleName(),"CamelID: " + camelConnectionManagement.getContext().getName()
                            + " EngineName: " + this.getEngineName()
                            + " PSEventID: " + event.getLongId()
                            + " ThreadID: " + Thread.currentThread().getId());
                    camelConnectionManagement.sendMessage("direct:micKafkaMessage",messsage);
                }
                return true;
            } catch (Exception e) {
                Log.error(this,"Error sending message " + e.getCause());
            }
        }
        return false;
    }

}

