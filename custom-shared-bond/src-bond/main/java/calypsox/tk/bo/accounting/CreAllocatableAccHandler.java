package calypsox.tk.bo.accounting;

import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.BOPosting;
import com.calypso.tk.bo.accounting.AccountingHandler;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.AccountingEventConfig;
import com.calypso.tk.refdata.AccountingRule;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.util.CreArray;

import java.util.TimeZone;
import java.util.Vector;

public class CreAllocatableAccHandler {

    protected static void getALLOCATED(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        BOPosting accEvent = new BOPosting(eventConfig);

        accEvent.setAmount(trade.getAllocatedQuantity());
        accEvent.setBookingDate(trade.getTradeDate().getJDate(TimeZone.getDefault()));
        accEvent.setCurrency(trade.getTradeCurrency());
        accEvent.setEffectiveDate(trade.getTradeDate().getJDate(TimeZone.getDefault()));
        accountingEvents.addElement(accEvent);

        cancelNotSentCres(trade);
    }

    private static void cancelNotSentCres(Trade trade) {
        try {
            CreArray cres = DSConnection.getDefault().getRemoteBO().getBOCres(trade.getLongId());
            CreArray cresToCancel = new CreArray();
            if (cres != null && !cres.isEmpty()) {
                for (int i = 0; i < cres.size(); i++) {
                    BOCre cre = cres.get(i);
                    if (Util.isEmpty(cre.getSentStatus()) || "NOT_SENT".equals(cre.getSentStatus())) {
                        cre.setSentStatus(BOCre.DELETED);
                        cre.setStatus(BOCre.DELETED);
                        Log.info(AccountingHandler.class.getSimpleName(), "ALLOCATED event has triggered CRE id: " + cre.getId() + " DELETION");
                        cresToCancel.add(cre);
                    }
                }
                DSConnection.getDefault().getRemoteBO().saveCres(cres);
            }
        } catch (CalypsoServiceException e) {
            Log.error(AccountingHandler.class.getSimpleName(), "Error while canceling unsent cres for ALLOCATED eventType");
        }
    }
}
