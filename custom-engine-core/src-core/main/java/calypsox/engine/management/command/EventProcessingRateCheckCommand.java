package calypsox.engine.management.command;

import calypsox.engine.ControlManagementEngine;
import calypsox.engine.management.model.ExtendedEngineDescription;
import com.calypso.tk.core.Log;

/**
 * @author aalonsop
 */
public class EventProcessingRateCheckCommand extends EngineEventsControlCommand{

    private static final String MAX_ALLOWED_EVENT_PROCESS_TIME =
            ControlManagementEngine.ENGINE_NAME + ".MAX_ALLOWED_EVENT_PROCESS_TIME";

    /** Value in milliseconds */
    private static final int DEFAULT_MAX_EVENT_PROCESSING_TIME = 60000;

    @Override
    public void checkEngineMetrics(ExtendedEngineDescription engineBean) {
        if(engineBean.getEventCountDeltaBetweenChecks()==0){
            Log.system(
                    ControlManagementEngine.LOG_CATEGORY,
                    "[" + engineBean.getName() + "] Engine is not processing events");
        }
    }

    @Override
    String getThresholdDomainValueName() {
        return  MAX_ALLOWED_EVENT_PROCESS_TIME;
    }

    @Override
    int getDefaultThresholdValue() {
        return DEFAULT_MAX_EVENT_PROCESSING_TIME;
    }
}
