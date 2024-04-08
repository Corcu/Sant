package calypsox.tk.bo.accounting;

import com.calypso.tk.bo.BOCre;
import com.calypso.tk.core.*;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.AccountingEventConfig;
import com.calypso.tk.refdata.AccountingRule;
import com.calypso.tk.refdata.CurrencyPair;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.service.DSConnection;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

public class BondCreHandler extends com.calypso.tk.bo.accounting.BondCreHandler {

    private static final String PARTENON_ID = "PartenonAccountingID";
    private static final String OLD_PARTENON_ID = "OldPartenonAccountingID";
    private static final String BOOKING = "BOOKING";

    @Override
    public void fillAttributes(BOCre cre, Trade trade, PSEvent event, AccountingEventConfig eventConfig, AccountingRule rule, AccountingBook accountingBook) {
        super.fillAttributes(cre, trade, event, eventConfig, rule, accountingBook);

        final List<String> acceptedEventList = DomainValues.values("BondCreHandler.AcceptedEvents");
        if(Optional.ofNullable(cre).isPresent() && !Util.isEmpty(acceptedEventList)){
            if(Arrays.stream(acceptedEventList.toArray()).map(String.class::cast).anyMatch(Optional.of(cre).map(BOCre::getEventType).orElse("")::equalsIgnoreCase)){
                cre.addAttribute(PARTENON_ID,trade.getKeywordValue(PARTENON_ID));
                if (Util.isEmpty(trade.getKeywordValue(PARTENON_ID)) && "WRITE_OFF".equals(cre.getEventType()))
                    cre.addAttribute(PARTENON_ID,trade.getKeywordValue(OLD_PARTENON_ID));
            }
        }

        if(isDualCcy(trade) && BOOKING.equalsIgnoreCase(cre.getEventType())){
            CurrencyPair currencyPair = null;
            try {
                currencyPair = DSConnection.getDefault().getRemoteReferenceData().getCurrencyPair(trade.getTradeCurrency(), trade.getSettleCurrency());
                if (!currencyPair.getPrimaryCode().equals(trade.getTradeCurrency())) {
                    cre.setAmount(1, cre.getAmount(0) * (1 / trade.getSplitBasePrice()));
                } else if (currencyPair.getIsPairPositionRefB()) {
                    cre.setAmount(1, cre.getAmount(0) * trade.getSplitBasePrice());
                } else {
                    cre.setAmount(1, cre.getAmount(0) * (1 / trade.getSplitBasePrice()));
                }
            } catch (CalypsoServiceException e) {
                throw new RuntimeException(e);
            }
            cre.setCurrency(1, trade.getSettleCurrency());
        }
    }

    public static boolean isDualCcy(Trade trade) {
        return trade.getKeywordValue("Dual_CCY") != null &&
                trade.getKeywordValue("Dual_CCY").equals("true");
    }

    public void getALLOCATED(Trade trade, Trade otherTrade, PSEvent event, AccountingEventConfig eventConfig, BOCre cre, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        cre.setAmount(0, trade.getAllocatedQuantity());
        cre.setCurrency(0, trade.getTradeCurrency());
        cre.setBookingDate(trade.getTradeDate().getJDate(TimeZone.getDefault()));
        cre.setEffectiveDate(trade.getTradeDate().getJDate(TimeZone.getDefault()));
        cre.setDescription(eventConfig.getDescription());
    }

    public void getWRITE_OFF(Trade trade, Trade otherTrade, PSEvent event, AccountingEventConfig eventConfig, BOCre cre, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        if (event instanceof PSEventTransfer && !Status.S_SETTLED.equals (((PSEventTransfer)event).getStatus() )) {
            cre.initAmounts();
        }
    }
}
