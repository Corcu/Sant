package calypsox.tk.bo.cremapping.util;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Util;

import static calypsox.tk.bo.cremapping.util.BOCreUtils.getInstance;

public class BondBOCreSettleMethodHandler {

    public String getCreSettlementMethod(BOTransfer xfer){
        String settleMethod = getInstance().getSettleMethodFromSdi(xfer);
        if (Util.isEmpty(settleMethod)) {
            settleMethod = "SWIFT";
            if ("TARGET2".equalsIgnoreCase(xfer.getSettlementMethod())||"Direct".equalsIgnoreCase(xfer.getSettlementMethod())){
                    settleMethod=xfer.getSettlementMethod();
            }
        }
        return settleMethod;
    }
}
