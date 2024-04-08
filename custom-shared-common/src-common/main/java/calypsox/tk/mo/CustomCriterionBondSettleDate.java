package calypsox.tk.mo;

import com.calypso.tk.core.Holiday;
import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.mo.CustomCriterion;
import com.calypso.tk.product.Bond;

import java.util.Optional;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class CustomCriterionBondSettleDate extends CustomCriterion {

    @Override
    public boolean accept(Trade trade, JDate valDate) {
        boolean res=false;
        if(trade!=null) {
            JDate bondSettleDate =computeBondSettleDate(trade);
            res=valDate.equals(bondSettleDate);

        }
        return res;
    }

    private JDate computeBondSettleDate(Trade trade){
        JDate bondSettleDate = trade.getSettleDate();
        int bondDaysToAdjust=Optional.ofNullable(trade.getKeywordValue("BondSettleDays"))
                .map(this::parseInteger).orElse(0);
        Vector<String> holidays=getHolidaysFromBond(trade);
        bondSettleDate=bondSettleDate.addBusinessDays(-bondDaysToAdjust,holidays);
        return bondSettleDate;
    }


    private Vector<String> getHolidaysFromBond(Trade trade){
        return Optional.ofNullable(trade.getProduct()).filter(p->p instanceof Bond)
                .map(p->((Bond)p).getCouponHolidays()).orElse(getTargetHolidays());

    }

    private Vector<String> getTargetHolidays(){
        Vector<String> holidays=new Vector<>();
        holidays.add("TARGET");
        return holidays;
    }
    private int parseInteger(String intString){
        int res=0;
        try{
            res=Integer.parseInt(intString);
        }catch(NumberFormatException exc){
            Log.error(this, exc.getCause());
        }
        return res;
    }
}
