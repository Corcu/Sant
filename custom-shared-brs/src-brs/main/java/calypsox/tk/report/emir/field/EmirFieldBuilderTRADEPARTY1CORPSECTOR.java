
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderTRADEPARTY1CORPSECTOR implements EmirFieldBuilder {

    @Override
    public String getValue(Trade trade) {
        return EmirSnapshotReduxConstants.CREDIT_INSTITUTION;
    }
}
