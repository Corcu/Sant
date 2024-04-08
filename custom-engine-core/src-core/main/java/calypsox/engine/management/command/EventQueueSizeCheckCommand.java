package calypsox.engine.management.command;

import calypsox.engine.ControlManagementEngine;
import calypsox.engine.management.model.ExtendedEngineDescription;
import com.calypso.engine.metrics.EngineMetrics;
import com.calypso.tk.core.Log;

/**
 * @author aalonsop
 */
public class EventQueueSizeCheckCommand extends EngineEventsControlCommand{

    private static final String MAX_ENQUEUED_EVENTS_DV_NAME =
            ControlManagementEngine.ENGINE_NAME + ".MAX_ENQUEUED_EVENTS";
    private static final int DEFAULT_MAX_ENQUEUED_EVENTS_VALUE = 1000000;

    @Override
    public void checkEngineMetrics(ExtendedEngineDescription engineBean) {
        int maxAllowedEventQueueSize = getCheckingThresholdFromDomainValue(engineBean.getName());
        if (ExtendedEngineDescription.getLongMetric(EngineMetrics.Type.DB_QUEUE,engineBean.getEngineDescription())
                > maxAllowedEventQueueSize) {
            Log.system(
                    ControlManagementEngine.LOG_CATEGORY
                    ,
                    "[" + engineBean.getName() + "] Has exceded total unconsumed events threshold");
        }
    }

    @Override
    String getThresholdDomainValueName() {
        return MAX_ENQUEUED_EVENTS_DV_NAME;
    }

    @Override
    int getDefaultThresholdValue() {
        return DEFAULT_MAX_ENQUEUED_EVENTS_VALUE;
    }
}
