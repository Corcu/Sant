
package calypsox.tk.report.emir.field;

import com.calypso.tk.core.Trade;

public class EmirFieldBuilderUNDERLYINGIDENTIFICATIONTYPE
        implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        return EmirFieldBuilderUtil.getInstance().getUnderlyingIdentificationType(trade);

    }
}
