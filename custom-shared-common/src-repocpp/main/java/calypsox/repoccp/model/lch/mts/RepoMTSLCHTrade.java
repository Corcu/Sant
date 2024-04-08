package calypsox.repoccp.model.lch.mts;

import calypsox.repoccp.ReconCCPUtil;
import calypsox.repoccp.model.lch.LCHTrade;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

import java.util.Optional;

public class RepoMTSLCHTrade extends MTSLCHTrade {

    @Override
    public boolean matchReference(Trade calypsoTrade) {
        if(ReconCCPUtil.matchReferenceByProductType() && !ReconCCPUtil.isRepoCalypso(calypsoTrade)){
            return false;
        }
        String mxElectpatid = calypsoTrade.getKeywordValue(LCHTrade.Mx_Electplatid);
        if (Util.isEmpty(mxElectpatid)) {
            mxElectpatid = calypsoTrade.getKeywordValue(LCHTrade.Mx_ELECPLATID);
        }
        if (Util.isEmpty(mxElectpatid)) {
            mxElectpatid = calypsoTrade.getKeywordValue(LCHTrade.Mx_GID);
        }
        if (!Util.isEmpty(mxElectpatid) && mxElectpatid.length() >= 11) {
            try {
                return getExternalId() == Long.parseLong(Optional.ofNullable(mxElectpatid.substring(5, 11)).orElse("-1"));
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

}
