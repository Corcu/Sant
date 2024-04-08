package calypsox.tk.event;

import calypsox.ctm.util.CTMUploaderConstants;
import calypsox.ctm.util.PlatformAllocationTradeFilterAdapter;
import com.calypso.tk.event.*;
import com.calypso.tk.product.Bond;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class CTMConnectionEngineEventFilter implements EventFilter, PlatformAllocationTradeFilterAdapter {

    PSEventUploadIONEventFilter ionEventFilter;
    PSEventUploadCTMEventFilter ctmEventFilter;


    public CTMConnectionEngineEventFilter() {
        this.ctmEventFilter = new PSEventUploadCTMEventFilter();
        this.ionEventFilter = new PSEventUploadIONEventFilter();
    }

    @Override
    public boolean accept(PSEvent event) {
        boolean res;
        if (event instanceof PSEventMessage) {
            res = acceptPSEventMessage((PSEventMessage) event);
        } else if (event instanceof PSEventTrade) {
            res = acceptPSEventTrade((PSEventTrade) event);
        } else {
            res = acceptPSEventUpload(event);
        }
        return res;
    }

    private boolean acceptPSEventUpload(PSEvent event) {
        return ionEventFilter.accept(event) || ctmEventFilter.accept(event);
    }

    /**
     * @param event
     * @return true in case of being a VERIFIED_TRADE ION Block Trade
     */
    private boolean acceptPSEventTrade(PSEventTrade event) {
        boolean res = false;
        if ("VERIFIED_TRADE".equals(event.getEventType())) {
            res = Optional.ofNullable(event.getTrade())
                    .filter(t -> t.getProduct() instanceof Bond)
                    .filter(this::isPlatformBlockTrade)
                    .isPresent();
        }
        return res;
    }

    /**
     * @param event
     * @return true in case of COMPLETED_GATEWAYMSG and BOMessage.Gateway='ION'
     */
    private boolean acceptPSEventMessage(PSEventMessage event) {
        boolean res = false;
        if ("COMPLETED_GATEWAYMSG".equals(event.getEventType())) {
            res = Optional.ofNullable(event.getBoMessage())
                    .map(msg -> CTMUploaderConstants.ION_STR.equals(msg.getGateway()))
                    .orElse(false);
        }
        return res;
    }
}
