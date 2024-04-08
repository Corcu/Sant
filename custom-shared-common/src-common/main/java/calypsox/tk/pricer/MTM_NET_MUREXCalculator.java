package calypsox.tk.pricer;

import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PricingEnv;

/**
 * @author aalonsop
 */
public interface MTM_NET_MUREXCalculator {

    void calculate(Trade trade, JDatetime valDatetime, PricingEnv env, Pricer pricer, PricerMeasure measureToCalculate) throws PricerException;
}
