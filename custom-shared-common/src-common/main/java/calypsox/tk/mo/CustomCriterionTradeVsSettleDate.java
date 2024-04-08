package calypsox.tk.mo;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.mo.CustomCriterion;
import com.calypso.tk.product.SecFinance;

import java.util.Optional;
import java.util.TimeZone;
import java.util.Vector;

/**
 * @author aalonsop
 */
public class CustomCriterionTradeVsSettleDate extends CustomCriterion {

    private static final long serialVersionUID = 9051558883363794672L;

    @Override
    public boolean accept(Trade trade, JDate valDate) {
        boolean res=true;
        if(trade!=null && trade.getProduct() != null && valDate != null) {
            JDate tradeDate=trade.getTradeDate().getJDate(TimeZone.getDefault());
            CriterionComparatorValue comparator= getComparatorValue();

            if(comparator.equals(CriterionComparatorValue.NETTING_GROSS)){
                if(trade.getProduct() instanceof SecFinance){
                    return tradeDate.equals(((SecFinance) trade.getProduct()).getStartDate());
                }
                return tradeDate.equals(trade.getSettleDate());
            }else if(comparator.equals(CriterionComparatorValue.MTINEXTDAY)) {
                if(trade.getProduct() instanceof SecFinance){
                    SecFinance repo = (SecFinance) trade.getProduct();
                    return !tradeDate.equals(repo.getStartDate()) || !tradeDate.equals(repo.getEndDate());
                }
                return !tradeDate.equals(trade.getSettleDate());
            }

        }
        return res;
    }


    public CriterionComparatorValue getComparatorValue(){
        return  Optional.ofNullable(this.getValues()).map(v->v.get(0))
                .filter(value -> value instanceof String)
                .map(value -> CriterionComparatorValue.valueOf((String) value))
                .orElse(CriterionComparatorValue.NONE);
    }


    public static Vector<CriterionComparatorValue> getComparatorValueList(){
        Vector<CriterionComparatorValue> comparatorList=new Vector<>();
        comparatorList.add(CriterionComparatorValue.NONE);
        comparatorList.add(CriterionComparatorValue.NETTING_GROSS);
        comparatorList.add(CriterionComparatorValue.MTINEXTDAY);
        return comparatorList;
    }

    public enum CriterionComparatorValue{
        NETTING_GROSS,
        MTINEXTDAY,
        NONE
    }
}
