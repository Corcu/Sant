package calypsox.tk.event;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.collateral.service.CollateralServiceException;
import com.calypso.tk.collateral.service.ServiceRegistry;
import com.calypso.tk.core.Log;
import com.calypso.tk.event.EventFilter;
import com.calypso.tk.event.PSEvent;
import com.calypso.tk.event.PSEventTransfer;
import com.calypso.tk.refdata.CollateralConfig;

import java.util.Optional;

/**
 * @author aalonsop
 */
public class FilterByMarginCallAFEventFilter  implements EventFilter {

    @Override
    public boolean accept(PSEvent event) {
        boolean res = true;
        if (event instanceof PSEventTransfer) {
            res=isAdditionalFieldMatchingValue(getMarginCallConfig((PSEventTransfer) event),"CouponType","Interest");
        }
        return res;
    }

    private boolean isAdditionalFieldMatchingValue(CollateralConfig cc,String afName, String afTargetValue){
        return Optional.ofNullable(cc).map(contract->contract.getAdditionalField(afName))
                .map(value->value.equals(afTargetValue)).orElse(false);
    }

    private CollateralConfig getMarginCallConfig(PSEventTransfer eventTransfer){
        int mccId=getMarginCallConfigId(eventTransfer);
        CollateralConfig cc=null;
        if(mccId>0) {
            try {
                cc= ServiceRegistry.getDefault().getCollateralDataServer().getMarginCallConfig(mccId);
            } catch (CollateralServiceException exc) {
                Log.error(this,exc.getCause());
            }
        }
        return cc;
    }

    private int getMarginCallConfigId(PSEventTransfer evTransfer) {
        int mccId = 0;
        BOTransfer transfer=evTransfer.getBoTransfer();
        try {
            mccId = Integer.parseInt(transfer.getAttribute("MarginCall"));
        } catch (Exception exc) {
            Log.error("MarginCallEngine", exc);
        }
        return mccId;
    }
}
