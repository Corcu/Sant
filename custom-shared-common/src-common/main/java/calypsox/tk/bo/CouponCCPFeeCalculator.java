package calypsox.tk.bo;

import com.calypso.infra.util.Util;
import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.bo.FeeDefinition;
import com.calypso.tk.bo.FeeGridFeeCalculator;
import com.calypso.tk.core.PricerException;
import com.calypso.tk.core.Trade;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class CouponCCPFeeCalculator extends FeeGridFeeCalculator {

    @Override
    public double calculate(Fee fee, FeeDefinition def, Trade trade, double amount, PricingEnv env) throws PricerException {
        double calcAmount=super.calculate(fee,def,trade,amount,env);
        int feeLeId=Optional.ofNullable(trade).map(t->this.getFeeCounterpartyIdFromTradeKwd(t,fee))
                .orElse(0);
        fee.setLegalEntityId(feeLeId);
        return calcAmount;
    }

    private int getFeeCounterpartyIdFromTradeKwd(Trade trade, Fee fee){
        int feeLeId=Optional.ofNullable(fee).map(Fee::getLegalEntityId).orElse(0);
        String kwdName="NameBranch";
        String kwdValue=trade.getKeywordValue(kwdName);
        if(!Util.isEmpty(kwdValue)){
            feeLeId = BOCache.getLegalEntityId(DSConnection.getDefault(),kwdValue);
        }
        return feeLeId;
    }
}
