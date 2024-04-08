package calypsox.engine.management.command;

import calypsox.engine.management.model.ExtendedEngineDescription;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

public abstract class EngineEventsControlCommand {
    /**
     * Command method
     * @param engineDescription
     */
    public abstract void checkEngineMetrics(ExtendedEngineDescription engineBean);

    abstract String getThresholdDomainValueName();
    abstract int getDefaultThresholdValue();


    protected int getCheckingThresholdFromDomainValue(String engineName){
        String dvComment =
                LocalCache.getDomainValueComment(
                        DSConnection.getDefault(), getThresholdDomainValueName(), engineName);
        return getIntFromString(dvComment, getDefaultThresholdValue());
    }

    protected int getIntFromString(String stringValue, int defaultValue) {
        int integerValue = defaultValue;
        if (!Util.isEmpty(stringValue)) {
            try {
                integerValue = Integer.valueOf(stringValue);
            } catch (NumberFormatException exc) {
                Log.debug(this, "Couldn't convert " + stringValue + " to an integer");
            }
        }
        return integerValue;
    }
}
