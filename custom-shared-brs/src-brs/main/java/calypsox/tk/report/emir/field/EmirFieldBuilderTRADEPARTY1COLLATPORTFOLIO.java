
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;
import com.calypso.tk.core.Util;

public class EmirFieldBuilderTRADEPARTY1COLLATPORTFOLIO
        implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        /*
        Si el campo COLLATERALIZED esta informado con el valor "UNCOLLATERALIZED" o vacio, entonces este campo se envia a vacio.
        En otro caso, se envia el valor "Y"
         */
        return EmirFieldBuilderUtil.getInstance().getLogicTRADEPARTY1COLLATPORTFOLIO(trade);
    }


}
