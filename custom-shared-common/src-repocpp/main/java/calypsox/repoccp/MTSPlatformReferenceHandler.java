package calypsox.repoccp;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Product;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.Bond;
import com.calypso.tk.product.Repo;
import com.calypso.tk.service.DSConnection;

import java.util.Optional;

import static calypsox.repoccp.ReconCCPConstants.TRADE_KWD_BUYER_SELLER_REF;

/**
 * @author aalonsop
 */
public class MTSPlatformReferenceHandler {


    public boolean isNotMTSPlatformAndItalianBond(BOTransfer xfer) {
        return !isMTSPlatformAndItalianBond(xfer);
    }


    public boolean isMTSPlatformAndItalianBond(BOTransfer xfer) {
        boolean res=false;
        long tradeId = Optional.ofNullable(xfer).map(BOTransfer::getTradeLongId).orElse(0L);
        if (tradeId > 0L) {
            try {
                Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(tradeId);
                if(isMTSPlatformTrade(trade)) {
                    res = Optional.of(trade).map(Trade::getProduct).map(p -> BOCache.getExchangedTradedProduct(DSConnection.getDefault(), p.getId()))
                            .map(b -> ((Product) b).getSecCode("ISIN").startsWith("IT")).orElse(false);
                }
            } catch (CalypsoServiceException exc) {
                Log.error(this, exc.getCause());
            }
        }
        return res;
    }


    public boolean isMTSPlatformTrade(Trade trade){
        return Optional.ofNullable(trade).map(t->t.getKeywordValue("Mx Electplatf"))
                .map(v->v.equals("MTS")).orElse(false);
    }

    public String getMTSSettleReference(Trade trade){
        String mtsRef="";
        Product product=Optional.ofNullable(trade).map(Trade::getProduct).orElse(null);
        if(product instanceof Bond){
            mtsRef=getBondMTSSettleReference(trade);
        }else if(product instanceof Repo){
            mtsRef=getRepoMTSSettleReference(trade);
        }
        return mtsRef;
    }

    public String getBondMTSSettleReference(Trade trade){
        return getAndCropMTSReference(trade,7,12);
    }

    public String getRepoMTSSettleReference(Trade trade){
       return getAndCropMTSReference(trade,7,13);
    }

    private String getAndCropMTSReference(Trade trade, int beginIndex, int endIndex){
        return Optional.ofNullable(trade).map(t->t.getKeywordValue(TRADE_KWD_BUYER_SELLER_REF))
                .filter(ref->ref.length()>endIndex).map(ref->ref.substring(beginIndex,endIndex))
                .orElse("");
    }
}
