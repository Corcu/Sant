package calypsox.ctm.util;

import com.calypso.infra.util.Util;
import com.calypso.tk.core.Trade;

import java.util.Optional;

public interface PlatformAllocationTradeFilterAdapter {

    /**
     * Note that the checked KWD is AllocatedFromExtRef instead of the core's one.
     * This kwd is UNIQUE for ION/CTM incoming child trades.
     *
     * @param trade
     * @return true if CTM Child. No productType check
     */
    default boolean isPlatformOrCTMChild(Trade trade) {
        return Optional.ofNullable(trade)
                .map(t -> !Util.isEmpty(t.getKeywordValue(CTMUploaderConstants.ALLOCATED_FROM_EXT_REF))
                        || !Util.isEmpty(t.getKeywordValue(CTMUploaderConstants.ALLOCATED_FROM_MX_GLOBALID)))
                .orElse(false);
    }

    /**
     * @param trade
     * @return true if CTM Block Trade. No productType check
     */
    default boolean isPlatformOrCTMBlockTrade(Trade trade) {
        return Optional.ofNullable(trade)
                .map(t -> t.getKeywordValue(CTMUploaderConstants.TRADE_KEYWORD_BLOCK_TRADE_DETAIL))
                .map(kwdValue -> CTMUploaderConstants.CTM_STR.equals(kwdValue) || CTMUploaderConstants.PLATFORM_STR.equals(kwdValue))
                .orElse(false);
    }

    /**
     * @param trade
     * @return true if ION Block Trade. No productType check
     */
    default boolean isPlatformBlockTrade(Trade trade) {
        return Optional.ofNullable(trade)
                .map(t -> t.getKeywordValue(CTMUploaderConstants.TRADE_KEYWORD_BLOCK_TRADE_DETAIL))
                .map(CTMUploaderConstants.PLATFORM_STR::equals)
                .orElse(false);
    }

    /**
     * @param trade
     * @return true if child
     */
    default boolean isChildTrade(Trade trade){
        return Optional.ofNullable(trade)
                .map(t -> !Util.isEmpty(t.getKeywordValue("AllocatedFrom")))
                .orElse(false);
    }
}
