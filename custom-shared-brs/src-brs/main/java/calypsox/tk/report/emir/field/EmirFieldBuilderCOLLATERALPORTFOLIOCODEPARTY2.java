
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderCOLLATERALPORTFOLIOCODEPARTY2
        implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        final String actionType = EmirFieldBuilderUtil.getInstance()
                .getLogicActionType(trade);
        final String collateralPortfolio = getLogicTradeParty2CollatPortfolio(
                trade);

        if (EmirSnapshotReduxConstants.ACTION_N.equalsIgnoreCase(actionType)
                && EmirSnapshotReduxConstants.YES.equalsIgnoreCase(collateralPortfolio)) {
            rst = EmirSnapshotReduxConstants.XXXX;
        }

        return rst;
    }

    private String getLogicTradeParty2CollatPortfolio(Trade trade) {
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
