package calypsox.tk.bo;

import com.calypso.tk.core.JDate;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;
import com.calypso.tk.marketdata.PricingEnv;
import com.calypso.tk.refdata.DomainValues;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.List;
import java.util.Vector;

public class BOPledgeHandler extends com.calypso.tk.bo.BOPledgeHandler {

    @Override
    public Vector generateTransfers(Trade trade, PricingEnv env, Vector exceptions, DSConnection dsCon) {
        final Vector vector = super.generateTransfers(trade, env, exceptions, dsCon);
        if(!isValidPledgeStatus(trade)){
            return new Vector();
        }
        return vector;
    }

    @Override
    public Vector generateTransfers(Trade trade, PricingEnv env, Vector exceptions, boolean usePastGeneration, JDate asOfDate, DSConnection dsCon) {
        final Vector vector = super.generateTransfers(trade, env, exceptions, usePastGeneration, asOfDate, dsCon);
        if(!isValidPledgeStatus(trade)){
            return new Vector();
        }
        return vector;
    }

    private boolean isValidPledgeStatus(Trade trade){
        if(isFilterActivated() && null!=trade){
            final String status = trade.getStatus().getStatus();
            final List<String> validPledgeXferStatus = getValidPledgeXferStatus();
            return !Util.isEmpty(validPledgeXferStatus) && validPledgeXferStatus.contains(status);
        }
        return true;
    }

    public boolean isFilterActivated() {
        String activationFlag = LocalCache.getDomainValueComment(DSConnection.getDefault(),"CodeActivationDV","PledgeStatusXferFilter");
        return !Util.isEmpty(activationFlag) && Boolean.parseBoolean(activationFlag);
    }

    private List<String> getValidPledgeXferStatus(){
        return DomainValues.values("PledgeValidXferStatus");
    }

}
