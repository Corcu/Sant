package calypsox.tk.refdata.sdfilter.criterionimpl;

import com.calypso.tk.bo.BOTransfer;
import com.calypso.tk.core.CalypsoServiceException;
import com.calypso.tk.core.Log;
import com.calypso.tk.core.Trade;
import com.calypso.tk.refdata.sdfilter.AbstractSDFilterCriterion;
import com.calypso.tk.refdata.sdfilter.SDFilterCategory;
import com.calypso.tk.refdata.sdfilter.SDFilterInput;
import com.calypso.tk.service.DSConnection;

public class HasRoleAllocationsFromTransferSDFilterCriterion extends AbstractSDFilterCriterion<Boolean> {

    /**
     * SDFilterCriterion attribute name
     **/
    private static final String CRITERION_NAME = "HasRoleAllocationsFromTransfer";

    public HasRoleAllocationsFromTransferSDFilterCriterion() {
        setName(CRITERION_NAME);
        setCategory(SDFilterCategory.TRANSFER);
    }

    @Override
    public SDFilterCategory getCategory() {
        return SDFilterCategory.TRANSFER;
    }

    @Override
    public Class<Boolean> getValueType() {
        return Boolean.class;
    }

    @Override
    public Boolean getValue(SDFilterInput input) {
        final BOTransfer transfer = input.getTransfer();
        if (transfer != null) {
            if (transfer.getTradeLongId() > 0) {
                try {
                    Trade trade = DSConnection.getDefault().getRemoteTrade().getTrade(transfer.getTradeLongId());
                    if (trade != null) {
                        return (trade.getRoleAllocations() != null && !trade.getRoleAllocations().isEmpty());
                    }

                } catch (CalypsoServiceException exc) {
                    Log.error(this, exc.getCause());
                }
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return CRITERION_NAME;
    }
}
