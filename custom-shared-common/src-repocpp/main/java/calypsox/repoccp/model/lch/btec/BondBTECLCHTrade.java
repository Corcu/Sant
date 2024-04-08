package calypsox.repoccp.model.lch.btec;

import calypsox.repoccp.ReconCCPUtil;
import calypsox.repoccp.model.lch.LCHTrade;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

public class BondBTECLCHTrade extends BTECLCHTrade {

    @Override
    public boolean matchReference(Trade calypsoTrade) {
        if (ReconCCPUtil.matchReferenceByProductType() && !ReconCCPUtil.isBondCalypso(calypsoTrade)) {
            return false;
        }
        String mxElectplatid = calypsoTrade.getKeywordValue(LCHTrade.Mx_ELECPLATID);
        if (Util.isEmpty(mxElectplatid)) {
            mxElectplatid = calypsoTrade.getKeywordValue(LCHTrade.Mx_Electplatid);
        }
        if (Util.isEmpty(mxElectplatid)) {
            mxElectplatid = calypsoTrade.getKeywordValue(LCHTrade.Mx_GID);
        }
        if (!Util.isEmpty(mxElectplatid)) {
            try {
                return getExternalId() == Long.parseLong(mxElectplatid.replace("BTEC", ""));
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    protected long getExternalId() {
        if (!Util.isEmpty(this.getBuyerSellerReference())) {
            try {
                return Long.parseLong(this.getBuyerSellerReference().replace("C", ""));
            } catch (NumberFormatException e) {
                Log.error(this, e.getCause());
            }
        }
        return 0L;
    }
}
