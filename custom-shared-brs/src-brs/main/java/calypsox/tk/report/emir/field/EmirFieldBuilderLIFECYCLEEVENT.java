
package calypsox.tk.report.emir.field;

import calypsox.tk.util.emir.EmirSnapshotReduxConstants;
import com.calypso.tk.core.Trade;

public class EmirFieldBuilderLIFECYCLEEVENT implements EmirFieldBuilder {
    @Override
    public String getValue(Trade trade) {
        return trade.getKeywordValue(EmirSnapshotReduxConstants.TRADE_KEYWORD_EMIR_LIFECYCLE_EVENT);
    }
}
