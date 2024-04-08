package calypsox.tk.bo.accounting;

import com.calypso.tk.core.Pricer;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.AccountingEventConfig;
import com.calypso.tk.refdata.AccountingRule;

import java.util.Vector;

public class BondMMInterestAccountingHandler extends BondAccountingHandler{

    public void getALLOCATED(Trade trade, PSEvent event, AccountingEventConfig eventConfig, Vector accountingEvents, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer){
        CreAllocatableAccHandler.getALLOCATED(trade,event,eventConfig,accountingEvents,rule,pricingEnv,pricer);
    }
}
