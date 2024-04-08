
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import calypsox.tk.util.LegalEntityAttributesCache;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

public class EmirFieldBuilderREPORTINGDELEGATIONMODEL
        implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {

        return EmirSnapshotReduxConstants.INDEPENDENT;
    }
}
