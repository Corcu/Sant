package calypsox.tk.pricer;

import calypsox.tk.bo.BondForwardFilterAdapter;
import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.PricerException;
import com.calypso.tk.core.PricerMeasure;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;

/**
 * @author aalonsop
 */
public class PricerBondMMDiscount extends com.calypso.tk.pricer.PricerBondMMDiscount implements PricerFromDBAdapter, BondForwardFilterAdapter {

    @Override
    public void price(Trade trade, JDatetime valDatetime, PricingEnv env, PricerMeasure[] measures) throws PricerException {
        if(isBondForward(trade)){
            measures=priceFromDB(trade,valDatetime,env,measures);
        }
        super.price(trade, valDatetime, env, measures);
    }
}
