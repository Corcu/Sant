package calypsox.tk.util;

import com.calypso.tk.core.Frequency;
import com.calypso.tk.product.Cash;
import com.calypso.tk.product.Repo;
import com.calypso.tk.refdata.RateIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author aalonsop
 */
public class ScheduledTaskREPO_ESTRAVG_RESET extends ScheduledTaskREPO_RFR_CALENDAR{


    @Override
    void updateCash(Repo repo){
        final RateIndex rateIndex = Optional.ofNullable(repo).map(Repo::getCash).map(Cash::getRateIndex).orElse(null);
        Cash cash = Optional.ofNullable(repo).map(Repo::getCash).orElse(null);

        if(Optional.ofNullable(cash).isPresent() && Optional.ofNullable(rateIndex).isPresent()) {
            String final_rate_dec = Optional.ofNullable(rateIndex.getDefaults().getAttribute("FINAL_RATE_DEC")).orElse("");
            String final_rate_rounding_method = Optional.ofNullable(rateIndex.getDefaults().getAttribute("FINAL_RATE_ROUNDING_METHOD")).orElse("");

            if ("true".equalsIgnoreCase(rateIndex.getDefaults().getAttribute("RFRAverage"))) {
                cash.setAveragingResetB(true);
                cash.setAveragingResetMethod(rateIndex.getDefaults().getAvgMethod().getAveragingMethod());
                cash.setSampleFrequency(rateIndex.getDefaults().getAvgMethod().getFrequency());
                cash.setSamplePeriodRule(rateIndex.getDefaults().getAvgMethod().getPeriodRule());
                cash.setCompoundingMethod("None");
                cash.setCompoundFrequency(Frequency.F_NONE);
                if (com.calypso.infra.util.Util.isEmpty(final_rate_dec) && com.calypso.infra.util.Util.isEmpty(final_rate_rounding_method)) {
                    final_rate_dec = "4";
                    final_rate_rounding_method = "NEAREST";
                }
                if (!com.calypso.infra.util.Util.isEmpty(final_rate_dec) && !com.calypso.infra.util.Util.isEmpty(final_rate_rounding_method)) {
                    cash.setSecCode("RATE_ROUNDING_DEC", final_rate_dec);
                    cash.setSecCode("RATE_ROUNDING", final_rate_rounding_method);
                }
            }
        }
    }


    @Override
    protected List<AttributeDefinition> buildAttributeDefinition() {
        List<AttributeDefinition> attributeList = new ArrayList<>();
        attributeList.add(attribute(ACTION_TO_APPLY).description("Action for apply in trades."));
        return attributeList;
    }
}
