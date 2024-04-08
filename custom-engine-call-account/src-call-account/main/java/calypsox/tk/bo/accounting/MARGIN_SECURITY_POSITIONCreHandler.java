package calypsox.tk.bo.accounting;

import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.accounting.EventCreHandler;
import com.calypso.tk.core.Pricer;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventPositionValuation;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.AccountingEventConfig;
import com.calypso.tk.refdata.AccountingRule;

import java.util.Optional;
import java.util.Vector;

public class MARGIN_SECURITY_POSITIONCreHandler implements EventCreHandler {

    @Override
    public void fill(String eventType, Trade trade, Trade otherTrade, PSEvent event, AccountingEventConfig eventConfig, BOCre cre, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        Optional<PricerMeasure> nominalMeasure = getNominalMeasure(event);
        if(null!=cre && Util.isNonZeroNumber(cre.getAmount(0)) && nominalMeasure.isPresent()){
            cre.setAmount(1,nominalMeasure.get().getValue());
            cre.setCurrency(1,nominalMeasure.get().getCurrency());
        }
    }

    private Optional<PricerMeasure> getNominalMeasure(PSEvent event){
        if(event instanceof PSEventPositionValuation ){
            Optional<Vector> measures = Optional.ofNullable(((PSEventPositionValuation) event).getMeasures());
            return measures.map(mea -> mea.stream()
                    .filter(PricerMeasure.class::isInstance)
                    .filter(h -> ((PricerMeasure) h).getType() == 666)
                    .findFirst()
                    .orElse(new PricerMeasure()))
                    .map(PricerMeasure.class::cast);
        }
        return Optional.of(new PricerMeasure());
    }
}
