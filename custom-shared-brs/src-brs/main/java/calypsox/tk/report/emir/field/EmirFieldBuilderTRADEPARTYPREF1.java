
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderTRADEPARTYPREF1 implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        return EmirSnapshotReduxConstants.LEI;
    }
}
