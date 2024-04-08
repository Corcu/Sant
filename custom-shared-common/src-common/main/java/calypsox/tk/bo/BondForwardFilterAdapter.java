package calypsox.tk.bo;

import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;

import java.util.Optional;

/**
 * @author paisanu
 */
public interface BondForwardFilterAdapter {

    /**
     * @param trade
     * @return true if in case of being a BondForward trade
     */
    default boolean isBondForward(Trade trade) {
        return isBondProductType(trade) && isBondForwardTrade(trade);
    }

    default boolean isBondForwardTrade(Trade trade) {
        return Optional.ofNullable(trade).map(t -> t.getKeywordValue("BondForward"))
                .map(Boolean::parseBoolean).orElse(false);
    }

    default boolean isBondProductType(Trade trade) {
        return Optional.ofNullable(trade).map(Trade::getProduct)
                .map(p -> p instanceof Bond).orElse(false);
    }
}
