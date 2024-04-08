package calypsox.tk.util.cache;

import calypsox.util.SantDomainValuesUtil;
import com.calypso.infra.util.Util;
import com.calypso.tk.core.Trade;
import com.calypso.tk.util.cache.Cache;

/**
 * @author aalonsop
 * This class implements CacheValidator to avoid TripartyTrades to be inserted in cache. This will fix "immutable"
 * errors seen during MT569's processing.
 */
public class TradeCacheValidator implements com.calypso.tk.util.cache.CacheValidator {

    private static final String MT569_TRADE_KEYWORD = "MT569MessageId";
    private static final String VALIDATOR_ACTIVATION_DV = "EnableTripartyTradeCacheValidator";
    private boolean isEnabled = true;

    public TradeCacheValidator() {
        isEnabled = SantDomainValuesUtil.getBooleanDV(VALIDATOR_ACTIVATION_DV);
    }

    /**
     * @param cache
     * @param cacheableObj
     * @return
     */
    @Override
    public boolean isCacheable(Cache cache, Object cacheableObj) {
        boolean res = true;
        if (isEnabled) {
            res = isNotTripartyTrade((Trade) cacheableObj);
        }
        return res;
    }

    /**
     * @param trade
     * @return
     */
    private boolean isNotTripartyTrade(Trade trade) {
        return Util.isEmpty(trade.getKeywordValue(MT569_TRADE_KEYWORD));
    }
}
