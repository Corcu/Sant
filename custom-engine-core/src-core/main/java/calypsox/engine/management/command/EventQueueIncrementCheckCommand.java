package calypsox.engine.management.command;

import calypsox.engine.ControlManagementEngine;
import calypsox.engine.management.model.ExtendedEngineDescription;
import com.calypso.engine.metrics.EngineMetrics;
import com.calypso.tk.core.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * @author aalonsop
 */
public class EventQueueIncrementCheckCommand extends EngineEventsControlCommand {

    private static final String MAX_ALLOWD_EVENT_INCREASE_DV_NAME =
            ControlManagementEngine.ENGINE_NAME + ".MAX_EVENT_QUEUE_INCREASE";
    private static final int DEFAULT_MAX_ALLOW_EVENT_INCREASE_VALUE = 100000;

    private Map<String, ExtendedEngineDescription> previousEngineSnapshot  = new HashMap<>();


    @Override
    public void checkEngineMetrics(ExtendedEngineDescription currentEngineBean) {
        ExtendedEngineDescription previousEngineBean=previousEngineSnapshot.get(currentEngineBean.getName());
    if (previousEngineBean != null && previousEngineBean.getEngineDescription()!=null) {
      long eventIncrement = getEventIncrementBetweenIterations(previousEngineBean, currentEngineBean);
      int eventIncrementMaxAllowedValue = getCheckingThresholdFromDomainValue(currentEngineBean.getName());
      if (eventIncrement > eventIncrementMaxAllowedValue) {
        Log.system(
                ControlManagementEngine.LOG_CATEGORY,
            "[" + currentEngineBean.getName() + "] Unconsumed queue is criticaly increasing");
        //If queue is increasing, check if engine is currently processing events.
        checkEngineEventProcessingRate(currentEngineBean);
      }
        }
        previousEngineSnapshot.put(currentEngineBean.getName(), new ExtendedEngineDescription(currentEngineBean));
    }

    private long getEventIncrementBetweenIterations(ExtendedEngineDescription previousEngineBean, ExtendedEngineDescription currentEngineBean){
        long currentEventQueue= ExtendedEngineDescription.getLongMetric(EngineMetrics.Type.DB_QUEUE,currentEngineBean.getEngineDescription());
        long previousEventQueue= ExtendedEngineDescription.getLongMetric(EngineMetrics.Type.DB_QUEUE,previousEngineBean.getEngineDescription());
        return currentEventQueue-previousEventQueue;
    }

    private void checkEngineEventProcessingRate(ExtendedEngineDescription currentEngineBean){
        new EventProcessingRateCheckCommand().checkEngineMetrics(currentEngineBean);
    }
    @Override
    String getThresholdDomainValueName() {
        return MAX_ALLOWD_EVENT_INCREASE_DV_NAME;
    }

    @Override
    int getDefaultThresholdValue() {
        return DEFAULT_MAX_ALLOW_EVENT_INCREASE_VALUE;
    }
}
