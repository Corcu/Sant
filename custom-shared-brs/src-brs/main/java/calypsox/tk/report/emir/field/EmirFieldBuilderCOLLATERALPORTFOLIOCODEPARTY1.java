
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderCOLLATERALPORTFOLIOCODEPARTY1
        implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {

        /*
        "Si el campo ACTIONTYPE1 tiene el valor ""N"" y el campo ""TRADEPARTY1COLLATPORTFOLIO"" tiene el calor ""Y"", entonces el campo se informará con el valor ""XXXX""
            En otro caso, el campo se informará a vacio.
         */

        String rst = EmirSnapshotReduxConstants.EMPTY_SPACE;
        final String actionType = EmirFieldBuilderUtil.getInstance()
                .getLogicActionType(trade);
        final String tradePartyCollatPortfId =  EmirFieldBuilderUtil.getInstance().getLogicTRADEPARTY1COLLATPORTFOLIO(trade);

        if (EmirSnapshotReduxConstants.ACTION_N.equalsIgnoreCase(actionType)
                && EmirSnapshotReduxConstants.YES.equalsIgnoreCase(tradePartyCollatPortfId)) {
            rst =  EmirSnapshotReduxConstants.XXXX;
        }

        return rst;
    }
}
