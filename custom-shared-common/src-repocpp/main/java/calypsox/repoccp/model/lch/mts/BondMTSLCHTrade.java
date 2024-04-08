package calypsox.repoccp.model.lch.mts;

import calypsox.repoccp.ReconCCPUtil;
import calypsox.repoccp.model.lch.LCHTrade;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

import java.util.Optional;

public class BondMTSLCHTrade extends MTSLCHTrade {

    @Override
    public boolean matchReference(Trade calypsoTrade) {
        if(ReconCCPUtil.matchReferenceByProductType() && !ReconCCPUtil.isBondCalypso(calypsoTrade)){
            return false;
        }
        String mxElectplatid = calypsoTrade.getKeywordValue(LCHTrade.Mx_ELECPLATID);
        if (Util.isEmpty(mxElectplatid)) {
            mxElectplatid = calypsoTrade.getKeywordValue(LCHTrade.Mx_Electplatid);
        }
        if (Util.isEmpty(mxElectplatid)) {
            mxElectplatid = calypsoTrade.getKeywordValue(LCHTrade.Mx_GID);
        }

        if (!Util.isEmpty(mxElectplatid) && mxElectplatid.length() >= 6) {
            try {
                return getExternalId() == Long.parseLong(Optional.ofNullable(mxElectplatid.substring(0, 6)).orElse("-1"));
            } catch (NumberFormatException ignored) {
            }
        }

        return false;

    }

}
