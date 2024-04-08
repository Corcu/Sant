
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

public class EmirFieldBuilderEXECUTIONAGENTPARTY2PREFIX
        implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String value = EmirSnapshotReduxConstants.EMPTY_SPACE;

        String executionAgentPartyValue2 = EmirFieldBuilderUtil.getInstance()
                .getLogicExecutionAgentPartyValue2(trade);
        if (!Util.isEmpty(executionAgentPartyValue2)) {
            value = EmirSnapshotReduxConstants.LEI;
        }

        return value;
    }
}
