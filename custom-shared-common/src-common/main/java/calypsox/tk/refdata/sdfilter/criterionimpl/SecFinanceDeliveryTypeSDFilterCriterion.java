package calypsox.tk.refdata.sdfilter.criterionimpl;

import com.calypso.tk.core.*;
import com.calypso.tk.product.SecFinance;
import com.calypso.tk.refdata.sdfilter.AbstractSDFilterCriterion;
import com.calypso.tk.refdata.sdfilter.SDFilterCategory;
import com.calypso.tk.refdata.sdfilter.SDFilterInput;
import com.calypso.tk.refdata.sdfilter.SDFilterOperatorType;

import java.util.*;


public class SecFinanceDeliveryTypeSDFilterCriterion extends AbstractSDFilterCriterion<String> {

    private static final String CRIT_NAME = "SecFinanceDeliveryType";

    public SecFinanceDeliveryTypeSDFilterCriterion() {
        setName(CRIT_NAME);
        setCategory(SDFilterCategory.TRADE);
        setTradeNeeded(true);
    }

    @Override
    public Class<String> getValueType() {
        return String.class;
    }

    /**
     * A list of possible choices when applicable. Item selected is saved as String value.
     */
    @Override
    public List<String> getDomainValues() {
        return Arrays.asList("DAP","DFP");
    }

    @Override
    public List<SDFilterOperatorType> getOperatorTypes() {
        return Arrays.asList(SDFilterOperatorType.IN, SDFilterOperatorType.NOT_IN);
    }

    @Override
    public boolean hasDomainValues() {
        return true;
    }

    @Override
    public String getValue(SDFilterInput sdFilterInput) {
        final Trade trade = sdFilterInput.getTrade();
            return Optional.ofNullable(trade).map(this::checkDeliveryType).orElse(null);
    }

    private String checkDeliveryType(Trade trade){
          return trade.getProduct() instanceof SecFinance ? ((SecFinance) trade.getProduct()).getDeliveryType() : null;
    }
}
