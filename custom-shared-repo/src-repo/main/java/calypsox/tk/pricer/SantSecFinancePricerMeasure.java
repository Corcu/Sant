package calypsox.tk.pricer;

import com.calypso.tk.core.JDatetime;
import com.calypso.tk.core.Pricer;
import com.calypso.tk.core.PricerException;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.pricer.PricerInput;
import com.calypso.tk.pricer.PricerSecFinance;
import com.calypso.tk.pricer.PricerSecFinanceInput;
import com.calypso.tk.pricer.SecFinancePricerMeasure;
import com.calypso.tk.product.util.CollateralBasedUtil;

import java.util.ArrayList;
import java.util.List;

public class SantSecFinancePricerMeasure extends SecFinancePricerMeasure {

    @Override
    public void calculate(Trade trade, JDatetime valDatetime, PricingEnv env, Pricer pricer, PricerInput input) throws PricerException {
        String name = this.getName();
        if(CollateralBasedUtil.isBSB(trade) && "SEC_FIN_SETTLED_INTEREST".equals(name)){
            if (trade.isTradeCanceled()) {
                this.setValue(0.0);
            } else {
                if (pricer instanceof PricerSecFinance && input instanceof PricerSecFinanceInput) {
                    PricerSecFinance pricerSecFinance = (PricerSecFinance)pricer;
                    PricerSecFinanceInput pricerSecFinanceinput = (PricerSecFinanceInput)input;
                    List<String> flowTypes = new ArrayList();
                    flowTypes.add("INTEREST");
                    flowTypes.add("SECLENDING_FEE");
                    this.setValue(pricerSecFinance.computeSettledAmount(flowTypes, trade, valDatetime, env, pricerSecFinanceinput));
                }
            }
        }else {
            super.calculate(trade, valDatetime, env, pricer, input);
        }
    }

    @Override
    public boolean isImplementedByPricer(Pricer pricer) {
        return true;
    }

}
