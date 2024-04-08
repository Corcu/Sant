package calypsox.tk.event;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventMessage;
import com.calypso.tk.event.PSEventTrade;
import com.calypso.tk.product.MarginCall;
import com.calypso.tk.refdata.MarginCallConfig;
import com.calypso.tk.service.DSConnection;
/**
 * @author acd
 */
/**
 * This Filter validate BoMessage || Trade
 * <p>
 * Product: MarginCall
 * SubProduct: COLLATERAL
 * ContractADF: NEW_CALL_ACCOUNT_CIRCUIT = True
 */
public class SantDataExportServiceMurexP37EventFilter implements EventFilter {

    private static final String VERIFIED_TRADE_STR = "VERIFIED_TRADE";
    private static final String WAITING_CANCEL_STR = "WAITING_CANCEL_TRADE";
    private static final String CALL_ACCOUNT = "NEW_CALL_ACCOUNT_CIRCUIT";
    private static final String COLLATERAL = "COLLATERAL";
    private static final String TRUE = "True";

    private static final String PS_EVENT_MESSAGE_EVENT_TYPE = "TO_BE_SENT_DATAEXPORTERMSG";

    @Override
    public boolean accept(PSEvent event) {
        boolean res = false;
        if (acceptPSEventType(event)) {
            Trade trade = getTradeFromPSEvent(event);
            if (acceptTrade(trade)) {
                res = proccesTrade(trade);
            }
        }
        return res;
    }

    /**
     * @param tradeid
     * @return
     */
    private Trade getTrade(long tradeid) {
        try {
            return DSConnection.getDefault().getRemoteTrade().getTrade(tradeid);
        } catch (CalypsoServiceException e) {
            Log.error(this, "Cannot load trade: " + tradeid);
        }
        return null;
    }

    /**
     * @param marginCallConfig
     * @return true if contract have NEW_CALL_ACCOUNT_CIRCUIT = True
     */
    private boolean checkContractCircuit(MarginCallConfig marginCallConfig) {
        if (marginCallConfig != null) {
            return TRUE.equalsIgnoreCase(marginCallConfig.getAdditionalField(CALL_ACCOUNT));
        }
        return false;
    }

    /**
     * @param event
     * @return
     */
    private Trade getTradeFromPSEvent(PSEvent event) {
        Trade trade = null;
        if (event instanceof PSEventTrade) {
            trade = ((PSEventTrade) event).getTrade();
        } else if (event instanceof PSEventMessage) {
            final BOMessage boMessage = ((PSEventMessage) event).getBoMessage();
            if(null!=boMessage){
                trade = getTrade(boMessage.getTradeLongId());
            }
        }
        return trade;
    }

    /**
     * @param event
     * @return
     */
    private boolean acceptPSEventType(PSEvent event) {
        return null!=event && acceptPSEventMessage(event.getEventType()) || acceptPSEventTrade(event.getEventType());
    }

    /**
     * @param eventType
     * @return
     */
    private boolean acceptPSEventMessage(String eventType) {
        return PS_EVENT_MESSAGE_EVENT_TYPE.equals(eventType);
    }

    /**
     * @param eventType
     * @return true if eventType is equals to the defined ones
     */
    private boolean acceptPSEventTrade(String eventType) {
        return VERIFIED_TRADE_STR.equalsIgnoreCase(eventType) || WAITING_CANCEL_STR.equalsIgnoreCase(eventType);
    }

    /**
     * @param trade
     * @return
     */
    private boolean proccesTrade(Trade trade) {
        final MarginCallConfig marginCallConfig = ((MarginCall) trade.getProduct()).getMarginCallConfig();
        return checkContractCircuit(marginCallConfig);
    }

    /**
     * @param trade
     * @return
     */
    private boolean acceptTrade(Trade trade) {
        return trade != null && trade.getProduct() instanceof MarginCall && COLLATERAL.equalsIgnoreCase(trade.getProductSubType());
    }

}
