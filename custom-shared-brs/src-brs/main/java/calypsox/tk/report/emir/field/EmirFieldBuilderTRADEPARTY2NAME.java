
package calypsox.tk.report.emir.field;

import calypsox.tk.core.SantanderUtil;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.emir.LegalEntitiesCache;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderTRADEPARTY2NAME implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        LegalEntity le = trade.getCounterParty();

        if ((le != null) && !SantanderUtil.PO_T99A.equals(le.getCode())) {
            return le.getName();
        } else if ((le != null) && SantanderUtil.PO_T99A.equals(le.getCode())) {
            le = LegalEntitiesCache.getInstance()
                    .getLegalEntity(SantanderUtil.PO_BSTE);
            return le.getName();
        }

        return EmirSnapshotReduxConstants.EMPTY_SPACE;
    }
}
