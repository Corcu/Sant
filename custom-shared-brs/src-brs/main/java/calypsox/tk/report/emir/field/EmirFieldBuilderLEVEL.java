
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.ActionTypeValue;
import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderLEVEL implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String result = EmirSnapshotReduxConstants.LITERAL_T;

        /*
        final String actionType = EmirFieldBuilderUtil.getInstance()
                .getLogicActionType(trade);
        if (ActionTypeValue.N.name().equalsIgnoreCase(actionType)
                || ActionTypeValue.R.name().equalsIgnoreCase(actionType)
                || ActionTypeValue.M.name().equalsIgnoreCase(actionType)) {
            result = EmirSnapshotReduxConstants.LITERAL_T;
        }
        */
        return result;
    }
}
