package calypsox.tk.refdata.sdfilter.criterionimpl;

import com.calypso.tk.bo.BOMessage;
import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.refdata.sdfilter.AbstractSDFilterCriterion;
import com.calypso.tk.refdata.sdfilter.SDFilterCategory;
import com.calypso.tk.refdata.sdfilter.SDFilterInput;
import com.calypso.tk.service.DSConnection;
import com.calypso.tk.service.LocalCache;

import java.util.List;

public class PaymentTypeSDFilterCriterion extends AbstractSDFilterCriterion<String> {

    @Override
    public String getName() {
        return "Payment Type";
    }

    @Override
    public Class<String> getValueType() {
        return String.class;
    }

    @Override
    public boolean hasDomainValues() {
        return true;
    }

    @Override
    public SDFilterCategory getCategory() {
        return SDFilterCategory.TRANSFER;
    }


    @Override
    public List<String> getDomainValues() {
        return LocalCache.getDomainValues(DSConnection.getDefault(), "flowType");
    }

    @Override
    public String getValue(SDFilterInput input) {
        try {
            BOTransfer xfer = input.getTransfer();
            if (xfer != null) {
                return getValue(xfer);
            }
            BOMessage msg = input.getMessage();
            if (msg != null && msg.getTransferLongId() > 0) {
                xfer = DSConnection.getDefault().getRemoteBO().getBOTransfer(msg.getTransferLongId());
                return xfer==null?null:getValue(xfer);
            }
        } catch (Exception e) {
            Log.error(this, e);
        }
        return null;
    }

    private String getValue(BOTransfer xfer) throws Exception {
        if (xfer.isPayment())
            return xfer.getTransferType();
        else {
            BOTransfer nettPayment = DSConnection.getDefault().getRemoteBO().getBOTransfer(xfer.getNettedTransferLongId());
            return nettPayment.getTransferType();
        }
    }
}
