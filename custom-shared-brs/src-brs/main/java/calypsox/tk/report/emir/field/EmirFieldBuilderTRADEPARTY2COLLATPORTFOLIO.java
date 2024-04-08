
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderTRADEPARTY2COLLATPORTFOLIO
        implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        if (EmirSnapshotReduxConstants.ACTION_N
                .equalsIgnoreCase(EmirFieldBuilderUtil.getInstance()
                        .getLogicActionType(trade))
                && !(EmirFieldBuilderUtil.getInstance()
                        .getLogicCollateralized(trade)
                        .equalsIgnoreCase(EmirSnapshotReduxConstants.UNCOLLATERALIZED))) {
            rst = EmirSnapshotReduxConstants.YES;
        }

        return rst;
    }
}
