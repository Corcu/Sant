package calypsox.tk.refdata.sdfilter.criterionimpl;

import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.sdfilter.AbstractSDFilterCriterion;
import com.calypso.tk.refdata.sdfilter.SDFilterCategory;
import com.calypso.tk.refdata.sdfilter.SDFilterInput;
import com.calypso.tk.refdata.sdfilter.SDFilterOperatorType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;


public class CheckTradeQtyDecimalSDFilterCriterion extends AbstractSDFilterCriterion<Boolean> {
    private static final String CRIT_NAME = "CheckTradeQtyDecimal";

    public CheckTradeQtyDecimalSDFilterCriterion() {
        setName(CRIT_NAME);
        setCategory(SDFilterCategory.TRADE);
        setTradeNeeded(true);
    }

    @Override
    public List<SDFilterOperatorType> getOperatorTypes() {
        return Arrays.asList(SDFilterOperatorType.IS);
    }

    public SDFilterCategory getCategory() {
        return SDFilterCategory.TRADE;
    }

    @Override
    public Class<Boolean> getValueType() {
        return Boolean.class;
    }

    @Override
    public Boolean getValue(SDFilterInput sdFilterInput) {
        final Trade trade = sdFilterInput.getTrade();
        return Optional.ofNullable(trade).map(Trade::getQuantity).map(s -> s != 0 && s % 1 == 0).orElse(true);
    }
}
