
package calypsox.tk.report.emir.field;

import com.calypso.tk.core.Trade;

public class EmirFieldBuilderINIPAYMENTAMOUNT implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        String rst = EmirFieldBuilderUtil.getInstance().getLogicINIPAYMENTAMOUNT(trade);
        return rst;
    }
}

