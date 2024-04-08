
package calypsox.tk.report.emir.field;

import com.calypso.tk.core.Trade;

public class EmirFieldBuilderTRADEPARTYPREF2 implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        return EmirFieldBuilderUtil.getInstance().logicTRADEPARTYPREF2(trade);
    }
}
