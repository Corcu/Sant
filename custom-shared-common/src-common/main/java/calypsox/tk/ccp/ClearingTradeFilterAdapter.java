package calypsox.tk.ccp;

import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.StaticDataFilter;

import java.util.Optional;

/**
 * @author paisanu
 */

public interface ClearingTradeFilterAdapter {


    String SDFILTER_NAME = "IS_CLEARED_TRADE";

    /**
     *
     * @param trade
     * @return true if in case of being a Cleared trade
     */
    default boolean isClearedTrade(Trade trade){
        return Optional.ofNullable(StaticDataFilter.valueOf(SDFILTER_NAME))
                .map(filter -> filter.accept(trade))
                .orElse(false);
    }
}
