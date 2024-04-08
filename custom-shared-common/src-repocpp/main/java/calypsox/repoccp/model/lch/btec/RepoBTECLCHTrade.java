package calypsox.repoccp.model.lch.btec;

import calypsox.repoccp.ReconCCPUtil;
import calypsox.repoccp.model.lch.LCHTrade;
import calypsox.repoccp.model.lch.mts.MTSLCHTrade;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

import java.util.Optional;

public class RepoBTECLCHTrade extends BTECLCHTrade {

    @Override
    public boolean matchReference(Trade calypsoTrade) {
        if (ReconCCPUtil.matchReferenceByProductType() && !ReconCCPUtil.isRepoCalypso(calypsoTrade)) {
            return false;
        }
        String mxElectpatid = calypsoTrade.getKeywordValue(LCHTrade.Mx_Electplatid);
        if (Util.isEmpty(mxElectpatid)) {
            mxElectpatid = calypsoTrade.getKeywordValue(LCHTrade.Mx_ELECPLATID);
        }
        if (Util.isEmpty(mxElectpatid)) {
            mxElectpatid = calypsoTrade.getKeywordValue(LCHTrade.Mx_GID);
        }
        if (!Util.isEmpty(mxElectpatid)) {
            String[] splitUnderscoreMxElectpaid = mxElectpatid.split("_");
            String[] splitDashMxElectpaid = mxElectpatid.split("-");
            if (splitUnderscoreMxElectpaid.length >= 2) {
                String parseElectpaid = splitUnderscoreMxElectpaid[1];
                if (splitDashMxElectpaid.length == 2) {
                    parseElectpaid += splitDashMxElectpaid[1];
                }
                try {
                    return getExternalId() == Long.parseLong(parseElectpaid);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return false;
    }

    @Override
    protected long getExternalId() {
        return Long.parseLong(this.getBuyerSellerReference().substring(3));
    }
}
