package calypsox.tk.bo.accounting;

import com.calypso.tk.bo.BOCre;
import com.calypso.tk.bo.accounting.CreHandler;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Pricer;
import com.calypso.tk.core.Trade;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.InterestBearing;
import com.calypso.tk.refdata.AccountingEventConfig;
import com.calypso.tk.refdata.AccountingRule;
/**
 * @author acd
 */
/**
 * Change EffectiveDate for EndDate where Product = InterestBearing and CreEvent = CST_VERIFIED
 */
public class InterestBearingCreHandler extends CreHandler
{
    private static final String CST = "CST_VERIFIED";

    /**
     * Call on CST_VERIFIED accounting event generation
     * @param trade
     * @param otherTrade
     * @param event
     * @param eventConfig
     * @param cre
     * @param rule
     * @param pricingEnv
     * @param pricer
     */
    public void getCST_VERIFIED(Trade trade, Trade otherTrade, PSEvent event, AccountingEventConfig eventConfig, BOCre cre, AccountingRule rule, PricingEnv pricingEnv, Pricer pricer) {
        if(null!=cre && CST.equalsIgnoreCase(cre.getEventType())
                && null!=trade && trade.getProduct() instanceof InterestBearing){
            final JDate expDate = ((InterestBearing) trade.getProduct()).getEndDate();
            if(expDate!=null){
                Log.info(InterestBearingCreHandler.class.getName(),"Updating Effective Date for CST_VERIFIED" );
                cre.setEffectiveDate(expDate);
            }
        }
    }
}
