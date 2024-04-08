
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.LegalEntity;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderTRADEPARTY1NAME implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        //Se fija el valor del campo Full name de la PO.

        LegalEntity le = trade.getBook().getLegalEntity();

        if (le != null) {
            return le.getName();
        }
        return EmirSnapshotReduxConstants.EMPTY_SPACE;
    }
}
