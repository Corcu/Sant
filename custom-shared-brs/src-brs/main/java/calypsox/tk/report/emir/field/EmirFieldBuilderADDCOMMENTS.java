package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderADDCOMMENTS implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        final LegalEntity cp = trade.getCounterParty();
        if (cp != null) {
            return cp.getCode();
        }

        return EmirSnapshotReduxConstants.EMPTY_SPACE;
    }
}
