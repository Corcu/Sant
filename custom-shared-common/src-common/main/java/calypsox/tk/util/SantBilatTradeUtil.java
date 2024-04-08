package calypsox.tk.util;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.Vector;

/**
 * Util class for bilateral trades
 *
 * @author Ruben Garcia
 */
public class SantBilatTradeUtil {

    /**
     * DV name UpdateMarketPlace, mapping TradeCounterparty-MarketPlace
     */
    public static final String UPDATE_MARKET_PLACE_DV = "UpdateMarketPlace";

    /**
     * MurexBilateralCounterparty trade keyword
     */
    public static final String MUREX_BILATERAL_COUNTERPARTY_KW = "MurexBilateralCounterparty";

    /**
     * Murex Electplatf trade keyword
     */
    public static final String Mx_Electplatf = "Mx Electplatf";

    /**
     * Murex Electplatf keyword value VOZ
     */
    public static final String VOZ = "VOZ";

    /**
     * No marketplace in mapping ID
     */
    public static final int NO_MARKET_PLACE = -1;

    /**
     * Check if trade is bilteral (LV4V, LGWM, MXElectplatf VOZ)
     *
     * @param dsCon the Data Server connection
     * @param trade the trade
     * @return true if trade is bilateral
     */
    public static boolean isBilateralETCMSTrade(DSConnection dsCon, Trade trade) {
        if (trade != null && trade.getCounterParty() != null && !Util.isEmpty(trade.getKeywordValue(Mx_Electplatf))
                && VOZ.equalsIgnoreCase(trade.getKeywordValue(Mx_Electplatf))) {
            Vector<String> values = LocalCache.getDomainValues(dsCon, UPDATE_MARKET_PLACE_DV);
            return !Util.isEmpty(values) && values.contains(trade.getCounterParty().getCode());
        }
        return false;
    }

    /**
     * Get the marketplace code using Trade Counterparty and mapping in DV UpdateMarketPlace
     *
     * @param dsCon the Data Server connection
     * @param trade the trade
     * @return the marketplace ID
     */
    public static int getMarketPlaceCode(DSConnection dsCon, Trade trade) {
        if (!Util.isEmpty(trade.getCounterParty().getCode())) {
            String marketPlace = LocalCache.getDomainValueComment(dsCon, UPDATE_MARKET_PLACE_DV,
                    trade.getCounterParty().getCode());
            if (!Util.isEmpty(marketPlace)) {
                LegalEntity marketPlaceE = BOCache.getLegalEntity(dsCon, marketPlace);
                return marketPlaceE != null ? marketPlaceE.getId() : NO_MARKET_PLACE;
            }
        }
        return NO_MARKET_PLACE;
    }
}
