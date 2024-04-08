package calypsox.tk.product;

import com.calypso.tk.bo.BOCache;
import com.calypso.tk.bo.TradeRoleAllocation;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.ProductException;
import com.calypso.tk.core.Trade;
import com.calypso.tk.product.NominalAllocator;
import com.calypso.tk.product.allocation.Allocatable;
import com.calypso.tk.refdata.LegalEntityAttribute;
import com.calypso.tk.service.DSConnection;

import java.util.List;
import java.util.Optional;

public class BondAllocator extends NominalAllocator {

    CTMAllocatorHandler ctmAllocatorHandler=new CTMAllocatorHandler();

    @Override
    public boolean performAllocation(Trade blockTrade, Trade allocatedTrade, TradeRoleAllocation tr, Object dbCon, Allocatable rolledUpAllocatable) throws ProductException {
        final boolean allocationResult = super.performAllocation(blockTrade, allocatedTrade, tr, dbCon, rolledUpAllocatable);
        if (allocationResult) {
            ctmAllocatorHandler.performCTMAllocation(blockTrade, allocatedTrade);
        }
        return allocationResult;
    }

    @Override
    public boolean isLegalEntityValid(LegalEntity le, Trade trade, List messages) {
        if (le == null) {
            messages.add("Selected Legal Entity is empty");
            return false;
        } else if (this.checkParentChildAllocation(trade.getProduct())) {
            //Accept parents of child trades as allocation OR accept child LE's from parent trade cpty OR accept the cpty itself as allocation
            if (trade.getCounterParty().getParentId() == le.getEntityId() || /*not sure about this condition*/ trade.getCounterParty().getEntityId() == le.getParentId() || trade.getCounterParty().getEntityId() == le.getEntityId()) {
                return true;
            } else {
                messages.add("For product type " + trade.getProduct().getType() + ", Legal Entity must be a child of the original counterparty, or the counterparty itself.");
                return false;
            }
        } else {
            LegalEntity blockCounterparty = trade.getCounterParty();
            int blockCptyId = blockCounterparty.getEntityId();
            LegalEntityAttribute allocateToChildrenOnly = BOCache.getLegalEntityAttribute(DSConnection.getDefault(), 0, blockCptyId, "ALL", "ALLOCATE_TO_CHILDREN_ONLY");
            if (allocateToChildrenOnly != null && "true".equalsIgnoreCase(allocateToChildrenOnly.getAttributeValue())) {
                //accept child LE's from parent trade cpty OR accept the cpty itself as allocation
                boolean result = (le.getParentId() == blockCptyId) || (le.getEntityId() == blockCptyId);
                if (!result) {
                    messages.add("The selected legal entity must be a child of the original counterparty, or the counterparty itself.");
                }
                return result;
            } else {
                return true;
            }
        }
    }
}
