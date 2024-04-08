package calypsox.repoccp.model.lch.mts;

import calypsox.repoccp.model.lch.LCHTrade;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;

public abstract class MTSLCHTrade extends LCHTrade {

    public abstract boolean matchReference(Trade calypsoTrade);

    @Override
    protected long getExternalId() {
        try {
            return Long.parseLong(this.getBuyerSellerReference().substring(7, 13));
        } catch (NumberFormatException e){
            Log.error(this, e.getCause());
        }
        return 0L;
    }

}
