
package calypsox.tk.report.emir.field;

import com.calypso.tk.core.Trade;

public class EmirFieldBuilderEXECUTIONAGENTPARTYVALUE2
        implements EmirFieldBuilder {

    @Override
    public String getValue(Trade trade) {
        return EmirFieldBuilderUtil.getInstance()
                .getLogicExecutionAgentPartyValue2(trade);
    }
}
