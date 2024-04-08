
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderCONTRACTTYPE implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        return EmirSnapshotReduxConstants.SW;
    }
}
