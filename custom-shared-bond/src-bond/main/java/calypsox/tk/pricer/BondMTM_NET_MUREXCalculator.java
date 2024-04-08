package calypsox.tk.pricer;

import calypsox.util.collateral.CollateralUtilities;
import com.calypso.helper.RemoteAPI;
import com.calypso.tk.bo.Fee;
import com.calypso.tk.core.*;
import com.calypso.tk.marketdata.PLMark;
import com.calypso.tk.marketdata.PLMarkValue;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.product.Cash;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.RemoteMark;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.TimeZone;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class BondMTM_NET_MUREXCalculator implements MTM_NET_MUREXCalculator {

    private static final String TRADE_KEYWORD_BONDFORWARD = "BondForward";
    private static final String TRADE_KEYWORD_BONDFORWARDTYPE = "BondForwardType";
    private static final String TRADE_KEYWORD_BFFIXINGDATE = "BF_FixingDate";

    private static final String FWD_CASH_FIXING = "FWD_CASH_FIXING";


    @Override
    public void calculate(Trade trade, JDatetime valDatetime, PricingEnv env, Pricer pricer, PricerMeasure measureToCalculate) throws PricerException {

        PLMarkValue mtmNetMurexPLValue= Optional.ofNullable(getPLMark(trade, valDatetime, env))
                .map(mark->CollateralUtilities.retrievePLMarkValue(mark, PricerMeasureMTM_NET_MUREX.MTM_NET_MUREX))
                .orElse(null);

        if (mtmNetMurexPLValue != null) {
           calculateMeasure(mtmNetMurexPLValue,measureToCalculate,trade,valDatetime);
        } else {
           throwPricerException(trade,valDatetime,measureToCalculate);
        }
    }

    private void calculateMeasure(PLMarkValue mtmNetMurexPLValue, PricerMeasure measureToCalculate, Trade trade, JDatetime valDate){
        double mtmNetValue=applyFwdCashAdjustment(trade,valDate,mtmNetMurexPLValue.getMarkValue());

        measureToCalculate.setValue(mtmNetValue);
        measureToCalculate.setCurrency(mtmNetMurexPLValue.getCurrency());
    }

    private void throwPricerException(Trade trade,JDatetime valDatetime, PricerMeasure measureToCalculate) throws PricerException{
        measureToCalculate.setValue(Double.NaN);
        throw new PricerException("No PLMark " + PricerMeasureMTM_NET_MUREX.MTM_NET_MUREX + " found for Trade " + trade.getLongId() + " on date " + valDatetime);
    }

    private double applyFwdCashAdjustment(Trade trade, JDatetime valDate, double mtmNetValue){
        double adjustedMtm=mtmNetValue;
        if(isBondForwardCash(trade)&& isInsideFwdPeriodDate(trade,valDate)){
            adjustedMtm= adjustedMtm - getFwdCashFixingFeeAmt(trade);
        }
        return adjustedMtm;
    }

    public double getFwdCashFixingFeeAmt(Trade trade){
        double fwdCashAmt=0.0d;
        Vector<Fee> fees= Optional.ofNullable(trade).map(Trade::getFeesList)
                .orElse(new Vector<>());
        for(Fee fee:fees){
            if(FWD_CASH_FIXING.equals(fee.getType())){
                fwdCashAmt=fee.getAmount();
            }
        }
        return fwdCashAmt;
    }

    private PLMark getPLMark(Trade trade, JDatetime valDatetime, PricingEnv env) {
        JDate valDate = valDatetime.getJDate(TimeZone.getDefault());
        RemoteMark remoteMark = DSConnection.getDefault().getRemoteMark();
        PLMark plMark = null;
        try {
            plMark = RemoteAPI.getMark(remoteMark, "PL", trade.getLongId(), null, env.getName(), valDate);
        } catch (PersistenceException exc) {
            Log.error(this, exc.getCause());
        }
        return plMark;
    }

    private JDate getFwdFixingDate(Trade trade){
        JDate fixingDate=null;
       String dateStr= Optional.ofNullable(trade).map(t->t.getKeywordValue(TRADE_KEYWORD_BFFIXINGDATE))
               .orElse("19700101");
        SimpleDateFormat formatter=new SimpleDateFormat("yyyyMMdd");
        try {
             fixingDate=JDate.valueOf(formatter.parse(dateStr));
        } catch (ParseException exc) {
           Log.error(this,exc.getCause());
        }
            return fixingDate;
    }
    /**
     *
     * @return true if valDate >= tradeDate (fixingDate) and valDate < settleDate
     */
    private boolean isInsideFwdPeriodDate(Trade trade, JDatetime valDate){
        JDate fixingJDate=getFwdFixingDate(trade);
        JDate valDateJDate=valDate.getJDate(TimeZone.getDefault());
        boolean isAfterFixingDate=valDateJDate.gte(fixingJDate);
        boolean isBeforeSettleDate= valDateJDate.before(trade.getSettleDate());
        return isAfterFixingDate && isBeforeSettleDate;
    }

    private boolean isBondForwardCash(Trade trade){
        boolean isBondFwd=isBondForward(trade);
        boolean isBondFwdCash=Optional.ofNullable(trade).map(t->t.getKeywordValue(TRADE_KEYWORD_BONDFORWARDTYPE))
                .map(kwd->kwd.equalsIgnoreCase(Cash.class.getSimpleName())).orElse(false);
        return isBondFwd && isBondFwdCash;
    }

    private boolean isBondForward(Trade trade){
        return Optional.ofNullable(trade).map(t->t.getKeywordValue(TRADE_KEYWORD_BONDFORWARD))
                .map(Boolean::parseBoolean).orElse(false);
    }

}
