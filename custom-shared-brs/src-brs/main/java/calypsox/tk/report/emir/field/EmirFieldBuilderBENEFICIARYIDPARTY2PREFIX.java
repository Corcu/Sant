
package calypsox.tk.report.emir.field;

import com.calypso.tk.core.Trade;

public class EmirFieldBuilderBENEFICIARYIDPARTY2PREFIX
        implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String rst = EmirFieldBuilderUtil.getInstance().getLogicBENEFICIARYIDPARTY2PREFIX(trade);
        return rst;
    }


}
