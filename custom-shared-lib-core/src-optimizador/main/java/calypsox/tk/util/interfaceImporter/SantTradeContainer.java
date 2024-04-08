package calypsox.tk.util.interfaceImporter;

import com.calypso.tk.core.Trade;

import static calypsox.util.TradeInterfaceUtils.TRADE_KWD_BO_REFERENCE;
import static calypsox.util.TradeInterfaceUtils.TRADE_KWD_BO_SYSTEM;

/**
 * Container to keep trades in cache. Used too to check if duplicates exist in the system.
 *
 * @author xIS16412
 */
public class SantTradeContainer {

    private long tradeId;
    private String boSystem;
    private String boReference;
    //if count > 1, duplicates exist
    private int count;
    private Trade trade;

    /**
     * Constructor
     *
     * @param newTrade
     */
    public SantTradeContainer(Trade newTrade) {
        this.trade = newTrade;
        this.count = 1;
        this.tradeId = newTrade.getLongId();
        this.boReference = newTrade.getKeywordValue(TRADE_KWD_BO_REFERENCE);
        if (newTrade.getKeywordValue(TRADE_KWD_BO_SYSTEM) != null)
            this.boSystem = newTrade.getKeywordValue(TRADE_KWD_BO_SYSTEM).trim();
        else
            this.boSystem = "";
    }

    /**
     * @return bo reference + bo system
     */
    public String buildKey() {
        final String uniqueId = this.boReference.trim() + this.boSystem.trim();
        return uniqueId;
    }

    /**
     * marks a trade as duplicated
     */
    public void tradeDuplicate() {
        this.count++;
    }

    public boolean isTradeDuplicate() {
        return count != 1;
    }

    @Override
    public int hashCode() {
        return buildKey().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        final SantTradeContainer other = (SantTradeContainer) obj;
        return (other.getTradeId() == this.tradeId && other.getBoSystem().equals(this.boSystem));
    }

    public String getBoSystem() {
        return boSystem;
    }

    public String getBoReference() {
        return boReference;
    }

    public Trade getTrade() {
        return trade;
    }

    public int getNumberTrades() {
        return count;
    }

    public long getTradeId() {
        return tradeId;
    }

}
