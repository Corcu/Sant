package calypsox.tk.refdata.sdfilter.criterionimpl;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.sdfilter.AbstractSDFilterCriterion;
import com.calypso.tk.refdata.sdfilter.SDFilterCategory;
import com.calypso.tk.refdata.sdfilter.SDFilterInput;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.List;

public class IsTradeIdNettingSDFilterCriterion extends AbstractSDFilterCriterion<Boolean> {

    @Override
    public String getName() {
        return "Is Trade Id Netting";
    }

    @Override
    public Class<Boolean> getValueType() {
        return Boolean.class;
    }

    @Override
    public boolean hasDomainValues() {
        return false;
    }

    @Override
    public SDFilterCategory getCategory() {
        return SDFilterCategory.TRANSFER;
    }


    @Override
    public Boolean getValue(SDFilterInput input) {
        try {
            BOTransfer xfer = input.getTransfer();
            if (xfer != null) {
                return getValue(xfer);
            }
            BOMessage msg = input.getMessage();
            if (msg != null && msg.getTransferLongId() > 0) {
                xfer = DSConnection.getDefault().getRemoteBO().getBOTransfer(msg.getTransferLongId());
                return xfer == null ? null : getValue(xfer);
            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        return null;
    }

    private Boolean getValue(BOTransfer xfer) {
        return "None".equalsIgnoreCase(xfer.getNettingType()) || BOCache.getNettingConfig(DSConnection.getDefault(), xfer.getNettingType()).containsKey("TradeId");
    }
}
