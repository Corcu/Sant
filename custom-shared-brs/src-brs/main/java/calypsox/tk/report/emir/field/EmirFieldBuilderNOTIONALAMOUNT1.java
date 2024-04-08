
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.*;
import com.calypso.tk.product.SwapLeg;
import com.calypso.tk.product.flow.CashFlowInterest;
import com.calypso.tk.product.util.NotionalDate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.TimeZone;
import java.util.Vector;

public class EmirFieldBuilderNOTIONALAMOUNT1 implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        JDate eventDate = trade.getUpdatedTime().getJDate(TimeZone.getDefault());

        SwapLeg pLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
        if (pLeg != null) {
            if ("Bullet".equalsIgnoreCase(pLeg.getPrincipalStructure()))  {
                Double value = pLeg.getPrincipal();
                if (Double.compare(value, 0.0D) != 0)  {
                    rst = new BigDecimal(Math.abs(value)).setScale(2, RoundingMode.HALF_EVEN).toPlainString();
                }
            } else if ("Schedule".equalsIgnoreCase(pLeg.getPrincipalStructure()))  {
                SwapLeg swapLeg = EmirFieldBuilderUtil.getInstance().getSwapLeg(trade);
                if (swapLeg != null) {
                    try  {
                        double  amount = getAmportizationAmount(eventDate,swapLeg);
                        if (Double.isInfinite(amount)
                                || Double.isNaN(amount)
                                || Double.compare(0.00d, Math.abs(amount)) == 0) {
                            amount = swapLeg.getPrincipal();
                        }
                        rst = new BigDecimal(Math.abs(amount)).setScale(2, RoundingMode.HALF_EVEN).toPlainString();

                    } catch (Exception ex)    {
                        ex.printStackTrace();
                    }
                }
            }
        }
        return rst;
    }

    public double getAmportizationAmount(JDate eventDate, SwapLeg swapLeg) {
        double amount = 0.0;
        double lastAmount = 0.0;
        final Vector<NotionalDate> amortScheduled = swapLeg.getAmortSchedule();
        NotionalDate current = null;
        if(!Util.isEmpty(amortScheduled)) {
            for(NotionalDate notionalDate : amortScheduled){
                if(notionalDate.getStartDate().after(eventDate)
                        || notionalDate.getStartDate().equals(eventDate)){
                    if(null!=current && notionalDate.getStartDate().before(current.getStartDate())){
                        current = notionalDate;
                        if (current.getNotionalAmt() != 0) {
                            lastAmount = current.getNotionalAmt();
                        }
                    }else if(null==current){
                        current = notionalDate;
                    }
                }
            }
            if(null!=current){
                amount = current.getNotionalAmt();
            }
        }
        //Get last value informed.
        if (amount == 0.00
                && lastAmount != 0.00) {
            amount = lastAmount;
        }

        return amount;
    }


}
