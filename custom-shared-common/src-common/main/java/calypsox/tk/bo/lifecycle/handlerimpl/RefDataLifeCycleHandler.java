package calypsox.tk.bo.lifecycle.handlerimpl;

import com.calypso.tk.bo.lifecycle.ContainerBulkObjectSaver;
import com.calypso.tk.bo.lifecycle.LifeCycleHandler;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;
import com.calypso.tk.util.BulkObjectSaver;
import com.calypso.tk.util.InstantiateUtil;

import java.util.*;
import java.util.stream.Collectors;

public class RefDataLifeCycleHandler extends LifeCycleHandler {

    private final List<LifeCycleHandler> refDataHandlers;
    private PricingEnv env;

    public RefDataLifeCycleHandler() {
        Vector<String> handlerClassNames = LocalCache.getDomainValues(DSConnection.getDefault(), "RefDataLifeCycleHandler");
        refDataHandlers = (new ArrayList<>(handlerClassNames)).stream().map(h -> {
            String className = LifeCycleHandler.class.getName().replace("com.calypso.", "").replace(LifeCycleHandler.class.getSimpleName(), "handlerimpl." + h + LifeCycleHandler.class.getSimpleName());
            try {
                return (LifeCycleHandler) InstantiateUtil.getInstance(className, true);
            } catch (Exception e) {
                Log.error(this, e);
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public Class<? extends PSEvent>[] subscribe() {
        return refDataHandlers.stream().flatMap(h -> Arrays.stream(h.subscribe())).distinct().collect(Collectors.toList()).toArray(new Class[]{});
    }

    @Override
    public BulkObjectSaver handleEvent(PSEvent psEvent) throws Exception {
        List<LifeCycleHandler> eventHandlers = refDataHandlers.stream().filter(h ->h.isApplicable(psEvent) && h.accept(psEvent)).collect(Collectors.toList());
        if (!Util.isEmpty(eventHandlers)) {
            ContainerBulkObjectSaver wrapperSaver = new ContainerBulkObjectSaver();
            for (LifeCycleHandler handler : eventHandlers) {
                handler.setPricingEnv(env);
                BulkObjectSaver bulkObjectSaver = handler.handleEvent(psEvent);
                if (bulkObjectSaver != null)
                    wrapperSaver.add(BulkObjectSaver.class, bulkObjectSaver);
            }
            return Util.isEmpty(wrapperSaver.keySet())?null:wrapperSaver;
        }
        return null;
    }

    @Override
    public boolean accept(PSEvent psEvent) {
        return refDataHandlers.stream().anyMatch(h -> h.accept(psEvent));
    }

    public void setPricingEnv(PricingEnv env) {
        this.env = env;
    }
}
