package calypsox.repoccp.model.lch.etcms;

import calypsox.repoccp.model.lch.LCHTrade;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;

import java.util.Optional;

public class ETCMSLCHTrade extends LCHTrade {

    public boolean matchReference(Trade calypsoTrade){
        String mxTradeId = calypsoTrade.getKeywordValue("Mx Electplatid");
        try {
            return getExternalId() == Long.parseLong(Optional.ofNullable(mxTradeId).orElse("-1"));
        } catch (NumberFormatException ignored){}

        return false;
    }

    @Override
    protected long getExternalId() {
        try {
            return Long.parseLong(this.getBuyerSellerReference().substring(3));
        } catch (NumberFormatException e){
            Log.error(this, e.getCause());
        }
        return 0L;
    }
}
