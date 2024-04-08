package calypsox.engine.management.model;

import com.calypso.engine.configuration.EngineDescription;
import com.calypso.engine.metrics.EngineMetrics;
import com.calypso.tk.core.Log;

/**
 * @author aalonsop
 */
public class ExtendedEngineDescription{

    private long monitoringEventCount=0;
    private long eventCountDeltaBetweenChecks=0;

    private long monitoringTimestamp=0L;

    private EngineDescription engineDescription;

    public static ExtendedEngineDescription valueOf(ExtendedEngineDescription extendedEngineDescription,EngineDescription coreDescription){
        ExtendedEngineDescription updatedExEngineDescription=extendedEngineDescription;
        if(extendedEngineDescription!=null){
            updatedExEngineDescription.updateDescription(coreDescription);
        }else{
            updatedExEngineDescription=new ExtendedEngineDescription(coreDescription);
        }
        return updatedExEngineDescription;
    }

    public ExtendedEngineDescription(EngineDescription coreDescription){
        this.engineDescription=coreDescription;
        monitoringEventCount=getMonitoringTotalEventCount(coreDescription);
    }

    public ExtendedEngineDescription(ExtendedEngineDescription extendedEngineDescription){
        this.monitoringEventCount=extendedEngineDescription.getMonitoringEventCount();
        this.eventCountDeltaBetweenChecks=extendedEngineDescription.getEventCountDeltaBetweenChecks();
        this.monitoringTimestamp=extendedEngineDescription.getMonitoringTimestamp();
        this.engineDescription=extendedEngineDescription.getEngineDescription();
    }

    /**
     *
     * @param description
     * @return
     */
    private long getMonitoringTotalEventCount(EngineDescription description){
        long res=0;
        if(description!=null&&description.getMetrics()!=null&&description.getMetrics().getMetric(EngineMetrics.Type.CONSUMED)!=null){
           try{
            res=getLongMetric(EngineMetrics.Type.CONSUMED,description);
            }catch(NullPointerException exc){
               Log.error(this, "Hot fix to catch the NullPointer "+exc.getMessage(),exc.getCause());
           }
        }
        return res;
    }

    public static long getLongMetric(EngineMetrics.Type type, EngineDescription engineDescription){
        long res=0;
        if(engineDescription.getMetrics().getLongMetric(type)!=null){
            res=engineDescription.getMetrics().getLongMetric(type);
        }
        return res;
    }


    public void updateDescription(EngineDescription description) {
        this.engineDescription=description;
        long monitoringCurrentEventCount=getMonitoringTotalEventCount(description);
        eventCountDeltaBetweenChecks=monitoringCurrentEventCount-monitoringEventCount;
        monitoringEventCount=monitoringCurrentEventCount;

    }

    public String getName(){
        String res="";
        if(engineDescription!=null){
            res=engineDescription.getName();
        }
        return res;
    }

    public EngineMetrics getMetrics(){
        EngineMetrics res=new EngineMetrics();
        if(engineDescription!=null){
            res=engineDescription.getMetrics();
        }
        return res;
    }

    public long getLastConsumedEventTimeStamp(){
       return getLongMetric(EngineMetrics.Type.LAST_CONSUMED,engineDescription);
    }

    public EngineDescription getEngineDescription(){
        return engineDescription;
    }

    public long getMonitoringEventCount(){
        return monitoringEventCount;
    }
    public long getEventCountDeltaBetweenChecks() {
        return eventCountDeltaBetweenChecks;
    }

    public long getMonitoringTimestamp() {
        return monitoringTimestamp;
    }
}
