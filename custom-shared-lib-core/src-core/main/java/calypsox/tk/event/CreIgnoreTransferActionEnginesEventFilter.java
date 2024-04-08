package calypsox.tk.event;

import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class CreIgnoreTransferActionEnginesEventFilter implements EventFilter {
    private static final String IGNORE_TRANSFER_ACTION_FILTER_DV = "CreIgnoreTransferActionFilter";

    @Override
    public boolean accept(PSEvent event) {

        if (event instanceof PSEventTransfer) {
            PSEventTransfer eventTransfer = (PSEventTransfer) event;

            String eventAction = eventTransfer.getAction().toString();
            String transferProductType = eventTransfer.getBoTransfer().getProductType();
            String transferStatus = eventTransfer.getBoTransfer().getStatus().getStatus();

            Vector<String> checkProducts = LocalCache.getDomainValues(DSConnection.getDefault(), IGNORE_TRANSFER_ACTION_FILTER_DV);
            if (checkProducts != null && checkProducts.size() > 0 && checkProducts.contains(transferProductType)) {
                String statusAndActionsToIgnoreDVComment = LocalCache.getDomainValueComment(DSConnection.getDefault(), IGNORE_TRANSFER_ACTION_FILTER_DV, transferProductType);
                List<String> statusAndActionsToIgnore = Arrays.asList(statusAndActionsToIgnoreDVComment.split(";"));

                for (String statusAndAction : statusAndActionsToIgnore) {
                    if (statusAndAction.split("\\.").length == 2) {
                        String statusToIgnore = statusAndAction.split("\\.")[0];
                        String actionToIgnore = statusAndAction.split("\\.")[1];

                        if (transferStatus.equalsIgnoreCase(statusToIgnore) && eventAction.equalsIgnoreCase(actionToIgnore)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}
